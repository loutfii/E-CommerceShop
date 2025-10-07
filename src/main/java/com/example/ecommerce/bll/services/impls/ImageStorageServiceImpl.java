package com.example.ecommerce.bll.services.impls;

import com.example.ecommerce.bll.services.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

/**
 * Implémentation du service de stockage d'images.
 * - Valide le type MIME (sécurité basique).
 * - Crée le dossier uploads/ s'il n'existe pas.
 * - Génére un nom unique (UUID + extension).
 * - Écrit le fichier sur le disque.
 * - (Optionnel) Supprime l'ancienne image si demandée.
 */
@Service
public class ImageStorageServiceImpl implements ImageStorageService {

    // Types MIME autorisés pour éviter d'enregistrer autre chose qu'une image
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    // Chemin du dossier d'upload (défini dans application.yml: app.upload-dir: uploads)
    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public String saveImage(MultipartFile file, String oldPublicPathIfAny) throws IOException {
        // 1) Vérifications de base
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier n’a été fourni.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Type d'image non supporté : " + contentType);
        }

        // 2) Déterminer l'extension à partir du nom d'origine ou du type MIME
        String original = file.getOriginalFilename();
        String ext = getSafeExtension(original, contentType); // ex: ".png"

        // 3) Préparer le dossier d'upload (absolu, normalisé)
        Path folder = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(folder);

        // 4) Générer un nom unique et construire le chemin cible
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = folder.resolve(filename);

        // 5) Écrire le fichier sur le disque
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Impossible d’enregistrer l’image sur le disque.", e);
        }

        // 6) Supprimer l'ancienne image si fournie et appartenant à /uploads
        deleteIfOwned(oldPublicPathIfAny);

        // 7) Retourner le chemin public utilisable dans les templates
        return "/uploads/" + filename;
    }

    @Override
    public void deleteIfOwned(String publicPath) {
        // On ne supprime que si c'est un chemin sous /uploads/
        if (publicPath == null || !publicPath.startsWith("/uploads/")) {
            return;
        }
        try {
            String filename = publicPath.substring("/uploads/".length());
            Path folder = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path fileToDelete = folder.resolve(filename);
            Files.deleteIfExists(fileToDelete);
        } catch (IOException e) {
            // On journalise simplement l'erreur, on ne fait pas échouer l'opération d'upload
            System.err.println("Impossible de supprimer l’ancienne image : " + e.getMessage());
        }
    }

    // -------- utilitaires privés --------

    /**
     * Détermine l’extension de manière sûre.
     * - Si le nom d'origine contient une extension, on la récupère.
     * - Sinon on se base sur le type MIME.
     * - Par défaut ".png".
     */
    private String getSafeExtension(String originalFilename, String contentType) {
        String ext = "";

        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0) {
                ext = originalFilename.substring(dot).toLowerCase();
            }
        }

        if (ext.isEmpty() && contentType != null) {
            if (contentType.contains("jpeg")) ext = ".jpg";
            else if (contentType.contains("png")) ext = ".png";
            else if (contentType.contains("gif")) ext = ".gif";
            else if (contentType.contains("webp")) ext = ".webp";
        }

        return ext.isEmpty() ? ".png" : ext;
    }
}
