package ph.edu.dlsu.lbycpob.profilemanager.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ph.edu.dlsu.lbycpob.profilemanager.model.Friend;
import ph.edu.dlsu.lbycpob.profilemanager.model.Profile;
import ph.edu.dlsu.lbycpob.profilemanager.repository.FriendRepository;
import ph.edu.dlsu.lbycpob.profilemanager.repository.ProfileRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final FriendRepository friendRepository;
    private final ImageCompressionService imageCompressionService;
    private final SupabaseStorageService supabaseStorageService;

    public ProfileService(ProfileRepository profileRepository, FriendRepository friendRepository, ImageCompressionService imageCompressionService, SupabaseStorageService supabaseStorageService) {
        this.profileRepository = profileRepository;
        this.friendRepository = friendRepository;
        this.imageCompressionService = imageCompressionService;
        this.supabaseStorageService = supabaseStorageService;
    }

    public List<Profile> listProfiles() {
        return profileRepository.findAllByOrderByNameAsc();
    }

    public Profile getProfile(UUID id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Profile not found."));
    }

}
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

