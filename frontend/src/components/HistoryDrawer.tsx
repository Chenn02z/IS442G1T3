import * as React from 'react';
import { useEffect, useState, useCallback } from 'react';
import { History, Undo, Redo, Check } from 'lucide-react';
import { 
  Drawer, 
  DrawerContent, 
  DrawerHeader, 
  DrawerTitle, 
  DrawerDescription,
  DrawerFooter,
  DrawerClose
} from '@/components/ui/drawer';
import { StateManagementService } from '@/utils/StateManagementService';
import { useUpload } from '@/context/UploadContext';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';

interface HistoryDrawerProps {
  imageId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

interface VersionData {
  version: string;
  imageUrl: string;
  label: string;
}

export function HistoryDrawer({ imageId, open, onOpenChange }: HistoryDrawerProps) {
  const { toast } = useToast();
  const { setSelectedImageUrl, refreshImages, restoreCurrentImageUrl } = useUpload();

  const [history, setHistory] = useState<{
    undoStack: VersionData[];
    redoStack: VersionData[];
  }>({
    undoStack: [],
    redoStack: []
  });
  
  const [isLoading, setIsLoading] = useState(false);
  const [currentVersion, setCurrentVersion] = useState<string | null>(null);
  // Track the selected version (that would be applied on confirmation)
  const [previewVersion, setPreviewVersion] = useState<VersionData | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Fetch the history whenever the drawer opens or imageId changes
  useEffect(() => {
    if (open && imageId) {
      fetchHistory(imageId);
      setPreviewVersion(null); // Reset preview when drawer opens
    }
  }, [open, imageId]);

  const fetchHistory = async (id: string) => {
    setIsLoading(true);
    setError(null);
    
    try {
      // Get history data
      const historyResponse = await StateManagementService.getHistory(id);
      
      if (historyResponse.status === 'success') {
        // Convert version IDs to full version objects
        const undoStack = historyResponse.data.undoStack;
        const redoStack = historyResponse.data.redoStack;
        
        undoStack.sort((a, b) => parseInt(a, 10) - parseInt(b, 10));
        redoStack.sort((a, b) => parseInt(a, 10) - parseInt(b, 10));

        // Get the latest version data
        const latestResponse = await StateManagementService.getLatestVersion(id);
        
        if (latestResponse.status === 'success') {
          setCurrentVersion(latestResponse.data.version.toString());
        }
        
        // Create version objects from stack IDs
        const undoVersions = undoStack.map(versionId => ({
          version: versionId,
          imageUrl: StateManagementService.getFullImageUrl(`${id}_${versionId}.png`),
          label: versionId === '1' ? 'Original' : `Edit ${versionId}`
        }));
        
        const redoVersions = redoStack.map(versionId => ({
          version: versionId,
          imageUrl: StateManagementService.getFullImageUrl(`${id}_${versionId}.png`),
          label: `Edit ${versionId}`
        }));
        
        setHistory({
          undoStack: undoVersions,
          redoStack: redoVersions
        });
      }
    } catch (err: any) {
      setError(err.message);
      toast({
        title: "Error loading history",
        description: err.message,
        variant: "destructive"
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handlePreviewVersion = async (version: VersionData) => {
    if (!imageId) return;
    
    setIsLoading(true);
    try {
      // Get current history
      const historyResponse = await StateManagementService.getHistory(imageId);
      const currentVer = historyResponse.data.undoStack[historyResponse.data.undoStack.length - 1];
      
      // If selecting a different version than current
      if (currentVer !== version.version) {
        // If version is in undo stack, keep undoing until we reach it
        if (historyResponse.data.undoStack.includes(version.version)) {
          while (true) {
            const undoResponse = await StateManagementService.undo(imageId);
            const newHistory = await StateManagementService.getHistory(imageId);
            const newCurrent = newHistory.data.undoStack[newHistory.data.undoStack.length - 1];
            if (newCurrent === version.version) break;
          }
        } 
        // If version is in redo stack, keep redoing until we reach it
        else if (historyResponse.data.redoStack.includes(version.version)) {
          while (true) {
            const redoResponse = await StateManagementService.redo(imageId);
            const newHistory = await StateManagementService.getHistory(imageId);
            const newCurrent = newHistory.data.undoStack[newHistory.data.undoStack.length - 1];
            if (newCurrent === version.version) break;
          }
        }
      }

      // Update preview version
      setPreviewVersion(version);
      
      // Fetch latest history to update UI
      await fetchHistory(imageId);
      
      toast({
        title: "Version selected",
        description: `Viewing version ${version.version}. Select "Confirm" to apply.`
      });
    } catch (err: any) {
      setError(err.message);
      toast({
        title: "Error selecting version",
        description: err.message,
        variant: "destructive"
      });
    } finally {
      setIsLoading(false);
    }
  };

  // Simplified undo handler - no need for special preview handling
  const handleUndo = useCallback(async () => {
    if (!imageId) return;
    
    setIsLoading(true);
    try {
      const response = await StateManagementService.undo(imageId);
      
      if (response.status === 'success') {
        // Update history and preview
        await fetchHistory(imageId);
        const latestResponse = await StateManagementService.getLatestVersion(imageId);
        
        const newPreviewVersion = {
          version: latestResponse.data.version.toString(),
          imageUrl: StateManagementService.getFullImageUrl(latestResponse.data.currentImageUrl),
          label: latestResponse.data.version === 1 ? 'Original' : `Edit ${latestResponse.data.version}`
        };
        setPreviewVersion(newPreviewVersion);
        
        toast({
          title: "Version changed",
          description: `Viewing version ${latestResponse.data.version}. Select "Confirm" to apply changes.`
        });
      }
    } catch (err: any) {
      setError(err.message);
      toast({
        title: "Error undoing action",
        description: err.message,
        variant: "destructive"
      });
    } finally {
      setIsLoading(false);
    }
  }, [imageId, toast, fetchHistory]);

  // Simplified redo handler - no need for special preview handling
  const handleRedo = useCallback(async () => {
    if (!imageId) return;
    
    setIsLoading(true);
    try {
      const response = await StateManagementService.redo(imageId);
      
      if (response.status === 'success') {
        // Update history and preview
        await fetchHistory(imageId);
        const latestResponse = await StateManagementService.getLatestVersion(imageId);
        
        const newPreviewVersion = {
          version: latestResponse.data.version.toString(),
          imageUrl: StateManagementService.getFullImageUrl(latestResponse.data.currentImageUrl),
          label: latestResponse.data.version === 1 ? 'Original' : `Edit ${latestResponse.data.version}`
        };
        setPreviewVersion(newPreviewVersion);
        
        toast({
          title: "Version changed",
          description: `Viewing version ${latestResponse.data.version}. Select "Confirm" to apply changes.`
        });
      }
    } catch (err: any) {
      setError(err.message);
      toast({
        title: "Error redoing action",
        description: err.message,
        variant: "destructive"
      });
    } finally {
      setIsLoading(false);
    }
  }, [imageId, toast, fetchHistory]);

  const handleConfirm = useCallback(async () => {
    if (!imageId || !previewVersion) return;
    
    setIsLoading(true);
    try {
      const confirmResponse = await StateManagementService.confirm(imageId);
      
      if (confirmResponse.status === 'success') {
        await refreshImages();
        await restoreCurrentImageUrl(imageId);
        console.log("restored to:" + imageId);
        
        // Reset preview state
        setPreviewVersion(null);
        
        toast({
          title: "Version confirmed",
          description: "Changes have been applied to your image."
        });
        
        // Close the drawer after successful confirmation
        onOpenChange(false);
      }
    } catch (err: any) {
      setError(err.message);
      toast({
        title: "Error confirming version",
        description: err.message,
        variant: "destructive"
      });
    } finally {
      setIsLoading(false);
    }
  }, [imageId, previewVersion, refreshImages, restoreCurrentImageUrl, toast, onOpenChange]);

  // Create a preview image element
  const renderPreview = () => {
    if (!previewVersion) return null;
    
    return (
      <div className="my-4">
        <h3 className="text-sm font-medium mb-2">Preview</h3>
        <div className="w-full border rounded-md p-2">
          <div className="relative w-full h-48 bg-muted rounded-md overflow-hidden">
            <img 
              src={previewVersion.imageUrl}
            //   alt={`Preview of ${previewVersion.label}`}
              className="w-full h-full object-contain"
            />
          </div>
          <p className="text-center mt-2">
            Previewing {previewVersion.label} (Version {previewVersion.version})
          </p>
        </div>
      </div>
    );
  };

  return (
    <Drawer open={open} onOpenChange={onOpenChange}>
      <DrawerContent className="max-h-[85vh]">
        <DrawerHeader>
          <DrawerTitle>Edit History</DrawerTitle>
          <DrawerDescription>
            Browse previous versions and select one to restore
          </DrawerDescription>
        </DrawerHeader>
                  {/* Show preview if a version is selected */}
                  {renderPreview()}
          
          {/* Version Timeline */}
        <div className="my-4">
          <h3 className="text-sm font-medium mb-2">Version History</h3>
          <ScrollArea className="w-full whitespace-nowrap rounded-md border h-48">
            <div className="flex p-4 space-x-4">
              {history.undoStack.map((version) => (
                <div 
                  key={version.version}
                  onClick={() => handlePreviewVersion(version)}
                  className={`flex flex-col w-32 rounded-md border p-2 cursor-pointer transition-all ${
                    version.version === currentVersion ? 'border-primary border-2' : ''
                  } ${
                    previewVersion?.version === version.version ? 'ring-2 ring-primary bg-primary/10' : ''
                  }`}
                >
                  <div className="h-24 mb-2 bg-muted rounded-md overflow-hidden">
                    <img 
                      src={version.imageUrl}
                      alt={`Version ${version.version}`}
                      className="w-full h-full object-cover"
                    />
                  </div>
                  <div className="text-xs font-medium">{version.label}</div>
                  <div className="text-xs text-muted-foreground">Ver. {version.version}</div>
                </div>
              ))}
              
              {/* Only show redo versions if there are any */}
              {history.redoStack.length > 0 && (
                <>
                  <div className="flex items-center px-2">
                    <div className="h-px w-8 bg-muted-foreground"></div>
                  </div>
                  
                  {history.redoStack.map((version) => (
                    <div 
                      key={version.version}
                      onClick={() => handlePreviewVersion(version)}
                      className={`flex flex-col w-32 rounded-md border p-2 cursor-pointer transition-all opacity-60 hover:opacity-100 ${
                        previewVersion?.version === version.version ? 'ring-2 ring-primary bg-primary/10 opacity-100' : ''
                      }`}
                    >
                      <div className="h-24 mb-2 bg-muted rounded-md overflow-hidden">
                        <img 
                          src={version.imageUrl}
                          alt={`Version ${version.version}`}
                          className="w-full h-full object-cover"
                        />
                      </div>
                      <div className="text-xs font-medium">{version.label}</div>
                      <div className="text-xs text-muted-foreground">Ver. {version.version}</div>
                    </div>
                  ))}
                </>
              )}
            </div>
          </ScrollArea>
        </div>
        <div className="px-4 py-2">

          {/* Action Buttons */}
          <div className="flex space-x-4 mb-4">
            <Button
              variant="outline"
              size="sm"
              onClick={handleUndo}
              disabled={isLoading || history.undoStack.length <= 1}
            >
              <Undo className="h-4 w-4 mr-2" />
              Undo
            </Button>
            
            <Button
              variant="outline"
              size="sm"
              onClick={handleRedo}
              disabled={isLoading || history.redoStack.length === 0}
            >
              <Redo className="h-4 w-4 mr-2" />
              Redo
            </Button>
            
          </div>
          
          {isLoading}
          {error && <p className="text-red-500 text-center py-2">{error}</p>}
          

          
          {/* Help text */}
          <p className="text-xs text-muted-foreground mt-2">
            Select a version to preview, then press "Confirm" to apply it as your current image.
          </p>
        </div>
        
        <DrawerFooter className="px-4 py-2">
          <Button 
            variant="default" 
            onClick={handleConfirm}
            disabled={isLoading || !previewVersion}
            className="w-full"
          >
            <Check className="h-4 w-4 mr-2" />
            Apply Selected Version
          </Button>
          <DrawerClose asChild>
            <Button variant="outline" className="w-full">
              Close Without Changes
            </Button>
          </DrawerClose>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}