package com.passfamily.airesumebuilder.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdView;
import com.passfamily.airesumebuilder.R;
import com.passfamily.airesumebuilder.database.FirestoreManager;
import com.passfamily.airesumebuilder.model.Resume;
import com.passfamily.airesumebuilder.utils.AdHelper;
import com.passfamily.airesumebuilder.utils.Constants;
import com.passfamily.airesumebuilder.utils.PDFGenerator;

import java.io.File;

public class GeneratedResumeActivity extends AppCompatActivity {

    private TextView tvGeneratedResume;
    private Button btnEdit, btnDownload;

    private FirestoreManager firestoreManager;
    private Resume currentResume;
    private String resumeId;
    private String lastGeneratedPdfPath;

    private static final int PERMISSION_REQUEST_CODE = 100;

    // Add this at the end of onCreate() method in GeneratedResumeActivity
    private AdView bannerAdView;

    private final ActivityResultLauncher<Intent> editResumeLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            lastGeneratedPdfPath = null; // force fresh PDF
                            loadResume(); // reload edited content
                        }
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_generated_resume);

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
        setupClickListeners();
        loadResume();
        // ... existing code ...
        loadBannerAd();
    }

    private void loadBannerAd() {
        LinearLayout adContainer = findViewById(R.id.bannerAdContainer);
        bannerAdView = AdHelper.loadBannerAd(this, adContainer);
    }

    private void setupToolbar() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnShare = findViewById(R.id.btnSharePDF);
        TextView tvTitle = findViewById(R.id.tvAppBarTitle);

        btnBack.setOnClickListener(v -> onBackPressed());
        btnShare.setOnClickListener(v -> shareResume());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true; // Menu removed
    }

    private void initViews() {
        tvGeneratedResume = findViewById(R.id.tvGeneratedResume);
        btnEdit = findViewById(R.id.btnEdit);
        btnDownload = findViewById(R.id.btnDownload);
        firestoreManager = new FirestoreManager();

        tvGeneratedResume.setMovementMethod(LinkMovementMethod.getInstance());
        tvGeneratedResume.setHorizontallyScrolling(false);
        tvGeneratedResume.setSingleLine(false);
    }

    private void setupClickListeners() {
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(
                    GeneratedResumeActivity.this,
                    EditResumeActivity.class
            );
            intent.putExtra(Constants.EXTRA_RESUME_ID, resumeId);
            editResumeLauncher.launch(intent); // ✅ NOT startActivity()
        });

        btnDownload.setOnClickListener(v -> checkPermissionsAndDownload());
    }

    private void checkPermissionsAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            downloadPDF();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                downloadPDF();
            } else {
                ActivityCompat.requestPermissions(GeneratedResumeActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            }
        } else {
            downloadPDF();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadPDF();
            } else {
                Toast.makeText(this, "Permission denied. Cannot save PDF.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadResume() {
        firestoreManager.getResumeById(resumeId, new FirestoreManager.FirestoreCallback<Resume>() {
            @Override
            public void onSuccess(Resume resume) {
                runOnUiThread(() -> {
                    currentResume = resume;
                    if (currentResume != null && currentResume.getGeneratedContent() != null) {
                        String content = currentResume.getGeneratedContent();

                        SpannableString spannableContent = new SpannableString(content);
                        String[] words = content.split("\\s+");
                        int currentIndex = 0;

                        for (String word : words) {
                            int start = content.indexOf(word, currentIndex);
                            if (start >= 0) {
                                int end = start + word.length();

                                if (isUrl(word)) {
                                    String url = word;
                                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                        url = "https://" + url;
                                    }
                                    spannableContent.setSpan(new URLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                } else if (word.contains("@") && word.contains(".")) {
                                    spannableContent.setSpan(new URLSpan("mailto:" + word), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }

                                currentIndex = end;
                            }
                        }

                        tvGeneratedResume.setText(spannableContent);

                    } else {
                        Toast.makeText(GeneratedResumeActivity.this, "Resume not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(GeneratedResumeActivity.this, error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private boolean isUrl(String text) {
        return text.contains(".com") || text.contains(".org") || text.contains(".net") ||
                text.contains(".in") || text.contains("github.com") || text.contains("linkedin.com");
    }

    private void downloadPDF() {
        if (currentResume != null) {
            Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();

            PDFGenerator.generateStandardPDF(
                    GeneratedResumeActivity.this,
                    currentResume,
                    new PDFGenerator.PDFCallback() {
                        @Override
                        public void onSuccess(String filePath) {
                            runOnUiThread(() -> {
                                lastGeneratedPdfPath = filePath;
                                String fileName = new java.io.File(filePath).getName();
                                Toast.makeText(GeneratedResumeActivity.this,
                                        "Resume downloaded: " + fileName,
                                        Toast.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(GeneratedResumeActivity.this,
                                        "Error: " + error, Toast.LENGTH_LONG).show();
                            });
                        }
                    }
            );
        } else {
            Toast.makeText(this, "Resume not loaded", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareResume() {
        if (currentResume == null) {
            Toast.makeText(this, "Resume not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Force regenerate if edited
        lastGeneratedPdfPath = null;

        Toast.makeText(this, "Generating updated PDF...", Toast.LENGTH_SHORT).show();
        PDFGenerator.generateStandardPDF(
                this,
                currentResume,
                new PDFGenerator.PDFCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        lastGeneratedPdfPath = filePath;
                        sharePdfFile(new File(filePath));
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(
                                GeneratedResumeActivity.this,
                                error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }


    private void sharePdfFile(File pdfFile) {
        try {
            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentResume.getResumeName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Sharing my resume: " + currentResume.getResumeName());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareIntent, "Share Resume via");
            startActivity(chooser);

        } catch (Exception e) {
            Toast.makeText(this, "Error sharing PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadResume();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item); // No menu now
    }
}