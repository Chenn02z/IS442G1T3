import React, { useState, useEffect } from "react";
import { useUpload } from "@/context/UploadContext";
import { useToast } from "@/hooks/use-toast";
import { Button } from "@/components/ui/button";
import { Download } from "lucide-react";
import { CONFIG } from "../../config";
import PhotoSizeSelector, { idPhotoPresets } from './PhotoSizeSelector';
import DownloadOptionsModal from "./DownloadOptionsModal";

const DownloadButton: React.FC = () => {
  const { selectedImageId, getCropAspectRatio } = useUpload();
  const [isDownloading, setIsDownloading] = useState(false);
  const [selectedSize, setSelectedSize] = useState(idPhotoPresets[0]);
  const { toast } = useToast();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [originalAspectRatio, setOriginalAspectRatio] = useState<number | null>(null);

  const downloadImage = async () => {
    if (!selectedImageId) return;
    
    setIsDownloading(true);
    try {
      let downloadUrl = `${CONFIG.API_BASE_URL}/api/images/download/${selectedImageId}`;
      
      // If specific dimensions are selected
      if (selectedSize.physicalWidth && selectedSize.physicalHeight) {
        downloadUrl = `${CONFIG.API_BASE_URL}/api/images/download/${selectedImageId}/sized?width=${selectedSize.physicalWidth}&height=${selectedSize.physicalHeight}&dpi=${selectedSize.dpi}&unit=${selectedSize.unit}`;
      }
      
      // Create a link and trigger the download
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.setAttribute('download', '');
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      toast({
        title: "Image downloaded successfully",
      });
    } catch (error) {
      console.error('Error downloading image:', error);
      toast({
        title: "Error downloading image",
        description: "Please try again.",
        variant: "destructive",
      });
    } finally {
      setIsDownloading(false);
    }
  };
  
  // Fetch the aspect ratio when the component loads or selectedImageId changes
  useEffect(() => {
    const fetchAspectRatio = async () => {
      if (selectedImageId) {
        const ratio = await getCropAspectRatio();
        setOriginalAspectRatio(ratio);
      } else {
        setOriginalAspectRatio(null);
      }
    };
    
    fetchAspectRatio();
  }, [selectedImageId, getCropAspectRatio]);
  
  return (
    <>
      <Button 
        variant="default" 
        className="flex items-center gap-2"
        onClick={() => setIsModalOpen(true)}
        disabled={!selectedImageId}
      >
        <Download className="h-4 w-4" />
        Download
      </Button>
      
      <DownloadOptionsModal 
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        imageId={selectedImageId}
        originalAspectRatio={originalAspectRatio}
      />
      
      {selectedSize.physicalWidth && selectedSize.physicalHeight && (
        <div className="text-xs text-gray-500 text-center">
          This will download a {selectedSize.physicalWidth}{selectedSize.unit} × {selectedSize.physicalHeight}{selectedSize.unit} photo
          at {selectedSize.dpi} DPI ({calculatePixels(selectedSize)} pixels)
        </div>
      )}
    </>
  );
};

// Helper function to show pixel dimensions
function calculatePixels(preset: any) {
  if (!preset.physicalWidth || !preset.physicalHeight) return '';
  
  let pixelWidth, pixelHeight;
  
  if (preset.unit === 'mm') {
    // Convert mm to inches (1 inch = 25.4 mm)
    const widthInches = preset.physicalWidth / 25.4;
    const heightInches = preset.physicalHeight / 25.4;
    
    // Calculate pixel dimensions
    pixelWidth = Math.round(widthInches * preset.dpi);
    pixelHeight = Math.round(heightInches * preset.dpi);
  } else {
    // Direct inch to pixel conversion
    pixelWidth = preset.physicalWidth * preset.dpi;
    pixelHeight = preset.physicalHeight * preset.dpi;
  }
  
  return `${pixelWidth}×${pixelHeight}`;
}

export default DownloadButton;
