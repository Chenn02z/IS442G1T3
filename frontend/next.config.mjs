/** @type {import('next').NextConfig} */
const nextConfig = {
    images: {
        domains: ["localhost"], // Allow images from localhost
        remotePatterns: [
            {
                protocol: "http",
                hostname: "localhost",
                port: "8080",
                pathname: "/images/**", // Adjust path as needed
            },
        ],
    },
};

export default nextConfig;
