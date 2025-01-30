"use client";
import { Card, CardContent } from "./ui/card";
import { useState } from "react";
import { UploadCloud } from "lucide-react";
import { CONFIG } from "../../config";
import Image from "next/image";
import { useToast } from "@/hooks/use-toast";

const UploadImageForm = () => {
  const { toast } = useToast()

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState<boolean>(false);
  const [uploadedImageUrl, setUploadedImageUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    setError(null);
    const file = event.target.files?.[0];
    if (!file) {
      setError("Please select an image.");
      return;
    }
    setSelectedFile(file);
    setUploading(true);
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
      setUploadedImageUrl(CONFIG.API_BASE_URL + "/" + data.savedFilePath);
      toast({
        title: "Image Uploaded Successfully!",
      });
    } catch (error) {
      console.error("Error in Image Upload: " + error);
      setError("Upload failed. Try again.");
      toast({
        title: "Error Uploading Image.",
      });
    } finally {
      setUploading(false);
    }
  };
  return (
    <Card className="hover:cursor-pointer hover:bg-secondary hover:border-primary transition-all ease-in-out" >
      <CardContent className="flex flex-col h-full items-center justify-center px-2 py-24  text-xs ">
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

        {/* TODO: Change to spinner */}
        {uploading && "Uploading Image..."}

        {error && <p className="text-red-500 text-sm">{error}</p>}

        {uploadedImageUrl && (
          <div className="flex flex-col items-center space-y-2">
            <Image
              src={uploadedImageUrl}
              alt="Uploaded preview"
              height={500}
              width={500}
              className="object-cover rounded-md shadow-md"
            />
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default UploadImageForm;
