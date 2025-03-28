package IS442.G1T3.IDPhotoGenerator.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import IS442.G1T3.IDPhotoGenerator.dto.StateManagementResponse;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import IS442.G1T3.IDPhotoGenerator.service.PhotoSessionService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PhotoSessionServiceImpl implements PhotoSessionService {
    private final PhotoSessionRepository photoSessionRepository;
    private final ImageNewRepository imageNewRepository;

    public PhotoSessionServiceImpl(PhotoSessionRepository photoSessionRepository,
                                 ImageNewRepository imageNewRepository) {
        this.photoSessionRepository = photoSessionRepository;
        this.imageNewRepository = imageNewRepository;
    }

    @Override
    public StateManagementResponse undo(UUID imageId) {
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Parse undo and redo stacks
        String undoStack = photoSession.getUndoStack();
        String redoStack = photoSession.getRedoStack();

        List<String> undoList = new ArrayList<>();
        if (undoStack != null && !undoStack.isBlank()) {
            undoList = new ArrayList<>(Arrays.asList(undoStack.split(",")));
        }

        List<String> redoList = new ArrayList<>();
        if (redoStack != null && !redoStack.isBlank()) {
            redoList = new ArrayList<>(Arrays.asList(redoStack.split(",")));
        }

        if (undoList.size() <= 1) {
            throw new RuntimeException("Nothing to undo.");
        }

        // Pop last from undo and push to redo
        String lastVersion = undoList.remove(undoList.size() - 1);
        redoList.add(lastVersion);

        // Update stacks
        photoSession.setUndoStack(String.join(",", undoList));
        photoSession.setRedoStack(String.join(",", redoList));
        photoSessionRepository.save(photoSession);

        // Return just the top of redo stack
        return new StateManagementResponse(lastVersion);
    }

    @Override
    public StateManagementResponse redo(UUID imageId) {
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Parse undo and redo stacks
        String undoStack = photoSession.getUndoStack();
        String redoStack = photoSession.getRedoStack();

        List<String> undoList = new ArrayList<>();
        if (undoStack != null && !undoStack.isBlank()) {
            undoList = new ArrayList<>(Arrays.asList(undoStack.split(",")));
        }

        List<String> redoList = new ArrayList<>();
        if (redoStack != null && !redoStack.isBlank()) {
            redoList = new ArrayList<>(Arrays.asList(redoStack.split(",")));
        }

        if (redoList.isEmpty()) {
            throw new RuntimeException("Nothing to redo.");
        }

        // Pop last from redo and push to undo
        String versionToRedo = redoList.remove(redoList.size() - 1);
        undoList.add(versionToRedo);

        // Update stacks
        photoSession.setUndoStack(String.join(",", undoList));
        photoSession.setRedoStack(String.join(",", redoList));
        photoSessionRepository.save(photoSession);

        // Return just the next version that could be redone
        String nextRedoVersion = redoList.isEmpty() ? null : redoList.get(redoList.size() - 1);
        return new StateManagementResponse(nextRedoVersion);
    }

    @Override
    public ImageNewEntity confirm(UUID imageId) {
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Get the current version from undo stack
        String undoStack = photoSession.getUndoStack();
        if (undoStack == null || undoStack.isBlank()) {
            throw new RuntimeException("No versions available to confirm");
        }

        // Get the last version from undo stack
        String[] versions = undoStack.split(",");
        String currentVersion = versions[versions.length - 1];

        // Clear redo stack and save
        photoSession.setRedoStack("");
        photoSessionRepository.save(photoSession);

        // Get and return the current version's image
        String currentImageUrl = imageId.toString() + "_" + currentVersion + ".png";
        ImageNewEntity currentImage = imageNewRepository.findByCurrentImageUrl(currentImageUrl);
        
        if (currentImage == null) {
            throw new RuntimeException("Image url not found: " + currentImageUrl);
        }
        
        return currentImage;
    }

    @Override
    public Map<String, List<String>> getHistory(UUID imageId) {
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        Map<String, List<String>> history = new HashMap<>();
        
        // Parse undo stack
        List<String> undoList = new ArrayList<>();
        if (photoSession.getUndoStack() != null && !photoSession.getUndoStack().isBlank()) {
            undoList = Arrays.asList(photoSession.getUndoStack().split(","));
        }
        history.put("undoStack", undoList);

        // Parse redo stack
        List<String> redoList = new ArrayList<>();
        if (photoSession.getRedoStack() != null && !photoSession.getRedoStack().isBlank()) {
            redoList = Arrays.asList(photoSession.getRedoStack().split(","));
        }
        history.put("redoStack", redoList);

        return history;
    }

    @Override
    public ImageNewEntity getLatestVersion(UUID imageId) {
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Get the current version from undo stack
        String undoStack = photoSession.getUndoStack();
        if (undoStack == null || undoStack.isBlank()) {
            throw new RuntimeException("No versions available");
        }

        // Get the last version from undo stack
        String[] versions = undoStack.split(",");
        String currentVersion = versions[versions.length - 1];

        // Get and return the current version's image
        String currentImageUrl = imageId.toString() + "_" + currentVersion + ".png";
        ImageNewEntity currentImage = imageNewRepository.findByCurrentImageUrl(currentImageUrl);
        
        if (currentImage == null) {
            throw new RuntimeException("Image version not found: " + currentImageUrl);
        }
        
        return currentImage;
    }

    @Override
    public List<UUID> getUserImages(UUID userId) {
        List<ImageNewEntity> userImages = imageNewRepository.findByUserId(userId);
        return userImages.stream()
                .map(ImageNewEntity::getImageId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<ImageNewEntity> getUserLatestList(UUID userId) {
        List<UUID> imageIds = getUserImages(userId);
        return imageIds.stream()
                .map(imageId -> {
                    try {
                        return getLatestVersion(imageId);
                    } catch (RuntimeException e) {
                        // Skip images that might have been deleted or have no versions
                        return null;
                    }
                })
                .filter(image -> image != null)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteImage(UUID imageId) {
        // Find all image versions
        List<ImageNewEntity> imageVersions = imageNewRepository.findByImageId(imageId);
        if (imageVersions.isEmpty()) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Delete the photo session first
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession != null) {
            photoSessionRepository.delete(photoSession);
        }

        // Delete all image versions
        for (ImageNewEntity image : imageVersions) {
            imageNewRepository.delete(image);
        }
    }
}
