// src/context/UploadContext.tsx
import React, { createContext, useState, useContext, useCallback } from "react";

interface UploadContextProps {
  uploadedFiles: File[];
  setUploadedFiles: (files: File[]) => void;
  selectedImageUrl: string | null;
  setSelectedImageUrl: (url: string | null) => void;
  selectedImageId: string | null;
  setSelectedImageId: (id: string | null) => void;
  croppedImageUrl: string | null;
  setCroppedImageUrl: (url: string | null) => void;
  uploadedImageCount: number;
  setUploadedImageCount: (count: number) => void;
  refreshImages: () => void;
}

const UploadContext = createContext<UploadContextProps | undefined>(undefined);

export const UploadProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [uploadedFiles, setUploadedFiles] = useState<File[]>([]);;
  const [selectedImageUrl, setSelectedImageUrl] = useState<string | null>(null);
  const [croppedImageUrl, setCroppedImageUrl] = useState<string | null>(null);
  const [selectedImageId, setSelectedImageId] = useState<string | null>(null);
  const [uploadedImageCount, setUploadedImageCount] = useState<number>(0);

  const refreshImages = useCallback(() => {
    setUploadedImageCount((prev) => prev + 1); // Trigger a re-fetch in `PhotoList.tsx`
  }, []);

  return (
    <UploadContext.Provider value={{
      uploadedFiles, setUploadedFiles,
      selectedImageUrl, setSelectedImageUrl,
      croppedImageUrl, setCroppedImageUrl,
      uploadedImageCount, setUploadedImageCount,
      selectedImageId, setSelectedImageId,
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
