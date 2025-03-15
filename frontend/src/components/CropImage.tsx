import React, { useRef, useEffect, useState } from "react";
import { Rnd } from "react-rnd";
import { CONFIG } from "../../config";
import { useUpload } from "@/context/UploadContext";

type CropImageProps = {
  imageId: string | null;
  imageUrl: string | null;
  aspectRatio: number | null;
  onCropComplete: (cropBox: CropData) => void;
  isCropping: boolean;
};

type CropResponseDto = {
  savedFilePath: string;
};

type CropData = {
  x: number;
  y: number;
  width: number;
  height: number;
};

const CropImage: React.FC<CropImageProps> = ({
  imageId,
  imageUrl,
  aspectRatio,
  onCropComplete,
}) => {
  const { setSelectedImageUrl, setIsLoadingCropData, isLoadingCropData } = useUpload();
  const imageRef = useRef<HTMLImageElement | null>(null);

  // Natural size of the image (original file pixels)
  const [naturalSize, setNaturalSize] = useState({ width: 0, height: 0 });
  // Displayed (on-screen) size, after CSS scaling
  const [displayedSize, setDisplayedSize] = useState({ width: 0, height: 0 });

  // The bounding box we show on-screen (in displayed coords)
  const [cropBoxData, setCropBoxData] = useState<CropData | null>(null);

  // ──────────────────────────────────────────────────────────
  // 1. Whenever the imageUrl changes, reset the crop box so
  //    we can re-measure a brand-new image or newly cropped image.
  // ──────────────────────────────────────────────────────────
  useEffect(() => {
    if (imageUrl) {
      // Reset so handleImageLoad can compute a fresh box
      setCropBoxData(null);
    }
  }, [imageUrl]);

  // ──────────────────────────────────────────────────────────
  // 2. Fetch existing crop data (in natural coords) from server
  //    if available for this imageId.
  // ──────────────────────────────────────────────────────────
  useEffect(() => {
    if (!imageId) return;

    const fetchPreviousCrop = async () => {
      setIsLoadingCropData(true);
      try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/api/images/${imageId}/edit`);
        if (response.ok) {
          const data = await response.json();
          console.log("Crop data from backend:", data);

          if (
            data.x !== null &&
            data.y !== null &&
            data.width > 0 &&
            data.height > 0
          ) {
            // Store them temporarily in "natural" coords
            setCropBoxData({
              x: data.x,
              y: data.y,
              width: data.width,
              height: data.height,
            });
          } else {
            setCropBoxData(null);
          }
        }
      } catch (error) {
        console.error("Error fetching previous crop data:", error);
      } finally {
        setIsLoadingCropData(false);
      }
    };

    fetchPreviousCrop();
  }, [imageId, setIsLoadingCropData]);

  // ──────────────────────────────────────────────────────────
  // 3. Once the <img> loads, measure both natural & displayed
  //    sizes. Then:
  //    - If we have "natural" coords from server, scale → displayed
  //    - If no coords, create a default centered box
  // ──────────────────────────────────────────────────────────
  const handleImageLoad = () => {
    if (!imageRef.current) return;

    const { naturalWidth, naturalHeight } = imageRef.current;
    const { clientWidth, clientHeight } = imageRef.current;

    setNaturalSize({ width: naturalWidth, height: naturalHeight });
    setDisplayedSize({ width: clientWidth, height: clientHeight });

    // If we have NATURAL coords from the server, scale them to displayed coords:
    if (cropBoxData && !isLoadingCropData && cropBoxData.width > 0 && cropBoxData.height > 0) {
      // Detect if cropBoxData is still in "natural" space by comparing to image bounds
      const isLikelyNatural = cropBoxData.x + cropBoxData.width <= naturalWidth + 1;
      if (isLikelyNatural) {
        const displayedBox = scaleToDisplayed(cropBoxData, naturalWidth, naturalHeight, clientWidth, clientHeight);
        // Also clamp the box inside the displayed area
        const clampedBox = clampBoxToDisplay(displayedBox, clientWidth, clientHeight);
        setCropBoxData(clampedBox);
        return;
      }
    }

    // Otherwise, if no data or we've reset, create a default.
    if (!cropBoxData && !isLoadingCropData) {
      const defaultBox = calculateDefaultCropBox(clientWidth, clientHeight, aspectRatio);
      const clampedBox = clampBoxToDisplay(defaultBox, clientWidth, clientHeight);
      setCropBoxData(clampedBox);
    }
  };

  // ──────────────────────────────────────────────────────────
  // 4. If aspect ratio changes, recenter the bounding box
  // ──────────────────────────────────────────────────────────
  useEffect(() => {
    if (cropBoxData && displayedSize.width && displayedSize.height && aspectRatio) {
      const updated = calculateDefaultCropBox(
        displayedSize.width,
        displayedSize.height,
        aspectRatio,
        cropBoxData
      );
      const clamped = clampBoxToDisplay(updated, displayedSize.width, displayedSize.height);
      setCropBoxData(clamped);
    }
  }, [aspectRatio]);

  // ──────────────────────────────────────────────────────────
  // 5. Utility to scale NATURAL coords → DISPLAYED coords
  // ──────────────────────────────────────────────────────────
  function scaleToDisplayed(
    box: CropData,
    natW: number,
    natH: number,
    dispW: number,
    dispH: number
  ): CropData {
    const scaleX = dispW / natW;
    const scaleY = dispH / natH;
    return {
      x: box.x * scaleX,
      y: box.y * scaleY,
      width: box.width * scaleX,
      height: box.height * scaleY,
    };
  }

  // ──────────────────────────────────────────────────────────
  // 6. Constrain the box to be fully within the displayed area
  // ──────────────────────────────────────────────────────────
  function clampBoxToDisplay(
    box: CropData,
    dispW: number,
    dispH: number
  ): CropData {
    let { x, y, width, height } = box;

    // If the box is bigger than the display, clamp its size
    if (width > dispW) width = dispW;
    if (height > dispH) height = dispH;

    // Ensure top/left are >= 0
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    // Ensure bottom/right edges are within the displayed area
    if (x + width > dispW) x = dispW - width;
    if (y + height > dispH) y = dispH - height;

    return { x, y, width, height };
  }

  // ──────────────────────────────────────────────────────────
  // 7. Default bounding box at ~50% of displayed width & centered
  //    If we have currentCrop, preserve its center
  // ──────────────────────────────────────────────────────────
  function calculateDefaultCropBox(
    dispWidth: number,
    dispHeight: number,
    ratio: number | null,
    currentCrop?: CropData
  ): CropData {
    let w = dispWidth * 0.5;
    let h = ratio ? w / ratio : dispHeight * 0.5;

    // If too tall, clamp to 50% of display height
    if (h > dispHeight * 0.5) {
      h = dispHeight * 0.5;
      w = ratio ? h * ratio : w;
    }

    const centerX = currentCrop
      ? currentCrop.x + currentCrop.width / 2
      : dispWidth / 2;
    const centerY = currentCrop
      ? currentCrop.y + currentCrop.height / 2
      : dispHeight / 2;

    return {
      x: centerX - w / 2,
      y: centerY - h / 2,
      width: w,
      height: h,
    };
  }

  // ──────────────────────────────────────────────────────────
  // 8. "Apply Crop" => scale displayed coords → NATURAL coords
  //    & send to the server
  // ──────────────────────────────────────────────────────────
  const onCrop = () => {
    if (!cropBoxData) return;
    onCropComplete(cropBoxData);

    const scaleX = naturalSize.width / displayedSize.width;
    const scaleY = naturalSize.height / displayedSize.height;

    const actualX = Math.round(cropBoxData.x * scaleX);
    const actualY = Math.round(cropBoxData.y * scaleY);
    const actualW = Math.round(cropBoxData.width * scaleX);
    const actualH = Math.round(cropBoxData.height * scaleY);

    saveCropToBackend({ x: actualX, y: actualY, width: actualW, height: actualH });
  };

  // ──────────────────────────────────────────────────────────
  // 9. Save crop data to backend with NATURAL coords
  // ──────────────────────────────────────────────────────────
  async function saveCropToBackend(cropBox: CropData) {
    if (!imageId || !imageUrl) {
      console.error("Image ID or URL missing, cannot save crop.");
      return;
    }

    try {
      const response = await fetch(`${CONFIG.API_BASE_URL}/api/images/${imageId}/crop`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(cropBox),
      });

      if (!response.ok) {
        throw new Error(`Failed to save crop. Status: ${response.status}`);
      }

      const data: CropResponseDto = await response.json();
      const newImageUrl = data.savedFilePath;
      const fullImageUrl = `${CONFIG.API_BASE_URL}/${newImageUrl}`;
      console.log("New cropped image URL:", fullImageUrl);

      // Update the displayed image to the newly cropped version
      setSelectedImageUrl(fullImageUrl);

    } catch (error) {
      console.error("Error saving crop data:", error);
    }
  }

  // ──────────────────────────────────────────────────────────
  // 10. Render
  // ──────────────────────────────────────────────────────────
  return (
    <div className="w-full flex flex-col items-center">
      <div className="relative inline-block">
        {imageUrl && (
          <img
            ref={imageRef}
            src={imageUrl}
            alt="Crop Preview"
            className="max-w-full object-contain"
            onLoad={handleImageLoad}
          />
        )}

        {/* Only show RND if we have a box & the image is loaded */}
        {cropBoxData && displayedSize.width > 0 && displayedSize.height > 0 && (
          <Rnd
            bounds="parent"
            size={{ width: cropBoxData.width, height: cropBoxData.height }}
            position={{ x: cropBoxData.x, y: cropBoxData.y }}
            style={{
              position: "absolute",
              border: "2px dashed red",
              background: "rgba(255, 255, 255, 0.1)",
              cursor: "move",
            }}
            lockAspectRatio={aspectRatio || false}
            minWidth={50}
            minHeight={50}
            onDragStop={(e, d) =>
              setCropBoxData((prev) =>
                prev ? clampBoxToDisplay({ ...prev, x: d.x, y: d.y }, displayedSize.width, displayedSize.height)
                     : prev
              )
            }
            onResizeStop={(e, direction, ref, delta, position) => {
              const newWidth = parseFloat(ref.style.width);
              const newHeight = parseFloat(ref.style.height);
              const clamped = clampBoxToDisplay(
                {
                  width: newWidth,
                  height: newHeight,
                  x: position.x,
                  y: position.y,
                },
                displayedSize.width,
                displayedSize.height
              );
              setCropBoxData(clamped);
            }}
          />
        )}
      </div>

      <button
        className="mt-4 px-4 py-2 bg-primary text-white rounded"
        onClick={onCrop}
        disabled={!cropBoxData}
      >
        Apply Crop
      </button>
    </div>
  );
};

export default CropImage;
