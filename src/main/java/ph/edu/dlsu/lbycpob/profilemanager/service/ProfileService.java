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

    public ProfileService(
            ProfileRepository profileRepository,
            FriendRepository friendRepository,
            ImageCompressionService imageCompressionService,
            SupabaseStorageService supabaseStorageService) {
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

    public List<Profile> getFriendsOf(UUID profileId) {
        List<UUID> friendIds = friendRepository.findByProfileId(profileId).stream()
                .map(Friend::getFriendId)
                .toList();
        return friendIds.isEmpty() ? List.of() : profileRepository.findAllById(friendIds);
    }

    public Profile lookupFirstMatch(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Search field is empty. Please enter a name to search.");
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
        profileRepository.deleteById(id);
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

    @Transactional
    public void updatePictureUrl(UUID id, String pictureUrl) {
        String trimmed = pictureUrl == null ? "" : pictureUrl.trim();
        if (!trimmed.startsWith("https://")) {
            throw new IllegalArgumentException("URL must start with https://");
        }
        getProfile(id).setPicture(trimmed);
    }

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
