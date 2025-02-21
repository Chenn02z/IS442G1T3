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
  DialogClose
} from "@/components/ui/dialog";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";

export const BackgroundRemover = () => {
  const { uploadedFile, setUploadedFile } = useUpload();
  const [workingFile, setWorkingFile] = useState<File | null>(null);
  const { toast } = useToast();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [selectedPoints, setSelectedPoints] = useState<
    Array<{ x: number; y: number }>
  >([]);
  const [step, setStep] = useState<"select" | "manual">("select");
  const imageRef = useRef<HTMLImageElement>(null);

  // Initialize workingFile when dialog opens
  useEffect(() => {
    if (isDialogOpen && uploadedFile) {
      setWorkingFile(uploadedFile);
    }
  }, [isDialogOpen, uploadedFile]);

  // Debug logging
  useEffect(() => {
    // console.log("Dialog state:", isDialogOpen);
    // console.log("Current step:", step);
    // console.log("Selected points:", selectedPoints);
    console.log("Working file:", workingFile);
  }, [isDialogOpen, step, selectedPoints, workingFile]);

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
    // console.log(
    //   `Selected point: display(x=${displayX}, y=${displayY}), actual(x=${actualX}, y=${actualY})`
    // );
  };

  const handleBackgroundRemoval = async (type: string) => {
    console.log("Handling background removal:", type);
    if (!workingFile) {
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
        const formData = new FormData();
        formData.append("image", workingFile);
        formData.append("backgroundOption", "white");
        const response = await fetch(
          CONFIG.API_BASE_URL + "/api/background-removal/cartoonise",
          {
            method: "POST",
            body: formData,
          }
        );
        if (!response.ok) {
          throw new Error("Failed to send image for background removal");
        }

        // Get the processed image as a blob
        const processedImageBlob = await response.blob();

        // Create a new File object from the blob
        const processedFile = new File([processedImageBlob], workingFile.name, {
          type: workingFile.type,
        });

        console.log("Processed file:", processedFile);

        // Update the working file
        setUploadedFile(processedFile);

        toast({
          title: "Background removed successfully. Image saved to desktop.",
        });
        setIsDialogOpen(false);
      } catch (error: any) {
        console.error(error);
        toast({
          title: "Error processing image",
          description: error.message,
        });
      }
    }
  };

  const floodFill = async () => {
    if (!workingFile) {
      toast({
        title: "No uploaded file",
        description: "Please upload a file first.",
      });
      return;
    }

    const formData = new FormData();
    formData.append("file", workingFile);
    formData.append("tolerance", "30");
    console.log(JSON.stringify(selectedPoints));
    formData.append("seedPoints", JSON.stringify(selectedPoints));

    try {
      const response = await fetch(
        `${CONFIG.API_BASE_URL}/api/images/remove-background`,
        {
          method: "POST",
          body: formData,
        }
      );

      if (!response.ok) {
        throw new Error("Failed to process image");
      }

      // Get the processed image as a blob
      const processedImageBlob = await response.blob();

      // Create a new File object from the blob
      const processedFile = new File([processedImageBlob], workingFile.name, {
        type: workingFile.type,
      });

      // Update the working file
      setWorkingFile(processedFile);

      toast({
        title: "Image processed successfully",
      });     

      // Clear selected points but keep the dialog open
      setSelectedPoints([]);
    } catch (error: any) {
      console.error(error);
      toast({
        title: "Error processing image",
        description: error.message,
      });
    }
  };

  const handleDialogClose = () => {
    console.log("Closing dialog");
    setIsDialogOpen(false);
    setSelectedPoints([]);
    setStep("select");
    setWorkingFile(null);
  };

  const handleDone = () => {
    console.log("Done clicked - updating main file");
    if (workingFile) {
      setUploadedFile(workingFile);
      toast({
        title: "Changes saved successfully",
      });
    }
    handleDialogClose();
  };

  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <div className="relative">
            <div
              className="border border-1 p-1 rounded-md cursor-pointer"
              onClick={() => setIsDialogOpen(true)}
            >
              <SquareX className="h-5 w-5" />
            </div>

            <Dialog
              open={isDialogOpen}
              onOpenChange={(open) => {
                console.log("Dialog onOpenChange:", open);
                if (!open) {
                  handleDialogClose();
                }
                setIsDialogOpen(open);
              }}
            >
              <DialogContent className="max-w-[90vw] max-h-[90vh] overflow-auto z-[100]">
                {step === "select" ? (
                  <>
                    <DialogHeader>
                      <DialogTitle>Background Removal</DialogTitle>
                      <DialogDescription>
                        Choose how you want to remove the background
                      </DialogDescription>
                    </DialogHeader>
                    <div className="grid grid-cols-2 gap-4 p-4">
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
                        Click on the image to select points for background
                        removal
                      </DialogDescription>
                    </DialogHeader>
                    <div className="flex flex-col gap-4">
                      <div className="relative w-full">
                        {workingFile && (
                          <img
                            ref={imageRef}
                            src={URL.createObjectURL(workingFile)}
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
                                (point.x /
                                  (imageRef.current?.naturalWidth ?? 1)) *
                                100
                              }%`,
                              top: `${
                                (point.y /
                                  (imageRef.current?.naturalHeight ?? 1)) *
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
                        <Button variant="default" onClick={floodFill}>
                          Process Points
                        </Button>
                      </div>
                    </div>
                  </>
                )}
                <div className="flex justify-between mt-4">
                  <DialogClose asChild>
                    <Button
                      variant="outline"
                      onClick={() => {
                        console.log("Cancel clicked");
                        handleDialogClose();
                      }}
                    >
                      Cancel
                    </Button>
                  </DialogClose>
                  {step === "manual" && (
                    <Button
                      variant="outline"
                      onClick={() => {
                        console.log("Back clicked");
                        setStep("select");
                      }}
                    >
                      Back
                    </Button>
                  )}
                  <DialogClose asChild>
                    <Button variant="default" onClick={handleDone}>
                      Done
                    </Button>
                  </DialogClose>
                </div>
              </DialogContent>
            </Dialog>
          </div>
        </TooltipTrigger>
        <TooltipContent side="right">
          <p>Remove Background</p>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
};
