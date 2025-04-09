package IS442.G1T3.IDPhotoGenerator.service.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class StorageProviderFactory {
    
    private final Map<String, StorageProvider> storageProviders;
    
    @Autowired
    public StorageProviderFactory(Map<String, StorageProvider> storageProviders) {
        this.storageProviders = storageProviders;
    }
    
    public StorageProvider getStorageProvider(UUID userId, String preferenceType) {
        // Get user preference or use default
        String providerType = preferenceType != null ? 
                preferenceType : "localStorageProvider";
                
        // Return the provider or fallback to local
        return storageProviders.getOrDefault(providerType, 
                storageProviders.get("localStorageProvider"));
    }
}