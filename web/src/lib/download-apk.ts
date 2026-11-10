const CHUNK_SIZE = 1536 * 1024; // 1.5 MB binary chunks (~2 MB base64 JSON)

function base64ToUint8Array(base64: string): Uint8Array {
  const binaryString = window.atob(base64);
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
}

export async function downloadAndPrepareApkBlob(
  onProgress?: (percent: number, downloadedMB: string, totalMB: string) => void
): Promise<{ success: boolean; blobUrl?: string; sizeMB?: string; error?: string }> {
  try {
    const infoRes = await fetch('/api/download-apk', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'info' }),
      cache: 'no-store',
    });

    if (!infoRes.ok) {
      return {
        success: false,
        error: `Le serveur APK a répondu HTTP ${infoRes.status}`,
      };
    }

    const info = await infoRes.json();
    const totalSize: number = Number(info?.size || 0);
    if (!totalSize || totalSize < 500_000) {
      return {
        success: false,
        error: info?.error || 'Taille APK invalide sur le serveur.',
      };
    }

    const totalMB = (totalSize / (1024 * 1024)).toFixed(1);
    const chunks: Uint8Array[] = [];
    let received = 0;

    while (received < totalSize) {
      const start = received;
      const end = Math.min(totalSize, start + CHUNK_SIZE);

      const chunkRes = await fetch('/api/download-apk', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'chunk', start, end }),
        cache: 'no-store',
      });

      if (!chunkRes.ok) {
        return {
          success: false,
          error: `Échec du téléchargement du segment (${start}-${end}) : HTTP ${chunkRes.status}`,
        };
      }

      const payload = await chunkRes.json();
      if (!payload?.chunkBase64) {
        return {
          success: false,
          error: payload?.error || 'Réponse JSON APK invalide.',
        };
      }

      const bytes = base64ToUint8Array(payload.chunkBase64);

      // Verify ZIP/APK magic header (PK\x03\x04) on the very first chunk
      if (start === 0) {
        if (
          bytes.length < 4 ||
          bytes[0] !== 0x50 ||
          bytes[1] !== 0x4b ||
          bytes[2] !== 0x03 ||
          bytes[3] !== 0x04
        ) {
          return {
            success: false,
            error: "Les données reçues ne correspondent pas à une archive APK valide.",
          };
        }
      }

      chunks.push(bytes);
      received += bytes.byteLength;

      if (onProgress) {
        const pct = Math.min(100, Math.round((received / totalSize) * 100));
        const dlMB = (received / (1024 * 1024)).toFixed(1);
        onProgress(pct, dlMB, totalMB);
      }
    }

    const blob = new Blob(chunks as BlobPart[], {
      type: 'application/vnd.android.package-archive',
    });

    const blobUrl = URL.createObjectURL(blob);

    // Attempt immediate automatic save if transient user activation is still active
    try {
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = 'Net-CAR.apk';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch {
      // If browser blocked automatic click, the caller also renders a direct <a href={blobUrl}> button
    }

    return {
      success: true,
      blobUrl,
      sizeMB: totalMB,
    };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : String(err),
    };
  }
}
