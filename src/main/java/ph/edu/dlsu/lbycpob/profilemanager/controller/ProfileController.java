package ph.edu.dlsu.lbycpob.profilemanager.controller;


import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ph.edu.dlsu.lbycpob.profilemanager.dto.Dtos;
import ph.edu.dlsu.lbycpob.profilemanager.dto.Dtos.*;
import ph.edu.dlsu.lbycpob.profilemanager.model.Profile;
import ph.edu.dlsu.lbycpob.profilemanager.service.ProfileService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public List<Dtos.ProfileListItem> listProfiles() {
        return profileService.listProfiles().stream().map(ProfileListItem::of).toList();
    }

    @GetMapping("/{id}")
    public ProfileDetail getProfile(@PathVariable UUID id) {
        Profile profile = profileService.getProfile(id);
        return ProfileDetail.of(profile, profileService.getFriendsOf(id));
    }

    @GetMapping("/lookup")
    public ProfileDetail lookupProfile(@RequestParam String query) {
        Profile profile = profileService.lookupFirstMatch(query);
        return ProfileDetail.of(profile, profileService.getFriendsOf(profile.getId()));
    }

    @PostMapping
    public ProfileDetail createProfile(@RequestBody NewProfileRequest request) {
        Profile created = profileService.createProfile(request.name());
        return ProfileDetail.of(created, List.of());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteProfile(@PathVariable UUID id) {
        Profile profile = profileService.getProfile(id);
        profileService.deleteProfile(id);
        return Map.of("deletedName", profile.getName());
    }

    @PatchMapping("/{id}/status")
    public Map<String, String> updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        profileService.updateStatus(id, request.status());
        return Map.of("status", request.status().trim());
    }

    @PatchMapping("/{id}/quote")
    public Map<String, String> updateQuote(@PathVariable UUID id, @RequestBody UpdateQuoteRequest request) {
        profileService.updateQuote(id, request.quote());
        return Map.of("quote", request.quote().trim());
    }

    /** Mode B: paste-a-URL picture update. */
    @PatchMapping("/{id}/picture")
    public Map<String, String> updatePictureUrl(@PathVariable UUID id, @RequestBody UpdatePictureRequest request) {
        profileService.updatePictureUrl(id, request.pictureUrl());
        return Map.of("picture", request.pictureUrl().trim());
    }

    /**
     * Mode A: file upload. Compresses to WebP, uploads to the Supabase
     * Storage bucket, persists the URL, and returns it in one round trip
     * (unlike the Vercel-Blob design, there's no separate
     * "upload then PATCH" step needed -- this server does both itself).
     */
    @PostMapping(value = "/{id}/avatar", consumes = "multipart/form-data")
    public PictureResult uploadAvatar(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        String url = profileService.updatePictureFromUpload(id, file);
        return new PictureResult(url);
    }

    @PostMapping("/{id}/friends")
    public Map<String, String> addFriend(@PathVariable UUID id, @RequestBody FriendActionRequest request) {
        String friendName = profileService.addFriend(id, request.friendName());
        return Map.of("friendName", friendName);
    }

    @DeleteMapping("/{id}/friends")
    public Map<String, String> removeFriend(@PathVariable UUID id, @RequestBody FriendActionRequest request) {
        String friendName = profileService.removeFriend(id, request.friendName());
        return Map.of("friendName", friendName);
    }
}
