// src/context/UploadContext.tsx
/**
 * UploadContext - Central state management for image handling operations
 * 
 * This context provides state management for:
 * - Image selection, upload, and display
 * - Cropping operations
 * - Background removal
 * - Image metadata tracking
 */
import React, { createContext, useState, useContext, useCallback } from "react";

interface UploadContextProps {
  // File management
  uploadedFile: File | null;
  setUploadedFile: (file: File | null) => void;
  
  // Image selection and display
  selectedImageUrl: string | null;
  setSelectedImageUrl: (url: string | null) => void;
  selectedImageId: string | null;
  setSelectedImageId: (id: string | null) => void;
  
  // Cropped image state
  croppedImageUrl: string | null;
  setCroppedImageUrl: (url: string | null) => void;
  
  // Image collection management
  uploadedImageCount: number;
  setUploadedImageCount: (count: number) => void;
  refreshImages: () => void;
  
  // UI state for operations
  isCropping: boolean;
  setIsCropping: (isCropping: boolean) => void;
  isLoadingCropData: boolean;
  setIsLoadingCropData: (isLoading: boolean) => void;
}

// Create context with undefined initial value
const UploadContext = createContext<UploadContextProps | undefined>(undefined);

/**
 * Provider component that wraps the application to make upload state available
 */
export const UploadProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // File state
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  
  // Image selection state
  const [selectedImageUrl, setSelectedImageUrl] = useState<string | null>(null);
  const [selectedImageId, setSelectedImageId] = useState<string | null>(null);
  
  // Cropping state
  const [croppedImageUrl, setCroppedImageUrl] = useState<string | null>(null);
  const [isCropping, setIsCropping] = useState<boolean>(false);
  const [isLoadingCropData, setIsLoadingCropData] = useState<boolean>(false);
  
  // Collection management
  const [uploadedImageCount, setUploadedImageCount] = useState<number>(0);
  
  /**
   * Triggers a refresh of the images list
   * Works by incrementing a counter that PhotoList watches
   */
  const refreshImages = useCallback(() => {
    setUploadedImageCount((prev) => prev + 1);
  }, []);

  return (
    <UploadContext.Provider value={{
      uploadedFile, setUploadedFile,
      selectedImageUrl, setSelectedImageUrl,
      croppedImageUrl, setCroppedImageUrl,
      uploadedImageCount, setUploadedImageCount,
      selectedImageId, setSelectedImageId,
      isCropping, setIsCropping,
      isLoadingCropData, setIsLoadingCropData,
      refreshImages
    }}>
      {children}
    </UploadContext.Provider>
  );
};

/**
 * Custom hook for accessing the upload context
 * @returns The upload context with all state and methods
 * @throws Error if used outside of UploadProvider
 */
export const useUpload = () => {
  const context = useContext(UploadContext);
  if (context === undefined) {
    throw new Error("useUpload must be used within an UploadProvider");
  }
  return context;
};
