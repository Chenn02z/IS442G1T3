// src/components/GoogleDriveAuth.tsx
import { useState, useEffect } from 'react';
import { Button } from './ui/button';
import { UUID_LOOKUP_KEY } from "@/app/page";

const BACKEND_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

export function GoogleDriveAuth() {
  const [userId, setUserId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Get userId from localStorage on component mount
  useEffect(() => {
    const storedId = localStorage.getItem(UUID_LOOKUP_KEY);
    setUserId(storedId);
  }, []);

  const handleAuth = async () => {
    if (!userId) {
      console.error("User ID not found in localStorage");
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(
        `${BACKEND_URL}/api/google-drive/auth-url?userId=${userId}&redirectUrl=${window.location.origin}/drive-callback`,
        { method: 'GET' }
      );
      
      if (!response.ok) {
        throw new Error(`Auth URL request failed: ${response.status}`);
      }
      
      const data = await response.json();
      window.location.href = data.authUrl;
    } catch (error) {
      console.error("Google Drive auth error:", error);
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col gap-4 p-4 border rounded-lg">
      <h3 className="text-lg font-medium">Connect Google Drive</h3>
      <p className="text-sm text-gray-500">
        Connect your Google Drive to save and access your photos directly from your Drive.
      </p>
      <Button onClick={handleAuth} disabled={!userId || isLoading}>
        {isLoading ? "Connecting..." : "Connect Google Drive"}
      </Button>
      {!userId && (
        <p className="text-xs text-red-500">
          Session ID not found. Please refresh the page.
        </p>
      )}
    </div>
  );
}