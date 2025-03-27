/**
 * PhotoList Component - Displays a scrollable list of uploaded images
 * 
 * Features:
 * - Fetches user's uploaded images from backend
 * - Displays images in a scrollable sidebar
 * - Handles image selection and loading states
 * - Shows loading and error states
 */
import React, { useEffect, useState } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Trash2, UploadCloud } from "lucide-react";
import { CONFIG } from "../../config";
import { UUID_LOOKUP_KEY } from "@/app/page";
import { useUpload } from "@/context/UploadContext";
import { useImageUploadHandler } from "@/utils/ImageUploadHandler";
import { useToast } from "@/hooks/use-toast";
import Image from "next/image";

interface ImageEntity {
  imageId: string;
  version: number;
  baseImageUrl: string;
  currentImageUrl: string;
  label: string;
}

const PhotoList = () => {
  const { handleUpload } = useImageUploadHandler();
  const {
    setSelectedImageUrl,
    setUploadedImageCount,
    uploadedImageCount,
    setCroppedImageUrl,
    setSelectedImageId,
    setIsCropping,
    selectedImageId,
  } = useUpload();
  const [uploadedImages, setUploadedImages] = useState<ImageEntity[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const { toast } = useToast();

  // Debug image URLs
  useEffect(() => {
    uploadedImages.forEach((image) => {
      console.log(
        "Image URL:",
        `${CONFIG.API_BASE_URL}/api/images/${image.currentImageUrl}`
      );
    });
  }, [uploadedImages]);

  const handleDelete = async (e: React.MouseEvent, imageId: string) => {
    e.stopPropagation(); // Prevent triggering the parent div's onClick

    try {
      const response = await fetch(
        `${CONFIG.API_BASE_URL}/api/statemanagement/delete/${imageId}`,
        {
          method: "DELETE",
        }
      );

      if (!response.ok) {
        throw new Error("Failed to delete image");
      }

      // Update the UI by removing the deleted image
      setUploadedImages((prevImages) =>
        prevImages.filter((img) => img.imageId !== imageId)
      );
      setUploadedImageCount(uploadedImageCount - 1);

      // Clear selected image if it was the one deleted
      if (imageId === selectedImageId) {
        setSelectedImageUrl(null);
        setCroppedImageUrl(null);
        setSelectedImageId(null);
      }

      toast({
        title: "Image deleted successfully",
      });
    } catch (error) {
      console.error("Error deleting image:", error);
      toast({
        title: "Error deleting image",
        description: "Please try again",
        variant: "destructive",
      });
    }
  };

  /**
   * Fetch user's images on component mount and when uploadedImageCount changes
   */
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
        const response = await fetch(
          `${CONFIG.API_BASE_URL}/api/statemanagement/user/latest-list/${userId}`
        );

        if (!response.ok) {
          throw new Error(`Error fetching images: ${response.statusText}`);
        }

        const data = await response.json();
        if (data.status === "success" && Array.isArray(data.data)) {
          setUploadedImages(data.data);
          setUploadedImageCount(data.data.length);
        } else {
          throw new Error("Invalid response format");
        }
      } catch (error) {
        setError("Failed to fetch images.");
        console.error("Error fetching images:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchImages();
    // Only depend on uploadedImageCount to prevent unnecessary re-fetches
  }, [uploadedImageCount, setUploadedImageCount]);

  /**
   * Handles image selection when user clicks on an image thumbnail
   */
  const handleImageSelect = (id: string, url: string) => {
    setIsCropping(false);
    setSelectedImageUrl(url);
    setCroppedImageUrl(null);
    setSelectedImageId(id);
    localStorage.removeItem("selectedRatio");
  };

  return (
    <div className="h-screen border-r-2 p-2 flex flex-col">
      {/* Header */}
      <h2 className="mb-4 font-bold">Images</h2>

      {/* Error State */}
      {error && <p className="text-center text-red-500">{error}</p>}
      {error && <p className="text-center text-red-500">{error}</p>}

      {/* Loading State */}
      {loading && (
        <>
          {Array(5)
            .fill(0)
            .map((_, i) => (
              <div
                key={i}
                className="flex items-center space-x-3 p-2 rounded-md"
              >
                <Skeleton className="w-10 h-10 object-cover rounded-md" />
                <div className="space-y-2">
                  <Skeleton className="h-4 w-[100px]" />
                </div>
              </div>
            ))}
        </>
      )}

      {/* Scrollable area for images */}
      <ScrollArea className="flex-1 overflow-auto">
        <div className="space-y-2">
          {!loading && uploadedImages.length > 0
            ? uploadedImages.map((image) => (
                <div
                  key={`${image.imageId}-${image.version}`}
                  className="flex justify-between space-x-3 p-2 rounded-md hover:bg-gray-200 transition cursor-pointer"
                  onClick={() => {
                    console.log(
                      "Clicking image row, setting URL:",
                      image.currentImageUrl
                    );
                    // Use a timestamp for cache busting but applied only once at click time
                    const timestamp = Date.now();
                    setSelectedImageUrl(
                      `${CONFIG.API_BASE_URL}/api/images/${image.currentImageUrl}?t=${timestamp}`
                    );
                    setCroppedImageUrl(null);
                    setSelectedImageId(image.imageId);
                    localStorage.removeItem("selectedRatio");
                  }}
                >
                  <Image
                    // Use the image version as part of the key to force re-render when needed
                    // but keep the URL stable without changing on every render
                    key={`thumb-${image.imageId}-${image.version}`}
                    src={`${CONFIG.API_BASE_URL}/api/images/${image.currentImageUrl}`}
                    alt={`Image ${image.label || image.version}`}
                    className="w-10 h-10 object-cover rounded-md"
                    width={50}
                    height={50}
                    onClick={(e) => e.stopPropagation()} // Prevent double-triggering
                    onError={(e) => {
                      console.error("Error loading image:", e);
                      const imgElement = e.target as HTMLImageElement;
                      console.log("Failed URL:", imgElement.src);
                    }}
                  />
                  <Trash2
                    className="w-4 h-4 cursor-pointer hover:text-red-700"
                    onClick={(e) => handleDelete(e, image.imageId)}
                  />
                </div>
              ))
            : !loading && (
                <p className="text-sm text-center">No images uploaded yet.</p>
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
                <p className="text-gray-700 text-sm">Upload an Image</p>
                <p className="text-muted-foreground text-xs">
                  Supported formats: .jpeg, .png
                </p>
              </label>
            </>
          </p>
        </>
      )}
    </div>
  );
};

export default PhotoList;
