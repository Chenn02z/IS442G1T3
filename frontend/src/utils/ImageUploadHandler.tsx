"use client";
import { useState } from "react";
import { CONFIG } from "../../config";
import { UUID_LOOKUP_KEY } from "@/app/page";
import { useUpload } from "@/context/UploadContext";
import { useToast } from "@/hooks/use-toast";

export const useImageUploadHandler = () => {
  const { setUploadedFile, selectedImageUrl, setSelectedImageUrl, refreshImages, setSelectedImageId, selectedImageId } = useUpload();
  const { toast } = useToast();
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
    setUploadedFile(files[0]);
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
        const fullImageUrl = `${CONFIG.API_BASE_URL}/api/images/${data.savedFilePath}`;
        newUploadedUrls.push(fullImageUrl);
        newUploadedIds.push(data.imageId);
      }
      
      // Only proceed if we have successfully uploaded images
      if (newUploadedUrls.length > 0) {
        console.log("Upload successful, new image URLs:", newUploadedUrls);
        
        // Simplify the approach: 
        // 1. Refresh images first
        // 2. Then set the selected image with a small delay
        
        // First, wait for backend processing
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // Refresh the image list
        await refreshImages();
        
        // Wait a moment for the refresh to complete
        await new Promise(resolve => setTimeout(resolve, 500));
        
        // Now set the selected image
        const imageUrl = newUploadedUrls[0];
        const imageId = newUploadedIds[0];
        
        // Set the selection
        setSelectedImageUrl(imageUrl);
        setSelectedImageId(imageId);
        
        // One final refresh after selection to ensure everything is in sync
        setTimeout(() => {
          refreshImages();
        }, 500);
      }

      toast({
        title: "Image Uploaded Successfully!",
        description: "Your image has been uploaded and is now selected."
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
