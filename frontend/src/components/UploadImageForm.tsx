"use client";
import { Card, CardContent } from "@/components/ui/card";
import { UploadCloud } from "lucide-react";
import { useUpload } from "@/context/UploadContext";
import { useImageUploadHandler } from "@/utils/ImageUploadHandler";
import DisplayImage from "./DisplayImage";
import CropImage from "./CropImage";
import DownloadButton from "./DownloadButton";

const UploadImageForm = ({
  isCropping,
  setIsCropping,
  selectedAspectRatio,
}: {
  isCropping: boolean;
  setIsCropping: (cropping: boolean) => void;
  selectedAspectRatio: number | null;
}) => {
  const { selectedImageUrl, croppedImageUrl, setCroppedImageUrl, uploadedImageCount } = useUpload();
  const { handleUpload } = useImageUploadHandler();

  return (
    <Card className="hover:cursor-pointer hover:bg-secondary hover:border-primary transition-all ease-in-out">
      <CardContent className="flex flex-col h-full items-center justify-center px-2 py-24 text-xs">
        {/* Before image upload */}
        {uploadedImageCount === 0 && (
          <>
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
              className="cursor-pointer flex flex-col items-center gap-2"
            >
              <UploadCloud className="h-10 w-10 text-gray-500" />
              <p className="text-gray-700 text-sm">Click to upload an Image</p>
              <p className="text-muted-foreground text-xs">Supported formats: .jpeg, .png</p>
            </label>
          </>
        )}

        {/* After image upload */}
        {selectedImageUrl && (
          <>
            {isCropping ? (
              <CropImage
                imageUrl={selectedImageUrl}
                aspectRatio={selectedAspectRatio}
                isCropping={isCropping}
                onCropComplete={(croppedImage) => {
                  setCroppedImageUrl(croppedImage);
                  setIsCropping(false);
                }}
              />
            ) : (
              <DisplayImage imageUrl={croppedImageUrl || selectedImageUrl} />
            )}

            {/* Save and Download Buttons */}
            {!isCropping && (
              <div className="flex flex-col items-center mt-4 space-y-2">
                <DownloadButton croppedImageUrl={croppedImageUrl || selectedImageUrl} />
              </div>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
};

export default UploadImageForm;
