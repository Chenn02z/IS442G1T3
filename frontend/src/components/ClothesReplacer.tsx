"use client"

import { useState, useEffect } from "react"
import { ShirtIcon } from "lucide-react"
import { useUpload } from "@/context/UploadContext"
import { useToast } from "@/hooks/use-toast"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip"
import { Button } from "@/components/ui/button"

export const ClothesReplacer = () => {
  const {
    uploadedFile,
    setUploadedFile,
    selectedImageId,
    selectedImageUrl,
    setSelectedImageUrl,
    refreshImages,
    getFullImageUrl,
    restoreCurrentImageUrl,
    setIsCropping,
    isCropping,
  } = useUpload()
  const [workingFile, setWorkingFile] = useState<File | null>(null)
  const { toast } = useToast()
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [displayImageUrl, setDisplayImageUrl] = useState<string | null>(null)
  const [selectedStyle, setSelectedStyle] = useState<string | null>(null)

  // API base URL
  const API_BASE_URL = "http://localhost:8080"

  // Sample clothing styles
  const clothingStyles = [
    { id: "formal", name: "Casual", description: "Everyday comfortable clothing"},
    { id: "casual", name: "Formal", description: "Business and formal attire" },
    { id: "sporty", name: "Sporty", description: "Athletic and activewear" },
    { id: "vintage", name: "Vintage", description: "Classic retro styles" },
  ]

  // Initialize workingFile when dialog opens
  useEffect(() => {
    if (isDialogOpen && uploadedFile) {
      setWorkingFile(uploadedFile)
    }
  }, [isDialogOpen, uploadedFile])

  useEffect(() => {
    if (selectedImageUrl) {
      setDisplayImageUrl(getFullImageUrl(selectedImageUrl))
    }
  }, [selectedImageUrl, getFullImageUrl])

  const handleClothesReplacement = async () => {
    if (!selectedImageUrl || !selectedImageId || selectedStyle !== "casual") {
      toast({
        title: "Selection required",
        description: "Please select the Casual style.",
      })
      return
    }

    try {
      // Show loading toast
      toast({
        title: "Processing image",
        description: "Replacing clothes...",
      })

      // Call the Java endpoint
      const response = await fetch(`${API_BASE_URL}/api/replace-clothes/${selectedImageId}`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      })

      if (!response.ok) {
        throw new Error("Failed to process image for clothes replacement")
      }

      const processedImage = await response.json()
      console.log("Clothes replacement response:", processedImage)

      if (processedImage && processedImage.currentImageUrl) {
        // Update UI with new image
        setSelectedImageUrl(processedImage.currentImageUrl)
        refreshImages()

        if (selectedImageId) {
          await restoreCurrentImageUrl(selectedImageId)
        }

        toast({
          title: "Clothes replaced successfully.",
        })
      } else {
        throw new Error("Invalid response from server")
      }

      // Close the dialog
      setIsDialogOpen(false)
    } catch (error: any) {
      console.error("Clothes replacement error:", error)
      toast({
        title: "Error processing image",
        description: error.message,
        variant: "destructive",
      })
    }
  }

  const handleDialogClose = (open: boolean) => {
    // If the dialog is being closed and it wasn't closed programmatically
    if (!open) {
      // Reset the state
      setSelectedStyle(null)
      setWorkingFile(null)
      setIsDialogOpen(false)
    }
  }

  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <div
            className="border border-1 p-1 rounded-md cursor-pointer"
            onClick={() => {
              if (!selectedImageUrl) {
                toast({
                  title: "No image selected",
                  description: "Please upload an image first.",
                })
                return
              }

              if (isCropping) {
                setIsCropping(false)
                setTimeout(() => {
                  setIsDialogOpen(true)
                }, 100)
              } else {
                setIsDialogOpen(true)
              }
            }}
          >
            <ShirtIcon
              className="h-5 w-5"
              style={{ opacity: selectedImageUrl ? 1 : 0.5, cursor: selectedImageUrl ? "pointer" : "not-allowed" }}
            />
          </div>
        </TooltipTrigger>
        <TooltipContent side="right">
          <p>Replace Clothes</p>
        </TooltipContent>
      </Tooltip>

      <Dialog open={isDialogOpen} onOpenChange={handleDialogClose}>
        <DialogContent className="max-w-4xl">
          <DialogHeader>
            <DialogTitle>Replace Clothes</DialogTitle>
            <DialogDescription>Choose a clothing style to apply to your image</DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-4">
            {/* Image preview */}
            {displayImageUrl && (
              <div className="w-full flex justify-center">
                <img
                  src={displayImageUrl || "/placeholder.svg"}
                  alt="Upload preview"
                  className="max-h-[300px] object-contain"
                />
              </div>
            )}

            {/* Style selection */}
            <div className="grid grid-cols-2 gap-4">
              {clothingStyles.map((style) => (
                <Button
                  key={style.id}
                  variant={selectedStyle === style.id ? "default" : "outline"}
                  className="flex flex-col gap-2 h-auto p-6"
                  onClick={() => setSelectedStyle(style.id)}
                >
                  <div className="text-lg font-semibold">{style.name}</div>
                  <div className="text-sm text-muted-foreground">{style.description}</div>
                </Button>
              ))}
            </div>

            {/* Action buttons */}
            <div className="flex justify-end gap-2 mt-4">
              <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="default"
                onClick={handleClothesReplacement}
                disabled={!selectedImageUrl || !selectedImageId || selectedStyle !== "casual"}
              >
                Apply Style
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </TooltipProvider>
  )
}

