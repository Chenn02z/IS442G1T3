"use client";
import SideBar from "@/components/SideBar";
import { useEffect, useState } from "react";
import { v4 as getUUIDv4 } from "uuid";
import UploadImageForm from "@/components/UploadImageForm";
import PhotoList from "@/components/PhotoList";
import { UploadProvider } from "@/context/UploadContext";
export const UUID_LOOKUP_KEY = "userUUID_";

export default function Home() {
  const [userUUID, setUserUUID] = useState<string>("");
  const [selectedAspectRatio, setSelectedAspectRatio] = useState<number | null>(null);

  useEffect(() => {
    let storedUUID = localStorage.getItem(UUID_LOOKUP_KEY);
    if (!storedUUID) {
      // New user; assign UUID and save to local storage
      storedUUID = getUUIDv4();
      localStorage.setItem(UUID_LOOKUP_KEY, storedUUID);
    }
    setUserUUID(storedUUID);
  }, []);
  return (
    <UploadProvider>
      <div className="flex">
        {/* Left Sidebar */}
        <div className="w-14">
          <SideBar setSelectedAspectRatio={setSelectedAspectRatio} />
        </div>

        {/* Right Sidebar (PhotoList) */}
        <div className="w-fit">
          <PhotoList />
        </div>
        <div className="flex-1 px-3 py-3"> 
          {/* Main content here */}
          <div>
            User UUID: {userUUID}
          </div>
          <UploadImageForm selectedAspectRatio={selectedAspectRatio}/>
        </div>
      </div>
    </UploadProvider>
  );
}
