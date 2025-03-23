import { CONFIG } from "../../config";

/**
 * Service for managing version history
 * Note: These functions are currently commented out as they depend on API endpoints 
 * that have not been implemented yet.
 */
export const VersionHistoryService = {
  /**
   * Get history for an image
   */
  async getHistory(imageId: string) {
    // const response = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/history/${imageId}`);
    // if (!response.ok) throw new Error("Failed to fetch history");
    // return await response.json();
    
    // Mock implementation
    return {
      status: "success",
      data: {
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
      }
    };
  },
  
  /**
   * Get latest version for an image
   */
  async getLatestVersion(imageId: string) {
    // const response = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/latest/${imageId}`);
    // if (!response.ok) throw new Error("Failed to fetch latest version");
    // return await response.json();
    
    // Mock implementation
    return {
      status: "success",
      data: {
        imageId,
        version: "3"
      }
    };
  },
  
  /**
   * Undo the last edit
   */
  async undo(imageId: string) {
    // const response = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/undo`, {
    //   method: "POST",
    //   headers: { "Content-Type": "application/json" },
    //   body: JSON.stringify({ imageId })
    // });
    // if (!response.ok) throw new Error("Failed to undo");
    // return await response.json();
    
    // Mock implementation
    return {
      status: "success",
      data: {
        version: "2"
      }
    };
  },
  
  /**
   * Redo an undone edit
   */
  async redo(imageId: string) {
    // const response = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/redo`, {
    //   method: "POST",
    //   headers: { "Content-Type": "application/json" },
    //   body: JSON.stringify({ imageId })
    // });
    // if (!response.ok) throw new Error("Failed to redo");
    // return await response.json();
    
    // Mock implementation
    return {
      status: "success",
      data: {
        version: "3"
      }
    };
  },
  
  /**
   * Confirm the current version
   */
  async confirm(imageId: string) {
    // const response = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/confirm`, {
    //   method: "POST",
    //   headers: { "Content-Type": "application/json" },
    //   body: JSON.stringify({ imageId })
    // });
    // if (!response.ok) throw new Error("Failed to confirm version");
    // return await response.json();
    
    // Mock implementation
    return {
      status: "success",
      data: {
        version: "3"
      }
    };
  },
  
  /**
   * Get all image IDs for a user
   */
  async getUserImages(userId: string) {
    // const response = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/user/images/${userId}`);
    // if (!response.ok) throw new Error("Failed to fetch user images");
    // return await response.json();
    
    // Mock implementation
    return {
      status: "success",
      data: ["uuid1", "uuid2", "uuid3"]
    };
  },
  
  /**
   * Get latest version for each of user's images
   */
  async getUserLatestVersions(userId: string) {
    // const response = await fetch(`${CONFIG.API_BASE_URL}/statemanagement/user/latest-list/${userId}`);
    // if (!response.ok) throw new Error("Failed to fetch latest versions");
    // return await response.json();
    
    // Mock implementation
    return {
      status: "success",
      data: [
        { imageId: "uuid1", version: "3" },
        { imageId: "uuid2", version: "1" },
        { imageId: "uuid3", version: "5" }
      ]
    };
  }
};