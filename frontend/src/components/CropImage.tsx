// import React, { useRef, useEffect, useState, useCallback } from "react";
// import { Rnd } from "react-rnd";
// import { CONFIG } from "../../config";
// import { useUpload } from "@/context/UploadContext";
// import { useToast } from "@/hooks/use-toast";
// import { Button } from "@/components/ui/button";

// type CropImageProps = {
//   imageId: string | null;
//   imageUrl: string | null;
//   aspectRatio: number | null;
//   onCropComplete: (cropBox: CropData) => void;
//   isCropping: boolean;
// };

// type CropResponseDto = {
//   savedFilePath: string;
// };

// export type CropData = {
//   x: number;
//   y: number;
//   width: number;
//   height: number;
// };

// const CropImage: React.FC<CropImageProps> = ({
//   imageId,
//   imageUrl,
//   aspectRatio,
//   onCropComplete,
//   isCropping,
// }) => {
//   const {
//     setSelectedImageUrl,
//     setIsLoadingCropData,
//     isLoadingCropData,
//     getFullImageUrl,
//     setCroppedImageUrl,
//     refreshImages,
//     restoreCurrentImageUrl,
//     getBaseImageUrlForCropping,
//   } = useUpload();
//   const { toast } = useToast();
//   const imageRef = useRef<HTMLImageElement | null>(null);

//   // Track the actual URL to use for cropping (base image URL in crop mode)
//   const [cropImageUrl, setCropImageUrl] = useState<string | null>(null);
//   // Natural size of the image (original file dimensions)
//   const [naturalSize, setNaturalSize] = useState({ width: 0, height: 0 });
//   // Displayed (on-screen) size after CSS scaling
//   const [displayedSize, setDisplayedSize] = useState({ width: 0, height: 0 });
//   // Raw crop data from server (in natural coordinates)
//   const [serverCropData, setServerCropData] = useState<CropData | null>(null);
//   // The bounding box shown on-screen (in displayed coordinates)
//   const [cropBoxData, setCropBoxData] = useState<CropData | null>(null);
//   // Flag to track when image has loaded
//   const [isImageLoaded, setIsImageLoaded] = useState(false);

//   // 1. When imageUrl or aspectRatio changes, reset states for a fresh start.
//   useEffect(() => {
//     if (imageUrl) {
//       setIsImageLoaded(false);
//       setServerCropData(null);
//       setCropBoxData(null);

//     }
//   }, [imageUrl]);

//   // Add a new effect to reset the crop box when aspect ratio changes
//   useEffect(() => {
//     if (aspectRatio !== null && isCropping) {
//       // Only reset if we're in cropping mode and have a specific aspect ratio
//       setCropBoxData(null);
//     }
//   }, [aspectRatio, isCropping]);

//   // 2. Fetch existing crop data (in natural coords) from the server if available.
//   useEffect(() => {
//     if (!imageId || !isCropping) return;

//     const fetchPreviousCrop = async () => {
//       setIsLoadingCropData(true);
//       try {
//         const response = await fetch(
//           `${CONFIG.API_BASE_URL}/api/images/${imageId}/edit`
//         );
//         if (response.ok) {
//           const data = await response.json();
//           // console.log("DEBUG - /edit API response:", data);
//           // console.log("DEBUG - Current entity label:", data.data?.label);
//           // console.log("DEBUG - Base image URL:", data.data?.baseImageUrl);
//           // console.log("DEBUG - Current image URL:", data.data?.currentImageUrl);

//           if (
//             data.data &&
//             data.data.crop &&
//             data.data.crop.x !== null &&
//             data.data.crop.y !== null &&
//             data.data.crop.width > 0 &&
//             data.data.crop.height > 0
//           ) {
//             const cropData = {
//               x: data.data.crop.x,
//               y: data.data.crop.y,
//               width: data.data.crop.width,
//               height: data.data.crop.height,
//             };
//             // Force the crop box to be null before setting server data
//             setCropBoxData(null);
//             setServerCropData(cropData);
//             // console.log("DEBUG - Server crop data being set:", cropData);
//           } else {
//             setServerCropData(null);
//           }
//         }
//       } catch (error) {
//         console.error("Error fetching previous crop data:", error);
//         setServerCropData(null);
//       } finally {
//         setIsLoadingCropData(false);
//       }
//     };

