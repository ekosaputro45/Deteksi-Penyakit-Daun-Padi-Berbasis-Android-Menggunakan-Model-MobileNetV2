package com.example.deteksipenyakitdaunpadi;

import android.animation.ValueAnimator;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CompleteProfileActivity extends AppCompatActivity {

    private Button btnContinue;
    private ImageView ivBack;
    private AutoCompleteTextView actvGender;
    private TextInputEditText etDateOfBirth;
    private LinearLayout llGender;
    private Calendar calendar;
    private SimpleDateFormat dateFormatter;
    private ProgressBar progressBar;

    private ImageView ivProfilePhoto;
    private String selectedProfileImageUri;

    private final ActivityResultLauncher<String[]> pickProfileImageLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri == null) return;
                persistUriPermission(uri);
                selectedProfileImageUri = uri.toString();
                showSelectedImage(uri);
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        // Handle window insets for virtual navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, bottomInset);
            return insets;
        });

        btnContinue = findViewById(R.id.btnContinue);
        ivBack = findViewById(R.id.ivBack);
        actvGender = findViewById(R.id.actvGender);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        llGender = findViewById(R.id.llGender);
        progressBar = findViewById(R.id.progressBar);

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        View cvProfileImage = findViewById(R.id.cvProfileImage);
        View cvEditIcon = findViewById(R.id.cvEditIcon);

        View.OnClickListener pickImageClick = v -> pickProfileImageLauncher.launch(new String[]{"image/*"});
        if (cvProfileImage != null) cvProfileImage.setOnClickListener(pickImageClick);
        if (cvEditIcon != null) cvEditIcon.setOnClickListener(pickImageClick);
        if (ivProfilePhoto != null) ivProfilePhoto.setOnClickListener(pickImageClick);

        if (savedInstanceState != null) {
            selectedProfileImageUri = savedInstanceState.getString("selectedProfileImageUri");
            if (selectedProfileImageUri != null && !selectedProfileImageUri.trim().isEmpty()) {
                showSelectedImage(Uri.parse(selectedProfileImageUri));
            }
        }

        calendar = Calendar.getInstance();
        dateFormatter = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        // Setup Gender Dropdown
        String[] genders = new String[]{"Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genders);
        actvGender.setAdapter(genderAdapter);
        actvGender.setThreshold(0); // Show dropdown immediately when clicked
        actvGender.setKeyListener(null); // Prevent typing - only allow selection from dropdown
        
        // Make the entire LinearLayout clickable to trigger dropdown
        llGender.setOnClickListener(v -> {
            actvGender.requestFocus();
            actvGender.showDropDown();
        });
        
        // Handle click on AutoCompleteTextView itself
        actvGender.setOnClickListener(v -> {
            actvGender.requestFocus();
            actvGender.showDropDown();
        });
        
        // Handle click on dropdown icon
        ImageView ivGenderDropdown = findViewById(R.id.ivGenderDropdown);
        if (ivGenderDropdown != null) {
            ivGenderDropdown.setOnClickListener(v -> {
                actvGender.requestFocus();
                actvGender.showDropDown();
            });
        }

        // Setup Date Picker
        etDateOfBirth.setOnClickListener(v -> showDatePicker());
        
        // Also make the LinearLayout parent clickable
        LinearLayout llDateOfBirth = findViewById(R.id.llDateOfBirth);
        if (llDateOfBirth != null) {
            llDateOfBirth.setOnClickListener(v -> showDatePicker());
        }

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    animateProgressBarAndNavigate();
                }
            }
        });

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to the previous activity
            }
        });
    }

    private void showDatePicker() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                calendar.set(selectedYear, selectedMonth, selectedDay);
                String formattedDate = dateFormatter.format(calendar.getTime());
                etDateOfBirth.setText(formattedDate);
            },
            year,
            month,
            day
        );

        // Set max date to today (cannot select future dates)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private boolean validateInput() {
        TextInputEditText etFullName = findViewById(R.id.etFullName);
        TextInputEditText etPhoneNumber = findViewById(R.id.etPhoneNumber);

        String fullName = etFullName.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String gender = actvGender.getText().toString().trim();
        String dateOfBirth = etDateOfBirth.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (gender.isEmpty()) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (dateOfBirth.isEmpty()) {
            Toast.makeText(this, "Please select your date of birth", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void animateProgressBarAndNavigate() {
        // Animate progress bar from 50% to 100%
        ValueAnimator animator = ValueAnimator.ofInt(50, 100);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            int progress = (Integer) animation.getAnimatedValue();
            progressBar.setProgress(progress);
        });
        animator.start();

        // Navigate after animation completes
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                Intent intent = new Intent(CompleteProfileActivity.this, CreateAccountActivity.class);

                TextInputEditText etFullName = findViewById(R.id.etFullName);
                TextInputEditText etPhoneNumber = findViewById(R.id.etPhoneNumber);

                String fullName = etFullName != null && etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
                String phoneNumber = etPhoneNumber != null && etPhoneNumber.getText() != null ? etPhoneNumber.getText().toString().trim() : "";
                String gender = actvGender != null ? actvGender.getText().toString().trim() : "";
                String dateOfBirth = etDateOfBirth != null && etDateOfBirth.getText() != null ? etDateOfBirth.getText().toString().trim() : "";

                intent.putExtra("fullName", fullName);
                intent.putExtra("phoneNumber", phoneNumber);
                intent.putExtra("gender", gender);
                intent.putExtra("dateOfBirth", dateOfBirth);

                if (selectedProfileImageUri != null && !selectedProfileImageUri.trim().isEmpty()) {
                    intent.putExtra("profileImageUri", selectedProfileImageUri);
                }
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }

    private void showSelectedImage(Uri uri) {
        if (ivProfilePhoto == null) return;
        try {
            // Clear XML tint so the loaded image is not gray/hidden.
            ivProfilePhoto.setImageTintList(null);
            ivProfilePhoto.setColorFilter(null);
            Glide.with(this).load(uri).centerCrop().into(ivProfilePhoto);
        } catch (Exception e) {
            ivProfilePhoto.setImageResource(android.R.drawable.ic_menu_camera);
        }
    }

    private void persistUriPermission(Uri uri) {
        try {
            ContentResolver resolver = getContentResolver();
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
            resolver.takePersistableUriPermission(uri, flags);
        } catch (Exception ignored) {
            // Some providers don't support persistable permissions.
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedProfileImageUri", selectedProfileImageUri);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
