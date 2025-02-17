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
} from "@/components/ui/dialog";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  DialogClose,
  DialogClose as DialogCloseComponent,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

export const BackgroundRemover = () => {
  const { uploadedFile } = useUpload();
  const { toast } = useToast();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [selectedPoints, setSelectedPoints] = useState<
    Array<{ x: number; y: number }>
  >([]);
  const [step, setStep] = useState<"select" | "manual">("select");
  const imageRef = useRef<HTMLImageElement>(null);

  // Debug logging
  useEffect(() => {
    console.log("Dialog state:", isDialogOpen);
    console.log("Current step:", step);
    console.log("Selected points:", selectedPoints);
  }, [isDialogOpen, step, selectedPoints]);

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
    console.log("Handling background removal:", type);
    if (!uploadedFile) {
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
        formData.append("image", uploadedFile);
        formData.append("backgroundOption", "white");
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
        setIsDialogOpen(false);
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

  const handleDialogClose = () => {
    console.log("Closing dialog");
    setIsDialogOpen(false);
    setSelectedPoints([]);
    setStep("select");
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
                          Automatically remove background using AI
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
                        {uploadedFile && (
                          <img
                            ref={imageRef}
                            src={URL.createObjectURL(uploadedFile)}
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
                              left: `${point.x}px`,
                              top: `${point.y}px`,
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
