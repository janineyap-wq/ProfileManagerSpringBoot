package ph.edu.dlsu.lbycpob.profilemanager.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ph.edu.dlsu.lbycpob.profilemanager.model.Friend;
import ph.edu.dlsu.lbycpob.profilemanager.model.Profile;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public List<Profile> getFriendsOf(UUID profileId) {
    List<UUID> friendIds = friendRepository.findByProfileId(profileId).stream()
            .map(Friend::getFriendId)
            .toList();
    return friendIds.isEmpty() ? List.of() : profileRepository.findAllById(friendIds);
}

public Profile lookupFirstMatch(String query) {
    String trimmed = query == null ? "" : query.trim();
    if (trimmed.isEmpty()) {
        throw new IllegalArgumentException("Name field is empty. Please enter a name to search.");
    }
    List<Profile> matches = profileRepository.findByNameContainingIgnoreCaseOrderByNameAsc(trimmed);
    if (matches.isEmpty()) {
        throw new NoSuchElementException("No profile found matching \"" + trimmed + "\".");
    }
    return matches.getFirst();
}

@Transactional
public Profile createProfile(String name) {
    String trimmed = name == null ? "" : name.trim();
    if (trimmed.isEmpty()) {
        throw new IllegalArgumentException("Name field is empty. Please enter a name.");
    }
    if (profileRepository.findByNameIgnoreCase(trimmed).isPresent()) {
        throw new IllegalStateException("A profile named \"" + trimmed + "\" already exists.");
    }
    return profileRepository.save(Profile.builder().name(trimmed).build());
}

@Transactional
public void deleteProfile(UUID id) {
    if (!profileRepository.existsById(id)) {
        throw new NoSuchElementException("Profile not found.");
    }
    profileRepository.deleteById(id); // ON DELETE CASCADE removes related friends rows
}

@Transactional
public void updateStatus(UUID id, String status) {
    String trimmed = status == null ? "" : status.trim();
    if (trimmed.isEmpty()) {
        throw new IllegalArgumentException("Status field is empty.");
    }
    getProfile(id).setStatus(trimmed);
}

@Transactional
public void updateQuote(UUID id, String quote) {
    String trimmed = quote == null ? "" : quote.trim();
    if (trimmed.isEmpty()) {
        throw new IllegalArgumentException("Quote field is empty.");
    }
    getProfile(id).setQuote(trimmed);
}

/** Mode B: paste a URL directly, same as the original saveUrlDirectly(). */
@Transactional
public void updatePictureUrl(UUID id, String pictureUrl) {
    String trimmed = pictureUrl == null ? "" : pictureUrl.trim();
    if (!trimmed.startsWith("https://")) {
        throw new IllegalArgumentException("URL must start with https://");
    }
    getProfile(id).setPicture(trimmed);
}

/**
 * Mode A: upload a file. Compresses to WebP, uploads to the Supabase
 * Storage bucket at avatars/{profileId}.webp (upsert -- always the
 * same path per profile, so re-uploading cleanly replaces the old
 * image instead of accumulating orphaned files), then persists the
 * returned public URL. Returns that URL so the caller can respond
 * with it directly.
 */
@Transactional
public String updatePictureFromUpload(UUID id, MultipartFile file) {
    if (file == null || file.isEmpty()) {
        throw new IllegalArgumentException("No file was uploaded.");
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
        throw new IllegalArgumentException("The selected file is not an image.");
    }
    Profile profile = getProfile(id);

    byte[] original;
    try {
        original = file.getBytes();
    } catch (Exception e) {
        throw new IllegalStateException("Could not read the uploaded file.");
    }

    byte[] webp = imageCompressionService.compressToWebp(original);
    String path = "avatars/" + id + ".webp";
    String publicUrl = supabaseStorageService.uploadAndGetPublicUrl(path, webp, "image/webp");

    profile.setPicture(publicUrl);
    return publicUrl;
}

/**
 * Adds a bidirectional friendship. Checks EACH direction independently
 * rather than assuming they're always in sync -- if the two rows ever
 * became asymmetric (e.g. leftover data from before this method was
 * transactional, or any other partial write), a naive "check forward,
 * then blindly insert both" approach hits the unique constraint on the
 * direction that already exists and rolls back the whole transaction,
 * including the direction that legitimately needed to be inserted.
 */
@Transactional
public String addFriend(UUID profileId, String friendName) {
    Profile self = getProfile(profileId);
    Profile friend = findByNameOrThrow(friendName);

    if (friend.getId().equals(self.getId())) {
        throw new IllegalArgumentException("A profile cannot be friends with itself.");
    }

    boolean forwardExists = friendRepository.existsByProfileIdAndFriendId(self.getId(), friend.getId());
    boolean reverseExists = friendRepository.existsByProfileIdAndFriendId(friend.getId(), self.getId());

    if (forwardExists && reverseExists) {
        throw new IllegalStateException("\"" + friend.getName() + "\" is already a friend.");
    }

    if (!forwardExists) {
        friendRepository.save(Friend.builder().profileId(self.getId()).friendId(friend.getId()).build());
    }
    if (!reverseExists) {
        friendRepository.save(Friend.builder().profileId(friend.getId()).friendId(self.getId()).build());
    }
    return friend.getName();
}
@Transactional
public String removeFriend(UUID profileId, String friendName) {
    Profile friend = findByNameOrThrow(friendName);
    friendRepository.deleteByProfileIdAndFriendId(profileId, friend.getId());
    friendRepository.deleteByProfileIdAndFriendId(friend.getId(), profileId);
    return friend.getName();
}

private Profile findByNameOrThrow(String friendName) {
    String trimmed = friendName == null ? "" : friendName.trim();
    if (trimmed.isEmpty()) {
        throw new IllegalArgumentException("Friend name field is empty.");
    }
    return profileRepository.findByNameIgnoreCase(trimmed)
            .orElseThrow(() -> new NoSuchElementException(
                    "No profile named \"" + trimmed + "\" exists. Add that profile first."));
}
}

