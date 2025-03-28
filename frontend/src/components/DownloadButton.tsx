import React, { useState } from "react";
import { useUpload } from "@/context/UploadContext";
import { useToast } from "@/hooks/use-toast";
import { Button } from "@/components/ui/button";
import { Download } from "lucide-react";
import { CONFIG } from "../../config";

interface DownloadButtonProps {
  imageUrl: string;
  imageId: string;
}

const DownloadButton: React.FC<DownloadButtonProps> = ({
  imageUrl,
  imageId,
}) => {
  const { getFullImageUrl, restoreCurrentImageUrl, refreshImages } =
    useUpload();
  const [downloading, setDownloading] = useState(false);
  const { toast } = useToast();

  const handleDownload = async () => {
    if (!imageUrl || !imageId) return;

    setDownloading(true);
    try {
      // First ensure we have the latest image state
      await restoreCurrentImageUrl(imageId);

      // Refresh the image list to show the latest state
      // Add await to ensure the refresh completes but don't trigger an infinite loop
      refreshImages();
      // Use a simple timeout instead of modifying state in a promise
      await new Promise((resolve) => setTimeout(resolve, 300));

      // Then download using the API endpoint
      const response = await fetch(
        `${CONFIG.API_BASE_URL}/api/images/download/${imageId}`
      );
      if (!response.ok) throw new Error("Failed to fetch image");

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `cropped-image-${Date.now()}.png`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      toast({
        title: "Image downloaded successfully",
      });
    } catch (error) {
      console.error("Error downloading image:", error);
      toast({
        title: "Error downloading image",
        description: "Please try again.",
        variant: "destructive",
      });
    } finally {
      setDownloading(false);
    }
  };

  return (
    <Button
      onClick={handleDownload}
      disabled={downloading || !imageUrl || !imageId}
      className="flex items-center gap-2"
    >
      <Download className="w-4 h-4" />
      {downloading ? "Downloading..." : "Download"}
    </Button>
  );
};

export default DownloadButton;
