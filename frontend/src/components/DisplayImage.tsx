import React from "react";
import Image from "next/image";

type DisplayImageProps = {
  imageUrl: string | null;
};

const DisplayImage: React.FC<DisplayImageProps> = ({ imageUrl }) => {
  if (!imageUrl) return null;

  return (
    <div className="flex flex-col items-center space-y-2">
      <Image
        src={imageUrl}
        alt="Uploaded preview"
        height={250}
        width={250}
        className="object-cover rounded-md shadow-md"
        unoptimized
      />
    </div>
  );
};

export default DisplayImage;