//     fetchPreviousCrop();
//   }, [imageId, setIsLoadingCropData, isCropping]);

//   // Clean up effect – restore current image URL when component unmounts or crop mode ends.
//   useEffect(() => {
//     if (!isCropping && imageId) {
//       restoreCurrentImageUrl(imageId);
//     }
//     return () => {
//       if (imageId) {
//         restoreCurrentImageUrl(imageId);
//       }
//     };
//   }, [isCropping, imageId, restoreCurrentImageUrl]);

//   // 3. When imageId or isCropping changes, ensure we have the correct image URL.
//   useEffect(() => {
//     if (imageId && isCropping) {
//       const fetchBaseUrl = async () => {
//         const baseUrl = await getBaseImageUrlForCropping(imageId);
//         if (baseUrl) {
//           setCropImageUrl(baseUrl);
//         } else {
//           setCropImageUrl(imageUrl);
//         }
//       };
//       fetchBaseUrl();
//     } else {
//       setCropImageUrl(imageUrl);
//     }
//   }, [imageId, isCropping, imageUrl, getBaseImageUrlForCropping]);

//   // 4. Handle image load – measure dimensions.
//   const handleImageLoad = useCallback(() => {
//     if (!imageRef.current) return;

//     const { naturalWidth, naturalHeight, clientWidth, clientHeight } =
//       imageRef.current;

//     setNaturalSize({ width: naturalWidth, height: naturalHeight });
//     setDisplayedSize({ width: clientWidth, height: clientHeight });
//     setIsImageLoaded(true);
//   }, []);

//   // Helper to compare crop boxes.
//   // const isSameBox = useCallback((a: CropData | null, b: CropData): boolean => {
//   //   return (
//   //     !!a &&
//   //     a.x === b.x &&
//   //     a.y === b.y &&
//   //     a.width === b.width &&
//   //     a.height === b.height
//   //   );
//   // }, []);

//   // 5. Once image is loaded and server data is ready, determine the crop box.
//   useEffect(() => {
//     if (
//       !isImageLoaded ||
//       displayedSize.width === 0 ||
//       displayedSize.height === 0
//     ) {
//       return;
//     }
//     if (isLoadingCropData) {
//       return;
//     }

//     // Only set initial crop box if cropBoxData is null
//     // This prevents resetting the crop box after user interactions
    
//     // if (cropBoxData !== null) {
//     //   console.log("DEBUG - Crop box data already exists, skipping");
//     //   return;
//     // }

//     console.log("DEBUG - Calculating new crop box");
//     let newBox: CropData;
//     if (aspectRatio !== null) {
//       console.log("DEBUG - aspectRatio not null");
//       const defaultBox = calculateDefaultCropBox(
//         displayedSize.width,
//         displayedSize.height,
//         aspectRatio
//       );
//       newBox = clampBoxToDisplay(
//         defaultBox,
//         displayedSize.width,
//         displayedSize.height
//       );
//     } else if (serverCropData) {

//       console.log("DEBUG - Server crop data being used:", serverCropData);
//       const displayedBox = scaleToDisplayed(
//         serverCropData,
//         naturalSize.width,
//         naturalSize.height,
//         displayedSize.width,
//         displayedSize.height
//       );
//       newBox = clampBoxToDisplay(
//         displayedBox,
//         displayedSize.width,
//         displayedSize.height
//       );
//     } else {
//       console.log("DEBUG - Using default box due to serverCropData being null");
//       const defaultBox = calculateDefaultCropBox(
//         displayedSize.width,
//         displayedSize.height,
//         aspectRatio
//       );
//       newBox = clampBoxToDisplay(
//         defaultBox,
//         displayedSize.width,
//         displayedSize.height
//       );
//     }
//     setCropBoxData(newBox);
//   }, [
//     isImageLoaded,
//     displayedSize,
//     naturalSize,
//     serverCropData,
//     isLoadingCropData,
//     aspectRatio
//     // isSameBox,
//   ]);

