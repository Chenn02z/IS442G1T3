package IS442.G1T3.IDPhotoGenerator.dto;

public class StateManagementResponse {
    private String topOfStack;

    public StateManagementResponse(String topOfStack) {
        this.topOfStack = topOfStack;
    }

    public String getTopOfStack() {
        return topOfStack;
    }

    public void setTopOfStack(String topOfStack) {
        this.topOfStack = topOfStack;
    }
}