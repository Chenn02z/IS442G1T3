"use client";
import { Card, CardContent } from "@/components/ui/card";
import { UploadCloud, History } from "lucide-react";
import { useUpload } from "@/context/UploadContext";
import { useImageUploadHandler } from "@/utils/ImageUploadHandler";
import DisplayImage from "./DisplayImage";
import CropImage from "./CropImage";
import DownloadButton from "./DownloadButton";
import { HistoryDrawer } from "./HistoryDrawer";
import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";

// Update the props interface to remove isCropping
interface UploadImageFormProps {
  selectedAspectRatio: number | null;
}

const UploadImageForm: React.FC<UploadImageFormProps> = ({
  selectedAspectRatio,
}) => {
  const {
    selectedImageUrl,
    croppedImageUrl,
    selectedImageId,
    setIsCropping,
    isCropping,
  } = useUpload();
  const { handleUpload } = useImageUploadHandler();
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);

  // For debugging
  // useEffect(() => {
  //   console.log("UploadImageForm state:", {
  //     contextIsCropping: isCropping,
  //     selectedImageId,
  //     selectedImageUrl,
  //   });
  // }, [isCropping, selectedImageId, selectedImageUrl]);

  return (
    <Card className="w-full max-w-3xl mx-auto">
      <CardContent className="flex flex-col items-center justify-center min-h-[400px] p-6">
        {/* Before image upload */}
        {!selectedImageId && (
          <div className="flex flex-col items-center justify-center w-full h-full">
            <input
              type="file"
              accept="image/jpeg, image/png"
              className="hidden"
              id="file-upload"
              multiple
              onChange={handleUpload}
            />
            <label
              htmlFor="file-upload"
              className="cursor-pointer flex flex-col items-center gap-4 p-8 border-2 border-dashed rounded-lg hover:bg-secondary/50 transition-colors"
            >
              <UploadCloud className="h-12 w-12 text-muted-foreground" />
              <div className="text-center">
                <p className="text-base font-medium">
                  Click to upload an Image
                </p>
                <p className="text-sm text-muted-foreground mt-1">
                  Supported formats: .jpeg, .png
                </p>
              </div>
            </label>
          </div>
        )}

        {/* After image upload */}
        {selectedImageUrl && selectedImageId && (
          <div className="w-full">
            {isCropping ? (
              <>
                <div className="relative max-w-full overflow-hidden">
                  <CropImage
                    imageUrl={selectedImageUrl}
                    aspectRatio={selectedAspectRatio}
                    imageId={selectedImageId}
                    isCropping={true} // Always true when this component is rendered
                    onCropComplete={() => setIsCropping(false)}
                  />
                </div>
              </>
            ) : (
              <div className="flex flex-col items-center gap-6">
                <DisplayImage imageUrl={croppedImageUrl || selectedImageUrl} />
                <div className="flex items-center space-x-2">
                  <Button 
                    variant="outline" 
                    size="sm"
                    onClick={() => setIsHistoryOpen(true)}
                    className="flex items-center gap-2"
                  >
                    <History className="h-4 w-4" />
                    History
                  </Button>
                  <DownloadButton
                    imageUrl={croppedImageUrl || selectedImageUrl}
                    imageId={selectedImageId}
                  />
                </div>
              </div>
            )}
          </div>
        )}
      </CardContent>
      
      {/* History Drawer */}
      {selectedImageId && (
        <HistoryDrawer
          imageId={selectedImageId}
          open={isHistoryOpen}
          onOpenChange={setIsHistoryOpen}
        />
      )}
    </Card>
  );
};

export default UploadImageForm;
