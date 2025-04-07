"use client";
import { useEffect, useState } from 'react';
import { UUID_LOOKUP_KEY } from '../page';
import { useRouter, useSearchParams } from 'next/navigation';
import { Card, CardContent } from "@/components/ui/card";

const BACKEND_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

export default function DriveCallback() {
  const [status, setStatus] = useState('Processing your Google Drive access...');
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    async function handleCallback() {
      try {
        // Parse the URL for code parameter
        const code = searchParams.get('code');
        const state = searchParams.get('state'); // This should be the userId
        
        if (!code || !state) {
          setStatus('Authentication failed: Missing parameters');
          return;
        }
        
        // Send the code to your backend
        const response = await fetch(`${BACKEND_URL}/api/google-drive/auth-callback`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body: new URLSearchParams({
            userId: state,
            code: code,
            redirectUrl: window.location.origin + '/drive-callback'
          })
        });
        
        if (response.ok) {
          setStatus('Google Drive connected successfully!');
          // Navigate back to main page after 2 seconds
          setTimeout(() => router.push('/'), 2000);
        } else {
          setStatus('Failed to connect Google Drive. Please try again.');
        }
      } catch (error) {
        console.error('Error during callback:', error);
        setStatus('An error occurred. Please try again.');
      }
    }
    
    handleCallback();
  }, [searchParams, router]);
  
  return (
    <div className="flex items-center justify-center h-screen">
      <Card className="max-w-md">
        <CardContent className="p-8 text-center">
          <h1 className="text-xl font-bold mb-4">Google Drive Authentication</h1>
          <div className="animate-pulse mb-4">
            <div className="h-2 bg-slate-200 rounded"></div>
            <div className="h-2 bg-slate-200 rounded mt-2"></div>
          </div>
          <p>{status}</p>
        </CardContent>
      </Card>
    </div>
  );
}