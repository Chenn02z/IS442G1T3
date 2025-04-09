import React, { useEffect, useState } from "react";
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Crop, Scan, Settings } from "lucide-react";
import { useUpload } from "@/context/UploadContext";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { CONFIG } from "../../config";

// for sidebar menu and CSS values for box in button
const aspectRatios = [
  { label: "Freeform", value: "freeform", aspectRatio: null, boxClass: "" },
  { label: "1:1", value: "1-1", aspectRatio: 1, boxClass: "w-7 h-7" },
  { label: "16:9", value: "16-9", aspectRatio: 16 / 9, boxClass: "w-16 h-9" },
  { label: "9:16", value: "9-16", aspectRatio: 9 / 16, boxClass: "w-3 h-8" },
  { label: "5:4", value: "5-4", aspectRatio: 5 / 4, boxClass: "w-10 h-8" },
  { label: "4:5", value: "4-5", aspectRatio: 4 / 5, boxClass: "w-6 h-9" },
  { label: "4:3", value: "4-3", aspectRatio: 4 / 3, boxClass: "w-12 h-9" },
  { label: "3:4", value: "3-4", aspectRatio: 3 / 4, boxClass: "w-9 h-12" },
  { label: "3:2", value: "3-2", aspectRatio: 3 / 2, boxClass: "w-12 h-8" },
  // // Singapore passport size - 35mm x 45mm (width x height)
  // { label: "SG Passport", value: "sg-passport", aspectRatio: 35/45, boxClass: "w-9 h-11" },
  // { label: "Custom", value: "custom", aspectRatio: null, boxClass: "" },
];

interface CropSidebarProps {
  setSelectedAspectRatio: (ratio: number | null) => void;
  setIsCropping: (cropping: boolean) => void;
}

const CropSidebar: React.FC<CropSidebarProps> = ({
  setSelectedAspectRatio,
  setIsCropping,
}) => {
  const [selectedRatio, setSelectedRatio] = useState<string>("freeform");
  const [isOpen, setIsOpen] = useState(false);
  const [customDialogOpen, setCustomDialogOpen] = useState(false);
  const [customWidth, setCustomWidth] = useState<string>("1");
  const [customHeight, setCustomHeight] = useState<string>("1");
  
  const {
    selectedImageId,
    isCropping,
    restoreCurrentImageUrl,
    setSelectedImageUrl,
  } = useUpload();

  // Reduced debug logs to prevent excessive console output
  useEffect(() => {
    if (isOpen || isCropping) {
      console.log("CropSidebar state:", { isOpen, selectedRatio, isCropping });
    }
  }, [isOpen, selectedRatio, isCropping]);

  // Handle sheet open/close
  const handleSheetChange = async (open: boolean) => {
    setIsOpen(open);

    if (open && selectedImageId) {
      // When opening, set crop mode
      setIsCropping(true);
      setSelectedRatio("freeform");
      setSelectedAspectRatio(null);
      
      // Fetch latest image version but don't create new URL unless needed
      try {
        console.log("Checking latest image version for cropping...");
        
        // Get the latest version from the API
        const response = await fetch(
          `${CONFIG.API_BASE_URL}/api/images/${selectedImageId}/edit`
        );
        
        if (!response.ok) {
          throw new Error("Failed to fetch latest image version");
        }
        
        // We're just validating we have the latest version
        // No need to create a new URL or update the image URL
        const imageData = await response.json();
        console.log("Latest image version checked:", imageData.version);
        
        // No URL changes needed - we'll just use the existing URL
      } catch (error) {
        console.error("Error checking image version:", error);
      }
    }
    // When closing, we don't change isCropping here - it's handled by handleDone/handleCancel
  };

  // Handle ratio selection without closing the sheet
  const handleRatioSelect = (ratio: (typeof aspectRatios)[0]) => {
    if (ratio.value === "custom") {
      setCustomDialogOpen(true);
      return;
    }
    
    setSelectedRatio(ratio.value);
    setSelectedAspectRatio(ratio.aspectRatio);
    
    // Add analytics/logging if needed
    // console.log(`Aspect ratio changed to ${ratio.value}`);
  };

  // Handle the Done button click
  const handleDone = () => {
    setIsOpen(false);
  };

  // Handle the Cancel button click
  const handleCancel = () => {
    setIsOpen(false);
    setIsCropping(false);

    // Reset to freeform when canceling
    setSelectedRatio("freeform");
    setSelectedAspectRatio(null);

    // Restore to current image URL
    if (selectedImageId) {
      restoreCurrentImageUrl(selectedImageId);
    }
  };

  // If crop mode is turned off externally (e.g., by other components),
  // we should close the panel
  useEffect(() => {
    if (!isCropping && isOpen) {
      setIsOpen(false);
    }
  }, [isCropping, isOpen]);

  // Check for saved aspect ratio in localStorage when component mounts
  // useEffect(() => {
  //   const savedRatio = localStorage.getItem("selectedRatio");
  //   if (savedRatio) {
  //     setSelectedRatio(savedRatio);
  //     const ratio = aspectRatios.find((r) => r.value === savedRatio);
  //     if (ratio) {
  //       setSelectedAspectRatio(ratio.aspectRatio);
  //     }
  //   }
  // }, [setSelectedAspectRatio]);

  const handleCustomRatioSubmit = () => {
    const width = parseFloat(customWidth);
    const height = parseFloat(customHeight);
    
    if (isNaN(width) || isNaN(height) || width <= 0 || height <= 0) {
      // Could add toast notification here
      console.error("Invalid dimensions");
      return;
    }
    
    const ratio = width / height;
    setSelectedRatio("custom");
    setSelectedAspectRatio(ratio);
    setCustomDialogOpen(false);
  };

  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <div className="border border-1 p-1 rounded-md cursor-pointer">
            <Sheet open={isOpen} onOpenChange={handleSheetChange}>
              <SheetTrigger asChild>
                <Crop className="w-5 h-5" />
              </SheetTrigger>
              <SheetContent
                side="left"
                className="w-80 p-4 flex flex-col justify-between"
              >
                <div>
                  <h2 className="text-lg font-semibold mb-4">Crop</h2>
                  <ScrollArea className="h-[400px]">
                    <div className="grid grid-cols-3 gap-4">
                      {aspectRatios.map((ratio) => (
                        <Button
                          key={ratio.value}
                          variant={
                            selectedRatio === ratio.value ? "default" : "outline"
                          }
                          className="w-20 h-20 flex flex-col items-center justify-center space-y-2 border rounded-md"
                          onClick={() => handleRatioSelect(ratio)}
                        >
                          {ratio.value === "freeform" ? (
                            <Scan className="text-gray-500 w-8 h-8" />
                          ) : ratio.value === "custom" ? (
                            <Settings className="text-gray-500 w-8 h-8" />
                          ) : (
                            <div
                              className={`border border-gray-500 ${ratio.boxClass}`}
                            />
                          )}
                          <span className="text-xs">{ratio.label}</span>
                        </Button>
                      ))}
                    </div>
                  </ScrollArea>
                </div>
                <div className="flex justify-between mt-4">
                  <Button variant="outline" onClick={handleCancel}>
                    Cancel
                  </Button>
                  <Button variant="default" onClick={handleDone}>
                    Done
                  </Button>
                </div>
              </SheetContent>
            </Sheet>
          </div>
        </TooltipTrigger>
        <TooltipContent side="right">
          <p>Crop Image</p>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
};

export default CropSidebar;
