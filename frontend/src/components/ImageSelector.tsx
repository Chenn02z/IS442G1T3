"use client";

import { useRef } from "react";
import { useUpload } from "@/context/UploadContext";

interface Point {
  x: number;
  y: number;
}

interface ImageSelectorProps {
  imageUrl: string;
  onPointClick: (e: React.MouseEvent<HTMLImageElement>) => void;
  selectedPoints: Point[];
}

const ImageSelector = ({ imageUrl, onPointClick, selectedPoints }: ImageSelectorProps) => {
  const imageRef = useRef<HTMLImageElement>(null);
  const { getFullImageUrl } = useUpload();

  return (
    <div className="relative inline-block">
      <img
        ref={imageRef}
        src={getFullImageUrl(imageUrl)}
        alt="Image to select points"
        onClick={onPointClick}
        className="w-full cursor-crosshair"
      />
      {selectedPoints.map((point, index) => {
        if (!imageRef.current) return null;
        
        const scaleX = imageRef.current.naturalWidth / imageRef.current.clientWidth;
        const scaleY = imageRef.current.naturalHeight / imageRef.current.clientHeight;
        
        const displayX = point.x / scaleX;
        const displayY = point.y / scaleY;

        return (
          <div
            key={index}
            className="absolute w-2 h-2 bg-red-500 rounded-full transform -translate-x-1 -translate-y-1"
            style={{
              left: `${displayX}px`,
              top: `${displayY}px`,
            }}
          />
        );
      })}
    </div>
  );
};

export default ImageSelector;