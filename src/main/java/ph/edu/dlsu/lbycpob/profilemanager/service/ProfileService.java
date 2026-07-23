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
