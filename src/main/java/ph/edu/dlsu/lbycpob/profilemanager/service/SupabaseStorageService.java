package ph.edu.dlsu.lbycpob.profilemanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;

/**
 * Uploads bytes to a Supabase Storage bucket over Supabase's documented
 * REST endpoint (Storage API reference: {@code POST /storage/v1/object/{bucket}/{path}}),
 * using the new Supabase "secret" API key (format {@code sb_secret_...}).
 * This REST surface IS officially documented for direct HTTP use -- no
 * SDK, no reverse engineering required. See: <a href="https://supabase.com/docs/guides/storage">...</a>
 * <p>
 * As of Supabase's new API key system, the legacy {@code service_role}
 * JWT key is being replaced by "Publishable" and "Secret" keys, managed
 * under Project Settings -> API Keys -> Publishable and secret API keys
 * tab. The secret key still bypasses Row Level Security, exactly like
 * service_role did, so it must never be exposed to a browser -- it's
 * safe here because only this server ever holds it.
 * <p>
 * Both {@code apikey} and {@code Authorization: Bearer} headers carry the
 * same secret key -- Supabase requires the two to match exactly.
 */
@Service
public class SupabaseStorageService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final String supabaseUrl;
    private final String secretKey;
    private final String bucket;

    public SupabaseStorageService(
            @Value("${app.supabase.url}") String supabaseUrl,
            @Value("${app.supabase.secret-key}") String secretKey,
            @Value("${app.supabase.avatar-bucket}") String bucket) {
        this.supabaseUrl = trimTrailingSlash(supabaseUrl);
        this.secretKey = secretKey;
        this.bucket = bucket;
    }
    /**
     * Uploads (or overwrites, via x-upsert) the given bytes at {@code path}
     * within the configured bucket, and returns the public URL. The bucket
     * must already exist and be set to Public in the Supabase dashboard --
     * this method doesn't create buckets or manage their visibility.
     */
    public String uploadAndGetPublicUrl(String path, byte[] content, String contentType) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "SUPABASE_SECRET_KEY is not configured -- avatar upload is unavailable. " +
                            "Set it to your Supabase Secret API key (sb_secret_...) from " +
                            "Project Settings -> API Keys -> Publishable and secret API keys, " +
                            "or use the 'paste an image URL' field instead.");
        }

        URI uploadUri = URI.create(supabaseUrl + "/storage/v1/object/" + bucket + "/" + path);

        HttpRequest request = HttpRequest.newBuilder(uploadUri)
                .header("apikey", secretKey)
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", contentType)
                .header("x-upsert", "true") // overwrite any existing file at this path
                .POST(BodyPublishers.ofByteArray(content))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("Could not reach Supabase Storage: " + e.getMessage(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Supabase Storage upload failed (HTTP " + response.statusCode() + "): " + response.body());
        }

        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
