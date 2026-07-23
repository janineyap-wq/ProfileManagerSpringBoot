package ph.edu.dlsu.lbycpob.profilemanager.dto;



import ph.edu.dlsu.lbycpob.profilemanager.model.Profile;

import java.util.List;
import java.util.UUID;

public class Dtos {

    private Dtos() {
    }

    /** Left-panel list row: id, name, picture only. */
    public record ProfileListItem(UUID id, String name, String picture) {
        public static ProfileListItem of(Profile p) {
            return new ProfileListItem(p.getId(), p.getName(), p.getPicture());
        }
    }

    /** Friend list row. */
    public record NameRef(UUID id, String name) {
        public static NameRef of(Profile p) {
            return new NameRef(p.getId(), p.getName());
        }
    }

    /** Full detail shown in the centre panel, including resolved friends. */
    public record ProfileDetail(UUID id, String name, String status, String quote, String picture, List<NameRef> friends) {
        public static ProfileDetail of(Profile p, List<Profile> friends) {
            return new ProfileDetail(
                    p.getId(), p.getName(), p.getStatus(), p.getQuote(), p.getPicture(),
                    friends.stream().map(NameRef::of).toList());
        }
    }

    public record NewProfileRequest(String name) {
    }

    public record UpdateStatusRequest(String status) {
    }

    public record UpdateQuoteRequest(String quote) {
    }

    public record UpdatePictureRequest(String pictureUrl) {
    }

    public record FriendActionRequest(String friendName) {
    }

    public record PictureResult(String url) {
    }

    public record ApiError(String error) {
    }
}