//   // Utility to scale natural coordinates to displayed coordinates.
//   function scaleToDisplayed(
//     box: CropData,
//     natW: number,
//     natH: number,
//     dispW: number,
//     dispH: number
//   ): CropData {
//     const scaleX = dispW / natW;
//     const scaleY = dispH / natH;
//     return {
//       x: box.x * scaleX,
//       y: box.y * scaleY,
//       width: box.width * scaleX,
//       height: box.height * scaleY,
//     };
//   }

//   // Constrain the crop box to be fully within the displayed area.
//   function clampBoxToDisplay(
//     box: CropData,
//     dispW: number,
//     dispH: number
//   ): CropData {
//     let { x, y, width, height } = box;
//     if (width > dispW) width = dispW;
//     if (height > dispH) height = dispH;
//     if (x < 0) x = 0;
//     if (y < 0) y = 0;
//     if (x + width > dispW) x = dispW - width;
//     if (y + height > dispH) y = dispH - height;
//     if (width < 0) width = 0;
//     if (height < 0) height = 0;
//     return { x, y, width, height };
//   }

//   // Default crop box at ~50% of displayed width and centered.
//   function calculateDefaultCropBox(
//     dispWidth: number,
//     dispHeight: number,
//     ratio: number | null,
//     currentCrop?: CropData | null
//   ): CropData {
//     console.log("DEBUG - Calculating default crop box with ratio:", ratio);
//     let w = dispWidth * 0.5;
//     let h = ratio ? w / ratio : dispHeight * 0.5;
//     if (h > dispHeight * 0.5) {
//       h = dispHeight * 0.5;
//       w = ratio ? h * ratio : w;
//     }

//     let centerX = dispWidth / 2;
//     let centerY = dispHeight / 2;
//     if (currentCrop) {
//       centerX = currentCrop.x + currentCrop.width / 2;
//       centerY = currentCrop.y + currentCrop.height / 2;
//     }

//     let calculatedX = centerX - w / 2;
//     let calculatedY = centerY - h / 2;
//     if (calculatedX < 0) calculatedX = 0;
//     if (calculatedY < 0) calculatedY = 0;

//     console.log("DEBUG - Calculated crop box:", {
//       x: calculatedX,
//       y: calculatedY,
//       width: w,
//       height: h,
//     });
//     return {
//       x: calculatedX,
//       y: calculatedY,
//       width: w,
//       height: h,
//     };
//   }

//   // "Apply Crop" – scale displayed coordinates back to natural coordinates.
//   const onCrop = useCallback(() => {
//     if (!cropBoxData) {
//       return;
//     }

//     const imgElement = imageRef.current;
//     if (!imgElement) {
//       console.error("Image element reference is missing");
//       return;
//     }

//     const currentNaturalWidth = imgElement.naturalWidth;
//     const currentNaturalHeight = imgElement.naturalHeight;
//     const currentDisplayWidth = imgElement.clientWidth;
//     const currentDisplayHeight = imgElement.clientHeight;

//     const scaleX = currentNaturalWidth / currentDisplayWidth;
//     const scaleY = currentNaturalHeight / currentDisplayHeight;

//     const actualX = Math.round(cropBoxData.x * scaleX);
//     const actualY = Math.round(cropBoxData.y * scaleY);
//     const actualW = Math.round(cropBoxData.width * scaleX);
//     const actualH = Math.round(cropBoxData.height * scaleY);

//     if (
//       actualX < 0 ||
//       actualY < 0 ||
//       actualW <= 0 ||
//       actualH <= 0 ||
//       actualX + actualW > currentNaturalWidth ||
//       actualY + actualH > currentNaturalHeight
//     ) {
//       toast({
//         title: "Invalid crop parameters",
//         description:
//           "The crop box is outside the image bounds. Please try again.",
//         variant: "destructive",
//       });
//       return;
//     }

