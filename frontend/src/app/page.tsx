"use client";
import SideBar from "@/components/SideBar";
import { useEffect, useState } from "react";
import { v4 as getUUIDv4 } from "uuid";
import UploadImageForm from "@/components/UploadImageForm";
const UUID_LOOKUP_KEY = "userUUID";

export default function Home() {
  const [userUUID, setUserUUID] = useState<string>("");
  const [isCropping, setIsCropping] = useState<boolean>(false);

  useEffect(() => {
    let storedUUID = localStorage.getItem("userUUID");
    if (!storedUUID) {
      // New user; assign UUID and save to local storage
      storedUUID = "user_" + getUUIDv4();
      localStorage.setItem(UUID_LOOKUP_KEY, storedUUID);
    }
    setUserUUID(storedUUID);
  }, [])
  return (
    <>
      <div className="flex">
      <SideBar setIsCropping={setIsCropping} />
        <div className="ml-14 flex-1 p-3"> 
          {/* Main content here */}
          <div>
            User UUID: {userUUID}
          </div>
          <UploadImageForm isCropping={isCropping} setIsCropping={setIsCropping} />
        </div>
    </div>
    </>
  );
}
