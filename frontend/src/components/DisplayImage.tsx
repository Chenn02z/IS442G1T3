import React from "react";
import Image from "next/image";
import { useUpload } from "@/context/UploadContext";

const DisplayImage: React.FC = () => {
  const { selectedImageUrl } = useUpload();

  if (!selectedImageUrl) return null;

  return (
    <div className="flex flex-col items-center space-y-2">
      <Image
        src={selectedImageUrl}
        alt="Uploaded preview"
        height={500}
        width={500}
        className="object-cover rounded-md shadow-md"
        unoptimized
      />
    </div>
  );
};

export default DisplayImage;


