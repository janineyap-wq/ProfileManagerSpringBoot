package ph.edu.dlsu.lbycpob.profilemanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.Objects;
import java.util.UUID;

/**
 * Maps to public.friends. Unlike a "canonical smaller-UUID-first" scheme,
 * this schema's unique(profile_id, friend_id) constraint is directional --
 * (A,B) and (B,A) are two distinct, independently-insertable rows.
 * <p>
 * id          uuid primary key default gen_random_uuid()
 * profile_id  uuid not null, references profiles(id) on delete cascade
 * friend_id   uuid not null, references profiles(id) on delete cascade
 * unique(profile_id, friend_id); check(profile_id <> friend_id)
 */

@Entity
@Table(name = "friends")
public class Friend {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "profile_id", nullable = false, columnDefinition = "uuid")
    private UUID profileId;

    @Column(name = "friend_id", nullable = false, columnDefinition = "uuid")
    private UUID friendId;

    public Friend() {
    }

    public Friend(UUID id, UUID profileId, UUID friendId) {
        this.id = id;
        this.profileId = profileId;
        this.friendId = friendId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public void setProfileId(UUID profileId) {
        this.profileId = profileId;
    }

    public UUID getFriendId() {
        return friendId;
    }

    public void setFriendId(UUID friendId) {
        this.friendId = friendId;
    }


