import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Vercel builds its own standard Next.js output, so the standalone bundle
  // (used by the self-hosted server.js) is disabled there.
  output: process.env.VERCEL ? undefined : process.platform === "win32" ? undefined : "standalone",
  experimental: {
    optimizePackageImports: ["lucide-react", "date-fns"],
  },
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "images.unsplash.com",
        port: "",
        pathname: "/**",
      },
      {
        protocol: "https",
        hostname: "picsum.photos",
        port: "",
        pathname: "/**",
      },
    ],
  },
};

export default nextConfig;
