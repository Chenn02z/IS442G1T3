"use client";
import SideBar from "@/components/SideBar";
import { useEffect, useState } from "react";
import { v4 as getUUIDv4 } from "uuid";
import UploadImageForm from "@/components/UploadImageForm";
import PhotoList from "@/components/PhotoList";
import { UploadProvider } from "@/context/UploadContext";
import { LoadingScreen } from "@/components/LoadingScreen";

export const UUID_LOOKUP_KEY = "userUUID_";

export default function Home() {
  const [userUUID, setUserUUID] = useState<string>("");
  const [isLoading, setIsLoading] = useState(true);
  const [selectedAspectRatio, setSelectedAspectRatio] = useState<number | null>(
    null
  );
  const [isCropping, setIsCropping] = useState<boolean>(false);
  const [uploadSource, setUploadSource] = useState<'local' | 'drive'>('local');

  useEffect(() => {
    let storedUUID = localStorage.getItem(UUID_LOOKUP_KEY);
    if (!storedUUID) {
      storedUUID = getUUIDv4();
      localStorage.setItem(UUID_LOOKUP_KEY, storedUUID);
    }
    setUserUUID(storedUUID);


    // Clear isCropping when the component unmounts
    return () => {
      setIsCropping(false);
    };
  }, []);

  return (
    <div>
    {isLoading ? (
      <LoadingScreen onFinish={() => setIsLoading(false)} />
    ) : (
        <UploadProvider>
          <div className="flex min-h-screen bg-background">
            {/* Left Tools Sidebar - fixed width */}
            <div className="w-16 min-w-16 h-screen bg-card border-r">
              <SideBar setSelectedAspectRatio={setSelectedAspectRatio} />
            </div>

            {/* Photo List Sidebar - fixed width */}
            <div className="w-64 min-w-64 h-screen bg-card border-r">
              <PhotoList />
            </div>

            {/* Main Content - flexible width */}
            <div className="flex-1 p-6 bg-background">
              <div className="text-sm text-muted-foreground mb-4">
                Session ID: {userUUID}
              </div>
              <UploadImageForm 
                selectedAspectRatio={selectedAspectRatio} 
                uploadSource={uploadSource}
                setUploadSource={setUploadSource}
              />
            </div>
          </div>
        </UploadProvider>
  )}
  </div>
  );
}
