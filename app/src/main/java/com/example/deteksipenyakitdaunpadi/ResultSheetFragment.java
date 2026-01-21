package com.example.deteksipenyakitdaunpadi;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.tabs.TabLayout;

import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

public class ResultSheetFragment extends BottomSheetDialogFragment {

    private Bitmap resultBitmap;
    private String penyakitName;
    private String confidence;
    private String saran;
    private boolean showWarning; // New field for warning flag
    private Map<String, String> deskripsi = new HashMap<>();

    public static ResultSheetFragment newInstance(Bitmap bitmap, String penyakit, String confidence, String saran, boolean showWarning) {
        ResultSheetFragment fragment = new ResultSheetFragment();
        Bundle args = new Bundle();
        args.putParcelable("bitmap", bitmap);
        args.putString("penyakit", penyakit);
        args.putString("confidence", confidence);
        args.putString("saran", saran);
        args.putBoolean("showWarning", showWarning); // Pass warning flag
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            resultBitmap = getArguments().getParcelable("bitmap");
            penyakitName = getArguments().getString("penyakit");
            confidence = getArguments().getString("confidence");
            saran = getArguments().getString("saran");
            showWarning = getArguments().getBoolean("showWarning", false); // Get warning flag
        }
        initDeskripsi();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result_sheet, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet == null) return;

                float radiusPx = dpToPx(24f);
                ShapeAppearanceModel model = new ShapeAppearanceModel.Builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, radiusPx)
                        .setTopRightCorner(CornerFamily.ROUNDED, radiusPx)
                        .build();

                MaterialShapeDrawable bg = new MaterialShapeDrawable(model);
                // Hard-coded sesuai desain: selalu putih (app juga sudah dipaksa Light).
                bg.setFillColor(ColorStateList.valueOf(Color.WHITE));

                bottomSheet.setBackground(bg);
            }
        });
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView ivResultImage = view.findViewById(R.id.ivResultImage);
        TextView tvPenyakitName = view.findViewById(R.id.tvPenyakitName);
        TextView tvConfidence = view.findViewById(R.id.tvConfidence);
        TextView tvSaranContent = view.findViewById(R.id.tvSaranContent);
        TextView tvDeskripsiContent = view.findViewById(R.id.tvDeskripsiContent);
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        LinearLayout llRekomendasiSection = view.findViewById(R.id.llRekomendasiSection);
        LinearLayout llDeskripsiSection = view.findViewById(R.id.llDeskripsiSection);

        if (resultBitmap != null) {
            ivResultImage.setImageBitmap(resultBitmap);
        } else {
            // Jika bitmap null, tampilkan placeholder
            ivResultImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        tvPenyakitName.setText(penyakitName != null ? penyakitName : "Unknown");
        tvConfidence.setText(confidence != null ? confidence : "");
        
        // Display warning if confidence is medium (60-75%)
        TextView tvWarningMessage = view.findViewById(R.id.tvWarningMessage);
        if (tvWarningMessage != null) {
            if (showWarning) {
                tvWarningMessage.setVisibility(View.VISIBLE);
                tvWarningMessage.setText("⚠️ Confidence " + confidence + " - Hasil mungkin kurang akurat. Untuk hasil lebih reliable, gunakan foto yang lebih jelas.");
            } else {
                tvWarningMessage.setVisibility(View.GONE);
            }
        }
        
        tvSaranContent.setText(saran != null ? saran : "Tidak ada saran tersedia");
        tvDeskripsiContent.setText(deskripsi.getOrDefault(penyakitName, "Deskripsi tidak tersedia."));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    llRekomendasiSection.setVisibility(View.VISIBLE);
                    llDeskripsiSection.setVisibility(View.GONE);
                } else {
                    llRekomendasiSection.setVisibility(View.GONE);
                    llDeskripsiSection.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    private void initDeskripsi() {
        deskripsi.put("Blast",
                "Penyakit Blast (Pyricularia oryzae) adalah salah satu penyakit paling merusak pada tanaman padi. " +
                        "Penyakit ini menyerang semua bagian tanaman di atas permukaan tanah, terutama daun dan leher malai.\n\n" +
                        "Gejala:\n" +
                        "• Bercak berbentuk belah ketupat berwarna coklat keabu-abuan dengan tepi coklat tua\n" +
                        "• Bercak dapat meluas dan menyatu, menyebabkan daun mengering\n" +
                        "• Pada leher malai, infeksi menyebabkan patah dan gabah hampa\n" +
                        "• Serangan berat dapat menyebabkan kehilangan hasil hingga 50-90%\n\n" +
                        "Penyebab:\n" +
                        "• Kelembaban tinggi (>90%)\n" +
                        "• Suhu 25-28°C\n" +
                        "• Pemupukan nitrogen berlebihan\n" +
                        "• Jarak tanam terlalu rapat");

        deskripsi.put("Blight",
                "Penyakit Blight atau Hawar Daun Bakteri (Xanthomonas oryzae) adalah penyakit bakteri yang menyerang tanaman padi.\n\n" +
                        "Gejala:\n" +
                        "• Garis-garis kuning kehijauan memanjang pada daun muda\n" +
                        "• Daun mengering dari ujung ke pangkal\n" +
                        "• Pada kondisi lanjut, daun berubah kuning kemudian coklat\n" +
                        "• Tetesan bakteri (eksudasi) terlihat di pagi hari\n" +
                        "• Serangan pada fase vegetatif menyebabkan anakan berkurang\n\n" +
                        "Penyebab:\n" +
                        "• Kelembaban tinggi dan curah hujan tinggi\n" +
                        "• Genangan air berlebihan\n" +
                        "• Luka pada daun akibat angin, serangga, atau aktivitas mekanis\n" +
                        "• Penggunaan benih terinfeksi\n\n" +
                        "Penyebaran:\n" +
                        "• Melalui air irigasi\n" +
                        "• Angin dan percikan air hujan\n" +
                        "• Alat pertanian yang terkontaminasi");

        deskripsi.put("Normal",
                "Daun padi Anda terlihat sehat dan tidak menunjukkan gejala penyakit!\n\n" +
                        "Karakteristik Daun Sehat:\n" +
                        "• Warna hijau segar merata\n" +
                        "• Tidak ada bercak atau garis abnormal\n" +
                        "• Permukaan daun halus dan utuh\n" +
                        "• Tumbuh tegak dan kuat\n" +
                        "• Tidak ada tanda kekuningan atau kekeringan\n\n" +
                        "Tips Menjaga Kesehatan Tanaman:\n" +
                        "• Lakukan pemupukan berimbang sesuai kebutuhan\n" +
                        "• Jaga kebersihan lahan dari gulma\n" +
                        "• Monitor kondisi tanaman secara rutin\n" +
                        "• Atur pengairan dengan baik\n" +
                        "• Lakukan rotasi tanaman untuk menjaga kesehatan tanah\n" +
                        "• Gunakan varietas unggul yang tahan penyakit");

        deskripsi.put("Tungro",
                "Penyakit Tungro adalah penyakit virus yang sangat berbahaya pada tanaman padi, disebabkan oleh kombinasi dua virus: " +
                        "Rice Tungro Spherical Virus (RTSV) dan Rice Tungro Bacilliform Virus (RTBV).\n\n" +
                        "Gejala:\n" +
                        "• Daun menguning mulai dari ujung daun muda\n" +
                        "• Pertumbuhan tanaman terhambat (kerdil)\n" +
                        "• Jumlah anakan berkurang drastis\n" +
                        "• Daun menjadi sempit dan pendek\n" +
                        "• Malai tidak keluar atau hanya sedikit\n" +
                        "• Gabah hampa atau berisi sebagian\n" +
                        "• Kehilangan hasil dapat mencapai 100% pada serangan berat\n\n" +
                        "Penyebab dan Penyebaran:\n" +
                        "• Ditularkan oleh wereng hijau (Nephotettix virescens)\n" +
                        "• Wereng menghisap cairan tanaman terinfeksi kemudian menularkan ke tanaman sehat\n" +
                        "• Virus dapat bertahan dalam tubuh wereng hingga 30 hari\n" +
                        "• Sumber infeksi dari tanaman sakit yang tidak dicabut\n\n" +
                        "Faktor Pendukung:\n" +
                        "• Populasi wereng hijau tinggi\n" +
                        "• Penanaman tidak serentak\n" +
                        "• Varietas rentan\n" +
                        "• Keberadaan tanaman padi sukarela (ratoon)");
    }
}
