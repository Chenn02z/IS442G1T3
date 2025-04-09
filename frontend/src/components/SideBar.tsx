import React from "react";
import { ThemeModeToggle } from "@/components/ThemeModeToggle";
import CropSidebar from "@/components/CropSideBar";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { BackgroundRemover } from "./BackgroundRemover";
import { useUpload } from "@/context/UploadContext";

const SideBar = ({
  setSelectedAspectRatio, // ✅ Accept as a prop
}: {
  setSelectedAspectRatio: (ratio: number | null) => void;
}) => {
  const { setIsCropping } = useUpload();
  return (
    <div className="flex flex-col space-y-2 w-full h-full p-2 items-center border-r-2">
      <ThemeModeToggle />
      <p className="text-xs pt-3 pb-1 font-semibold">Edit:</p>
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <div className="border border-1 p-1 rounded-md cursor-pointer">
              <CropSidebar
                setIsCropping={setIsCropping}
                setSelectedAspectRatio={setSelectedAspectRatio}
              />
            </div>
          </TooltipTrigger>
          <TooltipContent side="right">
            <p>Crop Image</p>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
      <BackgroundRemover />
    </div>
  );
};

export default SideBar;
