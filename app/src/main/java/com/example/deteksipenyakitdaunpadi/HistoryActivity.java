package com.example.deteksipenyakitdaunpadi;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnItemClickListener {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyItems;
    private DatabaseHelper dbHelper;
    private Map<String, String> saran;
    private LinearLayout llEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);
        initSaran();

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        rvHistory = findViewById(R.id.rvHistory);
        llEmptyState = findViewById(R.id.llEmptyState);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        setupBottomNavigation();
        setActiveNavItem(R.id.llNavHistory);

        // Ambil data dari database
        historyItems = dbHelper.getAllHistory();

        adapter = new HistoryAdapter(this, historyItems, this);
        rvHistory.setAdapter(adapter);

        // Tampilkan/sembunyikan empty state
        updateEmptyState();

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback());
        itemTouchHelper.attachToRecyclerView(rvHistory);
    }

    private void setupBottomNavigation() {
        LinearLayout llNavHome = findViewById(R.id.llNavHome);
        LinearLayout llNavCosmetologist = findViewById(R.id.llNavCosmetologist);
        LinearLayout llNavHistory = findViewById(R.id.llNavHistory);
        LinearLayout llNavProfile = findViewById(R.id.llNavProfile);

        if (llNavHome != null) {
            llNavHome.setOnClickListener(v -> {
                setActiveNavItem(R.id.llNavHome);
                Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (llNavCosmetologist != null) {
            llNavCosmetologist.setOnClickListener(v -> {
                setActiveNavItem(R.id.llNavCosmetologist);
                startActivity(new Intent(HistoryActivity.this, NewsActivity.class));
                finish();
            });
        }

        if (llNavHistory != null) {
            llNavHistory.setOnClickListener(v -> setActiveNavItem(R.id.llNavHistory));
        }

        if (llNavProfile != null) {
            llNavProfile.setOnClickListener(v -> {
                setActiveNavItem(R.id.llNavProfile);
                startActivity(new Intent(HistoryActivity.this, ProfileActivity.class));
                finish();
            });
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

    private void updateEmptyState() {
        if (historyItems.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvHistory.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
    }

    private void initSaran() {
        saran = new HashMap<>();
        saran.put("Blast",
                "- Gunakan varietas padi tahan blast\n" +
                        "- Atur jarak tanam agar tidak terlalu rapat\n" +
                        "- Kurangi pupuk nitrogen berlebih\n" +
                        "- Gunakan fungisida sesuai anjuran");

        saran.put("Blight",
                "- Gunakan benih sehat dan tahan penyakit\n" +
                        "- Jaga kebersihan lahan\n" +
                        "- Hindari pengairan berlebihan\n" +
                        "- Gunakan bakterisida jika diperlukan");

        saran.put("Normal",
                "- Lanjutkan perawatan rutin\n" +
                        "- Jaga pemupukan seimbang\n" +
                        "- Pantau kondisi daun secara berkala");

        saran.put("Tungro",
                "- Cabut dan musnahkan tanaman terinfeksi\n" +
                        "- Kendalikan vektor wereng hijau\n" +
                        "- Gunakan varietas tahan tungro\n" +
                        "- Lakukan rotasi tanaman");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(HistoryItem item) {
        // Load gambar dari imagePath
        Bitmap imageBitmap = null;
        if (item.imagePath != null && !item.imagePath.isEmpty()) {
            try {
                Uri imageUri = Uri.parse(item.imagePath);
                // Coba load dengan ContentResolver
                try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                    if (inputStream != null) {
                        imageBitmap = BitmapFactory.decodeStream(inputStream);
                    }
                } catch (Exception e) {
                    // Jika gagal, coba load dengan MediaStore
                    try {
                        imageBitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Ambil saran dari map
        String saranText = saran.get(item.penyakitName);
        if (saranText == null || saranText.isEmpty()) {
            saranText = "Tidak ada saran tersedia untuk " + item.penyakitName;
        }

        // Tampilkan bottom sheet dengan gambar dan saran
        ResultSheetFragment bottomSheet = ResultSheetFragment.newInstance(
                imageBitmap, 
                item.penyakitName, 
                item.confidence, 
                saranText,
                false // No warning for historical data
        );
        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }

    @Override
    public void onDeleteClick(HistoryItem item, int position) {
        showConfirmDeleteDialog(item, position);
    }

    private void showConfirmDeleteDialog(HistoryItem item, int position) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_delete);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(true);

        dialog.setOnCancelListener(d -> adapter.notifyItemChanged(position));

        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnDeleteConfirm = dialog.findViewById(R.id.btnDeleteConfirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDeleteConfirm.setOnClickListener(v -> {
            // Hapus dari database dan dari list
            dbHelper.deleteHistory(item.id);
            adapter.removeItem(position);
            // Update empty state setelah item dihapus
            updateEmptyState();
            dialog.dismiss();
            Toast.makeText(this, "Riwayat dihapus", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

        private final Drawable deleteIcon;
        private final ColorDrawable background;

        SwipeToDeleteCallback() {
            super(0, ItemTouchHelper.LEFT);
            deleteIcon = ContextCompat.getDrawable(HistoryActivity.this, android.R.drawable.ic_menu_delete);
            background = new ColorDrawable(Color.RED);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false; // Tidak menangani gestur seret
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            HistoryItem item = adapter.getItem(position);
            onDeleteClick(item, position);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            View itemView = viewHolder.itemView;
            int backgroundCornerOffset = 20; // Untuk menangani sudut CardView

            if (dX < 0) { // Hanya saat menggeser ke kiri
                background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                        itemView.getTop(), itemView.getRight(), itemView.getBottom());
            } else { // Tidak digeser
                background.setBounds(0, 0, 0, 0);
            }
            background.draw(c);

            int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

            if (dX < 0) { // Geser ke kiri
                int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                int iconRight = itemView.getRight() - iconMargin;
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            } else { // Tidak digeser
                deleteIcon.setBounds(0, 0, 0, 0);
            }
            deleteIcon.draw(c);
        }
    }
}
