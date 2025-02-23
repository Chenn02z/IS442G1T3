import React, { useEffect, useState } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Trash2, Image } from "lucide-react";
import { CONFIG } from "../../config";
import { UUID_LOOKUP_KEY } from "@/app/page";
import { useUpload } from "@/context/UploadContext";

const PhotoList = () => {
  const { setSelectedImageUrl,setUploadedImageCount, uploadedImageCount  } = useUpload();
  const [uploadedImages, setUploadedImages] = useState<{ id: string; url: string }[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchImages = async () => {
      setLoading(true);
      setError(null);

      const userId = localStorage.getItem(UUID_LOOKUP_KEY);
      if (!userId) {
        setError("User ID not found.");
        setLoading(false);
        return;
      }

      try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/api/images/userimages/${userId}`);

        if (!response.ok) {
          throw new Error(`Error fetching images: ${response.statusText}`);
        }

        const data = await response.json();

        const formattedImages = Object.entries(data).map(([id, path]) => ({
          id,
          url: `${CONFIG.API_BASE_URL}/${path}`,
        }));

        setUploadedImages(formattedImages);
        setUploadedImageCount(formattedImages.length);

        if (formattedImages.length > 0) {
          setSelectedImageUrl(formattedImages[0].url); // Auto-select first image
        }
      } catch (error) {
        setError("Failed to fetch images.");
        console.error("Error fetching images:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchImages();
  }, [uploadedImageCount, setSelectedImageUrl, setUploadedImageCount]);

  return (
    <div className="w-64 h-screen border-r p-4 flex flex-col">
      {/* Header */}
      <h2 className="mb-4">Uploaded Images</h2>

      {/* Error State */}
      {error && <p className="text-center">{error}</p>}

      {/* Loading State */}
      {loading && (
        <div className="flex items-center space-x-3 p-2 rounded-md">
          <Skeleton className="w-10 h-10 object-cover rounded-md" />
          <div className="space-y-2">
            <Skeleton className="h-4 w-[100px]" />
          </div>
        </div>
      )}

      {/* Scrollable area for images */}
      <ScrollArea className="flex-1 overflow-auto">
        <div className="space-y-2">
          {!loading && uploadedImages.length > 0 ? (
            uploadedImages.map(({ id, url }) => (
              <div
                key={id}
                className="flex items-center space-x-3 p-2 rounded-md hover:bg-gray-200 transition cursor-pointer"
                onClick={() => setSelectedImageUrl(url)} // Update UploadContext with selected image URL
              >
                <img src={url} alt={`Image ${id}`} className="w-10 h-10 object-cover rounded-md" />
                <span className="text-sm truncate flex-1">{url.split('/').pop()}</span> {/* Show only filename */}
                <Trash2 className="w-4 h-4 cursor-pointer hover:text-red-700" />
              </div>
            ))
          ) : (
            !loading && <p className="text-sm text-center">No images uploaded yet.</p>
          )}
        </div>
      </ScrollArea>

      {/* Footer */}
      {uploadedImages.length > 0 && (
        <>
          <Separator className="my-3" />
          <p className="text-center text-indigo-600 text-sm cursor-pointer hover:underline">
            Upload New Photos
          </p>
        </>
      )}
    </div>
  );
};

export default PhotoList;
