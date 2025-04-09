import React, { useState } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { CheckCircle, XCircle, ArrowLeft, ArrowRight } from 'lucide-react';
import { CONFIG } from '../../config';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { ComplianceResultType } from './ComplianceResultDisplay';
import { useUpload } from '@/context/UploadContext';

// Custom checkbox component
const Checkbox = ({ id, checked, onCheckedChange }: {
  id: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
}) => {
  return (
    <input
      type="checkbox"
      id={id}
      checked={checked}
      onChange={(e) => onCheckedChange(e.target.checked)}
      className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
    />
  );
};

interface ComplianceCheckDialogProps {
  imageId?: string;
  buttonText?: string;
  buttonVariant?: 'default' | 'destructive' | 'outline' | 'secondary' | 'ghost' | 'link';
  buttonSize?: 'default' | 'sm' | 'lg' | 'icon';
  onCheckComplete?: (result: ComplianceResultType) => void;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  initialTab?: 'check' | 'resize';
}

// Update the check for dimension failures to use the new details structure
const hasDimensionFailure = (result: ComplianceResultType): boolean => {
  return result.details?.some(detail => 
    detail.category === 'dimensions' && detail.status === 'FAIL'
  ) ?? false;
};

export function ComplianceCheckDialog({
  imageId: initialImageId,
  buttonText = 'Check Compliance',
  buttonVariant = 'default',
  buttonSize = 'default',
  onCheckComplete,
  open: externalOpen,
  onOpenChange: externalOpenChange,
  initialTab = 'check'
}: ComplianceCheckDialogProps) {
    const { getFullImageUrl, setSelectedImageUrl } = useUpload();
  const [internalOpen, setInternalOpen] = useState(false);
  
  const isOpen = externalOpen !== undefined ? externalOpen : internalOpen;
  const setIsOpen = (value: boolean) => {
    if (externalOpenChange) {
      externalOpenChange(value);
    } else {
      setInternalOpen(value);
    }
  };
  
  const [imageId, setImageId] = useState<string>(initialImageId || '');
  const [countryCode, setCountryCode] = useState<string>('SG');
  const [loading, setLoading] = useState<boolean>(false);
  const [resizing, setResizing] = useState<boolean>(false);
  const [result, setResult] = useState<ComplianceResultType | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<string>(initialTab);
  const [maintainAspectRatio, setMaintainAspectRatio] = useState<boolean>(true);
  const [allowCropping, setAllowCropping] = useState<boolean>(true);
  const [originalImageUrl, setOriginalImageUrl] = useState<string | null>(null);
  const [resizedImageUrl, setResizedImageUrl] = useState<string | null>(null);
  const [imageVersion, setImageVersion] = useState<number>(1);

  React.useEffect(() => {
    if (initialImageId) {
      setImageId(initialImageId);
      setResult(null);
      setError(null);
      setActiveTab('check');
      setOriginalImageUrl(null);
      setResizedImageUrl(null);
      setImageVersion(1);
    }
  }, [initialImageId]);

  const checkCompliance = async () => {
    if (!imageId) {
      setError('Please enter an image ID');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const queryParams = new URLSearchParams({
        imageId: imageId
      });

      if (countryCode && countryCode !== 'ANY') {
        queryParams.append('countryCode', countryCode);
      } else {
        queryParams.append('countryCode', 'SG');
      }

      const response = await fetch(`${CONFIG.API_BASE_URL}/api/compliance/check?${queryParams.toString()}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      console.log(response);
      if (!response.ok) {
        throw new Error(`Error: ${response.status}`);
      }

      const data = await response.json() as ComplianceResultType;
      setResult(data);
      
      if (onCheckComplete) {
        onCheckComplete(data);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'An error occurred while checking compliance';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const resizeImage = async () => {
    if (!imageId) {
      setError('No image ID provided');
      return;
    }

    setResizing(true);
    setError(null);

    try {
      const queryParams = new URLSearchParams({
        imageId: imageId,
        maintainAspectRatio: maintainAspectRatio.toString(),
        allowCropping: allowCropping.toString()
      });

      if (countryCode && countryCode !== 'ANY') {
        queryParams.append('countryCode', countryCode);
      } else {
        queryParams.append('countryCode', 'SG');
      }

      const response = await fetch(`${CONFIG.API_BASE_URL}/api/images/resize?${queryParams.toString()}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`Error: ${response.status}`);
      }

      const resizedImage = await response.json();
      
      setImageId(resizedImage.imageId);
      setImageVersion(resizedImage.version);
      setSelectedImageUrl(resizedImage.currentImageUrl);
      setResizedImageUrl(getFullImageUrl(resizedImage.currentImageUrl));
      
      await checkCompliance();
      
      setActiveTab('check');
      
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'An error occurred while resizing image';
      setError(errorMessage);
    } finally {
      setResizing(false);
    }
  };

  const resetDialog = () => {
    if (!initialImageId) {
      setImageId('');
    }
    setCountryCode('SG');
    setResult(null);
    setError(null);
    setActiveTab('check');
    setOriginalImageUrl(null);
    setResizedImageUrl(null);
    setMaintainAspectRatio(true);
    setAllowCropping(true);
    setImageVersion(1);
  };

  return (
    <Dialog open={isOpen} onOpenChange={(newOpen) => {
      setIsOpen(newOpen);
      if (!newOpen) {
        resetDialog();
      }
    }}>
      <DialogTrigger asChild>
        <Button variant={buttonVariant} size={buttonSize}>
          {buttonText}
        </Button>
      </DialogTrigger>
      
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Check Photo Compliance</DialogTitle>
          <DialogDescription>
            Verify if your photo meets ID/passport requirements
          </DialogDescription>
        </DialogHeader>
        
        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger 
              value="check" 
              className={activeTab === 'check' ? 'border-primary' : 'border-transparent'}
              onClick={() => setActiveTab('check')}
            >
              Check Compliance
            </TabsTrigger>
            <TabsTrigger 
              value="resize" 
              className={activeTab === 'resize' ? 'border-primary' : 'border-transparent'}
              disabled={!result || result.complianceCheckStatus !== 'FAIL' || !hasDimensionFailure(result)}
              onClick={() => setActiveTab('resize')}
            >
              Auto-Resize
            </TabsTrigger>
          </TabsList>
          
          {activeTab === 'check' && (
            <TabsContent value="check" className="mt-4">
              <div className="grid gap-4 py-2">
                {!initialImageId && (
                  <div className="grid grid-cols-4 items-center gap-4">
                    <Label htmlFor="imageId" className="text-right">
                      Image ID
                    </Label>
                    <Input
                      id="imageId"
                      placeholder="Enter the UUID of your image"
                      value={imageId}
                      onChange={(e) => setImageId(e.target.value)}
                      className="col-span-3"
                    />
                  </div>
                )}
                
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="country" className="text-right">
                    Country
                  </Label>
                  <Select value={countryCode} onValueChange={setCountryCode}>
                    <SelectTrigger id="country" className="col-span-3">
                      <SelectValue placeholder="Select a country standard" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ANY">Any Standard</SelectItem>
                      <SelectItem value="SG">Singapore</SelectItem>
                      <SelectItem value="US">United States</SelectItem>
                      <SelectItem value="EU">European Union</SelectItem>
                      <SelectItem value="GB">United Kingdom</SelectItem>
                      <SelectItem value="IN">India</SelectItem>
                      <SelectItem value="CN">China</SelectItem>
                      <SelectItem value="CA">Canada</SelectItem>
                      <SelectItem value="AU">Australia</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                
                {imageVersion > 1 && (
                  <div className="text-xs text-muted-foreground mt-1">
                    Using image version {imageVersion}
                  </div>
                )}
              </div>
              
              {error && (
                <Alert variant="destructive" className="my-4">
                  <XCircle className="h-4 w-4" />
                  <AlertTitle>Error</AlertTitle>
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              {result && (
                <Alert 
                  variant={result.complianceCheckStatus === 'PASS' ? 'default' : 'destructive'}
                  className="my-4"
                >
                  {result.complianceCheckStatus === 'PASS' ? (
                    <CheckCircle className="h-4 w-4 text-green-500" />
                  ) : (
                    <XCircle className="h-4 w-4" />
                  )}
                  <AlertTitle>
                    {result.complianceCheckStatus === 'PASS' ? 'Compliance Verified' : 'Compliance Failed'}
                  </AlertTitle>                  
                  {result.details && result.details.length > 0 && (
                    <div className="mt-3 space-y-2">
                      <div className="text-sm font-medium">Check Details:</div>
                      <div className="space-y-2">
                        {result.details.map((detail, index) => (
                          <div 
                            key={index}
                            className={`p-2 text-sm rounded-md ${
                              detail.status === 'PASS' 
                                ? 'bg-green-50 border border-green-100' 
                                : 'bg-red-50 border border-red-100'
                            }`}
                          >
                            <div className={`flex items-center ${
                              detail.status === 'PASS' 
                                ? 'text-green-600' 
                                : 'text-red-600'
                            }`}>
                              {detail.status === 'PASS' ? (
                                <CheckCircle className="h-3 w-3" />
                              ) : (
                                <XCircle className="h-3 w-3" />
                              )}
                              <span className="font-medium capitalize mx-1">
                                {detail.category} Check: 
                              </span>
                              <span>
                                {detail.status}
                              </span>
                            </div>
                            <div className="mt-1 text-xs text-gray-600">{detail.message}</div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                  
                  {result.complianceCheckStatus === 'FAIL' && 
                   result.details?.some(detail => 
                     detail.category === 'dimensions' && detail.status === 'FAIL'
                   ) && (
                    <Button 
                      className="mt-2 w-full" 
                      size="sm" 
                      onClick={() => setActiveTab('resize')}
                    >
                      Auto-resize to fix dimensions
                    </Button>
                  )}
                </Alert>
              )}
              
              <DialogFooter className="mt-4">
                <Button onClick={checkCompliance} disabled={loading}>
                  {loading ? 'Checking...' : 'Check'}
                </Button>
              </DialogFooter>
            </TabsContent>
          )}
          
          {activeTab === 'resize' && (
            <TabsContent value="resize" className="mt-4">
              <div className="space-y-4">
                <div className="text-sm text-muted-foreground">
                  Your image does not meet the required dimensions. Use the options below to automatically resize it.
                </div>
                
                <div className="space-y-4">
                  <div className="flex items-center space-x-2">
                    <Checkbox 
                      id="aspectRatio" 
                      checked={maintainAspectRatio} 
                      onCheckedChange={(checked) => setMaintainAspectRatio(checked)}
                    />
                    <Label htmlFor="aspectRatio">Maintain aspect ratio</Label>
                  </div>
                  
                  <div className="flex items-center space-x-2">
                    <Checkbox 
                      id="allowCrop" 
                      checked={allowCropping} 
                      onCheckedChange={(checked) => setAllowCropping(checked)}
                    />
                    <Label htmlFor="allowCrop">Allow cropping if needed</Label>
                  </div>
                  
                  {originalImageUrl && resizedImageUrl && (
                    <div className="grid grid-cols-2 gap-2 pt-2">
                      <div className="flex flex-col items-center">
                        <div className="text-xs text-muted-foreground mb-1">Original</div>
                        <img 
                          src={originalImageUrl} 
                          alt="Original" 
                          className="w-full h-auto object-contain border rounded-md"
                          style={{ maxHeight: '140px' }}
                        />
                      </div>
                      <div className="flex flex-col items-center">
                        <div className="text-xs text-muted-foreground mb-1">Resized</div>
                        <img 
                          src={resizedImageUrl} 
                          alt="Resized" 
                          className="w-full h-auto object-contain border rounded-md"
                          style={{ maxHeight: '140px' }}
                        />
                      </div>
                    </div>
                  )}
                </div>
              </div>
              
              {error && (
                <Alert variant="destructive" className="my-4">
                  <XCircle className="h-4 w-4" />
                  <AlertTitle>Error</AlertTitle>
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}
              
              <DialogFooter className="mt-4 flex justify-between items-center">
                <Button 
                  variant="outline"
                  onClick={() => setActiveTab('check')}
                  disabled={resizing}
                >
                  <ArrowLeft className="h-4 w-4 mr-2" />
                  Back
                </Button>
                <Button 
                  onClick={resizeImage} 
                  disabled={resizing}
                >
                  {resizing ? 'Resizing...' : 'Resize Image'}
                  {!resizing && <ArrowRight className="h-4 w-4 ml-2" />}
                </Button>
              </DialogFooter>
            </TabsContent>
          )}
        </Tabs>
      </DialogContent>
    </Dialog>
  );
} 