import React from "react";
import { Crop } from "lucide-react";
import { SquareX } from "lucide-react";
import { ThemeModeToggle } from "@/components/ThemeModeToggle";

const SideBar = () => {
  return (
    <div className="fixed flex flex-col space-y-2 w-fit h-full p-2 items-center border-2 border-r-lightgray-500">
      <ThemeModeToggle />
      <p className="text-xs pt-3 pb-1 font-semibold">Edit:</p>
      <div className="border border-1 p-1 rounded-md cursor-pointer">
        <Crop />
      </div>
      <div className="border border-1 p-1 rounded-md cursor-pointer">
        <SquareX />
      </div>
    </div>
  );
};

export default SideBar;
