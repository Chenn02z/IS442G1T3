import React, { useState, useEffect } from "react";
import { useUpload } from "@/context/UploadContext";
import { useToast } from "@/hooks/use-toast";
import { CONFIG } from "../../config";
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from "@/components/ui/drawer";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { History, Undo, Redo, Save } from "lucide-react";
import Image from "next/image";

interface VersionData {
  version: string;
  imageUrl: string;
  timestamp: string;
}

interface VersionHistoryDrawerProps {
  imageId: string | null;
}

const VersionHistoryDrawer: React.FC<VersionHistoryDrawerProps> = ({ imageId }) => {
  const { setSelectedImageUrl } = useUpload();
  const { toast } = useToast();
  
  // For now, we'll use mock data until the backend is implemented
  const [history, setHistory] = useState<{
    undoStack: VersionData[];
    redoStack: VersionData[];
  }>({
    undoStack: [],
    redoStack: []
  });
  
  const [currentVersion, setCurrentVersion] = useState<string>("1");
  const [isLoading, setIsLoading] = useState(false);
  
  // Populate with sample data for demonstration
  useEffect(() => {
    if (imageId) {
      // This would be replaced with an actual API call
      const mockData = {
        undoStack: [
          { 
            version: "1", 
            imageUrl: "http://localhost:8080/images/original.jpg",
            timestamp: "2023-10-15 14:30:22" 
          },
          { 
            version: "2", 
            imageUrl: "http://localhost:8080/images/cropped.jpg",
            timestamp: "2023-10-15 14:32:45" 
          },
          { 
            version: "3", 
            imageUrl: "http://localhost:8080/images/background-removed.jpg",
            timestamp: "2023-10-15 14:35:10" 
          }
        ],
        redoStack: []
      };
      
      setHistory(mockData);
      setCurrentVersion("3"); // Latest version
      
      // The actual API call would look like this:
      /*
      const fetchHistory = async () => {
        setIsLoading(true);
        try {
          const response = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/history/${imageId}`);
          if (!response.ok) throw new Error("Failed to fetch history");
          const data = await response.json();
          setHistory(data.data);
          
          // Get latest version
          const latestResponse = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/latest/${imageId}`);
          if (!latestResponse.ok) throw new Error("Failed to fetch latest version");
          const latestData = await latestResponse.json();
          setCurrentVersion(latestData.data.version);
        } catch (error) {
          console.error("Error fetching history:", error);
          toast({
            title: "Error",
            description: "Failed to load edit history."
          });
        } finally {
          setIsLoading(false);
        }
      };
      
      fetchHistory();
      */
    }
  }, [imageId, toast]);
  
  const handleUndo = async () => {
    if (!imageId || history.undoStack.length <= 1) return;
    
    setIsLoading(true);
    try {
      // Mock implementation - in reality, this would be an API call
      /*
      const response = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/undo`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ imageId })
      });
      
      if (!response.ok) throw new Error("Failed to undo");
      const data = await response.json();
      setCurrentVersion(data.data.version);
      
      // Refetch history
      const historyResponse = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/history/${imageId}`);
      if (!historyResponse.ok) throw new Error("Failed to refresh history");
      const historyData = await historyResponse.json();
      setHistory(historyData.data);
      
      // Update the image
      const latestVersionUrl = historyData.data.undoStack[historyData.data.undoStack.length - 1].imageUrl;
      setSelectedImageUrl(latestVersionUrl);
      */
      
      // For mock purposes, we'll simulate the undo
      const newUndoStack = [...history.undoStack];
      const lastVersion = newUndoStack.pop();
      if (lastVersion) {
        const newRedoStack = [...history.redoStack, lastVersion];
        setHistory({
          undoStack: newUndoStack,
          redoStack: newRedoStack
        });
        
        const newCurrentVersion = newUndoStack[newUndoStack.length - 1];
        setCurrentVersion(newCurrentVersion.version);
        setSelectedImageUrl(newCurrentVersion.imageUrl);
        
        toast({
          title: "Undo successful",
          description: `Reverted to version ${newCurrentVersion.version}`
        });
      }
    } catch (error) {
      console.error("Error during undo:", error);
      toast({
        title: "Error",
        description: "Failed to undo the last edit."
      });
    } finally {
      setIsLoading(false);
    }
  };
  
  const handleRedo = async () => {
    if (!imageId || history.redoStack.length === 0) return;
    
    setIsLoading(true);
    try {
      // Mock implementation - in reality, this would be an API call
      /*
      const response = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/redo`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ imageId })
      });
      
      if (!response.ok) throw new Error("Failed to redo");
      const data = await response.json();
      setCurrentVersion(data.data.version);
      
      // Refetch history
      const historyResponse = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/history/${imageId}`);
      if (!historyResponse.ok) throw new Error("Failed to refresh history");
      const historyData = await historyResponse.json();
      setHistory(historyData.data);
      
      // Update the image
      const latestVersionUrl = historyData.data.undoStack[historyData.data.undoStack.length - 1].imageUrl;
      setSelectedImageUrl(latestVersionUrl);
      */
      
      // For mock purposes, we'll simulate the redo
      const newRedoStack = [...history.redoStack];
      const versionToRestore = newRedoStack.pop();
      if (versionToRestore) {
        const newUndoStack = [...history.undoStack, versionToRestore];
        setHistory({
          undoStack: newUndoStack,
          redoStack: newRedoStack
        });
        
        setCurrentVersion(versionToRestore.version);
        setSelectedImageUrl(versionToRestore.imageUrl);
        
        toast({
          title: "Redo successful",
          description: `Advanced to version ${versionToRestore.version}`
        });
      }
    } catch (error) {
      console.error("Error during redo:", error);
      toast({
        title: "Error",
        description: "Failed to redo the edit."
      });
    } finally {
      setIsLoading(false);
    }
  };
  
  const handleConfirm = async () => {
    if (!imageId) return;
    
    setIsLoading(true);
    try {
      // Mock implementation - in reality, this would be an API call
      /*
      const response = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/confirm`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ imageId })
      });
      
      if (!response.ok) throw new Error("Failed to confirm version");
      
      // Refetch history
      const historyResponse = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/history/${imageId}`);
      if (!historyResponse.ok) throw new Error("Failed to refresh history");
      const historyData = await historyResponse.json();
      setHistory(historyData.data);
      */
      
      // For mock purposes, we'll simulate confirming the current version
      setHistory({
        ...history,
        redoStack: [] // Clear redo stack
      });
      
      toast({
        title: "Version confirmed",
        description: "All future edits will branch from this version."
      });
    } catch (error) {
      console.error("Error confirming version:", error);
      toast({
        title: "Error",
        description: "Failed to confirm the version."
      });
    } finally {
      setIsLoading(false);
    }
  };
  
  const handleRevertToVersion = (version: VersionData) => {
    setSelectedImageUrl(version.imageUrl);
    setCurrentVersion(version.version);
    
    toast({
      title: "Version restored",
      description: `Viewing version ${version.version}. Confirm to keep this version.`
    });
  };

  return (
    <Drawer>
      <DrawerTrigger asChild>
        <Button variant="outline" className="gap-2">
          <History className="h-4 w-4" />
          History
        </Button>
      </DrawerTrigger>
      <DrawerContent>
        <div className="mx-auto w-full max-w-7xl">
          <DrawerHeader>
            <DrawerTitle>Edit History</DrawerTitle>
            <DrawerDescription>
              View and restore previous versions of your image.
            </DrawerDescription>
          </DrawerHeader>
          
          <div className="p-4">
            {/* Action buttons */}
            <div className="flex space-x-2 mb-4">
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
              
              <Button 
                variant="outline" 
                size="sm"
                onClick={handleConfirm}
                disabled={isLoading}
              >
                <Save className="h-4 w-4 mr-2" />
                Confirm Version
              </Button>
            </div>
            
            {/* Version timeline */}
            <ScrollArea className="w-full whitespace-nowrap rounded-md border h-80">
              <div className="flex p-4 space-x-4">
                {history.undoStack.map((version) => (
                  <div 
                    key={version.version}
                    className={`flex flex-col w-48 rounded-md border p-2 cursor-pointer transition-all ${
                      version.version === currentVersion ? 'ring-2 ring-primary' : ''
                    }`}
                    onClick={() => handleRevertToVersion(version)}
                  >
                    <div className="relative h-32 mb-2">
                      <Image
                        src={version.imageUrl}
                        alt={`Version ${version.version}`}
                        fill
                        sizes="100%"
                        className="object-cover rounded-md"
                        unoptimized
                      />
                    </div>
                    <div className="text-sm font-medium">Version {version.version}</div>
                    <div className="text-xs text-muted-foreground">{version.timestamp}</div>
                  </div>
                ))}
              </div>
            </ScrollArea>
          </div>
          
          <DrawerFooter>
            <DrawerClose asChild>
              <Button variant="outline">Close</Button>
            </DrawerClose>
          </DrawerFooter>
        </div>
      </DrawerContent>
    </Drawer>
  );
};

export default VersionHistoryDrawer;