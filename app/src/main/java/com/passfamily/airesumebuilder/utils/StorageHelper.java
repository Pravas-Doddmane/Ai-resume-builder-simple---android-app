package com.passfamily.airesumebuilder.utils;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class StorageHelper {
    private static final String TAG = "StorageHelper";
    private FirebaseStorage storage;
    private String userId;

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String error);
        void onProgress(double progress);
    }

    public StorageHelper() {
        storage = FirebaseStorage.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    /**
     * Upload profile photo
     */
    public void uploadProfilePhoto(Uri fileUri, UploadCallback callback) {
        if (userId == null) {
            callback.onError("User not authenticated");
            return;
        }

        String fileName = "photo_" + System.currentTimeMillis() + ".jpg";
        StorageReference photoRef = storage.getReference()
                .child("photos")
                .child(userId)
                .child(fileName);

        uploadFile(photoRef, fileUri, callback);
    }

    /**
     * Upload certificate (PDF or image)
     */
    public void uploadCertificate(Uri fileUri, String extension, UploadCallback callback) {
        if (userId == null) {
            callback.onError("User not authenticated");
            return;
        }

        String fileName = "cert_" + UUID.randomUUID().toString() + "." + extension;
        StorageReference certRef = storage.getReference()
                .child("certificates")
                .child(userId)
                .child(fileName);

        uploadFile(certRef, fileUri, callback);
    }

    /**
     * Generic file upload
     */
    private void uploadFile(StorageReference reference, Uri fileUri, UploadCallback callback) {
        UploadTask uploadTask = reference.putFile(fileUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress(progress);
        }).addOnSuccessListener(taskSnapshot -> {
            reference.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d(TAG, "File uploaded successfully: " + uri.toString());
                callback.onSuccess(uri.toString());
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting download URL: " + e.getMessage());
                callback.onError("Error getting download URL");
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Upload failed: " + e.getMessage());
            callback.onError("Upload failed: " + e.getMessage());
        });
    }

    /**
     * Delete file from storage
     */
    public void deleteFile(String fileUrl, UploadCallback callback) {
        try {
            StorageReference fileRef = storage.getReferenceFromUrl(fileUrl);
            fileRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "File deleted successfully");
                        callback.onSuccess("File deleted");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting file: " + e.getMessage());
                        callback.onError("Error deleting file");
                    });
        } catch (Exception e) {
            Log.e(TAG, "Invalid file URL: " + e.getMessage());
            callback.onError("Invalid file URL");
        }
    }
}