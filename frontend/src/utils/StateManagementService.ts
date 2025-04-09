import { CONFIG } from "../../config";

// Type definitions for API responses
interface ApiResponse<T> {
  status: "success" | "error";
  data: T;
}

interface ImageVersion {
  currentImageUrl: string;
  imageId: string;
  version: number;
  userId: string;
  label: string;
  baseImageUrl: string;
  cropData: string | null;
}

interface HistoryResponse {
  undoStack: string[];
  redoStack: string[];
}

// New interface for undo/redo responses
interface UndoRedoResponse {
  topOfStack: string;
}

/**
 * Service for managing image version history and state
 */
export const StateManagementService = {
  /**
   * Undo the last edit for an image
   */
  async undo(imageId: string): Promise<ApiResponse<UndoRedoResponse>> {
    // console.log('🔄 StateManagementService: Undo request for image:', imageId);
    
    const response = await fetch(`${CONFIG.API_BASE_URL}/api/statemanagement/undo`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ imageId }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      // console.error('❌ StateManagementService: Undo failed:', errorText);
      throw new Error(`Undo failed: ${response.statusText}`);
    }

    const data = await response.json();
    // console.log('✅ StateManagementService: Undo response:', data);
    return data;
  },

  /**
   * Redo a previously undone edit
   */
  async redo(imageId: string): Promise<ApiResponse<UndoRedoResponse>> {
    // console.log('🔄 StateManagementService: Redo request for image:', imageId);
    
    const response = await fetch(`${CONFIG.API_BASE_URL}/api/statemanagement/redo`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ imageId }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('❌ StateManagementService: Redo failed:', errorText);
      throw new Error(`Redo failed: ${response.statusText}`);
    }

    const data = await response.json();
    // console.log('✅ StateManagementService: Redo response:', data);
    return data;
  },

  /**
   * Confirm the current version and clear redo history
   */
  async confirm(imageId: string): Promise<ApiResponse<ImageVersion>> {
    // console.log('🔄 StateManagementService: Confirm request for image:', imageId);
    
    const response = await fetch(`${CONFIG.API_BASE_URL}/api/statemanagement/confirm`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ imageId }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('❌ StateManagementService: Confirm failed:', errorText);
      throw new Error(`Confirm failed: ${response.statusText}`);
    }

    const data = await response.json();
    // console.log('✅ StateManagementService: Confirm response:', data);
    return data;
  },

  /**
   * Get the version history for an image
   */
  async getHistory(imageId: string): Promise<ApiResponse<HistoryResponse>> {
    // console.log('🔄 StateManagementService: Getting history for image:', imageId);
    
    const response = await fetch(
      `${CONFIG.API_BASE_URL}/api/statemanagement/history/${imageId}`
    );

    if (!response.ok) {
      const errorText = await response.text();
      console.error('❌ StateManagementService: Failed to fetch history:', errorText);
      throw new Error(`Failed to fetch history: ${response.statusText}`);
    }

    const data = await response.json();
    // console.log('✅ StateManagementService: History response:', data);
    return data;
  },

  /**
   * Get the latest version of an image
   */
  async getLatestVersion(imageId: string): Promise<ApiResponse<ImageVersion>> {
    const response = await fetch(
      `${CONFIG.API_BASE_URL}/api/statemanagement/latest/${imageId}`
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch latest version: ${response.statusText}`);
    }

    return response.json();
  },

  /**
   * Get the latest image information
   */
  async getLatestImageInfo(imageId: string): Promise<ApiResponse<ImageVersion>> {
    const response = await fetch(
      `${CONFIG.API_BASE_URL}/api/statemanagement/images/${imageId}/latest`
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch latest image info: ${response.statusText}`);
    }

    return response.json();
  },

  /**
   * Get all images for a user
   */
  async getUserImages(userId: string): Promise<ApiResponse<string[]>> {
    const response = await fetch(
      `${CONFIG.API_BASE_URL}/api/statemanagement/user/images/${userId}`
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch user images: ${response.statusText}`);
    }

    return response.json();
  },

  /**
   * Get latest versions of all images for a user
   */
  async getUserLatestList(userId: string): Promise<ApiResponse<ImageVersion[]>> {
    const response = await fetch(
      `${CONFIG.API_BASE_URL}/api/statemanagement/user/latest-list/${userId}`
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch user's latest images: ${response.statusText}`);
    }

    return response.json();
  },

  /**
   * Delete an image and its history
   */
  async deleteImage(imageId: string): Promise<ApiResponse<string>> {
    const response = await fetch(
      `${CONFIG.API_BASE_URL}/api/statemanagement/delete/${imageId}`,
      {
        method: 'DELETE',
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to delete image: ${response.statusText}`);
    }

    return response.json();
  },

  /**
   * Helper function to get full image URL
   */
  getFullImageUrl(imageUrl: string): string {
    if (!imageUrl) return '';
    if (imageUrl.startsWith('http')) return imageUrl;
    return `${CONFIG.API_BASE_URL}/api/images/${imageUrl}`;
  }
};