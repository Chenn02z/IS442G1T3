"use client";
import React, { createContext, useState, useContext, useCallback } from "react";
import { CONFIG } from "../../config";

interface UploadContextProps {
  uploadedFile: File | null;
  setUploadedFile: (file: File | null) => void;
  selectedImageUrl: string | null;
  setSelectedImageUrl: (url: string | null) => void;
  croppedImageUrl: string | null;
  setCroppedImageUrl: (url: string | null) => void;
  selectedImageId: string | null;
  setSelectedImageId: (id: string | null) => void;
  uploadedImageCount: number;
  setUploadedImageCount: (count: number) => void;
  isCropping: boolean;
  setIsCropping: (cropping: boolean) => void;
  isLoadingCropData: boolean;
  setIsLoadingCropData: (loading: boolean) => void;
  refreshImages: () => void;
  getFullImageUrl: (url: string) => string;
  restoreCurrentImageUrl: (imageId: string) => Promise<string | null>;
  getBaseImageUrlForCropping: (imageId: string) => Promise<string | null>;
  getCropAspectRatio: () => Promise<number | null>;
}

const UploadContext = createContext<UploadContextProps | undefined>(undefined);

export const UploadProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [selectedImageUrl, setSelectedImageUrl] = useState<string | null>(null);
  const [croppedImageUrl, setCroppedImageUrl] = useState<string | null>(null);
  const [selectedImageId, setSelectedImageId] = useState<string | null>(null);
  const [uploadedImageCount, setUploadedImageCount] = useState<number>(0);
  const [isCropping, setIsCroppingState] = useState<boolean>(false);
  const [isLoadingCropData, setIsLoadingCropData] = useState<boolean>(false);

  const refreshImages = useCallback(() => {
    // Add a timestamp-based cache buster to ensure fresh data
    setUploadedImageCount((prev) => prev + 1); // Trigger a re-fetch in PhotoList

    // Force browser to clear any cached images by updating timestamps
    // This is especially important for image URLs that haven't changed names
    if (selectedImageUrl) {
      // Only update URL if it doesn't already have a fresh timestamp
      // Check if URL already has a timestamp from the last 5 seconds
      const currentTime = Date.now();
      const urlTimeMatch = selectedImageUrl.match(/[?&]t=(\d+)/);

      if (urlTimeMatch) {
        const urlTimestamp = parseInt(urlTimeMatch[1], 10);
        // Only update if the timestamp is older than 5 seconds
        if (currentTime - urlTimestamp > 5000) {
          const updatedUrl = selectedImageUrl.replace(
            /([?&])t=\d+/,
            `$1t=${currentTime}`
          );
          setSelectedImageUrl(updatedUrl);
        }
        // Otherwise do nothing - URL already has a fresh timestamp
      } else {
        // No timestamp in URL, add one
        const cacheBuster = selectedImageUrl.includes("?")
          ? `&t=${currentTime}`
          : `?t=${currentTime}`;
        setSelectedImageUrl(`${selectedImageUrl}${cacheBuster}`);
      }
    }
  }, [setUploadedImageCount, setSelectedImageUrl]);

  const getFullImageUrl = useCallback((url: string) => {
    if (!url) return "";

    // Already a full URL
    if (url.startsWith("http")) {
      return url;
    }

    // Add API base URL
    return `${CONFIG.API_BASE_URL}/api/images/${url}`;
  }, []);

  // Restore current image URL - Define this FIRST to avoid the circular dependency
  const restoreCurrentImageUrl = useCallback(
    async (imageId: string, updateState = true) => {
      if (!imageId) return null;

      try {
        // console.log(
        //   "DEBUG - Fetching current image URL from /statemanagement/latest/",
        //   imageId
        // );
        const response = await fetch(
          `${CONFIG.API_BASE_URL}/api/statemanagement/latest/${imageId}`
        );
        if (response.ok) {
          const data = await response.json();
          // console.log("RESTORE TO IMAGE VER:", data.data?.version);
          // console.log("DEBUG - Entity label:", data.data?.label);

          if (
            data.status === "success" &&
            data.data &&
            data.data.currentImageUrl
          ) {
            const fullUrl = `${CONFIG.API_BASE_URL}/api/images/${data.data.currentImageUrl}`;
            // console.log("DEBUG - Using current image URL:", fullUrl);

            if (updateState) {
              setSelectedImageUrl(fullUrl);
            }

            return fullUrl;
          }
        }
        return null;
      } catch (error) {
        console.error("Error restoring current image URL:", error);
        return null;
      }
    },
    [setSelectedImageUrl]
  );

  // Fetch base image URL for cropping - Now this can depend on restoreCurrentImageUrl
  const getBaseImageUrlForCropping = useCallback(
    async (imageId: string) => {
      if (!imageId) return null;

      try {
        const response = await fetch(
          `${CONFIG.API_BASE_URL}/api/images/${imageId}/edit`
        );

        if (response.ok) {
          const data = await response.json();
          if (data.status === "success" && data.data) {
            // Log the complete response for debugging
            // console.log("Edit response data:", data.data);

            let sourceImageUrl;

            // If label is "Crop", use baseImageUrl (to maintain quality)
            // Otherwise, use currentImageUrl (for other edits like background removal)
            if (data.data.label === "Crop" && data.data.baseImageUrl) {
              sourceImageUrl = data.data.baseImageUrl;
              // console.log(
              //   "Using baseImageUrl for cropping (entity has Crop label):",
              //   sourceImageUrl
              // );
            } else {
              sourceImageUrl = data.data.currentImageUrl;
              // console.log(
              //   "Using currentImageUrl for cropping (entity has non-Crop label):",
              //   sourceImageUrl
              // );
            }

            // Create full URL with API base
            const fullSourceUrl = `${CONFIG.API_BASE_URL}/api/images/${sourceImageUrl}`;

            // Update the selected image URL to use this source image
            setSelectedImageUrl(fullSourceUrl);
            return fullSourceUrl;
          } else {
            console.warn(
              "Invalid response format or missing data in /edit endpoint"
            );
            return await restoreCurrentImageUrl(imageId, false); // Pass flag to not update state
          }
        }
        return null;
      } catch (error) {
        console.error("Error getting base image URL:", error);
        return null;
      }
    },
    [restoreCurrentImageUrl, setSelectedImageUrl]
  );

  // Optimized setIsCropping function
  const setIsCropping = useCallback(
    (value: boolean) => {
      // Only proceed if there's a change in state
      if (value === isCropping) return;

      // console.log(`Changing crop mode from ${isCropping} to ${value}`);

      if (value && selectedImageId) {
        // Entering crop mode - use getBaseImageUrlForCropping
        setIsCroppingState(true);
        getBaseImageUrlForCropping(selectedImageId).catch(console.error);
      } else if (!value && selectedImageId) {
        // Exiting crop mode - restore current image URL
        setIsCroppingState(false);


        // Ensure we restore to the latest image version
        restoreCurrentImageUrl(selectedImageId)
          .then(() => {
            // console.log(
            //   "Restored to current image URL after exiting crop mode"
            // );
            // Force a refresh of the image list after small delay
            setTimeout(() => refreshImages(), 100);
          })
          .catch(console.error);
      } else {
        // Just set the state if no image selected
        setIsCroppingState(value);
      }
    },
    [
      isCropping,
      selectedImageId,
      getBaseImageUrlForCropping,
      restoreCurrentImageUrl,
      refreshImages,
    ]
  );

  const getCropAspectRatio = useCallback(async () => {
    if (!selectedImageId) return null;
    
    try {
      const response = await fetch(`${CONFIG.API_BASE_URL}/api/images/${selectedImageId}/edit`);
      if (response.ok) {
        const data = await response.json();
        if (data.status === "success" && data.data && data.data.crop) {
          const { width, height } = data.data.crop;
          if (width > 0 && height > 0) {
            return width / height;
          }
        }
      }
      return null;
    } catch (error) {
      console.error("Error getting crop aspect ratio:", error);
      return null;
    }
  }, [selectedImageId]);

  const value = {
    uploadedFile,
    setUploadedFile,
    selectedImageUrl,
    setSelectedImageUrl,
    croppedImageUrl,
    setCroppedImageUrl,
    selectedImageId,
    setSelectedImageId,
    uploadedImageCount,
    setUploadedImageCount,
    isCropping,
    setIsCropping,
    isLoadingCropData,
    setIsLoadingCropData,
    refreshImages,
    getFullImageUrl,
    restoreCurrentImageUrl,
    getBaseImageUrlForCropping,
    getCropAspectRatio,
  };

  return (
    <UploadContext.Provider value={value}>{children}</UploadContext.Provider>
  );
};

export const useUpload = () => {
  const context = useContext(UploadContext);
  if (context === undefined) {
    throw new Error("useUpload must be used within an UploadProvider");
  }
  return context;
};
