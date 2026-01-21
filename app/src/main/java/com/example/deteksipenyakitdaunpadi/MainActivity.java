package com.example.deteksipenyakitdaunpadi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.graphics.Color;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final int PERMISSION_CAMERA_REQUEST_CODE = 1;
    private int imageSize = 224;
    private Map<String, String> saran = new HashMap<>();
    private DatabaseHelper dbHelper;
    private boolean suppressHomeHighlightOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

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

        initSaran();
        checkPermissions();
        setClickListeners();

        handleIntentActions(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentActions(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If we just navigated here to trigger AI/gallery, keep AI highlighted once.
        if (suppressHomeHighlightOnce) {
            suppressHomeHighlightOnce = false;
            return;
        }
        setActiveNavItem(R.id.llNavHome);
    }

    private void handleIntentActions(@Nullable Intent intent) {
        if (intent == null) return;

        if (intent.getBooleanExtra("openGallery", false)) {
            suppressHomeHighlightOnce = true;
            setActiveNavItem(R.id.llNavCosmetologist);
            // Prevent re-triggering when the activity resumes later
            intent.removeExtra("openGallery");
            openGallery();
        } else {
            setActiveNavItem(R.id.llNavHome);
        }
    }

    private void initSaran() {
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

    private void setClickListeners() {
        FloatingActionButton fabCenterAction = findViewById(R.id.fabCenterAction);
        CardView cvProductScanner = findViewById(R.id.cvProductScanner);

        // Listener untuk membuka galeri
        cvProductScanner.setOnClickListener(v -> openGallery());

        // Listener untuk membuka kamera
        fabCenterAction.setOnClickListener(v -> openCamera());

        // Set up bottom navigation click listeners
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        LinearLayout llNavHome = findViewById(R.id.llNavHome);
        LinearLayout llNavCosmetologist = findViewById(R.id.llNavCosmetologist);
        LinearLayout llNavHistory = findViewById(R.id.llNavHistory);
        LinearLayout llNavProfile = findViewById(R.id.llNavProfile);

        llNavHome.setOnClickListener(v -> {
            setActiveNavItem(R.id.llNavHome);
            // Already on home, do nothing
        });

        llNavCosmetologist.setOnClickListener(v -> {
            setActiveNavItem(R.id.llNavCosmetologist);
            startActivity(new Intent(MainActivity.this, NewsActivity.class));
        });

        llNavHistory.setOnClickListener(v -> {
            setActiveNavItem(R.id.llNavHistory);
            startActivity(new Intent(MainActivity.this, HistoryActivity.class));
        });

        llNavProfile.setOnClickListener(v -> {
            setActiveNavItem(R.id.llNavProfile);
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });
    }

    private void setActiveNavItem(int activeItemId) {
        // Reset all items to inactive
        setNavItemState(R.id.llNavHome, false);
        setNavItemState(R.id.llNavCosmetologist, false);
        setNavItemState(R.id.llNavHistory, false);
        setNavItemState(R.id.llNavProfile, false);

        // Set active item
        setNavItemState(activeItemId, true);
    }

    private void setNavItemState(int itemId, boolean isActive) {
        LinearLayout item = findViewById(itemId);
        if (item == null) return;

        ImageView icon = null;
        TextView text = null;

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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 20); // Request code 20 untuk galeri
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 10); // Request code 10 untuk kamera
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != AppCompatActivity.RESULT_OK || data == null) return;

        Uri imageUri = null;
        Bitmap image = null;

        if (requestCode == 20) { // Gallery
            imageUri = data.getData();
            try {
                image = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == 10) { // Camera
            image = (Bitmap) data.getExtras().get("data");
            if (image != null) {
                try {
                    imageUri = saveImageToInternalStorage(image);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (image != null) {
            String imagePath = (imageUri != null) ? imageUri.toString() : "";
            boolean isFromCamera = (requestCode == 10); // true if from camera
            classifyImage(image, imagePath, isFromCamera);
    }
    }

    private void classifyImage(Bitmap image, String imagePath, boolean isFromCamera) {
        Interpreter tflite = null;

        try {
            tflite = new Interpreter(loadModelFile());
            String[] labels = loadLabels();

            // Resize hanya untuk CNN
            Bitmap scaledImage = Bitmap.createScaledBitmap(image, imageSize, imageSize, true);

            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            inputBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            scaledImage.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize);

            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++];
                    inputBuffer.putFloat((((val >> 16) & 0xFF) - 127.5f) / 127.5f);
                    inputBuffer.putFloat((((val >> 8) & 0xFF) - 127.5f) / 127.5f);
                    inputBuffer.putFloat(((val & 0xFF) - 127.5f) / 127.5f);
                }
            }

            float[][] output = new float[1][labels.length];
            inputBuffer.rewind();
            tflite.run(inputBuffer, output);

            int maxIndex = 0;
            float maxConfidence = output[0][0];
            for (int i = 1; i < labels.length; i++) {
                if (output[0][i] > maxConfidence) {
                    maxConfidence = output[0][i];
                    maxIndex = i;
                }
            }

            // 🔒 FILTER 2: CONFIDENCE TIER SYSTEM
            // <60% = HARD REJECT (definitely not rice leaf)
            // 60-75% = ACCEPT WITH WARNING (might be valid, might be random)
            // >75% = ACCEPT NORMAL (confident it's rice leaf)
            if (maxConfidence < 0.60f) {
                showNotLeafDialog("confidence");
                return;
            }

            // 🔒 FILTER 3: AMBIGUITY
            float first = 0f, second = 0f;
            for (float v : output[0]) {
                if (v > first) {
                    second = first;
                    first = v;
                } else if (v > second) {
                    second = v;
                }
            }

            // Gap adjusted to 0.12 based on dataset testing
            if ((first - second) < 0.12f) {
                showNotLeafDialog("ambiguity");
                return;
            }

            // 🔒 FILTER 4: BLANK IMAGE DETECTION
            // Reject images that are mostly white/blank
            if (isBlankImage(image)) {
                showNotLeafDialog("blank");
                return;
            }

            // 🔒 FILTER 5: LEAF CONTOUR DETECTION
            // Analyze edge patterns and shape to validate leaf characteristics
            if (!hasLeafContour(scaledImage)) {
                showNotLeafDialog("contour");
                return;
            }

            // 🔒 FILTER 4: ASPECT RATIO VALIDATION
            // DISABLED: Too strict for dataset with backgrounds
            /*
            if (!hasValidAspectRatio(scaledImage)) {
                showNotLeafDialog("shape");
                return;
            }
            */

            // 🔒 FILTER 5: COLOR HISTOGRAM VALIDATION
            // DISABLED: Too strict for dataset with varied backgrounds/lighting
            /*
            if (!hasNaturalLeafColors(image)) {
                showNotLeafDialog("color");
                return;
            }
            */

            // 🔒 FILTER 6: TEXTURE/EDGE VALIDATION
            // DISABLED: Not effective for distinguishing rice leaves from random objects
            // The model needs to be retrained with "Not Rice Leaf" class for better filtering
            /*
            if (!hasLeafCharacteristics(scaledImage)) {
                showNotLeafDialog("texture");
                return;
            }
            */

            String result = labels[maxIndex];
            String confidenceStr = String.format("%.1f%%", maxConfidence * 100);
            String currentDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(new Date());

            String saranRekomendasi = saran.getOrDefault(
                    result, "Belum ada saran untuk penyakit ini");

            // Check if confidence is in warning range (60-75%)
            boolean showWarning = (maxConfidence >= 0.60f && maxConfidence < 0.75f);

            dbHelper.addHistory(
                    new HistoryItem(null, result, currentDate, confidenceStr, imagePath)
            );

            ResultSheetFragment.newInstance(
                    image, result, confidenceStr, saranRekomendasi, showWarning
            ).show(getSupportFragmentManager(), null);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tflite != null) tflite.close();
        }
    }


    /**
     * Detects if image is mostly uniform/monotone
     * Prevents false positives on empty, white, black, or uniform colored images
     */
    private boolean isBlankImage(Bitmap image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            
            // Sample pixels for efficiency
            int sampleStep = 5;
            int whitePixels = 0;
            int blackPixels = 0;
            int totalSampled = 0;
            
            // For variance calculation
            long sumR = 0, sumG = 0, sumB = 0;
            
            for (int y = 0; y < height; y += sampleStep) {
                for (int x = 0; x < width; x += sampleStep) {
                    int pixel = image.getPixel(x, y);
                    
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    
                    totalSampled++;
                    sumR += r;
                    sumG += g;
                    sumB += b;
                    
                    int avg = (r + g + b) / 3;
                    
                    // Check if pixel is white (very bright)
                    if (avg > 230) {
                        whitePixels++;
                    }
                    
                    // Check if pixel is black (very dark)
                    if (avg < 25) {
                        blackPixels++;
                    }
                }
            }
            
            if (totalSampled < 10) {
                return false;
            }
            
            // Check 1: Dominated by white (>80%)
            float whitePercentage = (float) whitePixels / totalSampled;
            if (whitePercentage > 0.80f) {
                return true; // Mostly white
            }
            
            // Check 2: Dominated by black (>80%)
            float blackPercentage = (float) blackPixels / totalSampled;
            if (blackPercentage > 0.80f) {
                return true; // Mostly black
            }
            
            // Check 3: Very low color variance (uniform color like laptop screen)
            // Calculate variance for each channel
            int avgR = (int) (sumR / totalSampled);
            int avgG = (int) (sumG / totalSampled);
            int avgB = (int) (sumB / totalSampled);
            
            long varianceR = 0, varianceG = 0, varianceB = 0;
            
            for (int y = 0; y < height; y += sampleStep) {
                for (int x = 0; x < width; x += sampleStep) {
                    int pixel = image.getPixel(x, y);
                    
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    
                    varianceR += (r - avgR) * (r - avgR);
                    varianceG += (g - avgG) * (g - avgG);
                    varianceB += (b - avgB) * (b - avgB);
                }
            }
            
            varianceR /= totalSampled;
            varianceG /= totalSampled;
            varianceB /= totalSampled;
            
            double totalVariance = Math.sqrt(varianceR + varianceG + varianceB);
            
            // If variance is very low (<15), it's a uniform color image
            // Examples: laptop screen, solid color wall, etc.
            if (totalVariance < 15.0) {
                return true; // Too uniform
            }
            
            return false;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Detects if image has leaf-like contour/edge characteristics
     * Analyzes edge patterns to distinguish rice leaves from random objects
     */
    private boolean hasLeafContour(Bitmap image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            
            // Convert to grayscale
            int[][] grayscale = new int[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = image.getPixel(x, y);
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    grayscale[y][x] = (r + g + b) / 3;
                }
            }
            
            // Simple edge detection (Sobel-like)
            int edgeCount = 0;
            int totalPixels = 0;
            int minX = width, maxX = 0, minY = height, maxY = 0;
            
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    // Sobel operators
                    int gx = -grayscale[y-1][x-1] + grayscale[y-1][x+1]
                           - 2*grayscale[y][x-1] + 2*grayscale[y][x+1]
                           - grayscale[y+1][x-1] + grayscale[y+1][x+1];
                    
                    int gy = -grayscale[y-1][x-1] - 2*grayscale[y-1][x]
                           - grayscale[y-1][x+1] + grayscale[y+1][x-1]
                           + 2*grayscale[y+1][x] + grayscale[y+1][x+1];
                    
                    int magnitude = (int) Math.sqrt(gx*gx + gy*gy);
                    
                    totalPixels++;
                    
                    // Threshold for edge detection
                    if (magnitude > 50) {
                        edgeCount++;
                        // Track bounding box of edges
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }
            
            // Calculate edge density
            float edgeDensity = (float) edgeCount / totalPixels;
            
            // Calculate bounding box aspect ratio
            int bboxWidth = maxX - minX;
            int bboxHeight = maxY - minY;
            
            if (bboxWidth == 0 || bboxHeight == 0) {
                return false; // No clear object detected
            }
            
            float aspectRatio = (float) bboxWidth / bboxHeight;
            
            // Validation criteria for rice leaf (LOOSENED):
            // 1. Edge density should be moderate (1-30%) - EXPANDED RANGE
            //    - Too low (<1%): completely uniform/blank
            //    - Too high (>30%): very noisy/complex patterns
            boolean hasModerateEdges = (edgeDensity >= 0.01f && edgeDensity <= 0.30f);
            
            // 2. Aspect ratio - MORE LENIENT
            //    Accept square IF edge density is reasonable
            //    Only hard reject if BOTH square AND low edges
            boolean isElongated = (aspectRatio < 0.8f || aspectRatio > 1.2f);
            boolean isSquareButHasEdges = (aspectRatio >= 0.8f && aspectRatio <= 1.2f && edgeDensity > 0.05f);
            boolean hasValidShape = isElongated || isSquareButHasEdges;
            
            // 3. Bounding box - VERY LENIENT (1-95%)
            float bboxArea = (float)(bboxWidth * bboxHeight) / (width * height);
            boolean hasReasonableSize = (bboxArea >= 0.01f && bboxArea <= 0.95f);
            
            return hasModerateEdges && hasValidShape && hasReasonableSize;
            
        } catch (Exception e) {
            e.printStackTrace();
            // On error, accept (be lenient for valid dataset)
            return true;
        }
    }

    /**
     * Validates aspect ratio - rice leaves are elongated, not square
     * REFINED: Reject perfect squares (tables, boxes) but accept elongated leaves with background
     */
    private boolean hasValidAspectRatio(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        float aspectRatio = (float) width / height;
        
        // CRITICAL: Reject PERFECT squares (0.95-1.05)
        // This filters out tables, boxes, and other square objects
        if (aspectRatio >= 0.95f && aspectRatio <= 1.05f) {
            return false; // Perfect square - NOT rice leaf
        }
        
        // Accept elongated shapes with reasonable tolerance
        // Rice leaves are elongated, but dataset might have background
        // Range 0.3-3.0 covers:
        // - Vertical leaf with background (ratio ~0.4-0.7)
        // - Horizontal leaf with background (ratio ~1.4-2.5)
        // - Pure leaf without much background (ratio ~0.2-4.0)
        if (aspectRatio >= 0.3f && aspectRatio <= 3.0f) {
            return true; // Reasonable elongated shape
        }
        
        // Also accept very elongated shapes (pure leaf, no background)
        // But not extreme ratios that are likely errors
        if (aspectRatio >= 0.1f && aspectRatio <= 10.0f) {
            return true; // Very elongated but still valid
        }
        
        // Reject extreme ratios
        return false;
    }

    /**
     * Validates color distribution - checks if image has natural leaf colors
     * Works for green (healthy), yellow-brown (Tungro), and diseased leaves
     * Made more lenient to accommodate various lighting and backgrounds
     */
    private boolean hasNaturalLeafColors(Bitmap image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            
            // Sample pixels (don't need to check every pixel)
            int sampleStep = 5;
            int greenish = 0;
            int brownish = 0;
            int yellowish = 0;
            int whitish = 0; // background
            int totalSampled = 0;
            
            for (int y = 0; y < height; y += sampleStep) {
                for (int x = 0; x < width; x += sampleStep) {
                    int pixel = image.getPixel(x, y);
                    
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    
                    // Convert to HSV for better color analysis
                    float[] hsv = new float[3];
                    android.graphics.Color.RGBToHSV(r, g, b, hsv);
                    
                    float hue = hsv[0];
                    float sat = hsv[1];
                    float val = hsv[2];
                    
                    totalSampled++;
                    
                    // Skip very dark pixels only
                    if (val < 0.1f) {
                        continue;
                    }
                    
                    // White/bright background (common in dataset)
                    if (val > 0.85f && sat < 0.2f) {
                        whitish++;
                        continue;
                    }
                    
                    // LOOSENED: More inclusive color ranges
                    // Green: hue 50-180 (expanded)
                    // Yellow-brown: hue 15-60 (expanded)
                    // Brown: hue 0-20
                    if (hue >= 50 && hue <= 180 && sat > 0.1f) {
                        greenish++;
                    } else if (hue >= 15 && hue <= 65 && sat > 0.1f) {
                        yellowish++;
                    } else if ((hue >= 0 && hue <= 20) || (hue >= 340 && hue <= 360)) {
                        brownish++;
                    }
                }
            }
            
            if (totalSampled < 10) {
                // Not enough samples, accept it (be lenient)
                return true;
            }
            
            // Calculate percentages
            int naturalColors = greenish + yellowish + brownish;
            float naturalPercentage = (float) naturalColors / totalSampled;
            
            // LOWERED: At least 20% of pixels should be natural leaf colors (was 30%)
            // This accommodates dataset with white backgrounds
            return naturalPercentage >= 0.20f;
            
        } catch (Exception e) {
            e.printStackTrace();
            // If analysis fails, accept it (be lenient for valid dataset)
            return true;
        }
    }

    /**
     * Validates if the image has leaf-like characteristics using texture and edge analysis
     * Works for all leaf colors (green, yellow, brown)
     */
    private boolean hasLeafCharacteristics(Bitmap image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();

            // Convert to grayscale and analyze texture/edges
            int[] pixels = new int[width * height];
            image.getPixels(pixels, 0, width, 0, 0, width, height);

            int edgeCount = 0;
            double totalBrightness = 0;
            int[] brightnessHisto = new int[256];

            // Simple edge detection using brightness differences
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    int idx = y * width + x;
                    int pixel = pixels[idx];

                    // Convert to grayscale
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    int gray = (r + g + b) / 3;

                    totalBrightness += gray;
                    brightnessHisto[gray]++;

                    // Check edges (simple gradient)
                    int rightPixel = pixels[idx + 1];
                    int bottomPixel = pixels[idx + width];

                    int rightGray = (((rightPixel >> 16) & 0xFF) + ((rightPixel >> 8) & 0xFF) + (rightPixel & 0xFF)) / 3;
                    int bottomGray = (((bottomPixel >> 16) & 0xFF) + ((bottomPixel >> 8) & 0xFF) + (bottomPixel & 0xFF)) / 3;

                    int gradientX = Math.abs(gray - rightGray);
                    int gradientY = Math.abs(gray - bottomGray);

                    if (gradientX > 25 || gradientY > 25) {
                        edgeCount++;
                    }
                }
            }

            // Calculate entropy (measure of texture complexity)
            double entropy = 0;
            int totalPixels = width * height;
            for (int count : brightnessHisto) {
                if (count > 0) {
                    double prob = (double) count / totalPixels;
                    entropy -= prob * (Math.log(prob) / Math.log(2));
                }
            }

            // Validation criteria
            double edgePercentage = (double) edgeCount / totalPixels;
            double avgBrightness = totalBrightness / totalPixels;

            // Reject if:
            // - Too few edges (likely uniform/solid color)
            // - Too low entropy (not enough texture variation)
            // - Extreme brightness (completely white/black)
            boolean hasEnoughEdges = edgePercentage > 0.05; // At least 5% edges
            boolean hasGoodEntropy = entropy > 3.5; // Reasonable texture complexity
            boolean hasValidBrightness = avgBrightness > 20 && avgBrightness < 235;

            return hasEnoughEdges && hasGoodEntropy && hasValidBrightness;

        } catch (Exception e) {
            e.printStackTrace();
            // If analysis fails, be conservative and reject
            return false;
        }
    }

    private void showNotLeafDialog(String reason) {
        String message;
        switch (reason) {
            case "confidence":
                message = "Model tidak yakin dengan gambar ini. Kemungkinan bukan daun padi.\n\n" +
                        "Tips:\n" +
                        "• Pastikan foto fokus pada 1 helai daun padi\n" +
                        "• Gunakan pencahayaan yang cukup\n" +
                        "• Hindari background yang ramai";
                break;
            case "ambiguity":
                message = "Gambar terlalu ambigu untuk diklasifikasikan.\n\n" +
                        "Tips:\n" +
                        "• Ambil foto daun yang lebih jelas\n" +
                        "• Pastikan daun terlihat utuh\n" +
                        "• Hindari blur atau pencahayaan buruk";
                break;
            case "texture":
                message = "Gambar tidak menunjukkan karakteristik daun padi.\n\n" +
                        "Tips:\n" +
                        "• Upload foto daun padi asli (bukan ilustrasi)\n" +
                        "• Pastikan ada detail tekstur daun\n" +
                        "• Hindari gambar yang terlalu gelap atau terang";
                break;
            case "shape":
                message = "Bentuk gambar tidak sesuai dengan daun padi.\n\n" +
                        "Tips:\n" +
                        "• Daun padi berbentuk memanjang (elongated)\n" +
                        "• Foto 1 helai daun padi secara vertikal atau horizontal\n" +
                        "• Hindari foto yang terlalu square (kotak)";
                break;
            case "color":
                message = "Warna gambar tidak menunjukkan karakteristik daun.\n\n" +
                        "Tips:\n" +
                        "• Pastikan foto daun padi dengan warna natural\n" +
                        "• Daun bisa hijau (sehat), kuning-coklat (sakit)\n" +
                        "• Hindari background dengan warna dominan";
                break;
            case "blank":
                message = "Gambar terlalu kosong atau warna uniform.\n\n" +
                        "Tips:\n" +
                        "• Upload foto daun padi yang jelas\n" +
                        "• Hindari foto layar laptop/HP\n" +
                        "• Hindari background putih/hitam kosong\n" +
                        "• Pastikan ada detail daun terlihat";
                break;
            case "contour":
                message = "Bentuk objek tidak sesuai dengan daun padi.\n\n" +
                        "Tips:\n" +
                        "• Daun padi memiliki bentuk memanjang dengan tepi halus\n" +
                        "• Gunakan background yang kontras (putih/biru)\n" +
                        "• Pastikan daun terlihat jelas tanpa banyak noise\n" +
                        "• Hindari objek dengan sudut tajam (meja, kotak)";
                break;
            default:
                message = "Maaf, gambar yang Anda upload tidak valid.\n\n" +
                        "Silakan unggah foto daun padi yang jelas.";
        }

        new AlertDialog.Builder(this)
                .setTitle("Gambar Tidak Valid")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model_padi.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private String[] loadLabels() throws IOException {
        InputStream is = getAssets().open("labels.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        ArrayList<String> labelList = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList.toArray(new String[0]);
    }

    private Uri saveImageToInternalStorage(Bitmap bitmap) throws IOException {
        File imagesDir = new File(getFilesDir(), "images");
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }

        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
        File imageFile = new File(imagesDir, fileName);

        FileOutputStream fos = new FileOutputStream(imageFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        fos.flush();
        fos.close();

        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }
}
