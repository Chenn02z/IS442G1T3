import React from "react";
import { ThemeModeToggle } from "@/components/ThemeModeToggle";
import CropSidebar from "@/components/CropSideBar";
import { BackgroundRemover } from "./BackgroundRemover";
import { useUpload } from "@/context/UploadContext";

const SideBar = ({
  setSelectedAspectRatio,
}: {
  setSelectedAspectRatio: (ratio: number | null) => void;
}) => {
  const { setIsCropping } = useUpload();
  
  return (
    <div className="flex flex-col space-y-2 w-full h-full p-2 items-center border-r-2">
      <ThemeModeToggle />
      <p className="text-xs pt-3 pb-1 font-semibold">Edit:</p>
      <CropSidebar
        setIsCropping={setIsCropping}
        setSelectedAspectRatio={setSelectedAspectRatio}
      />
      <BackgroundRemover />
    </div>
  );
};

export default SideBar;
