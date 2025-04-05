import React, { useState } from 'react';
import { 
  Dialog, 
  DialogContent, 
  DialogDescription, 
  DialogHeader, 
  DialogTitle,
  DialogFooter
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { CONFIG } from "../../config";
import { AlertCircle } from "lucide-react";
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert";

// Standard ID photo dimensions
export const standardPhysicalSizes = [
  { 
    label: "US Passport (2×2\")", 
    value: "us-passport", 
    physicalWidth: 2, 
    physicalHeight: 2,
    unit: "in",
    dpi: 300,
    aspectRatio: 1
  },
  { 
    label: "Singapore Passport (35×45mm)", 
    value: "sg-passport", 
    physicalWidth: 35, 
    physicalHeight: 45,
    unit: "mm",
    dpi: 300,
    aspectRatio: 35/45
  },
  { 
    label: "EU Passport (35×45mm)", 
    value: "eu-passport", 
    physicalWidth: 35, 
    physicalHeight: 45,
    unit: "mm",
    dpi: 300,
    aspectRatio: 35/45
  },
  { 
    label: "India Passport (35×45mm)", 
    value: "india-passport", 
    physicalWidth: 35, 
    physicalHeight: 45,
    unit: "mm",
    dpi: 300,
    aspectRatio: 35/45
  },
  { 
    label: "US Visa (2×2\")", 
    value: "us-visa", 
    physicalWidth: 2, 
    physicalHeight: 2,
    unit: "in",
    dpi: 300,
    aspectRatio: 1
  }
];

// Pixel-based presets
export const pixelPresets = [
  { 
    label: "SG Passport (413×531px)", 
    value: "sg-passport-px", 
    widthPx: 413, 
    heightPx: 531,
    aspectRatio: 413/531
  },
  { 
    label: "US Passport (600×600px)", 
    value: "us-passport-px", 
    widthPx: 600, 
    heightPx: 600,
    aspectRatio: 1
  },
  { 
    label: "LinkedIn Profile (400×400px)", 
    value: "linkedin", 
    widthPx: 400, 
    heightPx: 400,
    aspectRatio: 1
  }
];

type DownloadMode = 'as-is' | 'physical' | 'pixel';
type DownloadOption = 'preset' | 'custom';

interface DownloadOptionsModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  imageId: string | null;
  originalAspectRatio: number | null;
}

