package com.passfamily.airesumebuilder.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.passfamily.airesumebuilder.R;
import com.passfamily.airesumebuilder.database.FirestoreManager;
import com.passfamily.airesumebuilder.model.Resume;
import com.passfamily.airesumebuilder.utils.AdHelper;
import com.passfamily.airesumebuilder.utils.Constants;
import com.passfamily.airesumebuilder.utils.KeyboardUtils;

public class EditResumeActivity extends AppCompatActivity {

    private TextInputEditText etEditName, etEditEmail, etEditPhone, etEditGithub,
            etEditLinkedin, etEditPortfolio, etEditContent;
    private FirestoreManager firestoreManager;
    private Resume currentResume;
    private String resumeId;
    private InterstitialAd interstitialAd;

    private static final String TAG = "EditResumeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge for Android 15+ compatibility
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_edit_resume);

        // Improved edge-to-edge handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        resumeId = getIntent().getStringExtra(Constants.EXTRA_RESUME_ID);
        if (resumeId == null || resumeId.isEmpty()) {
            Toast.makeText(this, "Error loading resume", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initViews();
        loadResume();
        setupKeyboardBehavior();
        loadInterstitialAd(); // Load interstitial ad on startup
    }

    private void loadInterstitialAd() {
        AdHelper.loadInterstitialAd(this, new AdHelper.InterstitialAdCallback() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                interstitialAd = ad;
                Log.d(TAG, "Interstitial ad loaded successfully");
            }

            @Override
            public void onAdFailedToLoad(String error) {
                interstitialAd = null;
                Log.e(TAG, "Failed to load interstitial ad: " + error);
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
        etEditName = findViewById(R.id.etEditName);
        etEditEmail = findViewById(R.id.etEditEmail);
        etEditPhone = findViewById(R.id.etEditPhone);
        etEditGithub = findViewById(R.id.etEditGithub);
        etEditLinkedin = findViewById(R.id.etEditLinkedin);
        etEditPortfolio = findViewById(R.id.etEditPortfolio);
        etEditContent = findViewById(R.id.etEditContent);

        firestoreManager = new FirestoreManager();
    }

    private void setupKeyboardBehavior() {
        // Hide keyboard when clicking outside
        androidx.coordinatorlayout.widget.CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinatorLayout);
        if (coordinatorLayout != null) {
            KeyboardUtils.setupHideKeyboardOnTouch(coordinatorLayout, this);
        }

        // Also setup for the scroll view
        android.widget.ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            KeyboardUtils.setupHideKeyboardOnTouch(scrollView, this);
        }
    }

    private void loadResume() {
        firestoreManager.getResumeById(resumeId, new FirestoreManager.FirestoreCallback<Resume>() {
            @Override
            public void onSuccess(Resume resume) {
                runOnUiThread(() -> {
                    currentResume = resume;
                    if (currentResume != null) {
                        // Set all the personal information fields
                        etEditName.setText(currentResume.getName());
                        etEditEmail.setText(currentResume.getEmail());
                        etEditPhone.setText(currentResume.getPhone());
                        etEditGithub.setText(currentResume.getGithub());
                        etEditLinkedin.setText(currentResume.getLinkedin());
                        etEditPortfolio.setText(currentResume.getPortfolio());

                        if (currentResume.getGeneratedContent() != null) {
                            etEditContent.setText(currentResume.getGeneratedContent());
                        }

                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Edit - " + currentResume.getResumeName());
                        }
                    } else {
                        Toast.makeText(EditResumeActivity.this, "Resume not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EditResumeActivity.this, error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void saveResume() {
        if (currentResume != null) {
            // Get updated values from all fields
            String name = etEditName.getText() != null ? etEditName.getText().toString().trim() : "";
            String email = etEditEmail.getText() != null ? etEditEmail.getText().toString().trim() : "";
            String phone = etEditPhone.getText() != null ? etEditPhone.getText().toString().trim() : "";
            String github = etEditGithub.getText() != null ? etEditGithub.getText().toString().trim() : "";
            String linkedin = etEditLinkedin.getText() != null ? etEditLinkedin.getText().toString().trim() : "";
            String portfolio = etEditPortfolio.getText() != null ? etEditPortfolio.getText().toString().trim() : "";
            String editedContent = etEditContent.getText() != null ? etEditContent.getText().toString() : "";

            // Validate required fields
            if (name.isEmpty()) {
                etEditName.setError("Name is required");
                etEditName.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                etEditEmail.setError("Email is required");
                etEditEmail.requestFocus();
                return;
            }

            // Update the resume object with new values
            currentResume.setName(name);
            currentResume.setEmail(email);
            currentResume.setPhone(phone);
            currentResume.setGithub(github);
            currentResume.setLinkedin(linkedin);
            currentResume.setPortfolio(portfolio);
            currentResume.setGeneratedContent(editedContent);

            // Show progress message
            Toast.makeText(EditResumeActivity.this, "Saving...", Toast.LENGTH_SHORT).show();

            firestoreManager.updateResume(currentResume, new FirestoreManager.FirestoreCallback<Void>() {
                // inside onSuccess() of updateResume()
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditResumeActivity.this,
                                "Resume saved successfully!", Toast.LENGTH_SHORT).show();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(Constants.EXTRA_RESUME_ID, resumeId);
                        setResult(RESULT_OK, resultIntent);

                        showInterstitialAndFinish();
                    });
                }


                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditResumeActivity.this, "Error saving resume: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void showInterstitialAndFinish() {
        if (interstitialAd != null) {
            AdHelper.showInterstitialAd(this, interstitialAd,
                    new AdHelper.InterstitialAdShowCallback() {
                        @Override
                        public void onAdDismissed() {
                            finish();
                        }

                        @Override
                        public void onAdFailedToShow(String error) {
                            finish();
                        }
                    });
        } else {
            finish();
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        KeyboardUtils.dispatchTouchEvent(this, event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_resume_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            saveResume();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}