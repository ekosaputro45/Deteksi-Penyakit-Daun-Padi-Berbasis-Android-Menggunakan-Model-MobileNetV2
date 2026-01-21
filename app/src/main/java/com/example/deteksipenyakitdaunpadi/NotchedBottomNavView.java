package com.example.deteksipenyakitdaunpadi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class NotchedBottomNavView extends View {

    private Paint paint;
    private Path path;
    private float notchRadius = 30f; // Slightly larger than button radius (28dp), ~30-32px
    private float notchCenterX;
    private float cornerRadius = 16f; // Top corner radius

    public NotchedBottomNavView(Context context) {
        super(context);
        init();
    }

    public NotchedBottomNavView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NotchedBottomNavView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFFFFFFFF); // White
        paint.setStyle(Paint.Style.FILL);
        path = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        notchCenterX = w / 2f;
        createPath();
    }

    private void createPath() {
        path.reset();
        float width = getWidth();
        float height = getHeight();

        // Convert dp to pixels
        float density = getResources().getDisplayMetrics().density;
        float cornerRadiusPx = cornerRadius * density;
        float notchRadiusPx = notchRadius * density;
        
        // Notch depth: ~40-50% of navbar height (76dp), so ~30-38dp
        float notchDepth = height * 0.45f; // 45% of navbar height

        // Start from bottom-left
        path.moveTo(0, height);

        // Left edge to top-left corner
        path.lineTo(0, cornerRadiusPx);

        // Top-left rounded corner
        path.quadTo(0, 0, cornerRadiusPx, 0);

        // Top edge to notch start (left edge of notch)
        path.lineTo(notchCenterX - notchRadiusPx, 0);

        // Concave notch (smooth semicircle cutout going downward)
        // The notch should dip down from the top edge
        // Define bounding rectangle for semicircle: center at (notchCenterX, 0), radius = notchRadiusPx
        // To create downward arc, the bounding rect's top should be -notchRadiusPx
        RectF notchRect = new RectF(
            notchCenterX - notchRadiusPx, -notchRadiusPx,  // left, top (extends above top edge)
            notchCenterX + notchRadiusPx, notchRadiusPx    // right, bottom (extends below top edge)
        );
        
        // Draw a 180-degree arc going from left (180°) to right (0°)
        // Start at 180 degrees (left point), sweep -180 degrees (counterclockwise to right)
        // This creates a concave semicircle that dips down
        path.arcTo(notchRect, 180, -180, false);

        // Top edge from notch end to top-right
        path.lineTo(width - cornerRadiusPx, 0);

        // Top-right rounded corner
        path.quadTo(width, 0, width, cornerRadiusPx);

        // Right edge
        path.lineTo(width, height);

        // Bottom edge back to start
        path.lineTo(0, height);

        path.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }
}

