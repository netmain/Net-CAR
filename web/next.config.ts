import type { NextConfig } from "next";
import { PHASE_DEVELOPMENT_SERVER } from "next/constants";

export default function nextConfig(phase: string): NextConfig {
  return {
    distDir: phase === PHASE_DEVELOPMENT_SERVER ? "build" : ".next",
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
}
