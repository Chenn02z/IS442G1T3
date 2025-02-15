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
  // const { uploadedFile } = useUpload();
  // const { toast } = useToast();

  // // actual handler function
  // const handleBackgroundRemoval = async () => {
  //   if (!uploadedFile) {
  //     toast({
  //       title: "No uploaded file",
  //       description: "Please upload a file first.",
  //     });
  //     return;
  //   }

  //   // Prepare the file to be sent to the backend
  //   const formData = new FormData();
  //   formData.append("image", uploadedFile);
  //   formData.append("backgroundOption", "white"); // to revisit logic

  //   try {
  //     // const response = await fetch(CONFIG.API_BASE_URL + "/api/images/upload", {
  //     //   method: "POST",
  //     //   body: formData,
  //     // });
  //     const response = await fetch(
  //       CONFIG.API_BASE_URL + "/api/background-removal/remove",
  //       {
  //         method: "POST",
  //         body: formData,
  //         // mode: "cors",  // Enables CORS requests
  //         // headers: {
  //         //   "Accept": "application/json",  // Ensure the response is JSON
  //         //   "Access-Control-Allow-Origin": "*",  // Allow any domain (use a specific one in production)
  //         // }
  //       }
  //     );
  //     if (!response.ok) {
  //       throw new Error("Failed to send image for background removal");
  //     }
  //     toast({
  //       title: "New Image successfully saved to deskstop",
  //     });
  //   } catch (error: any) {
  //     console.error(error);
  //     toast({
  //       title: "Error sending image",
  //       description: error.message,
  //     });
  //   }
  // };

  // // tester function to check if file can be seen from the sidebar
  // const checkFile = () => {
  //   if (!uploadedFile) {
  //     toast({
  //       title: "No file",
  //       description: "Something wrong with context api",
  //     });
  //   } else {
  //     toast({
  //       title: "File is here",
  //       description: "Context api is working",
  //     });
  //   }
  // };
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
