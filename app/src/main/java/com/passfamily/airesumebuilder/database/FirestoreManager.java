package com.passfamily.airesumebuilder.database;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.passfamily.airesumebuilder.model.Resume;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static final String COLLECTION_RESUMES = "resumes";
    private static final String COLLECTION_USER_LIMITS = "user_limits";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public FirestoreManager() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // ========== RESUME METHODS ==========

    public void getAllResumesForUser(String userId, FirestoreCallback<List<Resume>> callback) {
        Log.d(TAG, "Fetching resumes for user: " + userId);

        db.collection(COLLECTION_RESUMES)
                .whereEqualTo("userId", userId)
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Resume> resumes = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Resume resume = documentToResume(document);
                            if (resume != null) {
                                resumes.add(resume);
                            }
                        }
                        Log.d(TAG, "Successfully fetched " + resumes.size() + " resumes");
                        callback.onSuccess(resumes);
                    } else {
                        Exception exception = task.getException();
                        Log.e(TAG, "Error getting resumes: ", exception);
                        String errorMessage = "Failed to load resumes";
                        if (exception != null) {
                            errorMessage += ": " + exception.getMessage();
                        }
                        callback.onError(errorMessage);
                    }
                });
    }

    public void getResumeById(String resumeId, FirestoreCallback<Resume> callback) {
        db.collection(COLLECTION_RESUMES)
                .document(resumeId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Resume resume = documentToResume(task.getResult());
                        callback.onSuccess(resume);
                    } else {
                        callback.onError("Resume not found");
                    }
                });
    }

    public void insertResume(Resume resume, FirestoreCallback<String> callback) {
        String resumeId = db.collection(COLLECTION_RESUMES).document().getId();
        resume.setId(resumeId);

        Map<String, Object> resumeMap = resumeToMap(resume);

        db.collection(COLLECTION_RESUMES)
                .document(resumeId)
                .set(resumeMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Resume added with ID: " + resumeId);
                    // Update user's monthly count
                    updateUserMonthlyCount(resume.getUserId());
                    callback.onSuccess(resumeId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding resume: ", e);
                    callback.onError("Failed to save resume: " + e.getMessage());
                });
    }

    public void updateResume(Resume resume, FirestoreCallback<Void> callback) {
        resume.setUpdatedAt(System.currentTimeMillis());

        Map<String, Object> resumeMap = resumeToMap(resume);

        db.collection(COLLECTION_RESUMES)
                .document(resume.getId())
                .set(resumeMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Resume updated: " + resume.getId());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating resume: ", e);
                    callback.onError("Failed to update resume: " + e.getMessage());
                });
    }

    public void deleteResume(Resume resume, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_RESUMES)
                .document(resume.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Resume deleted: " + resume.getId());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting resume: ", e);
                    callback.onError("Failed to delete resume: " + e.getMessage());
                });
    }

    // ========== USER MONTHLY LIMIT METHODS ==========

    /**
     * Get the user's monthly resume count and check if 30 days have passed since first resume
     */
    public void getUserMonthlyResumeCount(String userId, FirestoreCallback<MonthlyLimitInfo> callback) {
        String limitDocId = userId + "_monthly_limit";

        Log.d(TAG, "Checking monthly limit for: " + limitDocId);

        db.collection(COLLECTION_USER_LIMITS)
                .document(limitDocId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();

                        if (document.exists()) {
                            Long count = document.getLong("count");
                            Long firstResumeTimestamp = document.getLong("firstResumeTimestamp");

                            int resumeCount = (count != null) ? count.intValue() : 0;
                            long firstResume = (firstResumeTimestamp != null) ? firstResumeTimestamp : System.currentTimeMillis();

                            // Check if 30 days have passed since first resume
                            long currentTime = System.currentTimeMillis();
                            long daysPassed = (currentTime - firstResume) / (1000 * 60 * 60 * 24);

                            Log.d(TAG, "Monthly count: " + resumeCount + ", Days passed: " + daysPassed);

                            if (daysPassed >= 30) {
                                // 30 days have passed, reset the counter
                                Log.d(TAG, "30 days passed, resetting counter");
                                resetMonthlyLimit(userId, callback);
                            } else {
                                // Still within 30 days
                                long daysRemaining = 30 - daysPassed;
                                MonthlyLimitInfo info = new MonthlyLimitInfo(resumeCount, daysRemaining, firstResume);
                                callback.onSuccess(info);
                            }
                        } else {
                            // No document exists, meaning 0 resumes created
                            Log.d(TAG, "No document found, count = 0");
                            MonthlyLimitInfo info = new MonthlyLimitInfo(0, 30, System.currentTimeMillis());
                            callback.onSuccess(info);
                        }
                    } else {
                        Log.e(TAG, "Error fetching monthly count: " + task.getException());
                        callback.onError("Error checking monthly limit");
                    }
                });
    }

    /**
     * Reset the monthly limit counter
     */
    private void resetMonthlyLimit(String userId, FirestoreCallback<MonthlyLimitInfo> callback) {
        String limitDocId = userId + "_monthly_limit";

        Map<String, Object> data = new HashMap<>();
        data.put("count", 0);
        data.put("userId", userId);
        data.put("firstResumeTimestamp", System.currentTimeMillis());
        data.put("lastResetTimestamp", System.currentTimeMillis());
        data.put("lastUpdated", System.currentTimeMillis());

        db.collection(COLLECTION_USER_LIMITS)
                .document(limitDocId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Monthly limit reset successfully");
                    MonthlyLimitInfo info = new MonthlyLimitInfo(0, 30, System.currentTimeMillis());
                    callback.onSuccess(info);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error resetting monthly limit: ", e);
                    callback.onError("Error resetting limit");
                });
    }

    /**
     * Update the user's monthly count after creating a resume
     */
    private void updateUserMonthlyCount(String userId) {
        String limitDocId = userId + "_monthly_limit";

        Log.d(TAG, "Updating monthly count for: " + limitDocId);

        db.collection(COLLECTION_USER_LIMITS)
                .document(limitDocId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();

                        long currentTime = System.currentTimeMillis();

                        if (document.exists()) {
                            // Check if need to reset
                            Long firstResumeTimestamp = document.getLong("firstResumeTimestamp");
                            long firstResume = (firstResumeTimestamp != null) ? firstResumeTimestamp : currentTime;
                            long daysPassed = (currentTime - firstResume) / (1000 * 60 * 60 * 24);

                            int newCount;
                            long newFirstResumeTimestamp;

                            if (daysPassed >= 30) {
                                // Reset counter
                                newCount = 1;
                                newFirstResumeTimestamp = currentTime;
                            } else {
                                // Increment existing count
                                Long currentCount = document.getLong("count");
                                newCount = (currentCount != null) ? currentCount.intValue() + 1 : 1;
                                newFirstResumeTimestamp = firstResume;
                            }

                            Map<String, Object> data = new HashMap<>();
                            data.put("count", newCount);
                            data.put("userId", userId);
                            data.put("firstResumeTimestamp", newFirstResumeTimestamp);
                            data.put("lastUpdated", currentTime);

                            db.collection(COLLECTION_USER_LIMITS)
                                    .document(limitDocId)
                                    .set(data)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Monthly count updated to: " + newCount);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating monthly count: ", e);
                                    });
                        } else {
                            // Create new document with count = 1 and set first resume timestamp
                            Map<String, Object> data = new HashMap<>();
                            data.put("count", 1);
                            data.put("userId", userId);
                            data.put("firstResumeTimestamp", currentTime);
                            data.put("lastUpdated", currentTime);

                            db.collection(COLLECTION_USER_LIMITS)
                                    .document(limitDocId)
                                    .set(data)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Monthly count initialized to: 1");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error initializing monthly count: ", e);
                                    });
                        }
                    }
                });
    }

    // ========== HELPER CLASSES ==========

    /**
     * Class to hold monthly limit information
     */
    public static class MonthlyLimitInfo {
        private int count;
        private long daysRemaining;
        private long firstResumeTimestamp;

        public MonthlyLimitInfo(int count, long daysRemaining, long firstResumeTimestamp) {
            this.count = count;
            this.daysRemaining = daysRemaining;
            this.firstResumeTimestamp = firstResumeTimestamp;
        }

        public int getCount() {
            return count;
        }

        public long getDaysRemaining() {
            return daysRemaining;
        }

        public long getFirstResumeTimestamp() {
            return firstResumeTimestamp;
        }

        public String getResetDateString() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(firstResumeTimestamp);
            calendar.add(Calendar.DAY_OF_YEAR, 30);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(calendar.getTime());
        }
    }

    // ========== HELPER METHODS ==========

    private Map<String, Object> resumeToMap(Resume resume) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", resume.getId());
        map.put("userId", resume.getUserId());
        map.put("resumeName", resume.getResumeName());
        map.put("name", resume.getName());
        map.put("email", resume.getEmail());
        map.put("phone", resume.getPhone());
        map.put("github", resume.getGithub());
        map.put("linkedin", resume.getLinkedin());
        map.put("portfolio", resume.getPortfolio());
        map.put("education", resume.getEducation());
        map.put("skills", resume.getSkills());
        map.put("experience", resume.getExperience());
        map.put("projects", resume.getProjects());
        map.put("achievements", resume.getAchievements());
        map.put("courses", resume.getCourses());
        map.put("generatedContent", resume.getGeneratedContent());
        map.put("createdAt", resume.getCreatedAt());
        map.put("updatedAt", resume.getUpdatedAt());
        return map;
    }

    private Resume documentToResume(com.google.firebase.firestore.DocumentSnapshot document) {
        try {
            Resume resume = new Resume();
            resume.setId(document.getString("id"));
            resume.setUserId(document.getString("userId"));
            resume.setResumeName(document.getString("resumeName"));
            resume.setName(document.getString("name"));
            resume.setEmail(document.getString("email"));
            resume.setPhone(document.getString("phone"));
            resume.setGithub(document.getString("github"));
            resume.setLinkedin(document.getString("linkedin"));
            resume.setPortfolio(document.getString("portfolio"));
            resume.setEducation(document.getString("education"));
            resume.setSkills(document.getString("skills"));
            resume.setExperience(document.getString("experience"));
            resume.setProjects(document.getString("projects"));
            resume.setAchievements(document.getString("achievements"));
            resume.setCourses(document.getString("courses"));
            resume.setGeneratedContent(document.getString("generatedContent"));

            Object createdAt = document.get("createdAt");
            Object updatedAt = document.get("updatedAt");

            if (createdAt instanceof Long) {
                resume.setCreatedAt((Long) createdAt);
            } else if (createdAt instanceof Double) {
                resume.setCreatedAt(((Double) createdAt).longValue());
            }

            if (updatedAt instanceof Long) {
                resume.setUpdatedAt((Long) updatedAt);
            } else if (updatedAt instanceof Double) {
                resume.setUpdatedAt(((Double) updatedAt).longValue());
            }

            return resume;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to resume: ", e);
            return null;
        }
    }
}