import React, { useEffect, useState } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Trash2, UploadCloud } from "lucide-react";
import { CONFIG } from "../../config";
import { UUID_LOOKUP_KEY } from "@/app/page";
import { useUpload } from "@/context/UploadContext";
import { useImageUploadHandler } from "@/utils/ImageUploadHandler";


const PhotoList = () => {
  const { handleUpload } = useImageUploadHandler();
  const { setSelectedImageUrl,setUploadedImageCount, uploadedImageCount,setCroppedImageUrl,setSelectedImageId  } = useUpload();
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
          setSelectedImageUrl(formattedImages[0].url);
          setSelectedImageId(formattedImages[0].id);
        }
        
      } catch (error) {
        setError("Failed to fetch images.");
        console.error("Error fetching images:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchImages();
  }, [uploadedImageCount, setSelectedImageUrl, setUploadedImageCount, setCroppedImageUrl, setSelectedImageId]);

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
                onClick={() => {
                  setSelectedImageUrl(url);
                  console.log(url);
                  setCroppedImageUrl(null);
                  setSelectedImageId(id);
                  localStorage.removeItem("cropBoxData");
                }}
              >
                <img src={url} alt={`Image ${id}`} className="w-10 h-10 object-cover rounded-md" />
                <span className="text-sm truncate flex-1">{url}</span>
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
          <>
            <input
              type="file"
              accept="image/jpeg, image/png"
              className="hidden"
              id="file-upload"
              multiple
              onChange={handleUpload}
            />
            <label
              htmlFor="file-upload"
              className="cursor-pointer flex flex-col items-center gap-2"
            >
              <UploadCloud className="h-10 w-10 text-gray-500" />
              <p className="text-gray-700 text-sm">Click to upload an Image</p>
              <p className="text-muted-foreground text-xs">Supported formats: .jpeg, .png</p>
            </label>
          </>
          </p>
        </>
      )}
    </div>
  );
};

export default PhotoList;
