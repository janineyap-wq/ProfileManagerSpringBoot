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
