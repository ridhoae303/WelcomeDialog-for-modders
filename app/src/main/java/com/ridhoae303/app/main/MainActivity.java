// Created by ridhoae303
// Telegram: @ridhoae303 — https://t.me/ridhoae303

package com.ridhoae303.app.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.takane.app.TakaneActivity;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {

    private static final String EXPECTED_SIGNATURE = "e4201e2e32724c1ba1ef1100d35ff9f75c5d3e888a58c68b7747808f4c87607b";
    private static final String BACKGROUND_IMAGE_PATH = "ridhoae303/assets/fuuka.jpg";
    
    private RainbowDrawingView rainbowDrawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            FrameLayout rootLayout = new FrameLayout(this);
            setupBackground(rootLayout);
            setupRainbowDrawing(rootLayout);
            setContentView(rootLayout);
            
            checkSignatureAndLaunch();
            TakaneActivity.yuuka(this);
            
        } catch (Exception e) {
            showToast("Error inisialisasi aplikasi");
            finish();
        }
    }

    private void setupBackground(FrameLayout rootLayout) {
        try {
            ImageView background = new ImageView(this);
            InputStream is = getAssets().open(BACKGROUND_IMAGE_PATH);
            background.setImageBitmap(BitmapFactory.decodeStream(is));
            is.close();
            rootLayout.addView(background);
        } catch (IOException e) {
            rootLayout.setBackgroundColor(Color.BLACK);
        }
    }

    private void setupRainbowDrawing(FrameLayout rootLayout) {
        rainbowDrawingView = new RainbowDrawingView(this);
        rootLayout.addView(rainbowDrawingView);
    }

    private void checkSignatureAndLaunch() {
        if (!isSignatureValid()) {
            showToast("Signature tidak valid!");
            finish();
            return;
        }
      }
      
    private static class RainbowDrawingView extends View {
        private static class TouchTrail {
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

        private static class SmokeParticle {
            float x, y;
            float size;
            int alpha;
            float dx, dy;
            int color;
            long creationTime;
        }

        private static class SparkleParticle {
            float x, y;
            float size;
            int alpha;
            float speed;
            float angle;
            int color;
            long creationTime;
        }

        private static class GlowParticle {
            float x, y;
            float size;
            int alpha;
            int color;
            long creationTime;
            float lifeTime;
        }

        private static class SpectralParticle {
            float x, y;
            float size;
            int alpha;
            float speed;
            float angle;
            int color;
            long creationTime;
            float lifeTime;
        }

        private final SparseArray<TouchTrail> activeTrails = new SparseArray<>();
        private final ArrayList<TouchTrail> fadingTrails = new ArrayList<>();
        private final Random random = new Random();
        private final Paint smokePaint = new Paint();
        private final Paint sparklePaint = new Paint();
        private final Paint glowPaint = new Paint();
        private final Paint spectralPaint = new Paint();
        
        private final int[] rainbowColors = {
            Color.rgb(255, 0, 0),       // Red
            Color.rgb(255, 127, 0),     // Orange
            Color.rgb(255, 255, 0),     // Yellow
            Color.rgb(0, 255, 0),       // Green
            Color.rgb(0, 0, 255),       // Blue
            Color.rgb(75, 0, 130),      // Indigo
            Color.rgb(148, 0, 211),     // Violet
            Color.rgb(255, 0, 255),     // Magenta
            Color.rgb(0, 255, 255),     // Cyan
            Color.rgb(255, 192, 203),   // Pink
            Color.rgb(255, 215, 0),     // Gold
            Color.rgb(50, 205, 50),     // Lime Green
            Color.rgb(138, 43, 226),    // Blue Violet
            Color.rgb(255, 165, 0),     // Dark Orange
            Color.rgb(0, 255, 127),     // Spring Green
            Color.rgb(255, 20, 147)     // Deep Pink
        };

        private final int[] sparkleColors = {
            Color.WHITE,
            Color.rgb(255, 255, 200),
            Color.rgb(200, 255, 255),
            Color.rgb(255, 200, 255)
        };

        private long lastUpdateTime;
        private final float density;

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
            
            lastUpdateTime = System.currentTimeMillis();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            try {
                long currentTime = System.currentTimeMillis();
                float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
                lastUpdateTime = currentTime;
                
                drawFadingTrails(canvas);
                drawActiveTrails(canvas, deltaTime);
                drawSmokeEffects(canvas, deltaTime);
                drawSparkleEffects(canvas, deltaTime);
                drawGlowEffects(canvas, deltaTime);
                drawSpectralEffects(canvas, deltaTime);
                updateTrails(deltaTime);
                invalidate();
            } catch (Exception e) {
            }
        }

        private void updateTrails(float deltaTime) {
            Iterator<TouchTrail> iterator = fadingTrails.iterator();
            while (iterator.hasNext()) {
                TouchTrail trail = iterator.next();
                if (trail != null) {
                    trail.fadeAlpha -= (int)(150 * deltaTime);
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
                    if (System.currentTimeMillis() - trail.lastColorChange > 80) {
                        if (!trail.isColorTransitioning) {
                            trail.previousColorIndex = trail.colorIndex;
                            trail.colorIndex = (trail.colorIndex + 1) % rainbowColors.length;
                            trail.isColorTransitioning = true;
                            trail.colorTransitionProgress = 0f;
                        }
                        trail.lastColorChange = System.currentTimeMillis();
                    }
                    
                    if (trail.isColorTransitioning) {
                        trail.colorTransitionProgress += deltaTime * 8f; // Speed of transition
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
                    
                    canvas.drawPath(trail.path, trail.paint);
                }
            }
        }

        private void drawFadingTrails(Canvas canvas) {
            for (TouchTrail trail : fadingTrails) {
                if (trail != null && trail.path != null && trail.paint != null) {
                    canvas.drawPath(trail.path, trail.paint);
                }
            }
        }

        private void drawSmokeEffects(Canvas canvas, float deltaTime) {
            for (int i = 0; i < activeTrails.size(); i++) {
                TouchTrail trail = activeTrails.valueAt(i);
                if (trail == null) continue;
                
                if (trail.path != null && random.nextFloat() > 0.5f) {
                    addSmokeParticles(trail);
                }
                
                drawSmokeParticles(canvas, trail, deltaTime);
            }
            
            for (TouchTrail trail : fadingTrails) {
                if (trail != null) {
                    drawSmokeParticles(canvas, trail, deltaTime);
                }
            }
        }

        private void drawSparkleEffects(Canvas canvas, float deltaTime) {
            for (int i = 0; i < activeTrails.size(); i++) {
                TouchTrail trail = activeTrails.valueAt(i);
                if (trail == null) continue;
                
                if (trail.path != null && random.nextFloat() > 0.7f) {
                    addSparkleParticles(trail);
                }
                
                drawSparkleParticles(canvas, trail, deltaTime);
            }
            
            for (TouchTrail trail : fadingTrails) {
                if (trail != null) {
                    drawSparkleParticles(canvas, trail, deltaTime);
                }
            }
        }

        private void drawGlowEffects(Canvas canvas, float deltaTime) {
            for (int i = 0; i < activeTrails.size(); i++) {
                TouchTrail trail = activeTrails.valueAt(i);
                if (trail == null) continue;
                
                if (trail.path != null && random.nextFloat() > 0.8f) {
                    addGlowParticles(trail);
                }
                
                drawGlowParticles(canvas, trail, deltaTime);
            }
            
            for (TouchTrail trail : fadingTrails) {
                if (trail != null) {
                    drawGlowParticles(canvas, trail, deltaTime);
                }
            }
        }

        private void drawSpectralEffects(Canvas canvas, float deltaTime) {
            for (int i = 0; i < activeTrails.size(); i++) {
                TouchTrail trail = activeTrails.valueAt(i);
                if (trail == null) continue;
                
                if (trail.isColorTransitioning && trail.colorTransitionProgress > 0.2f && 
                    trail.colorTransitionProgress < 0.8f) {
                    addSpectralParticles(trail);
                }
                
                drawSpectralParticles(canvas, trail, deltaTime);
            }
            
            for (TouchTrail trail : fadingTrails) {
                if (trail != null) {
                    drawSpectralParticles(canvas, trail, deltaTime);
                }
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
                p.alpha -= (int)(40 * deltaTime);
                p.size += 5.0f * deltaTime;
                p.dx *= 0.95f;
                p.dy *= 0.95f;
                
                if (p.alpha <= 0 || lifeTime > 2.0f) {
                    iterator.remove();
                }
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
                p.alpha -= (int)(80 * deltaTime);
                p.size = Math.max(0, p.size - 1.5f * deltaTime);
                
                if (p.alpha <= 0 || p.size <= 0 || lifeTime > 1.5f) {
                    iterator.remove();
                }
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
                
                if (lifeTime > p.lifeTime) {
                    iterator.remove();
                }
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
                
                if (lifeTime > p.lifeTime) {
                    iterator.remove();
                }
            }
        }

        private void addSmokeParticles(TouchTrail trail) {
            if (trail == null) return;
            
            float x = trail.lastX;
            float y = trail.lastY;
            
            for (int i = 0; i < 3; i++) {
                SmokeParticle p = new SmokeParticle();
                p.x = x + random.nextFloat() * 40 - 20;
                p.y = y + random.nextFloat() * 40 - 20;
                p.size = 5 + random.nextFloat() * 20;
                p.alpha = 100 + random.nextInt(100);
                p.dx = random.nextFloat() * 6 - 3;
                p.dy = random.nextFloat() * 6 - 3;
                p.color = rainbowColors[(trail.colorIndex + i) % rainbowColors.length];
                p.creationTime = System.currentTimeMillis();
                trail.smokeParticles.add(p);
            }
        }

        private void addSparkleParticles(TouchTrail trail) {
            if (trail == null) return;
            
            float x = trail.lastX;
            float y = trail.lastY;
            
            for (int i = 0; i < 2; i++) {
                SparkleParticle p = new SparkleParticle();
                p.x = x + random.nextFloat() * 30 - 15;
                p.y = y + random.nextFloat() * 30 - 15;
                p.size = 3 + random.nextFloat() * 10;
                p.alpha = 200 + random.nextInt(55);
                p.speed = 1.0f + random.nextFloat() * 4;
                p.angle = random.nextFloat() * (float) Math.PI * 2;
                p.color = sparkleColors[random.nextInt(sparkleColors.length)];
                p.creationTime = System.currentTimeMillis();
                trail.sparkleParticles.add(p);
            }
        }

        private void addGlowParticles(TouchTrail trail) {
            if (trail == null) return;
            
            float x = trail.lastX;
            float y = trail.lastY;
            
            GlowParticle p = new GlowParticle();
            p.x = x;
            p.y = y;
            p.size = trail.strokeWidth * 1.5f;
            p.alpha = 100;
            p.color = rainbowColors[trail.colorIndex];
            p.creationTime = System.currentTimeMillis();
            p.lifeTime = 0.5f + random.nextFloat() * 0.5f;
            trail.glowParticles.add(p);
        }

        private void addSpectralParticles(TouchTrail trail) {
            if (trail == null || !trail.isColorTransitioning) return;
            
            float x = trail.lastX;
            float y = trail.lastY;
            
            for (int i = 0; i < 5; i++) {
                SpectralParticle p = new SpectralParticle();
                p.x = x + random.nextFloat() * 50 - 25;
                p.y = y + random.nextFloat() * 50 - 25;
                p.size = 2 + random.nextFloat() * 6;
                p.alpha = 150 + random.nextInt(105);
                p.speed = 2.0f + random.nextFloat() * 5;
                p.angle = random.nextFloat() * (float) Math.PI * 2;
                
                if (random.nextBoolean()) {
                    p.color = rainbowColors[trail.previousColorIndex];
                } else {
                    p.color = rainbowColors[trail.colorIndex];
                }
                
                p.creationTime = System.currentTimeMillis();
                p.lifeTime = 0.3f + random.nextFloat() * 0.3f;
                trail.spectralParticles.add(p);
            }
        }

        private int adjustColorAlpha(int color, int alpha) {
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            try {
                int action = event.getActionMasked();
                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        TouchTrail trail = new TouchTrail();
                        trail.path = new Path();
                        float x = event.getX(pointerIndex);
                        float y = event.getY(pointerIndex);
                        trail.path.moveTo(x, y);
                        trail.lastX = x;
                        trail.lastY = y;
                        trail.pointerId = pointerId;
                        trail.strokeWidth = 20f + random.nextInt(20);
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
                        break;
                        
                    case MotionEvent.ACTION_MOVE:
                        for (int i = 0; i < event.getPointerCount(); i++) {
                            pointerId = event.getPointerId(i);
                            TouchTrail t = activeTrails.get(pointerId);
                            if (t != null && t.path != null) {
                                float moveX = event.getX(i);
                                float moveY = event.getY(i);
                                float ctrlX = (t.lastX + moveX) / 2;
                                float ctrlY = (t.lastY + moveY) / 2;
                                
                                t.path.quadTo(t.lastX, t.lastY, ctrlX, ctrlY);
                                t.lastX = moveX;
                                t.lastY = moveY;
                                
                                if (i > 0) {
                                    float dx = moveX - t.lastX;
                                    float dy = moveY - t.lastY;
                                    float speed = (float) Math.sqrt(dx*dx + dy*dy);
                                    float newWidth = Math.max(15f, Math.min(40f, t.strokeWidth * (1 + speed/30f)));
                                    t.paint.setStrokeWidth(newWidth);
                                }
                            }
                        }
                        break;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        TouchTrail t = activeTrails.get(pointerId);
                        if (t != null) {
                            addGlowParticles(t);
                            
                            activeTrails.remove(pointerId);
                            t.isActive = false;
                            fadingTrails.add(t);
                        }
                        break;
                        
                    case MotionEvent.ACTION_CANCEL:
                        for (int i = 0; i < activeTrails.size(); i++) {
                            TouchTrail trailToFade = activeTrails.valueAt(i);
                            if (trailToFade != null) {
                                activeTrails.removeAt(i);
                                trailToFade.isActive = false;
                                fadingTrails.add(trailToFade);
                                i--;
                            }
                        }
                        break;
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
            return hexString.toString().equals(EXPECTED_SIGNATURE);
        } catch (Exception e) {
            return false;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}