const DownloadOptionsModal: React.FC<DownloadOptionsModalProps> = ({ 
  open, 
  onOpenChange,
  imageId,
  originalAspectRatio
}) => {
  const [downloadMode, setDownloadMode] = useState<DownloadMode>('as-is');
  const [physicalOption, setPhysicalOption] = useState<DownloadOption>('preset');
  const [pixelOption, setPixelOption] = useState<DownloadOption>('preset');
  
  // Physical dimension states
  const [selectedPhysicalPreset, setSelectedPhysicalPreset] = useState(standardPhysicalSizes[0]);
  const [customWidth, setCustomWidth] = useState('35');
  const [customHeight, setCustomHeight] = useState('45');
  const [customDpi, setCustomDpi] = useState('300');
  const [customUnit, setCustomUnit] = useState('mm');
  
  // Pixel dimension states
  const [selectedPixelPreset, setSelectedPixelPreset] = useState(pixelPresets[0]);
  const [customWidthPx, setCustomWidthPx] = useState('400');
  const [customHeightPx, setCustomHeightPx] = useState('600');
  
  // Warning state
  const [showAspectRatioWarning, setShowAspectRatioWarning] = useState(false);
  const [confirmAspectRatioMismatch, setConfirmAspectRatioMismatch] = useState(false);

  const calculateAspectRatio = (width: number, height: number) => {
    return width / height;
  };

  const checkAspectRatioDifference = (targetRatio: number): boolean => {
    if (!originalAspectRatio) return false;
    
    // Allow small tolerance (1% difference)
    const tolerance = 0.01;
    const difference = Math.abs(originalAspectRatio - targetRatio) / originalAspectRatio;
    return difference > tolerance;
  };
  
  const resetWarnings = () => {
    setShowAspectRatioWarning(false);
    setConfirmAspectRatioMismatch(false);
  };

  const downloadImage = () => {
    if (!imageId) return;
    
    // Build URL based on selected mode
    let downloadUrl = `${CONFIG.API_BASE_URL}/api/images/download/${imageId}`;
    let targetAspectRatio = 1;
    
    // Process based on selected mode
    if (downloadMode === 'physical') {
      let width, height, dpi, unit;
      
      if (physicalOption === 'preset') {
        width = selectedPhysicalPreset.physicalWidth;
        height = selectedPhysicalPreset.physicalHeight;
        dpi = selectedPhysicalPreset.dpi;
        unit = selectedPhysicalPreset.unit;
        targetAspectRatio = selectedPhysicalPreset.aspectRatio;
      } else {
        width = parseFloat(customWidth);
        height = parseFloat(customHeight);
        dpi = parseInt(customDpi);
        unit = customUnit;
        targetAspectRatio = calculateAspectRatio(width, height);
      }
      
      downloadUrl = `${CONFIG.API_BASE_URL}/api/images/download/${imageId}/sized?width=${width}&height=${height}&dpi=${dpi}&unit=${unit}`;
    } else if (downloadMode === 'pixel') {
      let widthPx, heightPx;
      
      if (pixelOption === 'preset') {
        widthPx = selectedPixelPreset.widthPx;
        heightPx = selectedPixelPreset.heightPx;
        targetAspectRatio = selectedPixelPreset.aspectRatio;
      } else {
        widthPx = parseInt(customWidthPx);
        heightPx = parseInt(customHeightPx);
        targetAspectRatio = calculateAspectRatio(widthPx, heightPx);
      }
      
      downloadUrl = `${CONFIG.API_BASE_URL}/api/images/download/${imageId}/pixel?widthPx=${widthPx}&heightPx=${heightPx}`;
    }
    
    // Check for aspect ratio mismatch
    if (downloadMode !== 'as-is' && originalAspectRatio && checkAspectRatioDifference(targetAspectRatio)) {
      if (!confirmAspectRatioMismatch) {
        setShowAspectRatioWarning(true);
        return;
      }
    }
    
    // Trigger the download
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.setAttribute('download', '');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    // Close the modal
    onOpenChange(false);
    resetWarnings();
  };

  // Handle tab change
  const handleTabChange = (value: string) => {
    resetWarnings();
    setDownloadMode(value as DownloadMode);
  };

  return (
    <Dialog open={open} onOpenChange={(newOpen) => {
      if (!newOpen) resetWarnings();
      onOpenChange(newOpen);
    }}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Download Options</DialogTitle>
          <DialogDescription>
            Choose how you want to download your photo
          </DialogDescription>
        </DialogHeader>
        
        {showAspectRatioWarning ? (
          <div className="space-y-4">
            <Alert variant="default">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Aspect Ratio Mismatch</AlertTitle>
              <AlertDescription>
                The selected dimensions have a different aspect ratio than your cropped image. 
                This may result in distortion or stretching of your photo.
              </AlertDescription>
            </Alert>
            <div className="flex justify-between">
              <Button variant="outline" onClick={() => setShowAspectRatioWarning(false)}>
                Cancel
              </Button>
              <Button onClick={() => {
                setConfirmAspectRatioMismatch(true);
                setShowAspectRatioWarning(false);
                downloadImage();
              }}>
                Download Anyway
              </Button>
            </div>
          </div>
        ) : (
          <>
            <Tabs defaultValue="as-is" onValueChange={handleTabChange}>
              <TabsList className="grid w-full grid-cols-3">
                <TabsTrigger value="as-is">Original</TabsTrigger>
                <TabsTrigger value="physical">Physical Size</TabsTrigger>
                <TabsTrigger value="pixel">Pixel Size</TabsTrigger>
              </TabsList>
              
              {/* Original (As-Is) Option */}
              <TabsContent value="as-is" className="space-y-4">
                <p>Download the image with its current dimensions without any resizing.</p>
              </TabsContent>
              
              {/* Physical Size Options */}
              <TabsContent value="physical" className="space-y-4">
                <RadioGroup defaultValue="preset" onValueChange={(val) => setPhysicalOption(val as DownloadOption)}>
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="preset" id="physical-preset" />
                    <Label htmlFor="physical-preset">Standard ID Sizes</Label>
                  </div>
                  
                  {physicalOption === 'preset' && (
                    <div className="ml-6 space-y-2">
                      {standardPhysicalSizes.map((size) => (
                        <div key={size.value} className="flex items-center space-x-2">
                          <input 
                            type="radio" 
                            id={size.value}
                            name="physicalPreset" 
                            checked={selectedPhysicalPreset.value === size.value}
                            onChange={() => setSelectedPhysicalPreset(size)}
                          />
                          <Label htmlFor={size.value}>
                            {size.label} ({size.physicalWidth}{size.unit} × {size.physicalHeight}{size.unit})
                          </Label>
                        </div>
                      ))}
                    </div>
                  )}
                  
                  <div className="flex items-center space-x-2 mt-2">
                    <RadioGroupItem value="custom" id="physical-custom" />
                    <Label htmlFor="physical-custom">Custom Size</Label>
                  </div>
                  
                  {physicalOption === 'custom' && (
                    <div className="ml-6 grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="custom-width">Width</Label>
                        <Input 
                          id="custom-width"
                          value={customWidth}
                          onChange={(e) => setCustomWidth(e.target.value)}
                          type="number"
                          min="1"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="custom-height">Height</Label>
                        <Input 
                          id="custom-height"
                          value={customHeight}
                          onChange={(e) => setCustomHeight(e.target.value)}
                          type="number"
                          min="1"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="custom-dpi">DPI</Label>
                        <Input 
                          id="custom-dpi"
                          value={customDpi}
                          onChange={(e) => setCustomDpi(e.target.value)}
                          type="number"
                          min="72"
                          max="600"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="custom-unit">Unit</Label>
                        <select 
                          id="custom-unit" 
                          value={customUnit}
                          onChange={(e) => setCustomUnit(e.target.value)}
                          className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background"
                        >
                          <option value="mm">mm</option>
                          <option value="in">inch</option>
                        </select>
                      </div>
                    </div>
                  )}
                </RadioGroup>
              </TabsContent>
              
              {/* Pixel Size Options */}
              <TabsContent value="pixel" className="space-y-4">
                <RadioGroup defaultValue="preset" onValueChange={(val) => setPixelOption(val as DownloadOption)}>
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="preset" id="pixel-preset" />
                    <Label htmlFor="pixel-preset">Standard Pixel Sizes</Label>
                  </div>
                  
                  {pixelOption === 'preset' && (
                    <div className="ml-6 space-y-2">
                      {pixelPresets.map((size) => (
                        <div key={size.value} className="flex items-center space-x-2">
                          <input 
                            type="radio" 
                            id={size.value}
                            name="pixelPreset" 
                            checked={selectedPixelPreset.value === size.value}
                            onChange={() => setSelectedPixelPreset(size)}
                          />
                          <Label htmlFor={size.value}>
                            {size.label}
                          </Label>
                        </div>
                      ))}
                    </div>
                  )}
                  
                  <div className="flex items-center space-x-2 mt-2">
                    <RadioGroupItem value="custom" id="pixel-custom" />
                    <Label htmlFor="pixel-custom">Custom Pixel Size</Label>
                  </div>
                  
                  {pixelOption === 'custom' && (
                    <div className="ml-6 grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="custom-width-px">Width (px)</Label>
                        <Input 
                          id="custom-width-px"
                          value={customWidthPx}
                          onChange={(e) => setCustomWidthPx(e.target.value)}
                          type="number"
                          min="1"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="custom-height-px">Height (px)</Label>
                        <Input 
                          id="custom-height-px"
                          value={customHeightPx}
                          onChange={(e) => setCustomHeightPx(e.target.value)}
                          type="number"
                          min="1"
                        />
                      </div>
                    </div>
                  )}
                </RadioGroup>
              </TabsContent>
            </Tabs>
            
            <DialogFooter>
              <Button variant="outline" onClick={() => onOpenChange(false)}>
                Cancel
              </Button>
              <Button onClick={downloadImage}>
                Download
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default DownloadOptionsModal;
