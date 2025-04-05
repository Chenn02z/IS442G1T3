import React from 'react';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Label } from '@/components/ui/label';

// ID photo presets with physical dimensions
export const idPhotoPresets = [
  { 
    label: "Freeform", 
    value: "freeform",
    physicalWidth: null,
    physicalHeight: null,
    unit: null,
    dpi: null
  },
  { 
    label: "US Passport (2×2\")", 
    value: "us-passport", 
    physicalWidth: 2, 
    physicalHeight: 2,
    unit: "in",
    dpi: 300
  },
  { 
    label: "Singapore Passport (35×45mm)", 
    value: "sg-passport", 
    physicalWidth: 35, 
    physicalHeight: 45,
    unit: "mm",
    dpi: 300
  },
  { 
    label: "EU Passport (35×45mm)", 
    value: "eu-passport", 
    physicalWidth: 35, 
    physicalHeight: 45,
    unit: "mm",
    dpi: 300
  },
  { 
    label: "India Passport (35×45mm)", 
    value: "india-passport", 
    physicalWidth: 35, 
    physicalHeight: 45,
    unit: "mm",
    dpi: 300
  },
  { 
    label: "US Visa (2×2\")", 
    value: "us-visa", 
    physicalWidth: 2, 
    physicalHeight: 2,
    unit: "in",
    dpi: 300
  },
  { 
    label: "Australia Passport (35×45mm)", 
    value: "australia-passport", 
    physicalWidth: 35, 
    physicalHeight: 45,
    unit: "mm",
    dpi: 300
  }
];

interface PhotoSizeSelectorProps {
  selectedSize: string;
  onSizeChange: (preset: any) => void;
}

const PhotoSizeSelector: React.FC<PhotoSizeSelectorProps> = ({ 
  selectedSize, 
  onSizeChange 
}) => {
  const handleSizeChange = (value: string) => {
    const selected = idPhotoPresets.find(preset => preset.value === value);
    onSizeChange(selected || idPhotoPresets[0]);
  };

  return (
    <div className="space-y-2">
      <Label htmlFor="photo-size">Photo Size</Label>
      <Select value={selectedSize} onValueChange={handleSizeChange}>
        <SelectTrigger id="photo-size" className="w-full">
          <SelectValue placeholder="Select a photo size" />
        </SelectTrigger>
        <SelectContent>
          {idPhotoPresets.map((preset) => (
            <SelectItem key={preset.value} value={preset.value}>
              {preset.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
};

export default PhotoSizeSelector;
