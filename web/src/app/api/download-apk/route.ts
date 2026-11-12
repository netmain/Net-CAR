import { NextRequest, NextResponse } from 'next/server';
import fs from 'fs';
import path from 'path';

export const dynamic = 'force-dynamic';

/** Latest published APK on GitHub - used when the binary is not on disk. */
const RELEASE_ASSET_URL =
  'https://github.com/netmain/Net-CAR/releases/latest/download/Net-CAR.apk';

type RemoteApk = { url: string; size: number };

let remoteApkCache: RemoteApk | null = null;
let remoteApkCheckedAt = 0;

function resolveApkPath(): string | null {
  const candidates = [
    path.join(process.cwd(), 'public', 'Net-CAR.apk'),
    path.join(process.cwd(), '..', 'Net-CAR.apk'),
    path.join(process.cwd(), '..', 'web', 'public', 'Net-CAR.apk'),
    path.join(process.cwd(), '..', 'android', 'Net-CAR-debug.apk'),
    '/app/applet/Net-CAR.apk',
    '/app/applet/web/public/Net-CAR.apk',
    '/app/applet/android/Net-CAR-debug.apk',
  ];

  for (const candidate of candidates) {
    try {
      if (fs.existsSync(candidate)) {
        const stat = fs.statSync(candidate);
        if (stat.isFile() && stat.size > 500_000) {
          return candidate;
        }
      }
    } catch {
      // continue checking next candidate
    }
  }
  return null;
}

/**
 * Resolves the latest release asset so the web app can serve the APK even
 * when the binary is not bundled with the deployment.
 */
async function resolveRemoteApk(): Promise<RemoteApk | null> {
  const now = Date.now();
  if (remoteApkCache && now - remoteApkCheckedAt < 5 * 60 * 1000) {
    return remoteApkCache;
  }
  try {
    const res = await fetch(RELEASE_ASSET_URL, {
      method: 'HEAD',
      redirect: 'follow',
      cache: 'no-store',
    });
    if (!res.ok) return null;
    const size = Number(res.headers.get('content-length') || 0);
    if (!size || size < 500_000) return null;
    remoteApkCache = { url: RELEASE_ASSET_URL, size };
    remoteApkCheckedAt = now;
    return remoteApkCache;
  } catch {
    return null;
  }
}

export async function POST(req: NextRequest) {
  try {
    const apkPath = resolveApkPath();
    const body = await req.json().catch(() => ({}));

    if (body.action === 'info') {
      if (apkPath) {
        const stat = fs.statSync(apkPath);
        return NextResponse.json({
          filename: 'Net-CAR.apk',
          size: stat.size,
          version: '2.0.0',
        });
      }
      const remote = await resolveRemoteApk();
      if (!remote) {
        return NextResponse.json(
          { error: 'Net-CAR.apk binary not found on server.' },
          { status: 404 }
        );
      }
      return NextResponse.json({
        filename: 'Net-CAR.apk',
        size: remote.size,
        version: '2.0.0',
      });
    }

    const start = Math.max(0, Number(body.start) || 0);

    if (apkPath) {
      const stat = fs.statSync(apkPath);
      const totalSize = stat.size;
      const end = Math.min(totalSize, Number(body.end) || totalSize);
      const length = Math.max(0, end - start);

      const buffer = Buffer.alloc(length);
      const fd = fs.openSync(apkPath, 'r');
      try {
        fs.readSync(fd, buffer, 0, length, start);
      } finally {
        fs.closeSync(fd);
      }

      return NextResponse.json({
        start,
        end,
        totalSize,
        chunkBase64: buffer.toString('base64'),
      });
    }

    const remote = await resolveRemoteApk();
    if (!remote) {
      return NextResponse.json(
        { error: 'Net-CAR.apk binary not found on server.' },
        { status: 404 }
      );
    }

    const end = Math.min(remote.size - 1, Number(body.end) || remote.size - 1);
    const chunk = await fetch(remote.url, {
      headers: { Range: `bytes=${start}-${end}` },
      cache: 'no-store',
    });
    if (!chunk.ok && chunk.status !== 206) {
      return NextResponse.json(
        { error: `Could not download the release asset (HTTP ${chunk.status}).` },
        { status: 502 }
      );
    }
    const arrayBuffer = await chunk.arrayBuffer();

    return NextResponse.json({
      start,
      end: Math.min(end, remote.size - 1),
      totalSize: remote.size,
      chunkBase64: Buffer.from(arrayBuffer).toString('base64'),
    });
  } catch (err) {
    return NextResponse.json(
      { error: err instanceof Error ? err.message : 'APK read error' },
      { status: 500 }
    );
  }
}

export async function GET(req: NextRequest) {
  const apkPath = resolveApkPath();

  if (apkPath) {
    const stat = fs.statSync(apkPath);
    const fileBuffer = fs.readFileSync(apkPath);
    return new NextResponse(fileBuffer, {
      status: 200,
      headers: {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': 'attachment; filename="Net-CAR.apk"',
        'Content-Length': String(stat.size),
        'Cache-Control': 'no-store',
      },
    });
  }

  const remote = await resolveRemoteApk();
  if (!remote) {
    return NextResponse.json(
      { error: 'Net-CAR.apk binary not found on server.' },
      { status: 404 }
    );
  }

  const { searchParams } = new URL(req.url);
  if (searchParams.get('info') === '1') {
    return NextResponse.json({
      filename: 'Net-CAR.apk',
      size: remote.size,
      version: '2.0.0',
    });
  }

  const range = req.headers.get('range');
  const res = await fetch(remote.url, {
    headers: range ? { Range: range } : undefined,
    cache: 'no-store',
  });

  return new NextResponse(res.body, {
    status: res.ok || res.status === 206 ? 200 : res.status,
    headers: {
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Disposition': 'attachment; filename="Net-CAR.apk"',
      'Content-Length': res.headers.get('content-length') || String(remote.size),
      'Cache-Control': 'no-store',
    },
  });
}
