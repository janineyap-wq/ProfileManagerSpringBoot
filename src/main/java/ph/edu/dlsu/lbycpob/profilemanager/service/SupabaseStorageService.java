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
