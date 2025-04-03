import React, { useEffect, useState } from "react";
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Crop, Scan } from "lucide-react";
import { useUpload } from "@/context/UploadContext";

// for sidebar menu and CSS values for box in button
const aspectRatios = [
  { label: "Freeform", value: "freeform", aspectRatio: null, boxClass: "" },
  { label: "1:1", value: "1-1", aspectRatio: 1, boxClass: "w-10 h-10" },
  { label: "16:9", value: "16-9", aspectRatio: 16 / 9, boxClass: "w-14 h-8" },
  { label: "9:16", value: "9-16", aspectRatio: 9 / 16, boxClass: "w-8 h-14" },
  { label: "5:4", value: "5-4", aspectRatio: 5 / 4, boxClass: "w-12 h-10" },
  { label: "4:5", value: "4-5", aspectRatio: 4 / 5, boxClass: "w-10 h-12" },
  { label: "4:3", value: "4-3", aspectRatio: 4 / 3, boxClass: "w-12 h-9" },
  { label: "3:4", value: "3-4", aspectRatio: 3 / 4, boxClass: "w-9 h-12" },
  { label: "3:2", value: "3-2", aspectRatio: 3 / 2, boxClass: "w-12 h-8" },
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
  const {
    selectedImageId,
    isCropping,
    restoreCurrentImageUrl,
  } = useUpload();

  // Reduced debug logs to prevent excessive console output
  useEffect(() => {
    if (isOpen || isCropping) {
      console.log("CropSidebar state:", { isOpen, selectedRatio, isCropping });
    }
  }, [isOpen, selectedRatio, isCropping]);

  // Handle sheet open/close
  const handleSheetChange = (open: boolean) => {
    setIsOpen(open);

    if (open && selectedImageId) {
      // When opening, set crop mode and use base image
      setIsCropping(true);
      setSelectedRatio("freeform");
      setSelectedAspectRatio(null);
    }
    // When closing, we don't change isCropping here - it's handled by handleDone/handleCancel
  };

  // Handle ratio selection without closing the sheet
  const handleRatioSelect = (ratio: (typeof aspectRatios)[0]) => {
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

  return (
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
  );
};

export default CropSidebar;
