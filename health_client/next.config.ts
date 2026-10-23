import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactCompiler: true,

  // Optimize images including favicons
  images: {
    loader: 'default',
    deviceSizes: [640, 750, 828, 1080, 1200],
    imageSizes: [16, 32, 48, 64, 96, 128, 256, 384],
  },

  // Handle icons and favicons properly on Netlify/GCP
  output: 'standalone',
};

export default nextConfig;
