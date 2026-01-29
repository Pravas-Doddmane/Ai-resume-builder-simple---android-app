package com.passfamily.airesumebuilder.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.passfamily.airesumebuilder.R;
import com.passfamily.airesumebuilder.utils.KeyboardUtils;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private MaterialButton googleSignInButton;
    private ProgressBar progressBar;
    private LinearLayout otpInputLayout;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private String verificationId;
    private String enteredPhone;

    // ActivityResultLauncher for Google Sign-In
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    handleGoogleSignInResult(task);
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Google Sign-In cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initViews();
        setupClickListeners();
        setupKeyboardBehavior();

        if (savedInstanceState != null) {
            verificationId = savedInstanceState.getString("verificationId");
            enteredPhone = savedInstanceState.getString("enteredPhone");
        }
    }

    private void initViews() {
        googleSignInButton = findViewById(R.id.googleSignInButton);
        progressBar = findViewById(R.id.progressBar);
        otpInputLayout = findViewById(R.id.otpInputLayout);

        // Make phone-related views GONE
        LinearLayout phoneInputLayout = findViewById(R.id.phoneInputLayout);
        if (phoneInputLayout != null) {
            phoneInputLayout.setVisibility(View.GONE);
        }

        // Hide the divider and "or continue with phone" text
        LinearLayout dividerLayout = findViewById(R.id.divider_layout);
        if (dividerLayout != null) {
            dividerLayout.setVisibility(View.GONE);
        }

        // Update subtitle text
        TextView subtitle = findViewById(R.id.subtitle);
        if (subtitle != null) {
            subtitle.setText("Sign in with Google to continue");
        }

        // Make the privacy policy link clickable
        TextView tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy);
        if (tvPrivacyPolicy != null) {
            tvPrivacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void setupKeyboardBehavior() {
        android.widget.ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            KeyboardUtils.setupHideKeyboardOnTouch(scrollView, this);
        }

        LinearLayout mainLayout = findViewById(R.id.main_layout);
        if (mainLayout != null) {
            KeyboardUtils.setupHideKeyboardOnTouch(mainLayout, this);
        }
    }

    private void setupClickListeners() {
        // Google Sign-In Button
        if (googleSignInButton != null) {
            googleSignInButton.setOnClickListener(v -> {
                KeyboardUtils.hideKeyboard(LoginActivity.this);
                signInWithGoogle();
            });
        }
    }

    private void signInWithGoogle() {
        progressBar.setVisibility(View.VISIBLE);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(TAG, "Google Sign-In successful: " + account.getEmail());
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.w(TAG, "Google Sign-In failed", e);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase authentication successful");
                        Toast.makeText(LoginActivity.this, "Sign-In successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Log.w(TAG, "Firebase authentication failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed: " +
                                Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        KeyboardUtils.dispatchTouchEvent(this, event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("verificationId", verificationId);
        outState.putString("enteredPhone", enteredPhone);
    }
}
