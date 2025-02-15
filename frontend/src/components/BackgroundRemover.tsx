"use client";

import { useState } from "react";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

import { SquareX } from "lucide-react";
import { useUpload } from "@/context/UploadContext";
import { useToast } from "@/hooks/use-toast";
import { CONFIG } from "../../config";

export const BackgroundRemover = () => {
  const { uploadedFile } = useUpload();
  const { toast } = useToast();

  // actual handler function
  const handleBackgroundRemoval = async (type: string) => {
    if (!uploadedFile) {
      toast({
        title: "No uploaded file",
        description: "Please upload a file first.",
      });
      return;
    }

    // Prepare the file to be sent to the backend
    const formData = new FormData();
    formData.append("image", uploadedFile);
    if (type == "auto") {
      formData.append("backgroundOption", "white"); // to revisit logic
      try {
        // const response = await fetch(CONFIG.API_BASE_URL + "/api/images/upload", {
        //   method: "POST",
        //   body: formData,
        // });
        const response = await fetch(
          CONFIG.API_BASE_URL + "/api/background-removal/remove",
          {
            method: "POST",
            body: formData,
            // mode: "cors",  // Enables CORS requests
            // headers: {
            //   "Accept": "application/json",  // Ensure the response is JSON
            //   "Access-Control-Allow-Origin": "*",  // Allow any domain (use a specific one in production)
            // }
          }
        );
        if (!response.ok) {
          throw new Error("Failed to send image for background removal");
        }
        toast({
          title: "New Image successfully saved to deskstop",
        });
      } catch (error: any) {
        console.error(error);
        toast({
          title: "Error sending image",
          description: error.message,
        });
      }
    } else {
      formData.append("backgroundOption", "white"); // to revisit logic
      try {
        // const response = await fetch(CONFIG.API_BASE_URL + "/api/images/upload", {
        //   method: "POST",
        //   body: formData,
        // });
        const response = await fetch(
          CONFIG.API_BASE_URL + "/api/background-removal/floodfill",
          {
            method: "POST",
            body: formData,
            // mode: "cors",  // Enables CORS requests
            // headers: {
            //   "Accept": "application/json",  // Ensure the response is JSON
            //   "Access-Control-Allow-Origin": "*",  // Allow any domain (use a specific one in production)
            // }
          }
        );
        if (!response.ok) {
          throw new Error("Failed to send image for background removal");
        }
        toast({
          title: "New Image successfully saved to deskstop",
        });
      } catch (error: any) {
        console.error(error);
        toast({
          title: "Error sending image",
          description: error.message,
        });
      }
    }
    // formData.append("backgroundOption", "white"); // to revisit logic

    // depending on the option selected, change formData or change endpoint?

    
  };

  // tester function to check if file can be seen from the sidebar
  const checkFile = () => {
    if (!uploadedFile) {
      toast({
        title: "No file",
        description: "Please upload a file",
      });
      return false;
    } else {
      toast({
        title: "File is here",
        description: "Context api is working",
      });
      return true;
    }
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <div className="border border-1 p-1 rounded-md cursor-pointer">
          <SquareX />
        </div>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem onClick={() => handleBackgroundRemoval("auto")}>
          Auto
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => handleBackgroundRemoval("manual")}>
          Manual
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
