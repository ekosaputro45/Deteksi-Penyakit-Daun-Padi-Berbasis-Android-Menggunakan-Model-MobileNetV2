package com.example.deteksipenyakitdaunpadi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.DatePicker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private String loggedInEmail;

    private ImageView ivProfilePhoto;
    private TextView tvProfileUsername;
    private TextView tvProfileEmail;

    private AlertDialog editProfileDialog;
    private ImageView dialogProfileImage;
    private TextInputEditText dialogUsername;

    private final ActivityResultLauncher<String[]> pickProfileImageLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri == null) return;
                persistUriPermission(uri);
                if (loggedInEmail != null) {
                    dbHelper.updateProfileImageUri(loggedInEmail, uri.toString());
                    bindUser();
                    if (dialogProfileImage != null) {
                        Glide.with(this).load(uri).centerCrop().into(dialogProfileImage);
                    }
                }
            }
    );

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        loggedInEmail = sessionManager.getLoggedInEmail();
        if (loggedInEmail == null || loggedInEmail.trim().isEmpty()) {
            startActivity(new Intent(this, SocialLoginActivity.class));
            finish();
            return;
        }

        // Ensure bottom navbar isn't covered by gesture/navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            View bottomNavCard = v.findViewById(R.id.bottomNavCard);
            if (bottomNavCard != null) {
                int extraLiftPx = Math.round(8f * getResources().getDisplayMetrics().density);
                android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) bottomNavCard.getLayoutParams();
                lp.bottomMargin = bottomInset + extraLiftPx;
                bottomNavCard.setLayoutParams(lp);
            }

            View bottomContainer = v.findViewById(R.id.llBottomNavContainer);
            if (bottomContainer != null) {
                bottomContainer.setPadding(0, 0, 0, 0);
            }
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbarProfile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        RelativeLayout logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> showLogoutDialog());

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvProfileUsername = findViewById(R.id.tvProfileUsername);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);

        View cardUserInfo = findViewById(R.id.cardUserInfo);
        if (cardUserInfo != null) {
            cardUserInfo.setOnClickListener(v -> showEditProfileDialog());
        }

        View rowPersonalInfo = findViewById(R.id.rowPersonalInfo);
        if (rowPersonalInfo != null) {
            rowPersonalInfo.setOnClickListener(v -> showPersonalInfoDialog());
        }

        View rowSecurity = findViewById(R.id.rowSecurity);
        if (rowSecurity != null) {
            rowSecurity.setOnClickListener(v -> showChangePasswordDialog());
        }

        bindUser();

        setupBottomNavigation();
        setActiveNavItem(R.id.llNavProfile);
    }

    private void bindUser() {
        if (loggedInEmail == null) return;
        UserAccount user = dbHelper.getUserByEmail(loggedInEmail);
        if (user == null) return;

        if (tvProfileUsername != null) {
            // Prefer fullName if present; fallback to username.
            String display = (user.fullName != null && !user.fullName.trim().isEmpty()) ? user.fullName : user.username;
            tvProfileUsername.setText(display);
        }
        if (tvProfileEmail != null) tvProfileEmail.setText(user.email);

        if (ivProfilePhoto != null) {
            if (user.profileImageUri != null && !user.profileImageUri.trim().isEmpty()) {
                try {
                    Glide.with(this)
                            .load(Uri.parse(user.profileImageUri))
                            .centerCrop()
                            .into(ivProfilePhoto);
                } catch (Exception e) {
                    ivProfilePhoto.setImageResource(android.R.drawable.ic_menu_camera);
                }
            } else {
                ivProfilePhoto.setImageResource(android.R.drawable.ic_menu_camera);
            }
        }
    }

    private void showEditProfileDialog() {
        if (loggedInEmail == null) return;
        UserAccount user = dbHelper.getUserByEmail(loggedInEmail);
        if (user == null) return;

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        dialogProfileImage = view.findViewById(R.id.ivEditProfilePhoto);
        dialogUsername = view.findViewById(R.id.etEditUsername);

        dialogUsername.setText(user.username);

        if (user.profileImageUri != null && !user.profileImageUri.trim().isEmpty()) {
            try {
                Glide.with(this).load(Uri.parse(user.profileImageUri)).centerCrop().into(dialogProfileImage);
            } catch (Exception e) {
                dialogProfileImage.setImageResource(android.R.drawable.ic_menu_camera);
            }
        } else {
            dialogProfileImage.setImageResource(android.R.drawable.ic_menu_camera);
        }

        dialogProfileImage.setOnClickListener(v -> pickProfileImageLauncher.launch(new String[]{"image/*"}));

        editProfileDialog = new AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(view)
                .setNegativeButton("Batal", (d, w) -> d.dismiss())
                .setPositiveButton("Simpan", null)
                .create();

        editProfileDialog.setOnShowListener(d -> {
            Button positive = editProfileDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                String newUsername = dialogUsername.getText() != null ? dialogUsername.getText().toString().trim() : "";
                if (newUsername.isEmpty()) {
                    dialogUsername.setError("Username tidak boleh kosong");
                    return;
                }

                boolean ok = dbHelper.updateUsername(loggedInEmail, newUsername);
                if (ok) {
                    Toast.makeText(this, "Profile diperbarui", Toast.LENGTH_SHORT).show();
                    bindUser();
                    editProfileDialog.dismiss();
                } else {
                    Toast.makeText(this, "Gagal memperbarui profile", Toast.LENGTH_SHORT).show();
                }
            });
        });

        editProfileDialog.show();
    }

    private void showPersonalInfoDialog() {
        if (loggedInEmail == null) return;
        UserAccount user = dbHelper.getUserByEmail(loggedInEmail);
        if (user == null) return;

        View view = getLayoutInflater().inflate(R.layout.dialog_personal_info, null);

        TextInputLayout tilFullName = view.findViewById(R.id.tilFullName);
        TextInputLayout tilPhone = view.findViewById(R.id.tilPhoneNumber);
        TextInputLayout tilDob = view.findViewById(R.id.tilDateOfBirth);

        TextInputEditText etFullName = view.findViewById(R.id.etFullName);
        TextInputEditText etPhone = view.findViewById(R.id.etPhoneNumber);
        TextInputEditText etGender = view.findViewById(R.id.etGender);
        TextInputEditText etDob = view.findViewById(R.id.etDateOfBirth);

        if (etFullName != null) etFullName.setText(user.fullName != null ? user.fullName : "");
        if (etPhone != null) etPhone.setText(user.phoneNumber != null ? user.phoneNumber : "");
        if (etGender != null) etGender.setText(user.gender != null ? user.gender : "");
        if (etDob != null) etDob.setText(user.dateOfBirth != null ? user.dateOfBirth : "");

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        View.OnClickListener pickDob = v -> {
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dp = new DatePickerDialog(
                    this,
                    (DatePicker picker, int y, int m, int d) -> {
                        cal.set(y, m, d);
                        if (etDob != null) {
                            etDob.setText(fmt.format(cal.getTime()));
                        }
                    },
                    year,
                    month,
                    day
            );
            dp.getDatePicker().setMaxDate(System.currentTimeMillis());
            dp.show();
        };

        if (etDob != null) etDob.setOnClickListener(pickDob);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Informasi Pribadi")
                .setView(view)
                .setNegativeButton("Batal", (d, w) -> d.dismiss())
                .setPositiveButton("Simpan", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                String fullName = etFullName != null && etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
                String phone = etPhone != null && etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                String dob = etDob != null && etDob.getText() != null ? etDob.getText().toString().trim() : "";

                // Validation follows registration (CompleteProfileActivity): required fields.
                if (fullName.isEmpty()) {
                    if (tilFullName != null) tilFullName.setError("Please enter your full name");
                    Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
                    return;
                } else if (tilFullName != null) {
                    tilFullName.setError(null);
                }

                if (phone.isEmpty()) {
                    if (tilPhone != null) tilPhone.setError("Please enter your phone number");
                    Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                    return;
                } else if (tilPhone != null) {
                    tilPhone.setError(null);
                }

                if (dob.isEmpty()) {
                    if (tilDob != null) tilDob.setError("Please select your date of birth");
                    Toast.makeText(this, "Please select your date of birth", Toast.LENGTH_SHORT).show();
                    return;
                } else if (tilDob != null) {
                    tilDob.setError(null);
                }

                boolean ok = dbHelper.updatePersonalInfo(loggedInEmail, fullName, phone, dob);
                if (ok) {
                    Toast.makeText(this, "Informasi pribadi diperbarui", Toast.LENGTH_SHORT).show();
                    bindUser();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Gagal memperbarui informasi", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        if (loggedInEmail == null) return;

        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText etOld = view.findViewById(R.id.etOldPassword);
        TextInputEditText etNew = view.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = view.findViewById(R.id.etConfirmNewPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Ubah Password")
                .setView(view)
                .setNegativeButton("Batal", (d, w) -> d.dismiss())
                .setPositiveButton("Simpan", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                String oldPw = etOld.getText() != null ? etOld.getText().toString() : "";
                String newPw = etNew.getText() != null ? etNew.getText().toString() : "";
                String confirm = etConfirm.getText() != null ? etConfirm.getText().toString() : "";

                if (newPw.length() < 6) {
                    etNew.setError("Password minimal 6 karakter");
                    return;
                }
                if (!newPw.equals(confirm)) {
                    etConfirm.setError("Konfirmasi password tidak cocok");
                    return;
                }

                boolean ok = dbHelper.changePassword(loggedInEmail, oldPw, newPw);
                if (ok) {
                    Toast.makeText(this, "Password berhasil diubah", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Password lama salah atau gagal mengubah", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
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

    private void setupBottomNavigation() {
        LinearLayout llNavHome = findViewById(R.id.llNavHome);
        LinearLayout llNavCosmetologist = findViewById(R.id.llNavCosmetologist);
        LinearLayout llNavHistory = findViewById(R.id.llNavHistory);
        LinearLayout llNavProfile = findViewById(R.id.llNavProfile);

        if (llNavHome != null) {
            llNavHome.setOnClickListener(v -> {
                setActiveNavItem(R.id.llNavHome);
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (llNavCosmetologist != null) {
            llNavCosmetologist.setOnClickListener(v -> {
                setActiveNavItem(R.id.llNavCosmetologist);
                startActivity(new Intent(ProfileActivity.this, NewsActivity.class));
                finish();
            });
        }

        if (llNavHistory != null) {
            llNavHistory.setOnClickListener(v -> {
                setActiveNavItem(R.id.llNavHistory);
                startActivity(new Intent(ProfileActivity.this, HistoryActivity.class));
                finish();
            });
        }

        if (llNavProfile != null) {
            llNavProfile.setOnClickListener(v -> setActiveNavItem(R.id.llNavProfile));
        }
    }

    private void setActiveNavItem(int activeItemId) {
        setNavItemState(R.id.llNavHome, false);
        setNavItemState(R.id.llNavCosmetologist, false);
        setNavItemState(R.id.llNavHistory, false);
        setNavItemState(R.id.llNavProfile, false);
        setNavItemState(activeItemId, true);
    }

    private void setNavItemState(int itemId, boolean isActive) {
        LinearLayout item = findViewById(itemId);
        if (item == null) return;

        android.widget.ImageView icon = null;
        android.widget.TextView text = null;

        if (itemId == R.id.llNavHome) {
            icon = findViewById(R.id.ivNavHome);
            text = findViewById(R.id.tvNavHome);
        } else if (itemId == R.id.llNavCosmetologist) {
            icon = findViewById(R.id.ivNavCosmetologist);
            text = findViewById(R.id.tvNavCosmetologist);
        } else if (itemId == R.id.llNavHistory) {
            icon = findViewById(R.id.ivNavHistory);
            text = findViewById(R.id.tvNavHistory);
        } else if (itemId == R.id.llNavProfile) {
            icon = findViewById(R.id.ivNavProfile);
            text = findViewById(R.id.tvNavProfile);
        }

        if (icon != null) {
            icon.setColorFilter(isActive ? Color.parseColor("#F05A7E") : Color.parseColor("#9CA3AF"));
        }
        if (text != null) {
            text.setTextColor(isActive ? Color.parseColor("#F05A7E") : Color.parseColor("#9CA3AF"));
        }
    }

    private void showLogoutDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_logout);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Blur background (Android 12+) + dim fallback
        setBackgroundBlur(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.35f);
        }

        dialog.setCancelable(true);

        dialog.setOnDismissListener(d -> setBackgroundBlur(false));
        dialog.setOnCancelListener(d -> setBackgroundBlur(false));

        Button btnCancel = dialog.findViewById(R.id.btnCancelLogout);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirmLogout);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            sessionManager.logout();
            Intent intent = new Intent(ProfileActivity.this, SocialLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
