// src/components/GoogleDriveImagePicker.tsx
import { useState, useEffect } from 'react';
import { Button } from './ui/button';
import { UUID_LOOKUP_KEY } from '@/app/page';

const BACKEND_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

export function GoogleDriveImagePicker({ onSelect }) {
  interface Image {
    id: string;
    name: string;
    thumbnail?: string;
  }

  const [images, setImages] = useState<Image[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(6);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    const fetchImages = async () => {
      await loadImages(page, size);
    };
    fetchImages();
  }, [page, size]);

  const connectToDrive = async () => {
    try {
      const userId = localStorage.getItem(UUID_LOOKUP_KEY);
      if (!userId) {
        setError('User ID not found. Please refresh the page or log in again.');
        return;
      }

      const response = await fetch(
        `${BACKEND_URL}/api/google-drive/auth-url?userId=${userId}&redirectUrl=${window.location.origin}/drive-callback`,
        { method: 'GET' }
      );

      if (!response.ok) {
        throw new Error('Failed to get authentication URL');
      }

      const data = await response.json();
      window.location.href = data.authUrl;
    } catch (error) {
      console.error('Failed to start Google Drive authentication', error);
      setError('Failed to connect to Google Drive: ' + (error.message || 'Unknown error'));
    }
  };

  const loadImages = async (targetPage: number, targetSize: number) => {
    setLoading(true);
    setError('');

    try {
      const userId = localStorage.getItem(UUID_LOOKUP_KEY);
      if (!userId) {
        setError('User ID not found. Please refresh the page or log in again.');
        setImages([]);
        return;
      }

      const timestamp = Date.now();
      const response = await fetch(`${BACKEND_URL}/api/google-drive/list-images?userId=${userId}&page=${targetPage}&size=${targetSize}&_t=${timestamp}`);

      if (response.status === 401 || response.status === 404) {
        // Either unauthorized or storage preference not found means we need authentication
        setError('Google Drive authentication required. Please connect your Google Drive account.');
        setImages([]);
        return;
      }

      if (!response.ok) {
        let errorMessage = `Server error: ${response.status} ${response.statusText}`;
        try {
          const errorData = await response.json();
          if (errorData?.error) {
            errorMessage = `Error: ${errorData.error}`;
          }
        } catch (e) {
          console.error('Could not parse error response', e);
        }

        setError(errorMessage);
        setImages([]);
        return;
      }

      const data = await response.json();

      if (Array.isArray(data.content)) {
        setImages(data.content);
        setTotalElements(data.totalElements);
        setTotalPages(data.totalPages);
      } else {
        console.error('Expected array but got:', typeof data, data);
        setImages([]);
        setError('Invalid data format received from server');
      }
    } catch (error) {
      console.error('Failed to load images', error);
      setError(error.message || 'Failed to load images from Google Drive');
      setImages([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSelect = (fileId: string) => {
    onSelect(fileId);
  };

  const handlePreviousPage = () => {
    if (page > 0) {
      setPage((prevPage) => prevPage - 1);
    }
  };

  const handleNextPage = () => {
    if (page < totalPages - 1) {
      setPage((prevPage) => prevPage + 1);
    }
  };

  const needsAuth = error.includes('Google Drive authentication required');

  return (
    <div className="p-4 border rounded-lg">
      <h3 className="text-lg font-medium mb-4">Select from Google Drive</h3>

      {loading ? (
        <div className="flex justify-center p-8">Loading...</div>
      ) : error ? (
        <div className="text-center p-8">
          <p className="text-red-500 mb-4">{error}</p>
          {needsAuth && (
            <Button
              onClick={connectToDrive}
              className="bg-blue-600 hover:bg-blue-700 text-white"
            >
              Connect to Google Drive
            </Button>
          )}
        </div>
      ) : (
        <>
          <div className="grid grid-cols-3 gap-4">
            {images.map((image) => (
              <div
                key={image.id}
                className="cursor-pointer border rounded-md overflow-hidden hover:border-blue-500"
                onClick={() => handleSelect(image.id)}
              >
                <div className="relative w-full aspect-square">
                  {image.thumbnail ? (
                    <img
                      src={image.thumbnail}
                      alt={image.name}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        console.error('Error loading thumbnail for image:', image.name);
                        e.currentTarget.style.display = 'none';
                      }}
                    />
                  ) : (
                    <div className="w-full aspect-square flex items-center justify-center bg-gray-100 text-gray-800 text-4xl font-bold">
                      {(image.name || 'Image').charAt(0).toUpperCase()}
                    </div>
                  )}
                </div>
                <div className="p-2 text-xs truncate">{image.name}</div>
              </div>
            ))}
          </div>

          <div className="flex justify-between mt-4">
            <Button onClick={handlePreviousPage} disabled={page === 0 || loading}>
              Previous
            </Button>
            <div className="text-sm text-gray-500 self-center">
              Page {page + 1} of {totalPages}
            </div>
            <Button onClick={handleNextPage} disabled={page >= totalPages - 1 || loading}>
              Next
            </Button>
          </div>
        </>
      )}

      {images.length === 0 && !loading && !error && (
        <div className="text-center p-8 text-gray-500">
          No images found in your Google Drive
        </div>
      )}
    </div>
  );
}