//     saveCropToBackend({
//       x: actualX,
//       y: actualY,
//       width: actualW,
//       height: actualH,
//     })
//       .then(() => {
//         onCropComplete(cropBoxData);
//       })
//       .catch((err) => {
//         console.error("Error saving crop:", err);
//       });
//   }, [cropBoxData, onCropComplete, toast]);

//   // Save crop data to backend with natural coordinates.
//   async function saveCropToBackend(cropBox: Omit<CropData, "imageId">) {
//     if (!imageId || !imageUrl) {
//       console.error("Image ID or URL missing, cannot save crop.");
//       throw new Error("Image ID or URL missing");
//     }

//     if (cropBox.width <= 0 || cropBox.height <= 0) {
//       throw new Error(
//         `Invalid crop dimensions: ${cropBox.width}x${cropBox.height}`
//       );
//     }

//     // console.log("DEBUG - Sending crop request with payload:", {
//     //   imageId,
//     //   cropBox,
//     // });

//     try {
//       const response = await fetch(
//         `${CONFIG.API_BASE_URL}/api/images/${imageId}/crop`,
//         {
//           method: "POST",
//           headers: { "Content-Type": "application/json" },
//           body: JSON.stringify(cropBox),
//         }
//       );
//       if (!response.ok) {
//         const errorText = await response.text();
//         console.error(`Server error: ${response.status}`, errorText);
//         throw new Error(`Failed to save crop. Status: ${response.status}`);
//       }
//       const data = await response.json();
//       // console.log("DEBUG - Crop API response:", data);
//       // console.log("DEBUG - New image label:", data.data?.label);
//       // console.log("DEBUG - New current image URL:", data.data?.currentImageUrl);

//       if (data.status === "success" && data.data) {
//         const newImageUrl = data.data.currentImageUrl;
//         setSelectedImageUrl(newImageUrl);
//         setCroppedImageUrl(newImageUrl);

//         // Ensure the image list is refreshed and give it time to complete
//         // But avoid causing an infinite update loop
//         refreshImages();
//         // Use a simple timeout instead of modifying state in a promise
//         await new Promise((resolve) => setTimeout(resolve, 300));

//         toast({
//           title: "Crop saved successfully",
//           description: "Image cropped successfully.",
//         });
//       } else {
//         throw new Error("Invalid response format from server");
//       }
//     } catch (error) {
//       console.error("Error saving crop data:", error);
//       toast({
//         title: "Error saving crop",
//         description: "Please try again.",
//         variant: "destructive",
//       });
//       throw error;
//     }
//   }

//   return (
//     <div className="w-full flex flex-col items-center">
//       <div className="relative w-full aspect-square">
//         <div className="relative w-full h-full">
//           {cropImageUrl && (
//             <img
//               src={
//                 cropImageUrl.includes(CONFIG.API_BASE_URL)
//                   ? cropImageUrl
//                   : getFullImageUrl(cropImageUrl)
//               }
//               ref={imageRef}
//               alt="Crop Preview"
//               className="w-full h-full object-contain rounded-lg"
//               style={{ maxHeight: "80vh" }}
//               onLoad={handleImageLoad}
//             />
//           )}

