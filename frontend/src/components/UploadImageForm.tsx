"use client";
import { Card, CardContent } from "@/components/ui/card";
import { useState } from "react";
import { UploadCloud } from 'lucide-react';
import { CONFIG } from "../../config";
import { useToast } from "@/hooks/use-toast";
import { useUpload } from "@/context/UploadContext";

// Import Statements for Display & Crop
import DisplayImage from "./DisplayImage";
import CropImage from "./CropImage";
import DownloadButton from "./DownloadButton";
import { UUID_LOOKUP_KEY } from "@/app/page";

const UploadImageForm = ({
  isCropping,
  setIsCropping,
  selectedAspectRatio,
}: {
  isCropping: boolean;
  setIsCropping: (cropping: boolean) => void;
  selectedAspectRatio: number | null;
}) => {
  const { toast } = useToast();
  const { setUploadedFiles,selectedImageUrl, setSelectedImageUrl, uploadedImageCount, refreshImages } = useUpload();
  const [uploading, setUploading] = useState<boolean>(false);
  // const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [uploadedImageUrls, setUploadedImageUrls] = useState<string[]>([]);
  const [croppedImageUrl, setCroppedImageUrl] = useState<string | null>(null); // handle crop version of image
  const [error, setError] = useState<string | null>(null);

  const handleUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    setError(null);
    const files = event.target.files ? Array.from(event.target.files) : [];
    if (files.length === 0) {
      setError("Please select at least one image.");
      return;
    }

    localStorage.removeItem("cropBoxData");
    // set the uploaded file to curr file as well
    // setSelectedFiles(files);
    setUploadedFiles(files);
    setUploading(true);
    setCroppedImageUrl(null);
    const newUploadedUrls: string[] = [];
  
    try {
      // TODO: add typing for response
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

        const data = await response.json()

        const fullImageUrl = CONFIG.API_BASE_URL + "/" + data.savedFilePath;
        const optimizedUrl = `/_next/image?url=${fullImageUrl}&w=640&q=75`;
        newUploadedUrls.push(optimizedUrl);
      }
      setUploadedImageUrls(newUploadedUrls);

      if (!selectedImageUrl && newUploadedUrls.length > 0) {
        setSelectedImageUrl(newUploadedUrls[0]);
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

  // TODO: replace Dummy function for sending x,y,height,width to backend
  const handleDummySave = () => {
    const cropBoxDataStr = localStorage.getItem("cropBoxData");
    if (!cropBoxDataStr) {
      console.log("No crop data found in localStorage.");
      return;
    }
    const cropBoxData = JSON.parse(cropBoxDataStr);
    const payload = {
      cropBoxData, 
    };
    console.log("Payload that would be sent to the backend:", payload);
  };

  return (
    <Card className="hover:cursor-pointer hover:bg-secondary hover:border-primary transition-all ease-in-out">
      <CardContent className="flex flex-col h-full items-center justify-center px-2 py-24 text-xs">
        {/* Before image upload */}
        {(uploadedImageCount === 0) && (
          <>
            <input
              type="file"
              accept="image/jpeg, image/png"
              className="hidden"
              id="file-upload"
              multiple
              onChange={handleUpload}
            />
            <label
              htmlFor="file-upload"
              className="cursor-pointer flex flex-col items-center gap-2"
            >
              <UploadCloud className="h-10 w-10 text-gray-500" />
              <p className="text-gray-700 text-sm">
                {uploadedImageUrls.length > 0 ? `${uploadedImageUrls.length} files have been uploaded` : "Click to upload an Image"}
              </p>
              <p className="text-muted-foreground text-xs">
                Supported formats: .jpeg, .png
              </p>
            </label>
          </>
        )}
        {/* After image upload */}
        {(selectedImageUrl) && (
          <>
            {isCropping ? (
              <CropImage
                imageUrl={selectedImageUrl}
                aspectRatio={selectedAspectRatio}
                isCropping={isCropping}
                onCropComplete={(croppedImage) => {
                  setCroppedImageUrl(croppedImage);
                  setIsCropping(false);
                  toast({
                    title: "Image Cropped Successfully!",
                  });
                }}
              />
            ) : (
              <DisplayImage imageUrl={croppedImageUrl || selectedImageUrl} />
            )}
            {/* dummy button to check function */}
            {!isCropping && (
              <div className="flex flex-col items-center mt-4 space-y-2">
                <button
                  onClick={handleDummySave}
                  className="px-4 py-2 bg-primary text-white rounded"
                >
                  Save Changes
                </button>
                <DownloadButton croppedImageUrl={croppedImageUrl} />
              </div>
            )}
          </>
        )}
        {error && <p className="text-red-500 text-sm mt-4">{error}</p>}
      </CardContent>
    </Card>
  );
};
export default UploadImageForm;