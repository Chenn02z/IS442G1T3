"use client";
import { useState } from "react";
import { CONFIG } from "../../config";
import { UUID_LOOKUP_KEY } from "@/app/page";
import { useUpload } from "@/context/UploadContext";
import { useToast } from "@/hooks/use-toast";

export const useImageUploadHandler = () => {
  const { setUploadedFiles, selectedImageUrl, setSelectedImageUrl, refreshImages,setSelectedImageId,selectedImageId } = useUpload();
  const { toast } = useToast();

  // Fix: Define states properly
  const [uploading, setUploading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [croppedImageUrl, setCroppedImageUrl] = useState<string | null>(null);

  const handleUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    setError(null);
    const files = event.target.files ? Array.from(event.target.files) : [];
    if (files.length === 0) {
      setError("Please select at least one image.");
      return;
    }

    localStorage.removeItem("cropBoxData");
    setUploadedFiles(files);
    setUploading(true);
    setCroppedImageUrl(null);
    const newUploadedUrls: string[] = [];
    const newUploadedIds: string[] = [];

    try {
      for (const file of files) {
        const formData = new FormData();
        formData.append("imageFile", file);
        formData.append("userId", localStorage.getItem(UUID_LOOKUP_KEY) ?? "");

        const response = await fetch(CONFIG.API_BASE_URL + "/api/images/upload", {
          method: "POST",
          body: formData,
        });

        if (!response.ok) {
          throw new Error("Failed to upload image.");
        }

        const data = await response.json();
        const fullImageUrl = CONFIG.API_BASE_URL + "/" + data.savedFilePath;
        newUploadedUrls.push(fullImageUrl);
        newUploadedIds.push(data.imageId);
      }

      console.log("Uploaded URLs:", newUploadedUrls);
      console.log("Uploaded IDs:", newUploadedIds);

      if (!selectedImageUrl && !selectedImageId && newUploadedUrls.length > 0) {
        setSelectedImageUrl(newUploadedUrls[0]);
        setSelectedImageId(newUploadedIds[0]);
      }

      refreshImages();

      toast({
        title: "Image Uploaded Successfully!",
      });
    } catch (error) {
      console.error("Error in Image Upload: ", error);
      setError("Upload failed. Try again.");
      toast({
        title: "Error Uploading Image.",
      });
    } finally {
      setUploading(false);
    }
  };

  return { handleUpload, uploading, error, croppedImageUrl };
};
