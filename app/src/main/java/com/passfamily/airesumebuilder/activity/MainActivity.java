package com.passfamily.airesumebuilder.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.passfamily.airesumebuilder.R;
import com.passfamily.airesumebuilder.adapter.ResumeListAdapter;
import com.passfamily.airesumebuilder.database.FirestoreManager;
import com.passfamily.airesumebuilder.model.Resume;
import com.passfamily.airesumebuilder.utils.AdHelper;
import com.passfamily.airesumebuilder.utils.Constants;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView resumeRecyclerView;
    private TextView emptyView, tvMonthlyLimit, tvResetInfo;
    private ResumeListAdapter adapter;
    private FirestoreManager firestoreManager;
    private FirebaseAuth mAuth;
    private AdView bannerAdView;
    private FloatingActionButton fabAddResume;
    private ProgressBar progressBarLimit;
    private InterstitialAd interstitialAd;

    private static final String TAG = "MainActivity";
    private static final int MAX_MONTHLY_RESUMES = 3;

    private ActivityResultLauncher<Intent> resumeFormLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadResumes();
                    checkMonthlyLimit(); // Refresh limit after creating resume
                    // Reload interstitial ad for next time
                    loadInterstitialAd();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        firestoreManager = new FirestoreManager();

        // Initialize Mobile Ads
        AdHelper.initializeMobileAds(this);

        setupViews();
        setupRecyclerView();
        loadResumes();
        checkMonthlyLimit();
        loadBannerAd();
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

    private void setupViews() {
        resumeRecyclerView = findViewById(R.id.resumeRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        tvMonthlyLimit = findViewById(R.id.tvMonthlyLimit);
        tvResetInfo = findViewById(R.id.tvResetInfo);
        fabAddResume = findViewById(R.id.fabAddResume);
        progressBarLimit = findViewById(R.id.progressBarLimit);

        fabAddResume.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(MainActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();

            // Check monthly limit before allowing to create resume
            progressBarLimit.setVisibility(View.VISIBLE);
            firestoreManager.getUserMonthlyResumeCount(userId, new FirestoreManager.FirestoreCallback<FirestoreManager.MonthlyLimitInfo>() {
                @Override
                public void onSuccess(FirestoreManager.MonthlyLimitInfo info) {
                    runOnUiThread(() -> {
                        progressBarLimit.setVisibility(View.GONE);
                        if (info.getCount() >= MAX_MONTHLY_RESUMES) {
                            // Limit reached - show error message, NO AD
                            Toast.makeText(MainActivity.this,
                                    "Monthly limit reached! You can create only " + MAX_MONTHLY_RESUMES + " resumes per month. Resets on: " + info.getResetDateString(),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // Limit not reached - show ad then start activity
                            showInterstitialAndOpenForm();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBarLimit.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Error checking limit: " + error, Toast.LENGTH_SHORT).show();
                        // Still allow creating resume if limit check fails
                        showInterstitialAndOpenForm();
                    });
                }
            });
        });

        ImageButton btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(this::showSettingsMenu);
    }

    private void showSettingsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_privacy_policy) {
                openPrivacyPolicy();
                return true;
            } else if (itemId == R.id.action_logout) {
                showLogoutDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void openPrivacyPolicy() {
        // IMPORTANT: Replace with your actual privacy policy URL
        String url = "https://ai-resume-builder-privacy-policy.vercel.app/";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open browser for Privacy Policy.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showInterstitialAndOpenForm() {
        if (interstitialAd != null) {
            AdHelper.showInterstitialAd(this, interstitialAd, new AdHelper.InterstitialAdShowCallback() {
                @Override
                public void onAdDismissed() {
                    // Ad was shown and dismissed, now open the form
                    openResumeForm();
                }

                @Override
                public void onAdFailedToShow(String error) {
                    // Ad failed to show, still open the form
                    Log.e(TAG, "Failed to show interstitial ad: " + error);
                    openResumeForm();
                }
            });
        } else {
            // No ad loaded, directly open the form
            Log.d(TAG, "No interstitial ad available, opening form directly");
            openResumeForm();
        }
    }

    private void openResumeForm() {
        Intent intent = new Intent(MainActivity.this, ResumeFormActivity.class);
        resumeFormLauncher.launch(intent);
    }

    private void checkMonthlyLimit() {
        if (mAuth.getCurrentUser() == null) {
            tvMonthlyLimit.setText("Monthly Limit: 0/" + MAX_MONTHLY_RESUMES + " resumes");
            tvResetInfo.setText("Resets 30 days after first resume");
            tvMonthlyLimit.setVisibility(View.VISIBLE);
            tvResetInfo.setVisibility(View.VISIBLE);
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        progressBarLimit.setVisibility(View.VISIBLE);

        firestoreManager.getUserMonthlyResumeCount(userId, new FirestoreManager.FirestoreCallback<FirestoreManager.MonthlyLimitInfo>() {
            @Override
            public void onSuccess(FirestoreManager.MonthlyLimitInfo info) {
                runOnUiThread(() -> {
                    progressBarLimit.setVisibility(View.GONE);
                    updateMonthlyLimitUI(info);
                    Log.d(TAG, "Monthly limit count: " + info.getCount() + ", Days remaining: " + info.getDaysRemaining());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBarLimit.setVisibility(View.GONE);
                    tvMonthlyLimit.setText("Monthly Limit: Error loading");
                    tvMonthlyLimit.setTextColor(getResources().getColor(R.color.red));
                    tvResetInfo.setText("Unable to load reset info");
                    tvMonthlyLimit.setVisibility(View.VISIBLE);
                    tvResetInfo.setVisibility(View.VISIBLE);
                    fabAddResume.setEnabled(true);
                    fabAddResume.setBackgroundTintList(getResources().getColorStateList(R.color.purple_500));
                    Log.e(TAG, "Error checking monthly limit: " + error);
                });
            }
        });
    }

    private void updateMonthlyLimitUI(FirestoreManager.MonthlyLimitInfo info) {
        int count = info.getCount();
        long daysRemaining = info.getDaysRemaining();

        String limitText = "Monthly Limit: " + count + "/" + MAX_MONTHLY_RESUMES + " resumes";
        tvMonthlyLimit.setText(limitText);

        // Reset info text
        if (count == 0) {
            tvResetInfo.setText("Counter starts when you create your first resume");
        } else {
            tvResetInfo.setText("Resets on: " + info.getResetDateString() + " (" + daysRemaining + " days)");
        }

        if (count >= MAX_MONTHLY_RESUMES) {
            // Limit reached - change text to red and disable FAB
            tvMonthlyLimit.setTextColor(getResources().getColor(R.color.red));
            tvResetInfo.setTextColor(getResources().getColor(R.color.red));
            fabAddResume.setEnabled(false);
            fabAddResume.setBackgroundTintList(getResources().getColorStateList(R.color.red));
            fabAddResume.setContentDescription("Monthly limit reached. Cannot create more resumes this month.");
            fabAddResume.setAlpha(0.5f); // Make it visually disabled
        } else {
            // Limit not reached - keep purple color and enable FAB
            tvMonthlyLimit.setTextColor(getResources().getColor(R.color.purple_500));
            tvResetInfo.setTextColor(getResources().getColor(R.color.purple_500));
            fabAddResume.setEnabled(true);
            fabAddResume.setBackgroundTintList(getResources().getColorStateList(R.color.purple_500));
            fabAddResume.setContentDescription("Add new resume");
            fabAddResume.setAlpha(1.0f); // Fully visible

            // Show remaining count message
            int remaining = MAX_MONTHLY_RESUMES - count;
            String message = remaining == 1
                    ? "You can create 1 more resume this month"
                    : "You can create " + remaining + " more resumes this month";

            // Don't show toast when first loading or when count is 0
            if (count > 0) {
                // Only show if we just came back from creating a resume
            }
        }

        // Always show the limit text
        tvMonthlyLimit.setVisibility(View.VISIBLE);
        tvResetInfo.setVisibility(View.VISIBLE);
    }

    private void loadBannerAd() {
        LinearLayout adContainer = findViewById(R.id.bannerAdContainer);
        bannerAdView = AdHelper.loadBannerAd(this, adContainer);
    }

    private void setupRecyclerView() {
        adapter = new ResumeListAdapter(this);
        resumeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resumeRecyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(resume -> {
            Intent intent = new Intent(MainActivity.this, GeneratedResumeActivity.class);
            intent.putExtra(Constants.EXTRA_RESUME_ID, resume.getId());
            startActivity(intent);
        });

        adapter.setOnEditNameClickListener(this::showEditResumeNameDialog);
        adapter.setOnDeleteClickListener(this::showDeleteResumeDialog);
    }

    private void loadResumes() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Loading resumes for user: " + userId);

        firestoreManager.getAllResumesForUser(userId, new FirestoreManager.FirestoreCallback<List<Resume>>() {
            @Override
            public void onSuccess(List<Resume> resumes) {
                Log.d(TAG, "Successfully loaded " + resumes.size() + " resumes");
                runOnUiThread(() -> {
                    if (resumes.isEmpty()) {
                        Log.d(TAG, "No resumes found, showing empty view");
                        resumeRecyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        Log.d(TAG, "Resumes found, updating adapter");
                        resumeRecyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                        adapter.updateResumes(resumes);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading resumes: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    resumeRecyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void showEditResumeNameDialog(Resume resume) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Resume Name");

        EditText input = new EditText(this);
        input.setText(resume.getResumeName());
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                resume.setResumeName(newName);
                firestoreManager.updateResume(resume, new FirestoreManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            loadResumes();
                            Toast.makeText(MainActivity.this, "Resume name updated", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteResumeDialog(Resume resume) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Resume")
                .setMessage("Are you sure you want to delete this resume?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firestoreManager.deleteResume(resume, new FirestoreManager.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            runOnUiThread(() -> {
                                loadResumes();
                                // Note: Deleting doesn't reduce the monthly count
                                Toast.makeText(MainActivity.this, "Resume deleted", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadResumes();
        checkMonthlyLimit();
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
