const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

process.env.NODE_ENV = process.env.NODE_ENV || 'production';
process.env.PORT = process.env.PORT || '3000';
process.env.HOSTNAME = process.env.HOSTNAME || '0.0.0.0';

const rootDir = __dirname;

// Case 1: Running from repo root where web/.next/standalone/web/server.js exists
const standaloneWebDir = path.join(rootDir, 'web', '.next', 'standalone', 'web');
const standaloneWebServer = path.join(standaloneWebDir, 'server.js');

// Case 2: Running from repo root where .next/standalone/web/server.js exists
const rootStandaloneWebDir = path.join(rootDir, '.next', 'standalone', 'web');
const rootStandaloneWebServer = path.join(rootStandaloneWebDir, 'server.js');

// Case 3: Running inside Cloud Run /workspace (which is the unpacked .next/standalone directory itself)
const directWebDir = path.join(rootDir, 'web');
const directWebServer = path.join(directWebDir, 'server.js');

// Case 4: Non-monorepo standalone fallback
const standaloneRootDir = path.join(rootDir, 'web', '.next', 'standalone');
const standaloneRootServer = path.join(standaloneRootDir, 'server.js');

function copyDirAlways(src, dest) {
  try {
    if (fs.existsSync(src)) {
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      fs.cpSync(src, dest, { recursive: true, force: true });
    }
  } catch (err) {
    console.warn(`[server.js] Warning copying ${src} -> ${dest}:`, err.message);
  }
}

if (fs.existsSync(standaloneWebServer)) {
  copyDirAlways(
    path.join(rootDir, 'web', '.next', 'static'),
    path.join(standaloneWebDir, '.next', 'static')
  );
  copyDirAlways(
    path.join(rootDir, 'web', 'public'),
    path.join(standaloneWebDir, 'public')
  );
  copyDirAlways(
    path.join(rootDir, 'public'),
    path.join(standaloneWebDir, 'public')
  );

  process.chdir(standaloneWebDir);
  require(standaloneWebServer);
} else if (fs.existsSync(rootStandaloneWebServer)) {
  process.chdir(rootStandaloneWebDir);
  require(rootStandaloneWebServer);
} else if (fs.existsSync(directWebServer)) {
  process.chdir(directWebDir);
  require(directWebServer);
} else if (fs.existsSync(standaloneRootServer)) {
  copyDirAlways(
    path.join(rootDir, 'web', '.next', 'static'),
    path.join(standaloneRootDir, '.next', 'static')
  );
  copyDirAlways(
    path.join(rootDir, 'web', 'public'),
    path.join(standaloneRootDir, 'public')
  );

  process.chdir(standaloneRootDir);
  require(standaloneRootServer);
} else {
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
