const CHUNK_SIZE = 2 * 1024 * 1024; // 2 MB chunks

export async function prepareVerifiedApkBlob(
  onProgress?: (percent: number, downloadedMB: string, totalMB: string) => void
): Promise<{ success: boolean; blobUrl?: string; sizeMB?: string; error?: string }> {
  try {
    const infoRes = await fetch('/api/download-apk?info=1', {
      method: 'GET',
      cache: 'no-store',
    });

    if (!infoRes.ok) {
      return {
        success: false,
        error: `APK server endpoint returned HTTP ${infoRes.status}`,
      };
    }

    const info = await infoRes.json();
    const totalSize: number = Number(info?.size || 0);
    if (!totalSize || totalSize < 1_000_000) {
      return {
        success: false,
        error: 'Invalid APK size reported by server.',
      };
    }

    const totalMB = (totalSize / (1024 * 1024)).toFixed(1);
    const chunks: Uint8Array[] = [];
    let received = 0;

    while (received < totalSize) {
      const start = received;
      const end = Math.min(totalSize, start + CHUNK_SIZE);

      const chunkRes = await fetch(`/api/download-apk?start=${start}&end=${end}`, {
        method: 'GET',
        cache: 'no-store',
      });

      if (!chunkRes.ok) {
        return {
          success: false,
          error: `Failed downloading APK chunk (${start}-${end}): HTTP ${chunkRes.status}`,
        };
      }

      const arrayBuffer = await chunkRes.arrayBuffer();
      const bytes = new Uint8Array(arrayBuffer);

      // Verify ZIP/APK magic header (PK\x03\x04) on the first chunk
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
            error: 'Downloaded data is not a valid APK binary archive.',
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
