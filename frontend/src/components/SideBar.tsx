import React from "react";
import { SquareX } from "lucide-react";
import { ThemeModeToggle } from "@/components/ThemeModeToggle";
import { useUpload } from "@/context/UploadContext";
import { useToast } from "@/hooks/use-toast";
import { CONFIG } from "../../config";
import CropSidebar from "@/components/CropSideBar";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";

const SideBar = ({ setIsCropping }: { setIsCropping: (cropping: boolean) => void }) => {
  const { uploadedFile } = useUpload();
  const { toast } = useToast();

  // actual handler function
  const handleBackgroundRemoval = async () => {
    if (!uploadedFile) {
      toast({
        title: "No uploaded file",
        description: "Please upload a file first.",
      });
      return;
    }

    // Prepare the file to be sent to the backend
    const formData = new FormData();
    formData.append("imageFile", uploadedFile);

    try {
      const response = await fetch(
        CONFIG.API_BASE_URL + "/api/mock/remove-background",
        {
          method: "POST",
          body: formData,
        }
      );
      if (!response.ok) {
        throw new Error("Failed to send image for background removal");
      }
      toast({
        title: "Image successfully sent to the backend",
      });
    } catch (error: any) {
      console.error(error);
      toast({
        title: "Error sending image",
        description: error.message,
      });
    }
  };

  // tester function to check if file can be seen from the sidebar
  const checkFile = () => {
    if (!uploadedFile) {
      toast({
        title: "No file",
        description: "Something wrong with context api",
      });
    } else {
      toast({
        title: "File is here",
        description: "Context api is working",
      });
    }
  };
  return (
    <div className="fixed flex flex-col space-y-2 w-fit h-full p-2 items-center border-2 border-r-lightgray-500">
      <ThemeModeToggle />
      <p className="text-xs pt-3 pb-1 font-semibold">Edit:</p>
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <div className="border border-1 p-1 rounded-md cursor-pointer">
            <CropSidebar setIsCropping={setIsCropping} />
            </div>
          </TooltipTrigger>
          <TooltipContent side="right">
            <p>Crop Image</p>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
      <div className="border border-1 p-1 rounded-md cursor-pointer">
        <SquareX onClick={checkFile} />
      </div>
    </div>
  );
};

export default SideBar;
