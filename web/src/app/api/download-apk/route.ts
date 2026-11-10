import { NextRequest, NextResponse } from 'next/server';
import fs from 'fs';
import path from 'path';

export const dynamic = 'force-dynamic';

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

export async function POST(req: NextRequest) {
  try {
    const apkPath = resolveApkPath();
    if (!apkPath) {
      return NextResponse.json(
        { error: 'Net-CAR.apk binary not found on server.' },
        { status: 404 }
      );
    }

    const stat = fs.statSync(apkPath);
    const totalSize = stat.size;
    const body = await req.json().catch(() => ({}));

    if (body.action === 'info') {
      return NextResponse.json({
        filename: 'Net-CAR.apk',
        size: totalSize,
        version: '2.0.0',
      });
    }

    const start = Math.max(0, Number(body.start) || 0);
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
  } catch (err) {
    return NextResponse.json(
      { error: err instanceof Error ? err.message : 'APK read error' },
      { status: 500 }
    );
  }
}

export async function GET(req: NextRequest) {
  const apkPath = resolveApkPath();
  if (!apkPath) {
    return NextResponse.json(
      { error: 'Net-CAR.apk binary not found on server.' },
      { status: 404 }
    );
  }

  const stat = fs.statSync(apkPath);
  const totalSize = stat.size;
  const { searchParams } = new URL(req.url);

  if (searchParams.get('info') === '1') {
    return NextResponse.json({
      filename: 'Net-CAR.apk',
      size: totalSize,
      version: '2.0.0',
    });
  }

  const fileBuffer = fs.readFileSync(apkPath);
  return new NextResponse(fileBuffer, {
    status: 200,
    headers: {
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Disposition': 'attachment; filename="Net-CAR.apk"',
      'Content-Length': String(totalSize),
      'Cache-Control': 'no-store',
    },
  });
}
