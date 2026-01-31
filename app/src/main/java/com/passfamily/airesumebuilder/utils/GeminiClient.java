package com.passfamily.airesumebuilder.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.passfamily.airesumebuilder.model.Resume;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GeminiClient {
    private static final String GEMINI_API_URL = "";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;
    private Gson gson;

    public interface GeminiCallback {
        void onSuccess(String generatedResume);
        void onError(String error);
    }

    public GeminiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    public void generateResume(Resume resume, GeminiCallback callback) {
        String prompt = createPrompt(resume);

        JsonObject requestBody = new JsonObject();

        JsonArray contentsArray = new JsonArray();
        JsonObject contentObject = new JsonObject();
        JsonArray partsArray = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        partsArray.add(textPart);
        contentObject.add("parts", partsArray);
        contentsArray.add(contentObject);

        requestBody.add("contents", contentsArray);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("topP", 0.8);
        generationConfig.addProperty("topK", 40);
        generationConfig.addProperty("maxOutputTokens", 3000);
        requestBody.add("generationConfig", generationConfig);

        RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);

        String apiKey = Constants.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            callback.onError("Gemini API key not configured. Please set your API key in Constants.java");
            return;
        }

        Request request = new Request.Builder()
                .url(GEMINI_API_URL + "?key=" + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    String errorMessage = "API error: " + response.code();
                    try {
                        JsonObject errorJson = gson.fromJson(responseBody, JsonObject.class);
                        if (errorJson.has("error") && errorJson.getAsJsonObject("error").has("message")) {
                            errorMessage = errorJson.getAsJsonObject("error").get("message").getAsString();
                        }
                    } catch (Exception e) {
                        errorMessage = "API error " + response.code() + ": " + responseBody;
                    }
                    callback.onError(errorMessage);
                    return;
                }

                try {
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                    if (jsonResponse.has("candidates") &&
                            jsonResponse.getAsJsonArray("candidates").size() > 0) {

                        JsonObject candidate = jsonResponse.getAsJsonArray("candidates")
                                .get(0).getAsJsonObject();

                        if (candidate.has("content")) {
                            JsonObject content = candidate.getAsJsonObject("content");
                            if (content.has("parts") && content.getAsJsonArray("parts").size() > 0) {
                                String generatedContent = content.getAsJsonArray("parts")
                                        .get(0)
                                        .getAsJsonObject()
                                        .get("text")
                                        .getAsString()
                                        .trim();

                                callback.onSuccess(generatedContent);
                                return;
                            }
                        }
                    }
                    callback.onError("Invalid response format from Gemini API");
                } catch (Exception e) {
                    callback.onError("Parsing error: " + e.getMessage());
                }
            }
        });
    }

    private String createPrompt(Resume resume) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert ATS-optimized resume writer. Create a professional, modern resume that will pass Applicant Tracking Systems (ATS) and impress recruiters.\n\n");

        prompt.append("CRITICAL FORMATTING REQUIREMENTS:\n");
        prompt.append("1. Return ONLY plain text - NO LaTeX, NO Markdown (no **, no ###), NO special formatting codes\n");
        prompt.append("2. Use CLEAR section headers in ALL CAPS (EDUCATION, EXPERIENCE, etc.)\n");
        prompt.append("3. For dates and positions, use this exact format:\n");
        prompt.append("   Position Title | Start Date - End Date\n");
        prompt.append("   Company/Institution | Location\n");
        prompt.append("   - Achievement or responsibility bullet point\n");
        prompt.append("4. Use simple bullet points (- ) for all lists\n");
        prompt.append("5. Keep content concise - target ONE PAGE maximum\n");
        prompt.append("6. Use professional language and strong action verbs\n");
        prompt.append("7. Quantify achievements wherever possible (increased by X%, managed Y projects)\n");
        prompt.append("8. DO NOT use any markdown formatting like ** for bold or ## for headers\n");
        prompt.append("9. Use plain text only with proper spacing and bullet points\n");
        prompt.append("10. DO NOT include the contact information section (name, email, etc.) in the body - just start with EDUCATION section\n\n");

        prompt.append("====================\n");
        prompt.append("CANDIDATE INFORMATION:\n");
        prompt.append("====================\n\n");

        // Personal Information (provided for context but NOT to be included in output)
        prompt.append("CONTACT INFORMATION (FOR CONTEXT ONLY - DO NOT INCLUDE IN OUTPUT):\n");
        prompt.append("Name: ").append(resume.getName()).append("\n");
        prompt.append("Email: ").append(resume.getEmail()).append("\n");
        if (resume.getPhone() != null && !resume.getPhone().isEmpty()) {
            prompt.append("Phone: ").append(resume.getPhone()).append("\n");
        }
        if (resume.getGithub() != null && !resume.getGithub().isEmpty()) {
            prompt.append("GitHub: ").append(resume.getGithub()).append("\n");
        }
        if (resume.getLinkedin() != null && !resume.getLinkedin().isEmpty()) {
            prompt.append("LinkedIn: ").append(resume.getLinkedin()).append("\n");
        }
        if (resume.getPortfolio() != null && !resume.getPortfolio().isEmpty()) {
            prompt.append("Portfolio: ").append(resume.getPortfolio()).append("\n");
        }

        // Content sections
        prompt.append("\nEDUCATION:\n").append(resume.getEducation()).append("\n");
        prompt.append("\nSKILLS:\n").append(resume.getSkills()).append("\n");

        if (resume.getExperience() != null && !resume.getExperience().isEmpty()) {
            prompt.append("\nWORK EXPERIENCE:\n").append(resume.getExperience()).append("\n");
        }

        if (resume.getProjects() != null && !resume.getProjects().isEmpty()) {
            prompt.append("\nPROJECTS:\n").append(resume.getProjects()).append("\n");
        }

        if (resume.getAchievements() != null && !resume.getAchievements().isEmpty()) {
            prompt.append("\nACHIEVEMENTS & CERTIFICATIONS:\n").append(resume.getAchievements()).append("\n");
        }

        if (resume.getCourses() != null && !resume.getCourses().isEmpty()) {
            prompt.append("\nRELEVANT COURSES:\n").append(resume.getCourses()).append("\n");
        }

        prompt.append("\n====================\n");
        prompt.append("TASK:\n");
        prompt.append("====================\n");
        prompt.append("Transform the above information into a polished, ATS-friendly professional resume.\n\n");

        prompt.append("IMPORTANT: DO NOT include the contact information (name, email, phone, etc.) in your response.\n");
        prompt.append("The contact information will be added separately in the PDF header.\n");
        prompt.append("Start directly with EDUCATION section.\n\n");

        prompt.append("STRUCTURE YOUR RESPONSE AS:\n");
        prompt.append("EDUCATION\n");
        prompt.append("[Education details with | separator for dates]\n\n");
        prompt.append("SKILLS\n");
        prompt.append("[Grouped skills, comma-separated or as bullet points]\n\n");
        prompt.append("EXPERIENCE\n");
        prompt.append("[Experience with | separator format]\n\n");
        prompt.append("PROJECTS\n");
        prompt.append("[Projects with | separator format]\n\n");
        prompt.append("ACHIEVEMENTS & CERTIFICATIONS\n");
        prompt.append("[List of achievements]\n\n");

        prompt.append("CRITICAL: Output ONLY the formatted resume content WITHOUT contact information. No markdown, no special formatting codes, no explanations.\n");
        prompt.append("Start directly with EDUCATION section.\n");

        return prompt.toString();
    }
}
