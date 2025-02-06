// src/context/UploadContext.tsx
import React, { createContext, useState, useContext } from "react";

interface UploadContextProps {
  uploadedFile: File | null;
  setUploadedFile: (file: File | null) => void;
}

const UploadContext = createContext<UploadContextProps | undefined>(undefined);

export const UploadProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  return (
    <UploadContext.Provider value={{ uploadedFile, setUploadedFile }}>
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
