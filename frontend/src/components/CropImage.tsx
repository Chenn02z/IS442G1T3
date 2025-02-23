import React, { useRef, useEffect, useState } from "react";

// Crop-related imports
import Cropper from "react-cropper";
import "cropperjs/dist/cropper.css";

// ========================================================
// PROPS
// imageURL: URL of uploaded image
// aspectRatio: The desired aspect ratio for the crop (or null for freeform)
// onCropComplete: Callback function, pass cropped image data back
// isCropping: A flag indicating whether cropping mode is active
// ========================================================
type CropImageProps = {
  imageUrl: string | null;
  aspectRatio: number | null;
  onCropComplete: (croppedImage: string) => void;
  isCropping: boolean;
};

const CropImage: React.FC<CropImageProps> = ({ imageUrl, aspectRatio, onCropComplete,isCropping }) => {
  const cropperRef = useRef<Cropper>(null);
  const [cropData, setCropData] = useState<string>("");
  // State to store the crop box dimensions (x, y, width, height)
  const [cropBoxData, setCropBoxData] = useState<{ x: number; y: number; width: number; height: number } | null>(null);

  useEffect(() => {
    const cropper = cropperRef.current?.cropper;
    if (cropper) {
      cropper.setAspectRatio(aspectRatio ?? NaN);

      //retrieve the saved crop box data from localStorage, if user wants to recrop
      if (aspectRatio === null && isCropping) {
        const savedCropBox = localStorage.getItem("cropBoxData");
        if (savedCropBox) {
          const parsedCropBox = JSON.parse(savedCropBox);
          setCropBoxData(parsedCropBox);
          cropper.setCropBoxData(parsedCropBox);
          setTimeout(() => cropper.setCropBoxData(parsedCropBox), 100);
        }
      }
    }
  }, [aspectRatio, isCropping] );

  // apply crop
  const onCrop = () => {
    const cropper = cropperRef.current?.cropper;
    if (cropper) {
      const canvas = cropper.getCroppedCanvas();
      if (canvas) {
        const croppedImage = canvas.toDataURL("image/png");
        setCropData(croppedImage);
        onCropComplete(croppedImage);

        const newCropBoxData = cropper.getCropBoxData(); // Get the current crop box data (dimensions) from the cropper
        setCropBoxData(newCropBoxData); // Update the state with the new crop box data
        localStorage.setItem("cropBoxData", JSON.stringify(newCropBoxData)); // Save the crop box data to localStorage
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