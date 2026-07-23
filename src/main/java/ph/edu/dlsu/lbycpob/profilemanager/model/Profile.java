package ph.edu.dlsu.lbycpob.profilemanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps to public.profiles. Field-for-field match with the schema:
 * <p>
 * id          uuid primary key default gen_random_uuid()
 * name        text not null, unique
 * status      text not null default ''
 * quote       text not null default ''
 * picture     text not null default '<vercel blob default avatar url>'
 * created_at  timestamptz not null default now()
 */
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String status = "";

    @Column(nullable = false)
    private String quote = "";

    @Column(nullable = false)
    private String picture = "https://6fkrqtkwbcnqsois.public.blob.vercel-storage.com/avatars/default.webp";

    // insertable = false, updatable = false: the DB's default now()
    // populates this column; we never write to it from Java. It reads
    // back correctly on any fresh SELECT (e.g. the redirect-after-POST
    // that follows profile creation).
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Profile() {
    }

    public Profile(UUID id, String name, String status, String quote, String picture, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.quote = quote;
        this.picture = picture;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Profile profile)) return false;
        return Objects.equals(id, profile.id)
                && Objects.equals(name, profile.name)
                && Objects.equals(status, profile.status)
                && Objects.equals(quote, profile.quote)
                && Objects.equals(picture, profile.picture)
                && Objects.equals(createdAt, profile.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, status, quote, picture, createdAt);
    }

    @Override
    public String toString() {
        return "Profile{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", quote='" + quote + '\'' +
                ", picture='" + picture + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

