// src/context/UploadContext.tsx
import React, { createContext, useState, useContext } from "react";

interface UploadContextProps {
  uploadedFiles: File[];
  setUploadedFiles: (files: File[]) => void;
}

const UploadContext = createContext<UploadContextProps | undefined>(undefined);

export const UploadProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [uploadedFiles, setUploadedFiles] = useState<File[]>([]);;
  return (
    <UploadContext.Provider value={{ uploadedFiles, setUploadedFiles }}>
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
