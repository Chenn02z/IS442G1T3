import React, { useState } from "react";
import { ThemeModeToggle } from "@/components/ThemeModeToggle";
import CropSidebar from "@/components/CropSideBar";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { BackgroundRemover } from "./BackgroundRemover";

const SideBar = ({
  setIsCropping,
  setSelectedAspectRatio, // ✅ Accept as a prop
}: {
  setIsCropping: (cropping: boolean) => void;
  setSelectedAspectRatio: (ratio: number | null) => void;
}) => {
  return (
    <div className="fixed flex flex-col space-y-2 w-fit h-full p-2 items-center border-2 border-r-lightgray-500">
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
