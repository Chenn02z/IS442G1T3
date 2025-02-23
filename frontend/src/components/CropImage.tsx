import React, { useRef, useEffect, useState } from "react";
import { CONFIG } from "../../config";
import { UUID_LOOKUP_KEY } from "@/app/page";
import Cropper from "react-cropper";
import "cropperjs/dist/cropper.css";

// ========================================================
// PROPS
// imageId: Unique identifier of the image
// imageURL: URL of uploaded image
// aspectRatio: The desired aspect ratio for the crop (or null for freeform)
// onCropComplete: Callback function, pass cropped image data back
// isCropping: A flag indicating whether cropping mode is active
// ========================================================
type CropImageProps = {
  imageId: string | null;
  imageUrl: string | null;
  aspectRatio: number | null;
  onCropComplete: (croppedImage: string) => void;
  isCropping: boolean;
};

const CropImage: React.FC<CropImageProps> = ({ imageId, imageUrl, aspectRatio, onCropComplete, isCropping }) => {
  const cropperRef = useRef<Cropper>(null);
  const [cropData, setCropData] = useState<string>("");
  const [cropBoxData, setCropBoxData] = useState<{
    left: number;
    top: number;
    width: number;
    height: number;
  } | null>(null);

  useEffect(() => {
    const cropper = cropperRef.current?.cropper;
    if (cropper) {
      cropper.setAspectRatio(aspectRatio ?? NaN);

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
  }, [aspectRatio, isCropping]);

  // Save crop data to backend dummy function
  const saveCropToBackend = async (cropBox: { left: number; top: number; width: number; height: number }) => {
    if (!imageId || !imageUrl) {
      console.error("Image ID or URL missing, cannot save crop.");
      return;
    }
  
    const formData = new FormData();
    formData.append("x", (Math.round(cropBox.left * 100) / 100).toFixed(2));
    formData.append("y", (Math.round(cropBox.top * 100) / 100).toFixed(2));
    formData.append("width", (Math.round(cropBox.width * 100) / 100).toFixed(2));
    formData.append("height", (Math.round(cropBox.height * 100) / 100).toFixed(2));
    
  
    // Dummy function to log formData contents
    const logFormData = (formData: FormData) => {
      for (let pair of formData.entries()) {
        console.log(`${pair[0]}: ${pair[1]}`);
      }
    };
  
    logFormData(formData); // Logs formData before sending
  
    // try {
    //   const response = await fetch(`${CONFIG.API_BASE_URL}/api/images/${imageId}/crop`, {
    //     method: "POST",
    //     body: formData,
    //   });
  
    //   if (!response.ok) {
    //     throw new Error(`Failed to save crop. Status: ${response.status}`);
    //   }
  
    //   console.log("Crop data saved successfully:", await response.json());
    // } catch (error) {
    //   console.error("Error saving crop data:", error);
    // }
  };
  

  // Apply crop
  const onCrop = () => {
    const cropper = cropperRef.current?.cropper;
    if (cropper) {
      const canvas = cropper.getCroppedCanvas();
      if (canvas) {
        const croppedImage = canvas.toDataURL("image/png");
        setCropData(croppedImage);
        onCropComplete(croppedImage);

        const newCropBoxData = cropper.getCropBoxData();
        setCropBoxData(newCropBoxData);
        localStorage.setItem("cropBoxData", JSON.stringify(newCropBoxData));

        // Call the API to save the crop data
        saveCropToBackend(newCropBoxData);
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
      <button className="mt-4 px-4 py-2 bg-primary text-white rounded" onClick={onCrop}>
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
