import { useState } from 'react';
import { useToast } from "@/hooks/use-toast";
import { Button } from './ui/button';

interface DownloadButtonProps {
  croppedImageUrl: string | null;
}

const DownloadButton: React.FC<DownloadButtonProps> = ({ croppedImageUrl }) => {
  const { toast } = useToast();
  const [downloading, setDownloading] = useState(false);

  const handleDownload = async () => {
    if (!croppedImageUrl) {
      toast({
        title: "Error",
        description: "No cropped image available to download.",
      });
      return;
    }

    setDownloading(true);
    try {
      const response = await fetch(croppedImageUrl);
      if (!response.ok) {
        throw new Error('Download failed')
      };

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.style.display = 'none';
      a.href = url;
      a.download = 'cropped-image.jpg';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      toast({
        title: "Success",
        description: "Cropped image downloaded successfully.",
      });
    } catch (error) {
      console.error('Error downloading image:', error);
      toast({
        title: "Error",
        description: "Failed to download the cropped image.",
      });
    } finally {
      setDownloading(false);
    }
  };

  return (
    <Button
      onClick={handleDownload}
      disabled={!croppedImageUrl || downloading}
    >
      {downloading ? 'Downloading...' : 'Download Image'}
    </Button>
  );
};

export default DownloadButton;