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
  const [cropBoxData, setCropBoxData] = useState<{ x: number; y: number; width: number; height: number } | null>(null);

  useEffect(() => {
    const cropper = cropperRef.current?.cropper;
    if (cropper) {
      cropper.setAspectRatio(aspectRatio ?? NaN);

      // ✅ Restore last crop box ONLY if Freeform is selected
      if (aspectRatio === null) {
        const savedCropBox = localStorage.getItem("cropBoxData");
        if (savedCropBox) {
          const parsedCropBox = JSON.parse(savedCropBox);
          setCropBoxData(parsedCropBox);
          cropper.setCropBoxData(parsedCropBox);
        }
      }
    }
  }, [aspectRatio]);

  const onCrop = () => {
    const cropper = cropperRef.current?.cropper;
    if (cropper) {
      const canvas = cropper.getCroppedCanvas();
      if (canvas) {
        const croppedImage = canvas.toDataURL("image/png");
        setCropData(croppedImage);
        onCropComplete(croppedImage);

        // ✅ Save exact crop box position and size
        const newCropBoxData = cropper.getCropBoxData();
        setCropBoxData(newCropBoxData);
        localStorage.setItem("cropBoxData", JSON.stringify(newCropBoxData));
      }
    }
  };

  return (
    <div className="w-full flex flex-col items-center">
      <Cropper
        key={imageUrl}
        src={imageUrl}
        style={{ height: 400, width: "100%" }}
        guides={true}
        aspectRatio={aspectRatio ?? NaN}
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