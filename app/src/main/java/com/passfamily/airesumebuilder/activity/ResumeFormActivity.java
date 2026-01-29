package com.passfamily.airesumebuilder.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.passfamily.airesumebuilder.R;
import com.passfamily.airesumebuilder.database.FirestoreManager;
import com.passfamily.airesumebuilder.model.Resume;
import com.passfamily.airesumebuilder.utils.AdHelper;
import com.passfamily.airesumebuilder.utils.GeminiClient;
import com.passfamily.airesumebuilder.utils.KeyboardUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResumeFormActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone, etGithub, etLinkedin, etPortfolio;
    private TextInputEditText etEducation, etSkills, etExperience, etProjects, etAchievements, etCourses;
    private Button btnGenerate;
    private ProgressBar progressBar;
    private AdView bannerAdView;

    private FirestoreManager firestoreManager;
    private GeminiClient geminiClient;
    private InterstitialAd interstitialAd;
    private FirebaseAuth mAuth;

    private static final int MAX_MONTHLY_RESUMES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resume_form);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupToolbar();
        initViews();
        setupClickListeners();
        setupKeyboardBehavior();

        mAuth = FirebaseAuth.getInstance();
        firestoreManager = new FirestoreManager();
        geminiClient = new GeminiClient();

        // Load interstitial ad
        loadInterstitialAd();

        // Load banner ad
        loadBannerAd();
    }

    private void loadBannerAd() {
        LinearLayout adContainer = findViewById(R.id.bannerAdContainer);
        bannerAdView = AdHelper.loadBannerAd(this, adContainer);
    }

    private void loadInterstitialAd() {
        AdHelper.loadInterstitialAd(this, new AdHelper.InterstitialAdCallback() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                interstitialAd = ad;
            }

            @Override
            public void onAdFailedToLoad(String error) {
                interstitialAd = null;
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etGithub = findViewById(R.id.etGithub);
        etLinkedin = findViewById(R.id.etLinkedin);
        etPortfolio = findViewById(R.id.etPortfolio);
        etEducation = findViewById(R.id.etEducation);
        etSkills = findViewById(R.id.etSkills);
        etExperience = findViewById(R.id.etExperience);
        etProjects = findViewById(R.id.etProjects);
        etAchievements = findViewById(R.id.etAchievements);
        etCourses = findViewById(R.id.etCourses);

        btnGenerate = findViewById(R.id.btnGenerate);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupKeyboardBehavior() {
        androidx.coordinatorlayout.widget.CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinatorLayout);
        if (coordinatorLayout != null) {
            KeyboardUtils.setupHideKeyboardOnTouch(coordinatorLayout, this);
        }

        androidx.core.widget.NestedScrollView nestedScrollView = findViewById(R.id.nestedScrollView);
        if (nestedScrollView != null) {
            KeyboardUtils.setupHideKeyboardOnTouch(nestedScrollView, this);
        }
    }

    private void setupClickListeners() {
        btnGenerate.setOnClickListener(v -> {
            // Check monthly limit before generating
            checkMonthlyLimitAndGenerate();
        });
    }

    private void checkMonthlyLimitAndGenerate() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        progressBar.setVisibility(View.VISIBLE);
        btnGenerate.setEnabled(false);

        firestoreManager.getUserMonthlyResumeCount(userId, new FirestoreManager.FirestoreCallback<FirestoreManager.MonthlyLimitInfo>() {
            @Override
            public void onSuccess(FirestoreManager.MonthlyLimitInfo info) {
                runOnUiThread(() -> {
                    if (info.getCount() >= MAX_MONTHLY_RESUMES) {
                        progressBar.setVisibility(View.GONE);
                        btnGenerate.setEnabled(true);
                        Toast.makeText(ResumeFormActivity.this,
                                "Monthly limit reached! You can create only " + MAX_MONTHLY_RESUMES + " resumes per month. Resets on: " + info.getResetDateString(),
                                Toast.LENGTH_LONG).show();
                    } else {
                        // Limit not reached, proceed with generation
                        generateResume();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);
                    Toast.makeText(ResumeFormActivity.this, "Error checking limit: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void generateResume() {
        if (!validateInputs()) {
            progressBar.setVisibility(View.GONE);
            btnGenerate.setEnabled(true);
            return;
        }

        KeyboardUtils.hideKeyboard(this);

        Resume resume = createResumeFromInputs();

        // progressBar is already visible from checkMonthlyLimitAndGenerate
        // btnGenerate is already disabled

        geminiClient.generateResume(resume, new GeminiClient.GeminiCallback() {
            @Override
            public void onSuccess(String generatedResume) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);

                    resume.setGeneratedContent(generatedResume);

                    firestoreManager.insertResume(resume, new FirestoreManager.FirestoreCallback<String>() {
                        @Override
                        public void onSuccess(String resumeId) {
                            runOnUiThread(() -> {

                                // Show interstitial ad before finishing
                                showInterstitialAndFinish(resumeId);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(ResumeFormActivity.this, "Error saving resume: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);
                    Toast.makeText(ResumeFormActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showInterstitialAndFinish(String resumeId) {
        if (interstitialAd != null) {
            AdHelper.showInterstitialAd(this, interstitialAd, new AdHelper.InterstitialAdShowCallback() {
                @Override
                public void onAdDismissed() {
                    navigateToGeneratedResume(resumeId);
                }

                @Override
                public void onAdFailedToShow(String error) {
                    navigateToGeneratedResume(resumeId);
                }
            });
        } else {
            navigateToGeneratedResume(resumeId);
        }
    }

    private void navigateToGeneratedResume(String resumeId) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("resume_id", resumeId);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private boolean validateInputs() {
        if (isEmpty(etName)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return false;
        }
        if (isEmpty(etEmail)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }
        if (isEmpty(etEducation)) {
            etEducation.setError("Education is required");
            etEducation.requestFocus();
            return false;
        }
        if (isEmpty(etSkills)) {
            etSkills.setError("Skills are required");
            etSkills.requestFocus();
            return false;
        }
        return true;
    }

    private boolean isEmpty(TextInputEditText editText) {
        return editText.getText() == null || editText.getText().toString().trim().isEmpty();
    }

    private Resume createResumeFromInputs() {
        Resume resume = new Resume();
        resume.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());

        String timestamp = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
        resume.setResumeName("Resume - " + timestamp);

        resume.setName(getText(etName));
        resume.setEmail(getText(etEmail));
        resume.setPhone(getText(etPhone));
        resume.setGithub(getText(etGithub));
        resume.setLinkedin(getText(etLinkedin));
        resume.setPortfolio(getText(etPortfolio));
        resume.setEducation(getText(etEducation));
        resume.setSkills(getText(etSkills));
        resume.setExperience(getText(etExperience));
        resume.setProjects(getText(etProjects));
        resume.setAchievements(getText(etAchievements));
        resume.setCourses(getText(etCourses));

        return resume;
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        KeyboardUtils.dispatchTouchEvent(this, event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerAdView != null) {
            bannerAdView.resume();
        }
    }

    @Override
    protected void onPause() {
        if (bannerAdView != null) {
            bannerAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (bannerAdView != null) {
            bannerAdView.destroy();
        }
        super.onDestroy();
    }
}