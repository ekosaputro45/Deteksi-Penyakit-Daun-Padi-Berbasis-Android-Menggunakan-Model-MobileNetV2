package com.example.deteksipenyakitdaunpadi;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.text.Editable;
import android.text.TextWatcher;

public class CreateAccountActivity extends AppCompatActivity {

    private Button btnFinish;
    private ImageView ivBack;
    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilConfirmPassword;
    private DatabaseHelper dbHelper;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        dbHelper = new DatabaseHelper(this);

        btnFinish = findViewById(R.id.btnFinish);
        ivBack = findViewById(R.id.ivBack);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword); // Menggunakan ID yang benar
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        progressBar = findViewById(R.id.progressBar);

        // Set progress bar to 100% on start (this is screen 2)
        progressBar.setProgress(100);

        TextWatcher passwordWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordMatchError();
            }

            @Override public void afterTextChanged(Editable s) {}
        };
        etPassword.addTextChangedListener(passwordWatcher);
        etConfirmPassword.addTextChangedListener(passwordWatcher);

        btnFinish.setOnClickListener(v -> {
            if (validateInput()) {
                if (saveUserToDatabase()) {
                    showSuccessDialog();
                } else {
                    Toast.makeText(this, "Email sudah terdaftar", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ivBack.setOnClickListener(v -> finish());
    }

    private void updatePasswordMatchError() {
        if (tilConfirmPassword == null) return;

        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        // Only show mismatch error while user is typing confirmation.
        if (!confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Password tidak cocok");
        } else {
            tilConfirmPassword.setError(null);
        }
    }

    private boolean validateInput() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Masukkan email yang valid");
            return false;
        }

        if (password.isEmpty() || password.length() < 6) {
            etPassword.setError("Password minimal 6 karakter");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            if (tilConfirmPassword != null) {
                tilConfirmPassword.setError("Password tidak cocok");
            } else {
                etConfirmPassword.setError("Password tidak cocok");
            }
            return false;
        }

        return true;
    }

    private boolean saveUserToDatabase() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        String fullName = getIntent() != null ? getIntent().getStringExtra("fullName") : null;
        String phoneNumber = getIntent() != null ? getIntent().getStringExtra("phoneNumber") : null;
        String gender = getIntent() != null ? getIntent().getStringExtra("gender") : null;
        String dateOfBirth = getIntent() != null ? getIntent().getStringExtra("dateOfBirth") : null;
        String profileImageUri = getIntent() != null ? getIntent().getStringExtra("profileImageUri") : null;

        boolean ok = dbHelper.addUser(email, password, fullName, phoneNumber, gender, dateOfBirth, profileImageUri);
        return ok;
    }

    private void setBackgroundBlur(boolean enabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        View content = findViewById(android.R.id.content);
        if (content == null) return;

        if (enabled) {
            content.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP));
        } else {
            content.setRenderEffect(null);
        }
    }

    private void showSuccessDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_signup_successful);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Blur background (Android 12+) + dim fallback
        setBackgroundBlur(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.35f);
        }

        dialog.setCancelable(false);

        dialog.setOnDismissListener(d -> setBackgroundBlur(false));

        Button btnGoToHome = dialog.findViewById(R.id.btnGoToHome);
        btnGoToHome.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(CreateAccountActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
