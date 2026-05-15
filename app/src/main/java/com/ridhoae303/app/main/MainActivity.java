// Created by ridhoae303 — https://github.com/ridhoae303

package com.ridhoae303.app.main;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {

    private FrameLayout backgroundContainer;
    private ImageView blurBackgroundImage;
    private ImageView mainImage;
    private GradientOverlayView gradientOverlay;
    private RainbowDrawingView rainbowDrawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        try {
            FrameLayout rootLayout = new FrameLayout(this);
            setupBackground(rootLayout);
            setupGradientOverlay(rootLayout);
            setupRainbowDrawing(rootLayout);
            setContentView(rootLayout);

            checkSignatureAndLaunch();
        } catch (Exception e) {
            showToast("Error initializing application");
            finish();
        }
    }

    private void setupBackground(FrameLayout rootLayout) {
        backgroundContainer = new FrameLayout(this);
        backgroundContainer.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        blurBackgroundImage = new ImageView(this);
        blurBackgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        blurBackgroundImage.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        mainImage = new ImageView(this);
        mainImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mainImage.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        try {
            InputStream is = getAssets().open("ridhoae303/assets/takane.jpg");
            Bitmap original = BitmapFactory.decodeStream(is);
            is.close();

            Bitmap blurred = createBlurredBitmap(original);
            blurBackgroundImage.setImageBitmap(blurred);
            mainImage.setImageBitmap(original);
        } catch (IOException e) {
            blurBackgroundImage.setBackgroundColor(Color.BLACK);
            mainImage.setBackgroundColor(Color.BLACK);
        }

        backgroundContainer.addView(blurBackgroundImage);
        backgroundContainer.addView(mainImage);
        rootLayout.addView(backgroundContainer);

        backgroundContainer.post(new Runnable() {
            @Override
            public void run() {
                mainImage.setPivotX(mainImage.getWidth() / 2f);
                mainImage.setPivotY(mainImage.getHeight() / 2f);
                mainImage.setScaleX(1.08f);
                mainImage.setScaleY(1.08f);
            }
        });
    }

    private Bitmap createBlurredBitmap(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        int downscale = 4;
        Bitmap small = Bitmap.createScaledBitmap(src, width / downscale, height / downscale, true);
        Bitmap blurred = Bitmap.createScaledBitmap(small, width, height, true);
        small.recycle();
        return blurred;
    }

    private void setupGradientOverlay(FrameLayout rootLayout) {
        gradientOverlay = new GradientOverlayView(this);
        gradientOverlay.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        rootLayout.addView(gradientOverlay);
    }

    private void setupRainbowDrawing(FrameLayout rootLayout) {
        rainbowDrawingView = new RainbowDrawingView(this);
        rainbowDrawingView.setBackgroundContainer(backgroundContainer);
        rootLayout.addView(rainbowDrawingView);
    }

    private void checkSignatureAndLaunch() {
        if (!isSignatureValid()) {
            showToast("Invalid signature!");
            finish();
        }
    }

    private class GradientOverlayView extends View {
        private Paint gradientPaint;
        private int colorStart = Color.argb(40, 255, 105, 180);
        private int colorEnd = Color.argb(40, 0, 255, 255);
        private long startTime;
        private final float DURATION = 20000f;

        public GradientOverlayView(Context context) {
            super(context);
            gradientPaint = new Paint();
            gradientPaint.setStyle(Paint.Style.FILL);
            startTime = System.currentTimeMillis();

            final Runnable updater = new Runnable() {
                @Override
                public void run() {
                    invalidate();
                    postDelayed(this, 50);
                }
            };
            postDelayed(updater, 50);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float progress = ((System.currentTimeMillis() - startTime) % DURATION) / DURATION;
            float factor = (float) Math.sin(progress * Math.PI * 2) * 0.5f + 0.5f;

            int r = (int) (Color.red(colorStart) * (1 - factor) + Color.red(colorEnd) * factor);
            int g = (int) (Color.green(colorStart) * (1 - factor) + Color.green(colorEnd) * factor);
            int b = (int) (Color.blue(colorStart) * (1 - factor) + Color.blue(colorEnd) * factor);
            int a = (int) (Color.alpha(colorStart) * (1 - factor) + Color.alpha(colorEnd) * factor);

            int gradientColorStart = Color.argb(a, r, g, b);
            int gradientColorEnd = Color.argb(a / 2, 255 - r, 255 - g, 255 - b);

            Shader shader = new LinearGradient(
                0, 0, getWidth(), getHeight(),
                gradientColorStart, gradientColorEnd,
                Shader.TileMode.CLAMP
            );
            gradientPaint.setShader(shader);
            canvas.drawRect(0, 0, getWidth(), getHeight(), gradientPaint);

            Paint vignettePaint = new Paint();
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            float radius = Math.max(getWidth(), getHeight()) * 0.7f;
            int vignetteColor = Color.argb(50, 0, 0, 0);
            int transparent = Color.argb(0, 0, 0, 0);
            RadialGradient vignette = new RadialGradient(
                centerX, centerY, radius,
                transparent, vignetteColor,
                Shader.TileMode.CLAMP
            );
            vignettePaint.setShader(vignette);
            canvas.drawRect(0, 0, getWidth(), getHeight(), vignettePaint);
        }
    }

    private class RainbowDrawingView extends View {
        private  class TouchTrail {
            Path path;
            Paint paint;
            int colorIndex;
            long lastColorChange;
            ArrayList<SparkleParticle> sparkleParticles = new ArrayList<>();
            ArrayList<SmokeParticle> smokeParticles = new ArrayList<>();
            ArrayList<GlowParticle> glowParticles = new ArrayList<>();
            ArrayList<SpectralParticle> spectralParticles = new ArrayList<>();
            float lastX, lastY;
            boolean isActive = true;
            int fadeAlpha = 255;
            int pointerId;
            float strokeWidth;
            long creationTime;
            float colorTransitionProgress = 0f;
            int previousColorIndex;
            boolean isColorTransitioning = false;
        }

        private  class SmokeParticle {
            float x, y, size;
            int alpha, color;
            float dx, dy;
            long creationTime;
        }

        private  class SparkleParticle {
            float x, y, size;
            int alpha, color;
            float speed, angle;
            long creationTime;
        }

        private  class GlowParticle {
            float x, y, size;
            int alpha, color;
            long creationTime;
            float lifeTime;
        }

        private  class SpectralParticle {
            float x, y, size;
            int alpha, color;
            float speed, angle;
            long creationTime;
            float lifeTime;
        }

        private  class Bubble {
            float x, y;
            float size;
            int color;
            int alpha;
            float speedY;
            float wobbleAmp;
            float wobbleFreq;
            float phase;
            long creationTime;
            float lifetime;
            boolean isIdle;
        }

        private  class FreeSparkle {
            float x, y, size;
            int alpha, color;
            float speed, angle;
            long creationTime;
            float lifeTime;
        }

        private  class FreeSmoke {
            float x, y, size;
            int alpha, color;
            float dx, dy;
            long creationTime;
            float lifeTime;
        }

        private  class Ripple {
            float x, y;
            float radius;
            float maxRadius;
            int alpha;
            long creationTime;
        }

        private final SparseArray<TouchTrail> activeTrails = new SparseArray<>();
        private final ArrayList<TouchTrail> fadingTrails = new ArrayList<>();
        private final ArrayList<Bubble> bubbles = new ArrayList<>();
        private final ArrayList<FreeSparkle> freeSparkles = new ArrayList<>();
        private final ArrayList<FreeSmoke> freeSmokes = new ArrayList<>();
        private final ArrayList<Ripple> ripples = new ArrayList<>();
        private final Random random = new Random();

        private final Paint smokePaint = new Paint();
        private final Paint sparklePaint = new Paint();
        private final Paint glowPaint = new Paint();
        private final Paint spectralPaint = new Paint();
        private final Paint bubblePaint = new Paint();
        private final Paint bubbleHighlightPaint = new Paint();
        private final Paint ripplePaint = new Paint();
        private final Paint trailGlowPaint = new Paint();

        private final int[] rainbowColors = {
            Color.rgb(255, 80, 80),   // Soft Red
            Color.rgb(255, 120, 90),  // Coral
            Color.rgb(255, 160, 80),  // Peach Orange
            Color.rgb(255, 200, 80),  // Warm Gold
            Color.rgb(255, 230, 120), // Soft Yellow
            Color.rgb(180, 255, 120), // Lime Pastel
            Color.rgb(120, 255, 140), // Mint Green
            Color.rgb(80, 255, 180),  // Aqua Green
            Color.rgb(80, 255, 220),  // Turquoise
            Color.rgb(80, 220, 255),  // Sky Cyan
            Color.rgb(80, 180, 255),  // Soft Sky Blue
            Color.rgb(100, 140, 255), // Dream Blue
            Color.rgb(120, 120, 255), // Periwinkle
            Color.rgb(150, 100, 255), // Soft Indigo
            Color.rgb(180, 90, 255),  // Violet
            Color.rgb(210, 90, 255),  // Purple Pink
            Color.rgb(255, 90, 255),  // Neon Pink
            Color.rgb(255, 100, 220), // Sakura Pink
            Color.rgb(255, 120, 180), // Rose Pink
            Color.rgb(255, 140, 160), // Soft Rose
            Color.rgb(255, 180, 200), // Pastel Pink
            Color.rgb(255, 220, 240), // Light Sakura
            Color.rgb(180, 240, 255), // Ice Blue
            Color.rgb(220, 180, 255), // Lavender
            Color.rgb(255, 255, 255)  // White Glow
        };

        private final int[] sparkleColors = {
            Color.WHITE,
            Color.rgb(255, 255, 200),
            Color.rgb(200, 255, 255),
            Color.rgb(255, 200, 255)
        };

        private long lastUpdateTime;
        private long lastBubbleSpawnTime = 0;
        private long lastTouchTime = 0;
        private static final int MAX_BUBBLES = 10;
        private static final int MAX_FREE_SPARKLES = 22;
        private static final int MAX_FREE_SMOKES = 15;
        private static final int MAX_RIPPLES = 5;
        private final float density;

        private FrameLayout backgroundContainer;
        private float parallaxTargetX = 0f, parallaxTargetY = 0f;
        private float parallaxCurrentX = 0f, parallaxCurrentY = 0f;
        private long driftStartTime;

        public RainbowDrawingView(Context context) {
            super(context);
            density = getResources().getDisplayMetrics().density;

            smokePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            smokePaint.setStyle(Paint.Style.FILL);
            sparklePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            sparklePaint.setStyle(Paint.Style.FILL);
            glowPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            glowPaint.setStyle(Paint.Style.FILL);
            spectralPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            spectralPaint.setStyle(Paint.Style.FILL);
            bubblePaint.setStyle(Paint.Style.FILL);
            bubblePaint.setAntiAlias(true);
            bubblePaint.setMaskFilter(new BlurMaskFilter(6, BlurMaskFilter.Blur.NORMAL));

            bubbleHighlightPaint.setColor(Color.WHITE);
            bubbleHighlightPaint.setStyle(Paint.Style.FILL);
            bubbleHighlightPaint.setAntiAlias(true);

            ripplePaint.setStyle(Paint.Style.STROKE);
            ripplePaint.setAntiAlias(true);
            ripplePaint.setStrokeWidth(2.0f);

            trailGlowPaint.setAntiAlias(true);
            trailGlowPaint.setStyle(Paint.Style.STROKE);
            trailGlowPaint.setStrokeJoin(Paint.Join.ROUND);
            trailGlowPaint.setStrokeCap(Paint.Cap.ROUND);
            trailGlowPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));

            lastUpdateTime = System.currentTimeMillis();
            lastBubbleSpawnTime = lastUpdateTime;
            driftStartTime = lastUpdateTime;
            lastTouchTime = lastUpdateTime;
        }

        public void setBackgroundContainer(FrameLayout container) {
            this.backgroundContainer = container;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            try {
                long currentTime = System.currentTimeMillis();
                float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
                lastUpdateTime = currentTime;

                if (backgroundContainer != null) {
                    float driftX = 0f;
                    float driftY = 0f;

                    if (currentTime - lastTouchTime > 2500) {
                        float driftTime = (currentTime - driftStartTime) / 1000f;
                        driftX = (float) Math.sin(driftTime * 0.3) * 2f;
                        driftY = (float) Math.cos(driftTime * 0.35) * 1.5f;
                    }

                    parallaxCurrentX += (parallaxTargetX - parallaxCurrentX) * Math.min(1f, deltaTime * 10f);
                    parallaxCurrentY += (parallaxTargetY - parallaxCurrentY) * Math.min(1f, deltaTime * 10f);

                    if (Math.abs(parallaxCurrentX) < 0.05f) parallaxCurrentX = 0f;
                    if (Math.abs(parallaxCurrentY) < 0.05f) parallaxCurrentY = 0f;

                    float totalX = driftX + parallaxCurrentX;
                    float totalY = driftY + parallaxCurrentY;
                    backgroundContainer.setTranslationX(totalX);
                    backgroundContainer.setTranslationY(totalY);
                }

                updateAndDrawRipples(canvas, deltaTime);
                updateAndDrawBubbles(canvas, deltaTime);
                updateAndDrawFreeSparkles(canvas, deltaTime);
                updateAndDrawFreeSmokes(canvas, deltaTime);
                drawFadingTrails(canvas);
                drawActiveTrails(canvas, deltaTime);
                drawSmokeEffects(canvas, deltaTime);
                drawSparkleEffects(canvas, deltaTime);
                drawGlowEffects(canvas, deltaTime);
                drawSpectralEffects(canvas, deltaTime);
                updateTrails(deltaTime);

                if (activeTrails.size() == 0 && currentTime - lastBubbleSpawnTime > 2500) {
                    spawnIdleBubble();
                    lastBubbleSpawnTime = currentTime;
                }

                postInvalidateOnAnimation();
            } catch (Exception e) {
            }
        }

        private void updateAndDrawRipples(Canvas canvas, float deltaTime) {
            long now = System.currentTimeMillis();
            Iterator<Ripple> iter = ripples.iterator();
            while (iter.hasNext()) {
                Ripple r = iter.next();
                float age = (now - r.creationTime) / 1000f;
                float progress = age / 0.8f;
                if (progress >= 1f) {
                    iter.remove();
                    continue;
                }
                r.radius = r.maxRadius * progress;
                r.alpha = (int) (80 * (1f - progress));
                ripplePaint.setColor(adjustColorAlpha(Color.WHITE, r.alpha));
                canvas.drawCircle(r.x, r.y, r.radius, ripplePaint);
            }
        }

        private void spawnRipple(float x, float y) {
            if (ripples.size() >= MAX_RIPPLES) return;
            Ripple r = new Ripple();
            r.x = x;
            r.y = y;
            r.maxRadius = 60f + random.nextFloat() * 30f;
            r.radius = 0f;
            r.alpha = 80;
            r.creationTime = System.currentTimeMillis();
            ripples.add(r);
        }

        private void spawnIdleBubble() {
            if (bubbles.size() >= MAX_BUBBLES) return;

            Bubble b = new Bubble();
            b.x = 50 + random.nextFloat() * (getWidth() - 100);
            b.y = getHeight() + 30;
            b.size = 12 + random.nextFloat() * 18;
            b.color = rainbowColors[random.nextInt(rainbowColors.length)];
            b.alpha = 70 + random.nextInt(60);
            b.speedY = -12 - random.nextFloat() * 15;
            b.wobbleAmp = 12 + random.nextFloat() * 20;
            b.wobbleFreq = 0.7f + random.nextFloat() * 1.1f;
            b.phase = random.nextFloat() * (float) Math.PI * 2;
            b.creationTime = System.currentTimeMillis();
            b.lifetime = 7 + random.nextFloat() * 6;
            b.isIdle = true;
            bubbles.add(b);
        }

        private void spawnTouchBubble(float x, float y) {
            if (bubbles.size() >= MAX_BUBBLES) return;

            Bubble b = new Bubble();
            b.x = x + random.nextFloat() * 30 - 15;
            b.y = y + random.nextFloat() * 30 - 15;
            b.size = 8 + random.nextFloat() * 15;
            b.color = rainbowColors[random.nextInt(rainbowColors.length)];
            b.alpha = 120 + random.nextInt(80);
            b.speedY = -20 - random.nextFloat() * 30;
            b.wobbleAmp = 10 + random.nextFloat() * 20;
            b.wobbleFreq = 2f + random.nextFloat() * 3f;
            b.phase = random.nextFloat() * (float) Math.PI * 2;
            b.creationTime = System.currentTimeMillis();
            b.lifetime = 2 + random.nextFloat() * 3;
            b.isIdle = false;
            bubbles.add(b);
        }

        private void popBubble(float x, float y, int color) {
            int count = 5 + random.nextInt(6);
            for (int i = 0; i < count; i++) {
                if (freeSparkles.size() < MAX_FREE_SPARKLES) {
                    FreeSparkle fs = new FreeSparkle();
                    fs.x = x;
                    fs.y = y;
                    fs.size = 2 + random.nextFloat() * 5;
                    fs.color = color;
                    fs.alpha = 200 + random.nextInt(55);
                    fs.speed = 2 + random.nextFloat() * 4;
                    fs.angle = random.nextFloat() * (float) Math.PI * 2;
                    fs.creationTime = System.currentTimeMillis();
                    fs.lifeTime = 0.5f + random.nextFloat() * 0.5f;
                    freeSparkles.add(fs);
                }
            }
            for (int i = 0; i < 3; i++) {
                if (freeSmokes.size() < MAX_FREE_SMOKES) {
                    FreeSmoke fs = new FreeSmoke();
                    fs.x = x;
                    fs.y = y;
                    fs.size = 3 + random.nextFloat() * 8;
                    fs.color = color;
                    fs.alpha = 150 + random.nextInt(80);
                    fs.dx = random.nextFloat() * 4 - 2;
                    fs.dy = random.nextFloat() * 4 - 2;
                    fs.creationTime = System.currentTimeMillis();
                    fs.lifeTime = 0.8f + random.nextFloat() * 0.7f;
                    freeSmokes.add(fs);
                }
            }
        }

        private void updateAndDrawBubbles(Canvas canvas, float deltaTime) {
            long now = System.currentTimeMillis();
            Iterator<Bubble> iter = bubbles.iterator();
            while (iter.hasNext()) {
                Bubble b = iter.next();
                float age = (now - b.creationTime) / 1000f;
                if (age > b.lifetime) {
                    if (b.lifetime > 0.5f) popBubble(b.x, b.y, b.color);
                    iter.remove();
                    continue;
                }

                float progress = age / b.lifetime;
                b.y += b.speedY * deltaTime;
                b.x += (float) Math.sin(age * b.wobbleFreq + b.phase) * b.wobbleAmp * deltaTime;
                b.alpha = (int) (b.alpha * (1f - progress));
                if (b.alpha < 0) b.alpha = 0;

                bubblePaint.setColor(adjustColorAlpha(b.color, b.alpha));
                canvas.drawCircle(b.x, b.y, b.size, bubblePaint);

                float highlightX = b.x - b.size * 0.25f;
                float highlightY = b.y - b.size * 0.25f;
                float highlightSize = b.size * 0.2f;
                bubbleHighlightPaint.setAlpha(b.alpha);
                canvas.drawCircle(highlightX, highlightY, highlightSize, bubbleHighlightPaint);

                if (b.y < -100) {
                    iter.remove();
                }
            }
        }

        private void updateAndDrawFreeSparkles(Canvas canvas, float deltaTime) {
            long now = System.currentTimeMillis();
            Iterator<FreeSparkle> iter = freeSparkles.iterator();
            while (iter.hasNext()) {
                FreeSparkle p = iter.next();
                float age = (now - p.creationTime) / 1000f;
                if (age > p.lifeTime) {
                    iter.remove();
                    continue;
                }
                float progress = age / p.lifeTime;
                int alpha = (int) (p.alpha * (1 - progress));
                sparklePaint.setColor(adjustColorAlpha(p.color, alpha));
                canvas.drawCircle(p.x, p.y, p.size * (1 - progress * 0.5f), sparklePaint);
                p.x += Math.cos(p.angle) * p.speed * deltaTime * 60;
                p.y += Math.sin(p.angle) * p.speed * deltaTime * 60;
            }
        }

        private void updateAndDrawFreeSmokes(Canvas canvas, float deltaTime) {
            long now = System.currentTimeMillis();
            Iterator<FreeSmoke> iter = freeSmokes.iterator();
            while (iter.hasNext()) {
                FreeSmoke p = iter.next();
                float age = (now - p.creationTime) / 1000f;
                if (age > p.lifeTime) {
                    iter.remove();
                    continue;
                }
                float progress = age / p.lifeTime;
                int alpha = (int) (p.alpha * (1 - progress));
                smokePaint.setColor(adjustColorAlpha(p.color, alpha));
                canvas.drawCircle(p.x, p.y, p.size * (1 + progress), smokePaint);
                p.x += p.dx * deltaTime * 60;
                p.y += p.dy * deltaTime * 60;
                p.dx *= 0.95f;
                p.dy *= 0.95f;
            }
        }

        private void updateTrails(float deltaTime) {
            Iterator<TouchTrail> iterator = fadingTrails.iterator();
            while (iterator.hasNext()) {
                TouchTrail trail = iterator.next();
                if (trail != null) {
                    trail.fadeAlpha -= (int) (150 * deltaTime);
                    if (trail.fadeAlpha < 0) trail.fadeAlpha = 0;
                    trail.paint.setAlpha(trail.fadeAlpha);
                    if (trail.fadeAlpha <= 0) {
                        iterator.remove();
                    }
                }
            }
        }

        private void drawActiveTrails(Canvas canvas, float deltaTime) {
            for (int i = 0; i < activeTrails.size(); i++) {
                TouchTrail trail = activeTrails.valueAt(i);
                if (trail != null && trail.path != null && trail.paint != null) {
                    if (System.currentTimeMillis() - trail.lastColorChange > 220) {
                        if (!trail.isColorTransitioning) {
                            trail.previousColorIndex = trail.colorIndex;
                            trail.colorIndex = (trail.colorIndex + 1) % rainbowColors.length;
                            trail.isColorTransitioning = true;
                            trail.colorTransitionProgress = 0f;
                        }
                        trail.lastColorChange = System.currentTimeMillis();
                    }

                    if (trail.isColorTransitioning) {
                        trail.colorTransitionProgress += deltaTime * 2.2f;
                        if (trail.colorTransitionProgress >= 1f) {
                            trail.colorTransitionProgress = 1f;
                            trail.isColorTransitioning = false;
                        }

                        int prevColor = rainbowColors[trail.previousColorIndex];
                        int nextColor = rainbowColors[trail.colorIndex];

                        int r = (int) (Color.red(prevColor) * (1 - trail.colorTransitionProgress) +
                                Color.red(nextColor) * trail.colorTransitionProgress);
                        int g = (int) (Color.green(prevColor) * (1 - trail.colorTransitionProgress) +
                                Color.green(nextColor) * trail.colorTransitionProgress);
                        int b = (int) (Color.blue(prevColor) * (1 - trail.colorTransitionProgress) +
                                Color.blue(nextColor) * trail.colorTransitionProgress);

                        trail.paint.setColor(Color.rgb(r, g, b));
                    }

                    trailGlowPaint.setColor(trail.paint.getColor());
                    trailGlowPaint.setAlpha(60);
                    trailGlowPaint.setStrokeWidth(trail.strokeWidth + 14f);
                    canvas.drawPath(trail.path, trailGlowPaint);

                    canvas.drawPath(trail.path, trail.paint);
                }
            }
        }

        private void drawFadingTrails(Canvas canvas) {
            for (TouchTrail trail : fadingTrails) {
                if (trail != null && trail.path != null && trail.paint != null) {
                    trailGlowPaint.setColor(trail.paint.getColor());
                    trailGlowPaint.setAlpha((int)(trail.fadeAlpha * 0.3f));
                    trailGlowPaint.setStrokeWidth(trail.strokeWidth + 14f);
                    canvas.drawPath(trail.path, trailGlowPaint);

                    canvas.drawPath(trail.path, trail.paint);
                }
            }
        }

        private void drawSmokeEffects(Canvas canvas, float deltaTime) {
            for (int i = 0; i < activeTrails.size(); i++) {
                TouchTrail trail = activeTrails.valueAt(i);
                if (trail == null) continue;
                if (trail.path != null && random.nextFloat() > 0.5f) addSmokeParticles(trail);
                drawSmokeParticles(canvas, trail, deltaTime);
            }
            for (TouchTrail trail : fadingTrails) {
                if (trail != null) drawSmokeParticles(canvas, trail, deltaTime);
            }
        }

        private void drawSparkleEffects(Canvas canvas, float deltaTime) {
            for (int i = 0; i < activeTrails.size(); i++) {
                TouchTrail trail = activeTrails.valueAt(i);
                if (trail == null) continue;
                if (trail.path != null && random.nextFloat() > 0.7f) addSparkleParticles(trail);
                drawSparkleParticles(canvas, trail, deltaTime);
            }
            for (TouchTrail trail : fadingTrails) {
                if (trail != null) drawSparkleParticles(canvas, trail, deltaTime);
            }
        }

        private void drawGlowEffects(Canvas canvas, float deltaTime) {
            for (int i = 0; i < activeTrails.size(); i++) {
                TouchTrail trail = activeTrails.valueAt(i);
                if (trail == null) continue;
                if (trail.path != null && random.nextFloat() > 0.8f) addGlowParticles(trail);
                drawGlowParticles(canvas, trail, deltaTime);
            }
            for (TouchTrail trail : fadingTrails) {
                if (trail != null) drawGlowParticles(canvas, trail, deltaTime);
            }
        }

        private void drawSpectralEffects(Canvas canvas, float deltaTime) {
            for (int i = 0; i < activeTrails.size(); i++) {
                TouchTrail trail = activeTrails.valueAt(i);
                if (trail == null) continue;
                if (trail.isColorTransitioning && trail.colorTransitionProgress > 0.2f &&
                        trail.colorTransitionProgress < 0.8f) addSpectralParticles(trail);
                drawSpectralParticles(canvas, trail, deltaTime);
            }
            for (TouchTrail trail : fadingTrails) {
                if (trail != null) drawSpectralParticles(canvas, trail, deltaTime);
            }
        }

        private void drawSmokeParticles(Canvas canvas, TouchTrail trail, float deltaTime) {
            Iterator<SmokeParticle> iterator = trail.smokeParticles.iterator();
            while (iterator.hasNext()) {
                SmokeParticle p = iterator.next();
                if (p == null) continue;
                long currentTime = System.currentTimeMillis();
                float lifeTime = (currentTime - p.creationTime) / 1000.0f;
                smokePaint.setColor(adjustColorAlpha(p.color, p.alpha));
                canvas.drawCircle(p.x, p.y, p.size, smokePaint);
                p.x += p.dx * deltaTime * 60;
                p.y += p.dy * deltaTime * 60;
                p.alpha -= (int) (40 * deltaTime);
                p.size += 5.0f * deltaTime;
                p.dx *= 0.95f;
                p.dy *= 0.95f;
                if (p.alpha <= 0 || lifeTime > 2.0f) iterator.remove();
            }
        }

        private void drawSparkleParticles(Canvas canvas, TouchTrail trail, float deltaTime) {
            Iterator<SparkleParticle> iterator = trail.sparkleParticles.iterator();
            while (iterator.hasNext()) {
                SparkleParticle p = iterator.next();
                if (p == null) continue;
                long currentTime = System.currentTimeMillis();
                float lifeTime = (currentTime - p.creationTime) / 1000.0f;
                sparklePaint.setColor(adjustColorAlpha(p.color, p.alpha));
                canvas.drawCircle(p.x, p.y, p.size, sparklePaint);
                p.x += (float) (Math.cos(p.angle) * p.speed * deltaTime * 60);
                p.y += (float) (Math.sin(p.angle) * p.speed * deltaTime * 60);
                p.alpha -= (int) (80 * deltaTime);
                p.size = Math.max(0, p.size - 1.5f * deltaTime);
                if (p.alpha <= 0 || p.size <= 0 || lifeTime > 1.5f) iterator.remove();
            }
        }

        private void drawGlowParticles(Canvas canvas, TouchTrail trail, float deltaTime) {
            Iterator<GlowParticle> iterator = trail.glowParticles.iterator();
            while (iterator.hasNext()) {
                GlowParticle p = iterator.next();
                if (p == null) continue;
                long currentTime = System.currentTimeMillis();
                float lifeTime = (currentTime - p.creationTime) / 1000.0f;
                float progress = lifeTime / p.lifeTime;
                if (progress > 1.0f) progress = 1.0f;
                int alpha = (int) (p.alpha * (1.0f - progress));
                glowPaint.setColor(adjustColorAlpha(p.color, alpha));
                canvas.drawCircle(p.x, p.y, p.size * (1.0f + progress * 0.5f), glowPaint);
                if (lifeTime > p.lifeTime) iterator.remove();
            }
        }

        private void drawSpectralParticles(Canvas canvas, TouchTrail trail, float deltaTime) {
            Iterator<SpectralParticle> iterator = trail.spectralParticles.iterator();
            while (iterator.hasNext()) {
                SpectralParticle p = iterator.next();
                if (p == null) continue;
                long currentTime = System.currentTimeMillis();
                float lifeTime = (currentTime - p.creationTime) / 1000.0f;
                float progress = lifeTime / p.lifeTime;
                if (progress > 1.0f) progress = 1.0f;
                int alpha = (int) (p.alpha * (1.0f - progress));
                spectralPaint.setColor(adjustColorAlpha(p.color, alpha));
                canvas.drawCircle(p.x, p.y, p.size, spectralPaint);
                canvas.drawCircle(p.x, p.y, p.size * 1.5f, spectralPaint);
                p.x += (float) (Math.cos(p.angle) * p.speed * deltaTime * 60);
                p.y += (float) (Math.sin(p.angle) * p.speed * deltaTime * 60);
                if (lifeTime > p.lifeTime) iterator.remove();
            }
        }

        private void addSmokeParticles(TouchTrail trail) {
            if (trail == null) return;
            for (int i = 0; i < 2; i++) {
                SmokeParticle p = new SmokeParticle();
                p.x = trail.lastX + random.nextFloat() * 40 - 20;
                p.y = trail.lastY + random.nextFloat() * 40 - 20;
                p.size = 5 + random.nextFloat() * 15;
                p.alpha = 100 + random.nextInt(80);
                p.dx = random.nextFloat() * 4 - 2;
                p.dy = random.nextFloat() * 4 - 2;
                p.color = rainbowColors[(trail.colorIndex + i) % rainbowColors.length];
                p.creationTime = System.currentTimeMillis();
                trail.smokeParticles.add(p);
            }
        }

        private void addSparkleParticles(TouchTrail trail) {
            if (trail == null) return;
            for (int i = 0; i < 2; i++) {
                SparkleParticle p = new SparkleParticle();
                p.x = trail.lastX + random.nextFloat() * 30 - 15;
                p.y = trail.lastY + random.nextFloat() * 30 - 15;
                p.size = 2 + random.nextFloat() * 8;
                p.alpha = 180 + random.nextInt(55);
                p.speed = 1.0f + random.nextFloat() * 4;
                p.angle = random.nextFloat() * (float) Math.PI * 2;
                p.color = sparkleColors[random.nextInt(sparkleColors.length)];
                p.creationTime = System.currentTimeMillis();
                trail.sparkleParticles.add(p);
            }
        }

        private void addGlowParticles(TouchTrail trail) {
            if (trail == null) return;
            GlowParticle p = new GlowParticle();
            p.x = trail.lastX;
            p.y = trail.lastY;
            p.size = trail.strokeWidth * 1.2f;
            p.alpha = 90;
            p.color = rainbowColors[trail.colorIndex];
            p.creationTime = System.currentTimeMillis();
            p.lifeTime = 0.5f + random.nextFloat() * 0.4f;
            trail.glowParticles.add(p);
        }

        private void addSpectralParticles(TouchTrail trail) {
            if (trail == null || !trail.isColorTransitioning) return;
            for (int i = 0; i < 4; i++) {
                SpectralParticle p = new SpectralParticle();
                p.x = trail.lastX + random.nextFloat() * 40 - 20;
                p.y = trail.lastY + random.nextFloat() * 40 - 20;
                p.size = 2 + random.nextFloat() * 5;
                p.alpha = 140 + random.nextInt(80);
                p.speed = 2.0f + random.nextFloat() * 4;
                p.angle = random.nextFloat() * (float) Math.PI * 2;
                p.color = random.nextBoolean() ? rainbowColors[trail.previousColorIndex] : rainbowColors[trail.colorIndex];
                p.creationTime = System.currentTimeMillis();
                p.lifeTime = 0.3f + random.nextFloat() * 0.3f;
                trail.spectralParticles.add(p);
            }
        }

        private int adjustColorAlpha(int color, int alpha) {
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        }

        private float parallaxBaseX = 0, parallaxBaseY = 0;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            try {
                int action = event.getActionMasked();
                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);
                lastTouchTime = System.currentTimeMillis();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN: {
                        TouchTrail trail = new TouchTrail();
                        trail.path = new Path();
                        float x = event.getX(pointerIndex);
                        float y = event.getY(pointerIndex);
                        trail.path.moveTo(x, y);
                        trail.lastX = x;
                        trail.lastY = y;
                        trail.pointerId = pointerId;
                        trail.strokeWidth = 18f + random.nextInt(16);
                        trail.creationTime = System.currentTimeMillis();

                        trail.paint = new Paint();
                        trail.paint.setAntiAlias(true);
                        trail.paint.setStrokeWidth(trail.strokeWidth);
                        trail.paint.setStyle(Paint.Style.STROKE);
                        trail.paint.setStrokeJoin(Paint.Join.ROUND);
                        trail.paint.setStrokeCap(Paint.Cap.ROUND);
                        trail.colorIndex = random.nextInt(rainbowColors.length);
                        trail.paint.setColor(rainbowColors[trail.colorIndex]);
                        trail.paint.setAlpha(trail.fadeAlpha);
                        trail.lastColorChange = System.currentTimeMillis();

                        addGlowParticles(trail);
                        activeTrails.put(pointerId, trail);

                        spawnRipple(x, y);

                        if (activeTrails.size() == 1) {
                            parallaxBaseX = x;
                            parallaxBaseY = y;
                            parallaxTargetX = 0;
                            parallaxTargetY = 0;
                        }
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        for (int i = 0; i < event.getPointerCount(); i++) {
                            pointerId = event.getPointerId(i);
                            TouchTrail t = activeTrails.get(pointerId);
                            if (t != null && t.path != null) {
                                float moveX = event.getX(i);
                                float moveY = event.getY(i);
                                float oldX = t.lastX;
                                float oldY = t.lastY;
                                float ctrlX = (oldX + moveX) / 2;
                                float ctrlY = (oldY + moveY) / 2;

                                t.path.quadTo(oldX, oldY, ctrlX, ctrlY);
                                t.lastX = moveX;
                                t.lastY = moveY;

                                if (i > 0) {
                                    float dx = moveX - oldX;
                                    float dy = moveY - oldY;
                                    float speed = (float) Math.sqrt(dx * dx + dy * dy);
                                    float newWidth = Math.max(12f, Math.min(36f, t.strokeWidth * (1 + speed / 30f)));
                                    t.paint.setStrokeWidth(newWidth);
                                }
                            }
                        }

                        if (event.getPointerCount() > 0) {
                            float firstX = event.getX(0);
                            float firstY = event.getY(0);
                            float dx = firstX - parallaxBaseX;
                            float dy = firstY - parallaxBaseY;
                            parallaxTargetX = dx * 0.08f;
                            parallaxTargetY = dy * 0.08f;
                        }

                        if (random.nextFloat() > 0.5f) {
                            spawnTouchBubble(event.getX(0), event.getY(0));
                        }
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP: {
                        TouchTrail t = activeTrails.get(pointerId);
                        if (t != null) {
                            addGlowParticles(t);
                            activeTrails.remove(pointerId);
                            t.isActive = false;
                            fadingTrails.add(t);
                        }

                        if (activeTrails.size() == 0) {
                            parallaxTargetX = 0f;
                            parallaxTargetY = 0f;
                        }
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        for (int i = 0; i < activeTrails.size(); i++) {
                            TouchTrail trailToFade = activeTrails.valueAt(i);
                            if (trailToFade != null) {
                                activeTrails.removeAt(i);
                                trailToFade.isActive = false;
                                fadingTrails.add(trailToFade);
                                i--;
                            }
                        }
                        parallaxTargetX = 0f;
                        parallaxTargetY = 0f;
                        break;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private boolean isSignatureValid() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            byte[] signature = packageInfo.signingInfo.getApkContentsSigners()[0].toByteArray();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(signature);
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString().equals("1e880257852a0a8502d6234797b27f487773a30531a3c132c9e88415ea13da83");
        } catch (Exception e) {
            return false;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}