/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import { useState, useEffect, useRef } from "react";
import { SquareX } from "lucide-react";
import { useUpload } from "@/context/UploadContext";
import { useToast } from "@/hooks/use-toast";
import { CONFIG } from "../../config";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogClose,
} from "@/components/ui/dialog";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";

export const BackgroundRemover = () => {
  const {
    uploadedFile,
    setUploadedFile,
    selectedImageId,
    selectedImageUrl,
    setSelectedImageUrl,
    refreshImages,
    getFullImageUrl,
    restoreCurrentImageUrl,
    setIsCropping,
    isCropping,
  } = useUpload();
  const [workingFile, setWorkingFile] = useState<File | null>(null);
  const { toast } = useToast();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [selectedPoints, setSelectedPoints] = useState<
    Array<{ x: number; y: number }>
  >([]);
  const [step, setStep] = useState<"select" | "manual">("select");
  const imageRef = useRef<HTMLImageElement>(null);
  const [displayImageUrl, setDisplayImageUrl] = useState<string | null>(null);

  // Initialize workingFile when dialog opens
  useEffect(() => {
    if (isDialogOpen && uploadedFile) {
      setWorkingFile(uploadedFile);
    }
  }, [isDialogOpen, uploadedFile]);

  useEffect(() => {
    if (selectedImageUrl) {
      setDisplayImageUrl(getFullImageUrl(selectedImageUrl));
    }
  }, [selectedImageUrl, getFullImageUrl]);

  const handleImageClick = (e: React.MouseEvent<HTMLImageElement>) => {
    if (!imageRef.current) return;

    const rect = imageRef.current.getBoundingClientRect();
    const image = imageRef.current;

    // Calculate the scaling factors
    const scaleX = image.naturalWidth / rect.width;
    const scaleY = image.naturalHeight / rect.height;

    // Get click coordinates relative to the displayed image
    const displayX = e.clientX - rect.left;
    const displayY = e.clientY - rect.top;

    // Convert to actual image coordinates
    const actualX = Math.round(displayX * scaleX);
    const actualY = Math.round(displayY * scaleY);

    setSelectedPoints((prev) => [...prev, { x: actualX, y: actualY }]);
  };

  const handleBackgroundRemoval = async (type: string) => {
    if (!selectedImageUrl) {
      toast({
        title: "No uploaded file",
        description: "Please upload a file first.",
      });
      return;
    }

    if (type === "manual") {
      setStep("manual");
    } else {
      try {
        // Show loading toast
        toast({
          title: "Processing image",
          description: "Removing background automatically...",
        });

        const response = await fetch(
          `${CONFIG.API_BASE_URL}/api/background-removal/${selectedImageId}/auto`,
          {
            method: "POST",
          }
        );
        if (!response.ok) {
          throw new Error("Failed to send image for background removal");
        }

        const processedImage = await response.json();
        console.log("Auto background removal response:", processedImage);

        if (processedImage && processedImage.currentImageUrl) {
          // Ensure we handle the URL correctly
          const newImageUrl = `${CONFIG.API_BASE_URL}/api/images/${processedImage.currentImageUrl}`;
          console.log(
            "New image URL after auto background removal:",
            newImageUrl
          );

          // Update UI with new image
          setSelectedImageUrl(newImageUrl);
          refreshImages();

          // Make sure we're using the latest image state
          await restoreCurrentImageUrl(selectedImageUrl);

          toast({
            title: "Background removed successfully.",
          });
        } else {
          throw new Error("Invalid response from server");
        }

        // Close the dialog
        setIsDialogOpen(false);
      } catch (error: any) {
        console.error("Auto background removal error:", error);
        toast({
          title: "Error processing image",
          description: error.message,
          variant: "destructive",
        });
      }
    }
  };

  const floodFill = async () => {
    if (!selectedImageId || selectedPoints.length === 0) {
      toast({
        title: "No points selected",
        description: "Please select at least one point on the background.",
      });
      return;
    }

    try {
      // Show loading toast
      toast({
        title: "Processing image",
        description: "Removing background manually...",
      });

      const response = await fetch(
        `${CONFIG.API_BASE_URL}/api/images/${selectedImageId}/remove-background`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
          body: `seedPoints=${JSON.stringify(selectedPoints)}&tolerance=30`,
        }
      );

      if (!response.ok) {
        throw new Error("Failed to process image");
      }

      const processedImage = await response.json();
      console.log("Manual background removal response:", processedImage);

      if (processedImage && processedImage.currentImageUrl) {
        // Ensure we handle the URL correctly
        const newImageUrl = `${CONFIG.API_BASE_URL}/api/images/${processedImage.currentImageUrl}`;
        console.log(
          "New image URL after manual background removal:",
          newImageUrl
        );

        // Update UI with new image URL
        setSelectedImageUrl(newImageUrl);
        refreshImages();

        // Make sure we're using the latest image state
        await restoreCurrentImageUrl(selectedImageId);

        toast({
          title: "Background removed successfully",
        });

        // Close the dialog after successful background removal
        handleDone();
      } else {
        throw new Error("Invalid response from server");
      }

      setSelectedPoints([]);
    } catch (error: any) {
      console.error("Manual background removal error:", error);
      toast({
        title: "Error processing image",
        description: error.message,
        variant: "destructive",
      });
    }
  };

  const handleDialogClose = (open: boolean) => {
    // If the dialog is being closed and it wasn't closed programmatically
    if (!open) {
      // Reset the state
      setSelectedPoints([]);
      setStep("select");
      setWorkingFile(null);
      setIsDialogOpen(false);
    }
  };

  const handleDone = () => {
    if (workingFile) {
      setUploadedFile(workingFile);
      toast({
        title: "Changes saved successfully",
      });
    }
    handleDialogClose(false);
  };

  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <Button
            variant="ghost"
            className="h-10 w-10 p-0 flex items-center justify-center"
            onClick={() => {
              if (isCropping) {
                setIsCropping(false);
                setTimeout(() => {
                  setIsDialogOpen(true);
                }, 100);
              } else {
                setIsDialogOpen(true);
              }
            }}
            disabled={!selectedImageUrl}
          >
            <SquareX className="h-5 w-5" />
          </Button>
        </TooltipTrigger>
        <TooltipContent>
          <p>Remove image background</p>
        </TooltipContent>
      </Tooltip>

      <Dialog open={isDialogOpen} onOpenChange={handleDialogClose}>
        <DialogContent className="max-w-4xl">
          {step === "select" ? (
            <>
              <DialogHeader>
                <DialogTitle>Remove Background</DialogTitle>
                <DialogDescription>
                  Choose a method to remove the background
                </DialogDescription>
              </DialogHeader>
              <div className="grid grid-cols-2 gap-4">
                <Button
                  variant="outline"
                  className="flex flex-col gap-2 h-auto p-6"
                  onClick={() => handleBackgroundRemoval("auto")}
                >
                  <div className="text-lg font-semibold">Auto</div>
                  <div className="text-sm text-muted-foreground">
                    Automatically remove background using OpenCV
                  </div>
                </Button>
                <Button
                  variant="outline"
                  className="flex flex-col gap-2 h-auto p-6"
                  onClick={() => handleBackgroundRemoval("manual")}
                >
                  <div className="text-lg font-semibold">Manual</div>
                  <div className="text-sm text-muted-foreground">
                    Select points to remove background
                  </div>
                </Button>
              </div>
            </>
          ) : (
            <>
              <DialogHeader>
                <DialogTitle>Manual Background Removal</DialogTitle>
                <DialogDescription>
                  Click on the image to select points for background removal
                </DialogDescription>
              </DialogHeader>
              <div className="flex flex-col gap-4">
                <div className="relative w-full">
                  {displayImageUrl && (
                    <img
                      ref={imageRef}
                      src={displayImageUrl}
                      alt="Upload preview"
                      className="w-full cursor-crosshair"
                      onClick={handleImageClick}
                    />
                  )}
                  {selectedPoints.map((point, index) => (
                    <div
                      key={index}
                      className="absolute w-2 h-2 bg-red-500 rounded-full transform -translate-x-1 -translate-y-1"
                      style={{
                        left: `${
                          (point.x / (imageRef.current?.naturalWidth ?? 1)) *
                          100
                        }%`,
                        top: `${
                          (point.y / (imageRef.current?.naturalHeight ?? 1)) *
                          100
                        }%`,
                      }}
                    />
                  ))}
                </div>

                <div className="grid grid-cols-2 gap-4 w-full">
                  <Button
                    variant="secondary"
                    onClick={() => setSelectedPoints([])}
                  >
                    Clear Points
                  </Button>
                  <Button
                    variant="default"
                    onClick={floodFill}
                    disabled={selectedPoints.length === 0}
                  >
                    Process Points
                  </Button>
                </div>
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>
    </TooltipProvider>
  );
};
