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

