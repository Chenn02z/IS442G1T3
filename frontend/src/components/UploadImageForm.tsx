"use client";
import { Card, CardContent } from "@/components/ui/card";
import { useState } from "react";
import { UploadCloud } from "lucide-react";
import { CONFIG } from "../../config";
import { useToast } from "@/hooks/use-toast";

// Import Statements for Display & Crop
import DisplayImage from "./DisplayImage";
import CropImage from "./CropImage";

const UploadImageForm = ({
  isCropping,
  setIsCropping,
  selectedAspectRatio,
}: {
  isCropping: boolean;
  setIsCropping: (cropping: boolean) => void;
  selectedAspectRatio: number | null; //
}) => {
  const { toast } = useToast();


  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState<boolean>(false);
  const [uploadedImageUrl, setUploadedImageUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Added croppedImageUrl state to handle the result of cropping
  const [croppedImageUrl, setCroppedImageUrl] = useState<string | null>(null);

  // Clears previous crop data from localStorage and resets croppedImageUrl when a new file is uploaded
  const handleUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    setError(null);
    const file = event.target.files?.[0];
    if (!file) {
      setError("Please select an image.");
      return;
    }

    localStorage.removeItem("cropBoxData");

    setSelectedFile(file);
    setUploading(true);
    setCroppedImageUrl(null);
    const formData = new FormData();
    formData.append("imageFile", file);
    formData.append("backgroundOption", "white"); // to revisit logic

    try {
        // TODO: add typing for response
      const response = await fetch(CONFIG.API_BASE_URL + "/api/images/upload", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error("Failed to upload image.");
      }

      const data = await response.json();

      const fullImageUrl = CONFIG.API_BASE_URL + "/" + data.savedFilePath;
      const optimizedUrl = `/_next/image?url=${fullImageUrl}&w=640&q=75`;
      setUploadedImageUrl(optimizedUrl);

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
        {!uploadedImageUrl && (
          <>
            <input
              type="file"
              accept="image/jpeg, image/png"
              className="hidden"
              id="file-upload"
              onChange={handleUpload}
            />
            <label
              htmlFor="file-upload"
              className="cursor-pointer flex flex-col items-center gap-2"
            >
              <UploadCloud className="h-10 w-10 text-gray-500" />
              <p className="text-gray-700 text-sm">
                {selectedFile ? selectedFile.name : "Click to upload an Image"}
              </p>
              <p className="text-muted-foreground text-xs">
                Supported formats: .jpeg, .png
              </p>
            </label>
          </>
        )}
        {/* After image upload */}
        {uploadedImageUrl && (
          <>
            {isCropping ? (
              <CropImage
                imageUrl={uploadedImageUrl}
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
            ) : ( <DisplayImage imageUrl={croppedImageUrl || uploadedImageUrl} />
            )}
            {/* dummy button to check function */}
            {!isCropping && (
              <button
                onClick={handleDummySave}
                className="mt-4 px-4 py-2 bg-primary text-white rounded"
              >
                Save Changes
              </button>
            )}
          </>
        )}
        {error && <p className="text-red-500 text-sm mt-4">{error}</p>}
      </CardContent>
    </Card>
  );
};
export default UploadImageForm;


