const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

process.env.NODE_ENV = process.env.NODE_ENV || 'production';
process.env.PORT = process.env.PORT || '3000';
process.env.HOSTNAME = process.env.HOSTNAME || '0.0.0.0';

const rootDir = __dirname;
const standaloneWebDir = path.join(rootDir, 'web', '.next', 'standalone', 'web');
const standaloneWebServer = path.join(standaloneWebDir, 'server.js');
const standaloneRootDir = path.join(rootDir, 'web', '.next', 'standalone');
const standaloneRootServer = path.join(standaloneRootDir, 'server.js');

function copyDirIfMissing(src, dest) {
  try {
    if (fs.existsSync(src) && !fs.existsSync(dest)) {
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      fs.cpSync(src, dest, { recursive: true });
    }
  } catch (err) {
    console.warn(`[server.js] Warning copying ${src} -> ${dest}:`, err.message);
  }
}

if (fs.existsSync(standaloneWebServer)) {
  // Ensure static assets and public directory are available inside standalone/web
  copyDirIfMissing(
    path.join(rootDir, 'web', '.next', 'static'),
    path.join(standaloneWebDir, '.next', 'static')
  );
  copyDirIfMissing(
    path.join(rootDir, 'web', 'public'),
    path.join(standaloneWebDir, 'public')
  );
  copyDirIfMissing(
    path.join(rootDir, 'public'),
    path.join(standaloneWebDir, 'public')
  );

  process.chdir(standaloneWebDir);
  require(standaloneWebServer);
} else if (fs.existsSync(standaloneRootServer)) {
  copyDirIfMissing(
    path.join(rootDir, 'web', '.next', 'static'),
    path.join(standaloneRootDir, '.next', 'static')
  );
  copyDirIfMissing(
    path.join(rootDir, 'web', 'public'),
    path.join(standaloneRootDir, 'public')
  );

  process.chdir(standaloneRootDir);
  require(standaloneRootServer);
} else {
  // Fallback to next start inside web workspace
  const nextBin = path.join(rootDir, 'node_modules', '.bin', 'next');
  const child = spawn(
    fs.existsSync(nextBin) ? nextBin : 'npx',
    fs.existsSync(nextBin)
      ? ['start', '-p', process.env.PORT, '-H', process.env.HOSTNAME]
      : ['next', 'start', '-p', process.env.PORT, '-H', process.env.HOSTNAME],
    {
      cwd: path.join(rootDir, 'web'),
      stdio: 'inherit',
      env: process.env,
      shell: process.platform === 'win32',
    }
  );
  child.on('exit', (code) => process.exit(code ?? 0));
}