//           {isCropping &&
//             cropBoxData &&
//             displayedSize.width > 0 &&
//             displayedSize.height > 0 && (
//               <Rnd
//                 size={{ width: cropBoxData.width, height: cropBoxData.height }}
//                 position={{ x: cropBoxData.x, y: cropBoxData.y }}
//                 bounds="parent"
//                 lockAspectRatio={aspectRatio || false}
//                 minWidth={50}
//                 minHeight={50}
//                 onDrag={(e, d) => {
//                   setCropBoxData((prev) =>
//                     prev ? { ...prev, x: d.x, y: d.y } : prev
//                   );
//                 }}
//                 onDragStop={(e, d) =>
//                   setCropBoxData((prev) =>
//                     prev
//                       ? clampBoxToDisplay(
//                           { ...prev, x: d.x, y: d.y },
//                           displayedSize.width,
//                           displayedSize.height
//                         )
//                       : prev
//                   )
//                 }
//                 onResize={(e, direction, ref, delta, position) => {
//                   const newWidth = parseFloat(ref.style.width);
//                   const newHeight = parseFloat(ref.style.height);
//                   setCropBoxData((prev) =>
//                     prev
//                       ? {
//                           ...prev,
//                           width: newWidth,
//                           height: newHeight,
//                           x: position.x,
//                           y: position.y,
//                         }
//                       : prev
//                   );
//                 }}
//                 onResizeStop={(e, direction, ref, delta, position) => {
//                   const newWidth = parseFloat(ref.style.width);
//                   const newHeight = parseFloat(ref.style.height);
//                   const clamped = clampBoxToDisplay(
//                     {
//                       width: newWidth,
//                       height: newHeight,
//                       x: position.x,
//                       y: position.y,
//                     },
//                     displayedSize.width,
//                     displayedSize.height
//                   );
//                   setCropBoxData(clamped);
//                 }}
//                 style={{
//                   position: "absolute",
//                   border: "2px dashed red",
//                   background: "rgba(255, 255, 255, 0.1)",
//                   cursor: "move",
//                   zIndex: 10,
//                 }}
//               />
//             )}
//         </div>
//       </div>

//       {isCropping && (
//         <Button className="mt-4" onClick={onCrop} disabled={!cropBoxData}>
//           Apply Crop
//         </Button>
//       )}
//     </div>
//   );
// };

// export default CropImage;


import React, { useRef, useEffect, useState, useCallback } from "react";
import { Rnd } from "react-rnd";
import { CONFIG } from "../../config";
import { useUpload } from "@/context/UploadContext";
import { useToast } from "@/hooks/use-toast";
import { Button } from "@/components/ui/button";

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

