import React, { useRef, useEffect, useState } from "react";
import Cropper from "react-cropper";
import "cropperjs/dist/cropper.css";

type CropImageProps = {
  imageUrl: string;
  aspectRatio: number | null;
  onCropComplete: (croppedImage: string) => void;
};

const CropImage: React.FC<CropImageProps> = ({ imageUrl, aspectRatio, onCropComplete }) => {
  const cropperRef = useRef<Cropper>(null);
  const [cropData, setCropData] = useState<string>("");

  useEffect(() => {
    const cropper = cropperRef.current?.cropper;
    if (cropper) {
      cropper.setAspectRatio(aspectRatio ?? NaN); // Update aspect ratio dynamically
    }
  }, [aspectRatio]); // Runs when aspectRatio changes

  const onCrop = () => {
    const cropper = cropperRef.current?.cropper;
    if (cropper) {
      const canvas = cropper.getCroppedCanvas();
      if (canvas) {
        const croppedImage = canvas.toDataURL("image/png");
        setCropData(croppedImage);
        onCropComplete(croppedImage);
      }
    }
  };

  return (
    <div className="w-full flex flex-col items-center">
      <Cropper
        key={imageUrl} // Forces re-initialization if image changes
        src={imageUrl}
        style={{ height: 400, width: "100%" }}
        guides={true}
        aspectRatio={aspectRatio ?? NaN} // Dynamically set aspect ratio
        ref={cropperRef}
        viewMode={1}
      />
      <button
        className="mt-4 px-4 py-2 bg-primary text-white rounded"
        onClick={onCrop}
      >
        Apply Crop
      </button>
      {cropData && (
        <div className="mt-4">
          <p className="mb-2">Cropped Preview:</p>
          <img src={cropData} alt="Cropped Preview" className="rounded shadow" />
        </div>
      )}
    </div>
  );
};

export default CropImage;



