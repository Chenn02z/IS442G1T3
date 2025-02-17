"use client";

import { useState, useEffect, useRef } from "react";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

import { SquareX } from "lucide-react";
import { useUpload } from "@/context/UploadContext";
import { useToast } from "@/hooks/use-toast";
import { CONFIG } from "../../config";
import { Dialog, DialogContent } from "@/components/ui/dialog";

export const BackgroundRemover = () => {
  const { uploadedFile } = useUpload();
  const { toast } = useToast();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedPoints, setSelectedPoints] = useState<
    Array<{ x: number; y: number }>
  >([]);
  const imageRef = useRef<HTMLImageElement>(null);

  const handleImageClick = (e: React.MouseEvent<HTMLImageElement>) => {
    if (!imageRef.current) return;

    const rect = imageRef.current.getBoundingClientRect();
    const x = Math.round(e.clientX - rect.left);
    const y = Math.round(e.clientY - rect.top);

    // Add the new point to our selected points
    setSelectedPoints((prev) => [...prev, { x, y }]);
    console.log(`Selected point: x=${x}, y=${y}`);
  };

  const handleBackgroundRemoval = async (type: string) => {
    if (!uploadedFile) {
      toast({
        title: "No uploaded file",
        description: "Please upload a file first.",
      });
      return;
    }

    if (type === "manual") {
      setIsModalOpen(true);
      setSelectedPoints([]); // Reset points when opening modal
      return;
    } else {
      try {
        // Prepare the file to be sent to the backend
        const formData = new FormData();
        formData.append("image", uploadedFile);
        formData.append("backgroundOption", "white"); // to revisit logic
        const response = await fetch(
          CONFIG.API_BASE_URL + "/api/background-removal/remove",
          {
            method: "POST",
            body: formData,
          }
        );
        if (!response.ok) {
          throw new Error("Failed to send image for background removal");
        }
        toast({
          title: "New Image successfully saved to desktop",
        });
      } catch (error: any) {
        console.error(error);
        toast({
          title: "Error sending image",
          description: error.message,
        });
      }
    }
  };

  const floodFill = async () => {
    if (!uploadedFile) {
      toast({
        title: "No uploaded file",
        description: "Please upload a file first.",
      });
      return;
    }

    const formData = new FormData();
    formData.append("file", uploadedFile);
    formData.append("tolerance", "30");
    formData.append("points", JSON.stringify(selectedPoints));

    try {
      const response = await fetch(
        `${CONFIG.API_BASE_URL}/api/background-remover/floodfill`,
        {
          method: "POST",
          body: formData,
        }
      );

      if (!response.ok) {
        throw new Error("Failed to process image");
      }

      // Create a blob URL from the response
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);

      // Update the image display or download the result
      // For example:
      const a = document.createElement("a");
      a.href = url;
      a.download = "processed-image.png";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      toast({
        title: "Image processed successfully",
      });
    } catch (error: any) {
      console.error(error);
      toast({
        title: "Error processing image",
        description: error.message,
      });
    }
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedPoints([]); // Clear points when closing
  };

  const checkFile = () => {
    if (!uploadedFile) {
      toast({
        title: "No file",
        description: "Please upload a file",
      });
      return false;
    } else {
      toast({
        title: "File is here",
        description: "Context api is working",
      });
      return true;
    }
  };

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <div className="border border-1 p-1 rounded-md cursor-pointer">
            <SquareX />
          </div>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={() => handleBackgroundRemoval("auto")}>
            Auto
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => handleBackgroundRemoval("manual")}>
            Manual
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <Dialog modal={true} open={isModalOpen} onOpenChange={handleCloseModal}>
        <DialogContent className="max-w-[90vw] max-h-[90vh] overflow-auto">
          <div className="flex flex-col gap-4">
            <div className="relative w-full">
              {uploadedFile && (
                <img
                  ref={imageRef}
                  src={URL.createObjectURL(uploadedFile)}
                  alt="Upload preview"
                  className="w-full cursor-crosshair"
                  onClick={handleImageClick}
                />
              )}
              {/* Render dots for selected points */}
              {selectedPoints.map((point, index) => (
                <div
                  key={index}
                  className="absolute w-2 h-2 bg-red-500 rounded-full transform -translate-x-1 -translate-y-1"
                  style={{
                    left: `${point.x}px`,
                    top: `${point.y}px`,
                  }}
                />
              ))}
            </div>

            <div className="grid grid-cols-2 gap-4 w-full">
              <button
                className="w-full px-4 py-2 bg-secondary text-black rounded hover:bg-primary"
                onClick={() => setSelectedPoints([])}
              >
                Clear Points
              </button>
              <button
                className="w-full px-4 py-2 bg-primary text-white rounded hover:bg-secondary"
                onClick={() => {
                  // Here you can send the points to your backend
                  floodFill();
                }}
              >
                Process Points
              </button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
};
