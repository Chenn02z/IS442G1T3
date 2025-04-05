import React from "react";
import { ShieldCheck, XCircle, CheckCircle } from "lucide-react";
import { Alert } from "./ui/alert";

export type ComplianceDetailType = {
  checkName: string;
  status: "PASS" | "FAIL";
  message: string;
  category: string;
};

export type ComplianceResultType = {
  complianceCheckStatus: "PASS" | "FAIL";
  message: string;
  details?: ComplianceDetailType[];
};

type ComplianceResultDisplayProps = {
  result: ComplianceResultType;
  showDetails?: boolean;
  onRequestResize?: () => void;
};

export const hasDimensionFailure = (result: ComplianceResultType): boolean => {
  return (
    result.details?.some(
      (detail) => detail.category === "dimensions" && detail.status === "FAIL"
    ) ?? false
  );
};

export const ComplianceResultDisplay: React.FC<
  ComplianceResultDisplayProps
> = ({ result, showDetails = false }) => {
  const isPassed = result.complianceCheckStatus === "PASS";

  return (
    <Alert
      className={`w-full p-3 rounded-md flex flex-col gap-2 ${
        isPassed
          ? "bg-green-50 text-green-700 border border-green-200"
          : "bg-red-50 text-red-700 border border-red-200"
      }`}
    >
      <div className="flex items-center gap-2">
        {isPassed ? (
          <ShieldCheck className="h-5 w-5 flex-shrink-0" />
        ) : (
          <ShieldCheck className="h-5 w-5 flex-shrink-0" />
        )}
        <div className="text-sm">
          <div className="font-medium">
            {isPassed ? "Compliance Verified" : "Compliance Failed"}
          </div>
        </div>
      </div>

      {/* Detailed check results */}
      {showDetails && result.details && result.details.length > 0 && (
        <div className="mt-3 space-y-2">
          <div className="text-sm font-medium">Check Details:</div>
          <div className="space-y-2">
            {result.details.map((detail, index) => (
              <div
                key={index}
                className={`p-2 text-sm rounded-md ${
                  detail.status === "PASS"
                    ? "bg-green-50 border border-green-100"
                    : "bg-red-50 border border-red-100"
                }`}
              >
                <div
                  className={`flex items-center ${
                    detail.status === "PASS" ? "text-green-600" : "text-red-600"
                  }`}
                >
                  {detail.status === "PASS" ? (
                    <CheckCircle className="h-3 w-3" />
                  ) : (
                    <XCircle className="h-3 w-3" />
                  )}
                  <span className="font-medium capitalize mx-1">
                    {detail.category} Check:
                  </span>
                  <span>{detail.status}</span>
                </div>
                <div className="mt-1 text-xs text-gray-600">
                  {detail.message}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </Alert>
  );
};

export default ComplianceResultDisplay;
