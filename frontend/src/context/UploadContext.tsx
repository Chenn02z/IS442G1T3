// src/context/UploadContext.tsx
import React, { createContext, useState, useContext, useCallback } from "react";

interface UploadContextProps {
  uploadedFile: File | null;
  setUploadedFile: (file: File | null) => void;
  selectedImageUrl: string | null;
  setSelectedImageUrl: (url: string | null) => void;
  selectedImageId: string | null;
  setSelectedImageId: (id: string | null) => void;
  croppedImageUrl: string | null;
  setCroppedImageUrl: (url: string | null) => void;
  uploadedImageCount: number;
  setUploadedImageCount: (count: number) => void;
  refreshImages: () => void;
  isLoadingCropData: boolean;
  setIsLoadingCropData: (isLoading: boolean) => void;
}


const UploadContext = createContext<UploadContextProps | undefined>(undefined);

export const UploadProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [selectedImageUrl, setSelectedImageUrl] = useState<string | null>(null);
  const [croppedImageUrl, setCroppedImageUrl] = useState<string | null>(null);
  const [selectedImageId, setSelectedImageId] = useState<string | null>(null);
  const [uploadedImageCount, setUploadedImageCount] = useState<number>(0);
  const [isLoadingCropData, setIsLoadingCropData] = useState<boolean>(false);
  const refreshImages = useCallback(() => {
    setUploadedImageCount((prev) => prev + 1); // Trigger a re-fetch in `PhotoList.tsx`
  }, []);

  return (
    <UploadContext.Provider value={{
      uploadedFile, setUploadedFile,
      selectedImageUrl, setSelectedImageUrl,
      croppedImageUrl, setCroppedImageUrl,
      uploadedImageCount, setUploadedImageCount,
      selectedImageId, setSelectedImageId,
      isLoadingCropData, setIsLoadingCropData,
      refreshImages
    }}>
      {children}
    </UploadContext.Provider>
  );
};

export const useUpload = (): UploadContextProps => {
  const context = useContext(UploadContext);
  if (!context) {
    throw new Error("useUpload must be used within an UploadProvider");
  }
  return context;
};
