"use client";
import { Card, CardContent } from "@/components/ui/card";
import { UploadCloud, History, Cloud } from "lucide-react";
import { useUpload } from "@/context/UploadContext";
import { useImageUploadHandler } from "@/utils/ImageUploadHandler";
import DisplayImage from "./DisplayImage";
import CropImage from "./CropImage";
import DownloadButton from "./DownloadButton";
import { HistoryDrawer } from "./HistoryDrawer";
import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { ComplianceCheckDialog } from "./ComplianceCheckDialog";
import ComplianceResultDisplay, { ComplianceResultType } from "./ComplianceResultDisplay";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { GoogleDriveAuth } from "./GoogleDriveAuth";
import { GoogleDriveImagePicker } from "@/components/GoogleDriveImagePicker";
import { UUID_LOOKUP_KEY } from "@/app/page";

// Update the props interface to remove isCropping
interface UploadImageFormProps {
  selectedAspectRatio: number | null;
  uploadSource: 'local' | 'drive';
  setUploadSource: (source: 'local' | 'drive') => void;
}

const BACKEND_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

const UploadImageForm: React.FC<UploadImageFormProps> = ({
  selectedAspectRatio,
  uploadSource,
  setUploadSource,
}) => {
  const {
    selectedImageUrl,
    croppedImageUrl,
    selectedImageId,
    setIsCropping,
    isCropping,
    setSelectedImageUrl,
    setSelectedImageId,
    uploadedImageCount,
    setUploadedImageCount,
    refreshImages,
  } = useUpload();

  // ✅ NEW: Track the uploaded file for use in Drive upload
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);

  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [photoUrlToComplianceResultMap, setPhotoUrlToComplianceResultMap] = useState<
    Record<string, ComplianceResultType | null>
  >({});
  const [isComplianceDialogOpen, setIsComplianceDialogOpen] = useState(false);

  // Google Drive
  const [isDriveConnected, setIsDriveConnected] = useState(false);
  const [uploadingToDrive, setUploadingToDrive] = useState(false);

  // Get the proper upload handler function that calls the API
  const { handleUpload } = useImageUploadHandler();

  useEffect(() => {
    async function checkDriveConnection() {
      try {
        const userId = localStorage.getItem(UUID_LOOKUP_KEY);
        if (!userId) return;

        const response = await fetch(`${BACKEND_URL}/api/google-drive/storage/preference/${userId}`);
        if (response.ok) {
          const data = await response.json();
          setIsDriveConnected(data.provider === 'googleDriveStorageProvider');
        } else if (response.status === 404) {
          console.log('Storage preference not found, assuming not connected');
          setIsDriveConnected(false);
        }
      } catch (error) {
        console.error('Failed to check drive connection:', error);
        setIsDriveConnected(false);
      }
    }

    checkDriveConnection();
  }, []);

  const handleComplianceResult = (result: ComplianceResultType) => {
    if (!selectedImageUrl) return;

    setPhotoUrlToComplianceResultMap((prev) => ({
      ...prev,
      [selectedImageUrl]: result,
    }));
  };

  const handleOpenResizeTab = () => {
    setIsComplianceDialogOpen(true);
  };

  const handleDriveSelect = async (fileId: string) => {
    try {
      const userId = localStorage.getItem(UUID_LOOKUP_KEY);
      const response = await fetch(`${BACKEND_URL}/api/google-drive/import-image`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          userId: userId || '',
          fileId,
        }),
      });

      if (response.ok) {
        const data = await response.json();

        if (data.imageUrl) {
          setSelectedImageUrl(data.imageUrl);
          setSelectedImageId(data.id || fileId);
        } else if (data.base64Image) {
          const imageUrl = `data:image/jpeg;base64,${data.base64Image}`;
          setSelectedImageUrl(imageUrl);
          setSelectedImageId(data.id || fileId);
        }

        setUploadedFile(null); // ✅ It's from Drive, so no local file to upload
        setUploadedImageCount((count) => count + 1);
        setTimeout(() => refreshImages(), 1000);
      } else {
        console.error('Failed to import image');
      }
    } catch (error) {
      console.error('Error importing from Drive:', error);
    }
  };

  const handleUploadToDrive = async () => {
    setUploadingToDrive(true);
    try {
      const userId = localStorage.getItem(UUID_LOOKUP_KEY);
      if (!userId) {
        alert('User ID not found');
        setUploadingToDrive(false);
        return;
      }

      if (!selectedImageId) {
        alert('No image selected to upload');
        setUploadingToDrive(false);
        return;
      }

      // Using the server-side approach with image ID
      const params = new URLSearchParams({
        imageId: selectedImageId,
        userId: userId
      });

      const response = await fetch(`${BACKEND_URL}/api/google-drive/upload-by-id?${params}`, {
        method: 'POST'
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`Failed to upload: ${errorData}`);
      }

      const data = await response.json();
      console.log('Uploaded to Google Drive:', data);
      alert('Image uploaded to Google Drive successfully!');
    } catch (error) {
      console.error('Error uploading to Google Drive:', error);
      alert('Upload failed: ' + error.message);
    } finally {
      setUploadingToDrive(false);
    }
  };

  const handleDriveImageSelect = (fileId: string) => {
    console.log("Selected Google Drive image:", fileId);
    setUploadSource('local');
  };

  return (
    <Card className="w-full max-w-3xl mx-auto">
      <CardContent className="flex flex-col items-center justify-center min-h-[400px] p-6">
        {!selectedImageId && (
          <div className="flex flex-col items-center justify-center w-full h-full">
            <Tabs defaultValue="local" className="w-full">
              <TabsList className="grid w-full grid-cols-2 mb-6">
                <TabsTrigger value="local" onClick={() => setUploadSource('local')}>
                  <UploadCloud className="h-4 w-4 mr-2" />
                  Local Upload
                </TabsTrigger>
                <TabsTrigger value="drive" onClick={() => setUploadSource('drive')} 
                // disabled={!isDriveConnected}
                >
                  <Cloud className="h-4 w-4 mr-2" />
                  Google Drive
                </TabsTrigger>
              </TabsList>

              <TabsContent value="local" className="w-full">
                <input
                  type="file"
                  accept="image/jpeg, image/png"
                  className="hidden"
                  id="file-upload"
                  onChange={handleUpload}
                />
                <label
                  htmlFor="file-upload"
                  className="cursor-pointer flex flex-col items-center gap-4 p-8 border-2 border-dashed rounded-lg hover:bg-secondary/50 transition-colors"
                >
                  <UploadCloud className="h-12 w-12 text-muted-foreground" />
                  <div className="text-center">
                    <p className="text-base font-medium">Click to upload an Image</p>
                    <p className="text-sm text-muted-foreground mt-1">Supported formats: .jpeg, .png</p>
                  </div>
                </label>
              </TabsContent>

              <TabsContent value="drive" className="w-full">
                {isDriveConnected ? (
                  <GoogleDriveImagePicker onSelect={handleDriveSelect} />
                ) : (
                  <div className="flex flex-col items-center justify-center gap-6 p-8">
                    <p className="text-center text-muted-foreground">Connect your Google Drive to access your photos</p>
                    <GoogleDriveAuth />
                  </div>
                )}
              </TabsContent>
            </Tabs>

            {uploadSource === 'local' && !isDriveConnected && (
              <div className="mt-8 w-full">
                <div className="flex items-center gap-2 mb-4">
                  <div className="h-px flex-1 bg-border"></div>
                  <span className="text-xs text-muted-foreground">OR</span>
                  <div className="h-px flex-1 bg-border"></div>
                </div>
                <GoogleDriveAuth />
              </div>
            )}
          </div>
        )}

        {selectedImageUrl && selectedImageId && (
          <div className="w-full">
            {isCropping ? (
              <CropImage
                imageUrl={selectedImageUrl}
                aspectRatio={selectedAspectRatio}
                imageId={selectedImageId}
                isCropping={true}
                onCropComplete={() => setIsCropping(false)}
              />
            ) : (
              <div className="flex flex-col items-center gap-6">
                <DisplayImage imageUrl={croppedImageUrl || selectedImageUrl} />

                {photoUrlToComplianceResultMap[selectedImageUrl] && (
                  <ComplianceResultDisplay
                    result={photoUrlToComplianceResultMap[selectedImageUrl]}
                    showDetails
                    onRequestResize={handleOpenResizeTab}
                  />
                )}

                <div className="flex items-center space-x-2">
                  <Button variant="outline" size="sm" onClick={() => setIsHistoryOpen(true)}>
                    <History className="h-4 w-4" />
                    History
                  </Button>

                  <ComplianceCheckDialog
                    imageId={selectedImageId}
                    buttonText="Check Compliance"
                    buttonVariant="outline"
                    buttonSize="sm"
                    onCheckComplete={handleComplianceResult}
                    open={isComplianceDialogOpen}
                    onOpenChange={setIsComplianceDialogOpen}
                    initialTab={isComplianceDialogOpen ? 'resize' : 'check'}
                  />

                  <DownloadButton imageUrl={croppedImageUrl || selectedImageUrl} imageId={selectedImageId} />


                  <Button
                    onClick={handleUploadToDrive}
                    // disabled={uploadingToDrive || (uploadSource === 'local' && !uploadedFile)}
                  >
                    {uploadingToDrive ? 'Uploading to Google Drive...' : 'Save to Google Drive'}
                  </Button>
                  
                </div>
              </div>
            )}
          </div>
        )}

      </CardContent>

      {selectedImageId && (
        <HistoryDrawer imageId={selectedImageId} open={isHistoryOpen} onOpenChange={setIsHistoryOpen} />
      )}
    </Card>
  );
};

export default UploadImageForm;
