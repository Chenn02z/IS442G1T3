import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { UUID_LOOKUP_KEY } from "@/app/page";
import { cn } from "@/lib/utils";
import { Target, Users, AtSign } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

interface LoadingScreenProps {
  onFinish: () => void;
}

const guidelines = [
  "Use a solid-colored background (white)",
  "Ensure consistent lighting with no harsh shadows",
  "Avoid glasses, hats, or filters",
  "Shoulders aligned with the camera and at the bottom of the image",
];

const tools = [
  { icon: <Target className="w-5 h-5" />, label: "Face Detection", stage: 1 },
  { icon: <AtSign className="w-5 h-5" />, label: "Feature Analysis", stage: 2 },
  { icon: <Users className="w-5 h-5" />, label: "Matching", stage: 3 },
];

export function LoadingScreen({ onFinish }: LoadingScreenProps) {
  const [progress, setProgress] = useState(0);
  const [activeStage, setActiveStage] = useState(0);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    setIsVisible(true);
    return () => setIsVisible(false);
  }, []);

  useEffect(() => {
    try {
      localStorage.getItem(UUID_LOOKUP_KEY);
    } catch (error) {
      console.error("LocalStorage not available:", error);
    }

    const startTime = Date.now();
    const duration = 3000;
    const stagesCount = tools.length;
    const timePerStage = duration / stagesCount;

    const updateProgress = () => {
      const elapsed = Date.now() - startTime;
      const newProgress = Math.min((elapsed / duration) * 100, 100);
      
      const currentStage = Math.min(Math.ceil(elapsed / timePerStage), stagesCount);
      setActiveStage(currentStage);
      
      if (newProgress < 100) {
        setProgress(newProgress);
        requestAnimationFrame(updateProgress);
      } else {
        setProgress(100);
        setActiveStage(stagesCount);
        setIsVisible(false);
        setTimeout(() => onFinish(), 400);
      }
    };

    const animation = requestAnimationFrame(updateProgress);
    return () => cancelAnimationFrame(animation);
  }, [onFinish]);

  return (
    <AnimatePresence>
      {isVisible && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-background/95 backdrop-blur-sm flex items-center justify-center p-4"
        >
          <motion.div
            initial={{ opacity: 0, y: 40, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 40, scale: 0.95 }}
            transition={{ duration: 0.4, ease: "easeOut" }}
            className="w-full max-w-lg"
          >
            <Card>
              <CardContent className="space-y-12 py-8">
                {/* Sample Image */}
                <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.1 }}
                  className="relative aspect-[4/5] w-48 mx-auto rounded-lg overflow-hidden"
                >
                  <img   
                    src="/sampleImage.jpg" 
                    alt="Sample ID" 
                    className="object-cover w-full h-full rounded-md border"
                  />
                </motion.div>

                {/* Progress Section with Squares */}
                <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.2 }}
                  className="space-y-8"
                >
                  <h2 className="text-xl font-semibold text-gray-700 text-center">
                    Preparing our Tools
                  </h2>
                  
                  <div className="flex justify-center items-center gap-8">
                    {tools.map((tool, index) => (
                      <motion.div
                        key={index}
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.3 + index * 0.1 }}
                        className="relative group"
                      >
                        <div 
                          className={cn(
                            "w-12 h-12 bg-white rounded-lg shadow-lg flex items-center justify-center transition-all duration-500",
                            tool.stage <= activeStage
                              ? "bg-blue-500 text-white transform -translate-y-1" 
                              : "bg-white text-gray-400 hover:bg-gray-50"
                          )}
                        >
                          {tool.icon}
                        </div>
                        <div className="absolute -bottom-6 left-1/2 -translate-x-1/2 opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap text-xs text-gray-600">
                          {tool.label}
                        </div>
                        {index < tools.length - 1 && (
                          <div className={cn(
                            "absolute top-1/2 -right-8 w-8 h-0.5 transition-colors duration-500",
                            tool.stage < activeStage ? "bg-blue-500" : "bg-gray-200"
                          )} />
                        )}
                      </motion.div>
                    ))}
                  </div>
                </motion.div>

                {/* Guidelines */}
                <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.4 }}
                  className="space-y-4 max-w-md mx-auto"
                >
                  <h3 className="font-semibold text-gray-700">Photo Guidelines:</h3>
                  <ul className="space-y-2">
                    {guidelines.map((guideline, index) => (
                      <motion.li 
                        key={index}
                        initial={{ opacity: 0, x: -20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: 0.5 + index * 0.1 }}
                        className="flex items-start gap-2"
                      >
                        <div className="mt-1.5">
                          <div className="h-2 w-2 rounded-full bg-gray-400" />
                        </div>
                        <span className="text-sm text-gray-500">{guideline}</span>
                      </motion.li>
                    ))}
                  </ul>
                </motion.div>
              </CardContent>
            </Card>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}