export type CropData = {
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
  isCropping,
}) => {
  const {
    setSelectedImageUrl,
    setIsLoadingCropData,
    isLoadingCropData,
    getFullImageUrl,
    setCroppedImageUrl,
    refreshImages,
    restoreCurrentImageUrl,
    getBaseImageUrlForCropping,
  } = useUpload();
  const { toast } = useToast();
  const imageRef = useRef<HTMLImageElement | null>(null);

  // Track the actual URL to use for cropping (base image URL in crop mode)
  const [cropImageUrl, setCropImageUrl] = useState<string | null>(null);
  // Natural size of the image (original file dimensions)
  const [naturalSize, setNaturalSize] = useState({ width: 0, height: 0 });
  // Displayed (on-screen) size after CSS scaling
  const [displayedSize, setDisplayedSize] = useState({ width: 0, height: 0 });
  // Raw crop data from server (in natural coordinates)
  const [serverCropData, setServerCropData] = useState<CropData | null>(null);
  // The bounding box shown on-screen (in displayed coordinates)
  const [cropBoxData, setCropBoxData] = useState<CropData | null>(null);
  // Flag to track when image has loaded
  const [isImageLoaded, setIsImageLoaded] = useState(false);

  // 1. When imageUrl or aspectRatio changes, reset states for a fresh start.
  useEffect(() => {
    if (imageUrl) {
      setIsImageLoaded(false);
      setServerCropData(null);
      setCropBoxData(null);
    }
  }, [imageUrl]);

  // Add a new effect to reset the crop box when aspect ratio changes
  useEffect(() => {
    if (aspectRatio !== null && isCropping) {
      // Only reset if we're in cropping mode and have a specific aspect ratio
      setCropBoxData(null);
    }
  }, [aspectRatio, isCropping]);

  // 2. Fetch existing crop data (in natural coords) from the server if available.
  useEffect(() => {
    if (!imageId || !isCropping) return;

    const fetchPreviousCrop = async () => {
      setIsLoadingCropData(true);
      try {
        const response = await fetch(
          `${CONFIG.API_BASE_URL}/api/images/${imageId}/edit`
        );
        if (response.ok) {
          const data = await response.json();
          // console.log("DEBUG - /edit API response:", data);
          // console.log("DEBUG - Current entity label:", data.data?.label);
          // console.log("DEBUG - Base image URL:", data.data?.baseImageUrl);
          // console.log("DEBUG - Current image URL:", data.data?.currentImageUrl);

          if (
            data.data &&
            data.data.crop &&
            data.data.crop.x !== null &&
            data.data.crop.y !== null &&
            data.data.crop.width > 0 &&
            data.data.crop.height > 0
          ) {
            const cropData = {
              x: data.data.crop.x,
              y: data.data.crop.y,
              width: data.data.crop.width,
              height: data.data.crop.height,
            };
            // Force the crop box to be null before setting server data
            setCropBoxData(null);
            setServerCropData(cropData);
            // console.log("DEBUG - Server crop data being set:", cropData);
          } else {
            setServerCropData(null);
          }
        }
      } catch (error) {
        console.error("Error fetching previous crop data:", error);
        setServerCropData(null);
      } finally {
        setIsLoadingCropData(false);
      }
    };

    fetchPreviousCrop();
  }, [imageId, setIsLoadingCropData, isCropping]);

  // Clean up effect – restore current image URL when component unmounts or crop mode ends.
  useEffect(() => {
    if (!isCropping && imageId) {
      restoreCurrentImageUrl(imageId);
    }
    return () => {
      if (imageId) {
        restoreCurrentImageUrl(imageId);
      }
    };
  }, [isCropping, imageId, restoreCurrentImageUrl]);

  // 3. When imageId or isCropping changes, ensure we have the correct image URL.
  useEffect(() => {
    if (imageId && isCropping) {
      const fetchBaseUrl = async () => {
        const baseUrl = await getBaseImageUrlForCropping(imageId);
        if (baseUrl) {
          setCropImageUrl(baseUrl);
        } else {
          setCropImageUrl(imageUrl);
        }
      };
      fetchBaseUrl();
    } else {
      setCropImageUrl(imageUrl);
    }
  }, [imageId, isCropping, imageUrl, getBaseImageUrlForCropping]);

  // 4. Handle image load – measure dimensions.
  const handleImageLoad = useCallback(() => {
    if (!imageRef.current) return;

    const { naturalWidth, naturalHeight, clientWidth, clientHeight } =
      imageRef.current;

    setNaturalSize({ width: naturalWidth, height: naturalHeight });
    setDisplayedSize({ width: clientWidth, height: clientHeight });
    setIsImageLoaded(true);
  }, []);

  // Helper to compare crop boxes.
  // const isSameBox = useCallback((a: CropData | null, b: CropData): boolean => {
  //   return (
  //     !!a &&
  //     a.x === b.x &&
  //     a.y === b.y &&
  //     a.width === b.width &&
  //     a.height === b.height
  //   );
  // }, []);

  // 5. Once image is loaded and server data is ready, determine the crop box.
  useEffect(() => {
    if (
      !isImageLoaded ||
      displayedSize.width === 0 ||
      displayedSize.height === 0
    ) {
      return;
    }
    if (isLoadingCropData) {
      return;
    }

    // Only set initial crop box if cropBoxData is null
    // This prevents resetting the crop box after user interactions
    
    // if (cropBoxData !== null) {
    //   console.log("DEBUG - Crop box data already exists, skipping");
    //   return;
    // }

    console.log("DEBUG - Calculating new crop box");
    let newBox: CropData;
    if (aspectRatio !== null) {
      console.log("DEBUG - aspectRatio not null");
      const defaultBox = calculateDefaultCropBox(
        displayedSize.width,
        displayedSize.height,
        aspectRatio
      );
      newBox = clampBoxToDisplay(
        defaultBox,
        displayedSize.width,
        displayedSize.height
      );
    } else if (serverCropData) {

      console.log("DEBUG - Server crop data being used:", serverCropData);
      const displayedBox = scaleToDisplayed(
        serverCropData,
        naturalSize.width,
        naturalSize.height,
        displayedSize.width,
        displayedSize.height
      );
      newBox = clampBoxToDisplay(
        displayedBox,
        displayedSize.width,
        displayedSize.height
      );
    } else {
      console.log("DEBUG - Using default box due to serverCropData being null");
      const defaultBox = calculateDefaultCropBox(
        displayedSize.width,
        displayedSize.height,
        aspectRatio
      );
      newBox = clampBoxToDisplay(
        defaultBox,
        displayedSize.width,
        displayedSize.height
      );
    }
    setCropBoxData(newBox);
  }, [
    isImageLoaded,
    displayedSize,
    naturalSize,
    serverCropData,
    isLoadingCropData,
    aspectRatio
    // isSameBox,
  ]);

  // Utility to scale natural coordinates to displayed coordinates.
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

  // Constrain the crop box to be fully within the displayed area.
  function clampBoxToDisplay(
    box: CropData,
    dispW: number,
    dispH: number
  ): CropData {
    let { x, y, width, height } = box;
    if (width > dispW) width = dispW;
    if (height > dispH) height = dispH;
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    if (x + width > dispW) x = dispW - width;
    if (y + height > dispH) y = dispH - height;
    if (width < 0) width = 0;
    if (height < 0) height = 0;
    return { x, y, width, height };
  }

  // Default crop box at ~50% of displayed width and centered.
  function calculateDefaultCropBox(
    dispWidth: number,
    dispHeight: number,
    ratio: number | null,
    currentCrop?: CropData | null
  ): CropData {
    console.log("DEBUG - Calculating default crop box with ratio:", ratio);
    let w = dispWidth * 0.5;
    let h = ratio ? w / ratio : dispHeight * 0.5;
    if (h > dispHeight * 0.5) {
      h = dispHeight * 0.5;
      w = ratio ? h * ratio : w;
    }

    let centerX = dispWidth / 2;
    let centerY = dispHeight / 2;
    if (currentCrop) {
      centerX = currentCrop.x + currentCrop.width / 2;
      centerY = currentCrop.y + currentCrop.height / 2;
    }

    let calculatedX = centerX - w / 2;
    let calculatedY = centerY - h / 2;
    if (calculatedX < 0) calculatedX = 0;
    if (calculatedY < 0) calculatedY = 0;

    console.log("DEBUG - Calculated crop box:", {
      x: calculatedX,
      y: calculatedY,
      width: w,
      height: h,
    });
    return {
      x: calculatedX,
      y: calculatedY,
      width: w,
      height: h,
    };
  }

  // "Apply Crop" – scale displayed coordinates back to natural coordinates.
  const onCrop = useCallback(() => {
    if (!cropBoxData) {
      return;
    }

    const imgElement = imageRef.current;
    if (!imgElement) {
      console.error("Image element reference is missing");
      return;
    }

    const currentNaturalWidth = imgElement.naturalWidth;
    const currentNaturalHeight = imgElement.naturalHeight;
    const currentDisplayWidth = imgElement.clientWidth;
    const currentDisplayHeight = imgElement.clientHeight;

    const scaleX = currentNaturalWidth / currentDisplayWidth;
    const scaleY = currentNaturalHeight / currentDisplayHeight;

    const actualX = Math.round(cropBoxData.x * scaleX);
    const actualY = Math.round(cropBoxData.y * scaleY);
    const actualW = Math.round(cropBoxData.width * scaleX);
    const actualH = Math.round(cropBoxData.height * scaleY);

    if (
      actualX < 0 ||
      actualY < 0 ||
      actualW <= 0 ||
      actualH <= 0 ||
      actualX + actualW > currentNaturalWidth ||
      actualY + actualH > currentNaturalHeight
    ) {
      toast({
        title: "Invalid crop parameters",
        description:
          "The crop box is outside the image bounds. Please try again.",
        variant: "destructive",
      });
      return;
    }

    saveCropToBackend({
      x: actualX,
      y: actualY,
      width: actualW,
      height: actualH,
    })
      .then(() => {
        onCropComplete(cropBoxData);
      })
      .catch((err) => {
        console.error("Error saving crop:", err);
      });
  }, [cropBoxData, onCropComplete, toast]);

  // Save crop data to backend with natural coordinates.
  async function saveCropToBackend(cropBox: Omit<CropData, "imageId">) {
    if (!imageId || !imageUrl) {
      console.error("Image ID or URL missing, cannot save crop.");
      throw new Error("Image ID or URL missing");
    }

    if (cropBox.width <= 0 || cropBox.height <= 0) {
      throw new Error(
        `Invalid crop dimensions: ${cropBox.width}x${cropBox.height}`
      );
    }

    // console.log("DEBUG - Sending crop request with payload:", {
    //   imageId,
    //   cropBox,
    // });

    try {
      const response = await fetch(
        `${CONFIG.API_BASE_URL}/api/images/${imageId}/crop`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(cropBox),
        }
      );
      if (!response.ok) {
        const errorText = await response.text();
        console.error(`Server error: ${response.status}`, errorText);
        throw new Error(`Failed to save crop. Status: ${response.status}`);
      }
      const data = await response.json();
      // console.log("DEBUG - Crop API response:", data);
      // console.log("DEBUG - New image label:", data.data?.label);
      // console.log("DEBUG - New current image URL:", data.data?.currentImageUrl);

      if (data.status === "success" && data.data) {
        const newImageUrl = data.data.currentImageUrl;
        setSelectedImageUrl(newImageUrl);
        setCroppedImageUrl(newImageUrl);

        // Ensure the image list is refreshed and give it time to complete
        // But avoid causing an infinite update loop
        refreshImages();
        // Use a simple timeout instead of modifying state in a promise
        await new Promise((resolve) => setTimeout(resolve, 300));

        toast({
          title: "Crop saved successfully",
          description: "Image cropped successfully.",
        });
      } else {
        throw new Error("Invalid response format from server");
      }
    } catch (error) {
      console.error("Error saving crop data:", error);
      toast({
        title: "Error saving crop",
        description: "Please try again.",
        variant: "destructive",
      });
      throw error;
    }
  }

  return (
    <div className="w-full flex flex-col items-center">
      <div className="relative inline-block">
        {cropImageUrl && (
          <img
            src={
              cropImageUrl.includes(CONFIG.API_BASE_URL)
                ? cropImageUrl
                : getFullImageUrl(cropImageUrl)
            }
            ref={imageRef}
            alt="Crop Preview"
            className="max-w-full max-h-[70vh] object-contain"
            onLoad={handleImageLoad}
          />
        )}

        {isCropping &&
          cropBoxData &&
          displayedSize.width > 0 &&
          displayedSize.height > 0 && (
            <Rnd
              size={{ width: cropBoxData.width, height: cropBoxData.height }}
              position={{ x: cropBoxData.x, y: cropBoxData.y }}
              bounds="parent"
              lockAspectRatio={aspectRatio || false}
              minWidth={50}
              minHeight={50}
              onDrag={(e, d) => {
                // Update position in real-time during drag
                setCropBoxData((prev) =>
                  prev ? { ...prev, x: d.x, y: d.y } : prev
                );
              }}
              onDragStop={(e, d) =>
                setCropBoxData((prev) =>
                  prev
                    ? clampBoxToDisplay(
                        { ...prev, x: d.x, y: d.y },
                        displayedSize.width,
                        displayedSize.height
                      )
                    : prev
                )
              }
              onResize={(e, direction, ref, delta, position) => {
                // Update size in real-time during resize
                const newWidth = parseFloat(ref.style.width);
                const newHeight = parseFloat(ref.style.height);
                setCropBoxData((prev) =>
                  prev
                    ? {
                        ...prev,
                        width: newWidth,
                        height: newHeight,
                        x: position.x,
                        y: position.y,
                      }
                    : prev
                );
              }}
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
              style={{
                position: "absolute",
                border: "2px dashed red",
                background: "rgba(255, 255, 255, 0.1)",
                cursor: "move",
                zIndex: 10,
              }}
            />
          )}
      </div>

      {isCropping && (
        <Button className="mt-4" onClick={onCrop} disabled={!cropBoxData}>
          Apply Crop
        </Button>
      )}
    </div>
  );
};

export default CropImage;