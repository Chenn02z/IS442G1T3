import React, { useRef, useEffect, useState } from "react";
import { Rnd } from "react-rnd";
import { CONFIG } from "../../config";
import { useUpload } from "@/context/UploadContext";

type CropImageProps = {
  imageId: string | null;
  imageUrl: string | null;
  aspectRatio: number | null;
  onCropComplete: (cropBox: { x: number; y: number; width: number; height: number }) => void;
  isCropping: boolean;
};

const CropImage: React.FC<CropImageProps> = ({
  imageId,
  imageUrl,
  aspectRatio,
  onCropComplete,
  isCropping,
}) => {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const imageRef = useRef<HTMLImageElement | null>(null);
  const [containerSize, setContainerSize] = useState<{ width: number; height: number }>({ width: 0, height: 0 });
  const [cropBoxData, setCropBoxData] = useState<{ x: number; y: number; width: number; height: number } | null>(null);

  // Calculate default crop box based on container size and aspect ratio
  const calculateDefaultCropBox = (width: number, height: number, currentCrop?: { x: number; y: number; width: number; height: number }) => {
    let defaultWidth = width * 0.5; // 50% of container's width
    let defaultHeight = aspectRatio ? defaultWidth / aspectRatio : height * 0.5;

    // Adjust if the calculated height exceeds half of container height
    if (defaultHeight > height * 0.5) {
      defaultHeight = height * 0.5;
      defaultWidth = aspectRatio ? defaultHeight * aspectRatio : defaultWidth;
    }

    // If we already have a crop box, keep its center position.
    let centerX = currentCrop ? currentCrop.x + currentCrop.width / 2 : width / 2;
    let centerY = currentCrop ? currentCrop.y + currentCrop.height / 2 : height / 2;

    return {
      x: centerX - defaultWidth / 2,
      y: centerY - defaultHeight / 2,
      width: defaultWidth,
      height: defaultHeight,
    };
  };

  // Called when the image is loaded to measure the container and set initial crop box
  const handleImageLoad = () => {
    if (containerRef.current) {
      const { offsetWidth, offsetHeight } = containerRef.current;
      setContainerSize({ width: offsetWidth, height: offsetHeight });
      const defaultCrop = calculateDefaultCropBox(offsetWidth, offsetHeight);
      setCropBoxData(defaultCrop);
    }
  };

  // Listen for changes in aspectRatio or containerSize and update cropBox accordingly
  useEffect(() => {
    if (containerSize.width && containerSize.height) {
      const updatedCrop = calculateDefaultCropBox(containerSize.width, containerSize.height, cropBoxData || undefined);
      setCropBoxData(updatedCrop);
    }
  }, [aspectRatio, containerSize]);

  // Save crop data to backend (API call is still commented out)
  const saveCropToBackend = async (cropBox: { x: number; y: number; width: number; height: number }) => {
    if (!imageId || !imageUrl) {
      console.error("Image ID or URL missing, cannot save crop.");
      return;
    }

    const formData = new FormData();
    formData.append("imageUrl", imageUrl);
    formData.append("x", cropBox.x.toFixed(2));
    formData.append("y", cropBox.y.toFixed(2));
    formData.append("width", cropBox.width.toFixed(2));
    formData.append("height", cropBox.height.toFixed(2));

    console.log("Crop Data:", { imageUrl, ...cropBox });

    // Uncomment for real API call
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

  // Apply crop (send to backend)
  const onCrop = () => {
    if (cropBoxData) {
      localStorage.setItem("cropBoxData", JSON.stringify(cropBoxData));
      // console.log("Crop Data:", JSON.stringify(cropBoxData));
      onCropComplete(cropBoxData);
      saveCropToBackend(cropBoxData);
    }
  };

  return (
    <div className="w-full flex flex-col items-center">
      <div ref={containerRef} className="relative w-full">
        {imageUrl && (
          <img
            ref={imageRef}
            src={imageUrl}
            alt="Crop Preview"
            className="w-full object-contain"
            onLoad={handleImageLoad}
          />
        )}

        {cropBoxData && (
          <Rnd
            bounds="parent"
            size={{ width: cropBoxData.width, height: cropBoxData.height }}
            position={{ x: cropBoxData.x, y: cropBoxData.y }}
            onDragStop={(e, d) =>
              setCropBoxData((prev) => (prev ? { ...prev, x: d.x, y: d.y } : prev))
            }
            onResizeStop={(e, direction, ref, delta, position) =>
              setCropBoxData({
                width: parseInt(ref.style.width),
                height: parseInt(ref.style.height),
                x: position.x,
                y: position.y,
              })
            }
            style={{
              position: "absolute",
              border: "2px dashed red",
              background: "rgba(255, 255, 255, 0.10)",
              cursor: "move",
            }}
            minWidth={50}
            minHeight={50}
          />
        )}
      </div>

      <button className="mt-4 px-4 py-2 bg-primary text-white rounded" onClick={onCrop}>
        Apply Crop
      </button>
    </div>
  );
};

export default CropImage;
