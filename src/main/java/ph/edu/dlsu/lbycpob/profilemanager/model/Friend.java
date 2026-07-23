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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Friend friend)) return false;
        return Objects.equals(id, friend.id)
                && Objects.equals(profileId, friend.profileId)
                && Objects.equals(friendId, friend.friendId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, profileId, friendId);
    }

    @Override
    public String toString() {
        return "Friend{" +
                "id=" + id +
                ", profileId=" + profileId +
                ", friendId=" + friendId +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Manual replacement for Lombok's @Builder.
     */
    public static final class Builder {
        private UUID id;
        private UUID profileId;
        private UUID friendId;

        private Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder profileId(UUID profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder friendId(UUID friendId) {
            this.friendId = friendId;
            return this;
        }

        public Friend build() {
            return new Friend(id, profileId, friendId);
        }
    }
}