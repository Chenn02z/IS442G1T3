import React, { useEffect, useState } from "react";
import { Sheet, SheetContent, SheetTrigger, SheetClose } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Crop, Scan } from "lucide-react";

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
  setIsCropping: (cropping: boolean) => void;
  setSelectedAspectRatio: (ratio: number | null) => void;
}

const CropSidebar: React.FC<CropSidebarProps> = ({ setIsCropping, setSelectedAspectRatio }) => {
  // default to "freeform"
  const [selectedRatio, setSelectedRatio] = useState<string>("freeform");

  useEffect(() => {
    // Prefer the stored ratio identifier
    if (typeof window !== "undefined") {
      const savedRatio = localStorage.getItem("selectedRatio");
      if (savedRatio) {
        setSelectedRatio(savedRatio);
        const matching = aspectRatios.find((r) => r.value === savedRatio);
        if (matching) {
          setSelectedAspectRatio(matching.aspectRatio);
        }
      }
    }
  }, [setSelectedAspectRatio]);

  const handleSidebarOpen = (isOpen: boolean) => {
    if (isOpen) {
      setIsCropping(true);
      // Optionally, if you want to force freeform when there's cropBoxData, you can do:
      if (localStorage.getItem("cropBoxData")) {
        setSelectedRatio("freeform");
        setSelectedAspectRatio(null);
        localStorage.setItem("selectedRatio", "freeform");
      }
    }
  };

  return (
    <Sheet onOpenChange={handleSidebarOpen}>
      <SheetTrigger asChild>
        <Crop className="w-5 h-5" />
      </SheetTrigger>
      <SheetContent side="left" className="w-80 p-4 flex flex-col justify-between">
        <div>
          <h2 className="text-lg font-semibold mb-4">Crop</h2>
          <ScrollArea className="h-[400px]">
            <div className="grid grid-cols-3 gap-4">
              {aspectRatios.map((ratio) => (
                <Button
                  key={ratio.value}
                  variant={selectedRatio === ratio.value ? "default" : "outline"}
                  className="w-20 h-20 flex flex-col items-center justify-center space-y-2 border rounded-md"
                  onClick={() => {
                    setSelectedRatio(ratio.value);
                    setSelectedAspectRatio(ratio.aspectRatio);
                    // store the selected ratio as a string
                    localStorage.setItem("selectedRatio", ratio.value);
                  }}
                >
                  {ratio.value === "freeform" ? (
                    <Scan className="text-gray-500 w-8 h-8" />
                  ) : (
                    <div className={`border border-gray-500 ${ratio.boxClass}`} />
                  )}
                  <span className="text-xs">{ratio.label}</span>
                </Button>
              ))}
            </div>
          </ScrollArea>
        </div>
        <div className="flex justify-between mt-4">
          <SheetClose asChild>
            <Button variant="outline">Cancel</Button>
          </SheetClose>
          <Button variant="default">Done</Button>
        </div>
      </SheetContent>
    </Sheet>
  );
};

export default CropSidebar;
