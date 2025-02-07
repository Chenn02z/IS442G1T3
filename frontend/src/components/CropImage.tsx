import React, { useState, useCallback } from "react";
import Cropper from "react-easy-crop";
import { Button } from "@/components/ui/button";

const CropImage = ({ imageSrc, imageId, onCropComplete }) => {
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [croppedAreaPixels, setCroppedAreaPixels] = useState(null);

  const onCropCompleteHandler = useCallback((_, croppedAreaPixels) => {
    setCroppedAreaPixels(croppedAreaPixels);
  }, []);

  const handleCropSubmit = async () => {
    if (!croppedAreaPixels) return;

    try {
      const response = await fetch(`/api/images/crop/${imageId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          x: Math.round(croppedAreaPixels.x),
          y: Math.round(croppedAreaPixels.y),
          width: Math.round(croppedAreaPixels.width),
          height: Math.round(croppedAreaPixels.height),
        }),
      });

      if (!response.ok) throw new Error("Failed to crop image");

      const data = await response.json();
      onCropComplete(data.imageUrl);
    } catch (error) {
      console.error("Error cropping image:", error);
    }
  };

  return (
    <div className="relative w-full h-[400px] bg-gray-200">
      {/* Cropper Component */}
      <Cropper
        image={imageSrc}
        crop={crop}
        zoom={zoom}
        aspect={1} // Default aspect ratio (can be dynamically set)
        onCropChange={setCrop}
        onZoomChange={setZoom}
        onCropComplete={onCropCompleteHandler}
      />

      {/* Controls */}
      <div className="mt-4 flex justify-between">
        <Button variant="outline" onClick={handleCropSubmit}>Crop Image</Button>
      </div>
    </div>
  );
};

export default CropImage;
