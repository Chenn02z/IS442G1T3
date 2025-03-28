import React, { useEffect } from "react";
import { useUpload } from "@/context/UploadContext";

interface DisplayImageProps {
  imageUrl: string;
}

const DisplayImage: React.FC<DisplayImageProps> = ({ imageUrl }) => {
  const {
    getFullImageUrl,
    selectedImageId,
    restoreCurrentImageUrl,
    selectedImageUrl,
  } = useUpload();

  // Use selectedImageUrl from context instead of prop
  const displayUrl = selectedImageUrl || imageUrl;

  // Ensure we're displaying the latest version of the image
  useEffect(() => {
    if (selectedImageId) {
      // Refresh the image list
      // refreshImages();

      // Use restoreCurrentImageUrl to ensure we have the latest image
      restoreCurrentImageUrl(selectedImageId).catch((error) => {
        console.error("Error restoring current image URL:", error);
      });
    }
  }, [selectedImageId, restoreCurrentImageUrl]);

  if (!displayUrl) {
    return null;
  }

  return (
    <div className="relative w-full aspect-square">
      <div className="relative w-full h-full">
        <img
          src={getFullImageUrl(displayUrl)}
          alt="Displayed image"
          className="w-full h-full object-contain rounded-lg"
          style={{ maxHeight: "80vh" }}
        />
      </div>
    </div>
  );
};

export default DisplayImage;
