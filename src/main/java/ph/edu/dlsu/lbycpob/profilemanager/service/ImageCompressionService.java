package ph.edu.dlsu.lbycpob.profilemanager.service;

import com.luciad.imageio.webp.WebPWriteParam;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
/**
 * Resizes an uploaded avatar (max 224px on the long edge, aspect ratio
 * preserved, never upscaled) and encodes it to WebP -- the same
 * dimension/quality targets the original app's sharp-based pipeline used,
 * just done in pure Java via the webp-imageio ImageIO plugin instead of
 * sharp/libvips.
 */
@Service
public class ImageCompressionService {

    private static final int MAX_DIMENSION = 224;
    private static final float WEBP_QUALITY = 0.8f; // 0.0 - 1.0

    /** @throws IllegalArgumentException if the bytes aren't a decodable image */
    public byte[] compressToWebp(byte[] originalBytes) {
        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(originalBytes));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file as an image.");
        }
        if (original == null) {
            throw new IllegalArgumentException("The uploaded file is not a supported image format.");
        }

        BufferedImage resized = resize(original);
        try {
            return encodeToWebp(resized);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode image as WebP: " + e.getMessage(), e);
        }
    }

    private BufferedImage resize(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return original; // never upscale, matches the original pipeline's withoutEnlargement
        }

        double scale = Math.min((double) MAX_DIMENSION / width, (double) MAX_DIMENSION / height);
        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(original, 0, 0, newWidth, newHeight, null);
        } finally {
            g.dispose();
        }
        return resized;
    }

