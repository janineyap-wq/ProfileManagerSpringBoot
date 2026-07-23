package ph.edu.dlsu.lbycpob.profilemanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import ph.edu.dlsu.lbycpob.profilemanager.model.Friend;

import java.util.List;
import java.util.UUID;

public interface FriendRepository extends JpaRepository<Friend, UUID> {

    List<Friend> findByProfileId(UUID profileId);

    boolean existsByProfileIdAndFriendId(UUID profileId, UUID friendId);

    @Transactional
    void deleteByProfileIdAndFriendId(UUID profileId, UUID friendId);
}
