package com.example.ecommerce.bll.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Service de stockage d'images.
 * - Enregistre un fichier image sur le disque (dans le dossier uploads/ de l'app).
 * - Retourne un chemin PUBLIC (ex: "/uploads/uuid.png") à stocker en base et utiliser dans les vues.
 * - (Optionnel) Supprime une ancienne image si on en remplace une.
 */
public interface ImageStorageService {

    /**
     * Sauvegarde l'image fournie et renvoie le chemin public accessible par le navigateur.
     * @param file                 le fichier image uploadé (depuis un formulaire multipart)
     * @param oldPublicPathIfAny   (optionnel) l'ancien chemin public à supprimer si on remplace l'image
     * @return chemin public commençant par "/uploads/..." (à stocker côté Product.imagePath)
     * @throws IOException en cas d'erreur d'écriture disque
     */
    String saveImage(MultipartFile file, String oldPublicPathIfAny) throws IOException;

    /**
     * Supprime une image existante si elle appartient à notre dossier /uploads.
     * @param publicPath chemin public commençant par "/uploads/...".
     */
    void deleteIfOwned(String publicPath);
}
