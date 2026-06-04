// Created by ridhoae303 — https://github.com/ridhoae303
// No lambda expression.

package com.ridhoae303.app.main;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.takane.app.TakaneActivity;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {

    private FrameLayout backgroundContainer;
    private FrameLayout wallpaperLayer;
    private GpuSmokeTextureView smokeTextureView;
    private FrameLayout currentWallpaperGroup;
    private ImageView mainImage;
    private RainbowDrawingView rainbowDrawingView;

    private String currentWallpaperAsset = "";

    private static final String WALLPAPER_PORTRAIT_ASSET = "ridhoae303/assets/wallpaper.jpg";
    private static final String WALLPAPER_LANDSCAPE_ASSET = "ridhoae303/assets/wallpaper2.jpg";

    private static final long WALLPAPER_FADE_OUT_DURATION = 260L;
    private static final long WALLPAPER_FADE_IN_DURATION = 320L;
    private static final long WALLPAPER_ZOOM_DURATION = 180L;
    private static final long WALLPAPER_ENTRANCE_DURATION = 440L;

    private float wallpaperScale = 1.0f;
    private float wallpaperParallaxX = 0.0f;
    private float wallpaperParallaxY = 0.0f;
    private boolean wallpaperTouchZoomActive = false;
    private boolean wallpaperTransitionRunning = false;
    private boolean homeEntrancePlayed = false;

    private final ArrayList<FrameLayout> oldWallpaperGroups = new ArrayList<FrameLayout>();
    private long lastBackPressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.BLACK));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        try {
            FrameLayout rootLayout = new FrameLayout(this);
            rootLayout.setClipChildren(false);
            rootLayout.setClipToPadding(false);

            setupBackground(rootLayout);
            setupRainbowDrawing(rootLayout);
            setContentView(rootLayout);

            checkSignatureAndLaunch();
            TakaneActivity.atsuko(this);
        } catch (Exception e) {
            showToast("Error initializing application");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (smokeTextureView != null) {
            smokeTextureView.resumeRenderer();
        }
    }

    @Override
    protected void onPause() {
        if (smokeTextureView != null) {
            smokeTextureView.pauseRenderer();
        }
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        wallpaperTouchZoomActive = false;
        resetWallpaperParallax();
        if (rainbowDrawingView != null) {
            rainbowDrawingView.resetWallpaperMotion();
        }
        if (smokeTextureView != null) {
            smokeTextureView.resetScene();
        }
        applyWallpaperForDevice(true);
    }

    private void startHomeEntranceAnimation(final FrameLayout wallpaperGroup) {
        if (wallpaperGroup == null) {
            return;
        }

        wallpaperGroup.animate().cancel();
        wallpaperGroup.setAlpha(0.0f);
        wallpaperGroup.setScaleX(1.018f);
        wallpaperGroup.setScaleY(1.018f);

        wallpaperGroup.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setStartDelay(0L)
                .setDuration(WALLPAPER_ENTRANCE_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        wallpaperTransitionRunning = false;
                    }
                })
                .start();
    }

    private void animateWallpaperZoom(boolean touchDown) {
        wallpaperTouchZoomActive = touchDown;
        wallpaperScale = getCurrentWallpaperScale();
        applyWallpaperTransform(true);
    }

    private void resetWallpaperParallax() {
        wallpaperParallaxX = 0.0f;
        wallpaperParallaxY = 0.0f;
        applyWallpaperTransform(false);
    }

    private void setWallpaperParallax(float x, float y) {
        wallpaperParallaxX = clamp(x, -dp(18), dp(18));
        wallpaperParallaxY = clamp(y, -dp(18), dp(18));
        applyWallpaperTranslationToView(mainImage, wallpaperParallaxX, wallpaperParallaxY);
    }

    private void applyWallpaperTranslationToView(ImageView imageView, float translationX, float translationY) {
        if (imageView == null) return;
        imageView.setTranslationX(translationX);
        imageView.setTranslationY(translationY);
    }

    private float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private float getCurrentWallpaperScale() {
        return wallpaperTouchZoomActive ? getTouchWallpaperScale() : getIdleWallpaperScale();
    }

    private float getIdleWallpaperScale() {
        return isTabletOrPc() ? 1.05f : 1.025f;
    }

    private float getTouchWallpaperScale() {
        return isTabletOrPc() ? 1.025f : 1.01f;
    }

    private void applyWallpaperTransform(boolean animate) {
        wallpaperScale = getCurrentWallpaperScale();
        applyWallpaperTransformToView(
                mainImage,
                wallpaperScale,
                wallpaperParallaxX,
                wallpaperParallaxY,
                animate
        );
    }

    private void applyWallpaperTransformToView(ImageView imageView, float scale, float translationX, float translationY, boolean animate) {
        if (imageView == null) return;

        imageView.setPivotX(imageView.getWidth() / 2f);
        imageView.setPivotY(imageView.getHeight() / 2f);

        if (animate) {
            imageView.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .translationX(translationX)
                    .translationY(translationY)
                    .setDuration(WALLPAPER_ZOOM_DURATION)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            imageView.setScaleX(scale);
            imageView.setScaleY(scale);
            imageView.setTranslationX(translationX);
            imageView.setTranslationY(translationY);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void setupBackground(FrameLayout rootLayout) {
        backgroundContainer = new FrameLayout(this);
        backgroundContainer.setClipChildren(false);
        backgroundContainer.setClipToPadding(false);
        backgroundContainer.setBackgroundColor(Color.BLACK);
        backgroundContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        wallpaperLayer = new FrameLayout(this);
        wallpaperLayer.setClipChildren(false);
        wallpaperLayer.setClipToPadding(false);
        wallpaperLayer.setBackgroundColor(Color.BLACK);
        wallpaperLayer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        backgroundContainer.addView(wallpaperLayer);

        smokeTextureView = new GpuSmokeTextureView(this);
        smokeTextureView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        smokeTextureView.setAlpha(0.72f);
        backgroundContainer.addView(smokeTextureView);

        rootLayout.addView(backgroundContainer);

        backgroundContainer.post(new Runnable() {
            @Override
            public void run() {
                applyWallpaperForDevice(false);
            }
        });
    }

    private Bitmap decodeWallpaperBitmap(String assetPath) {
        InputStream boundsStream = null;
        InputStream bitmapStream = null;

        try {
            int targetWidth = Math.max(1, wallpaperLayer == null ? getResources().getDisplayMetrics().widthPixels : wallpaperLayer.getWidth());
            int targetHeight = Math.max(1, wallpaperLayer == null ? getResources().getDisplayMetrics().heightPixels : wallpaperLayer.getHeight());

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            boundsStream = getAssets().open(assetPath);
            BitmapFactory.decodeStream(boundsStream, null, bounds);
            try {
                boundsStream.close();
            } catch (Throwable ignored) {
            }
            boundsStream = null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inDither = true;
            options.inScaled = false;
            options.inSampleSize = calculateInSampleSize(bounds, targetWidth, targetHeight);

            bitmapStream = getAssets().open(assetPath);
            return BitmapFactory.decodeStream(bitmapStream, null, options);
        } catch (Throwable e) {
            return null;
        } finally {
            if (boundsStream != null) {
                try {
                    boundsStream.close();
                } catch (Throwable ignored) {
                }
            }
            if (bitmapStream != null) {
                try {
                    bitmapStream.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        try {
            int height = options.outHeight;
            int width = options.outWidth;
            int inSampleSize = 1;

            if (height <= 0 || width <= 0) {
                return 1;
            }

            if (height > reqHeight || width > reqWidth) {
                int halfHeight = height / 2;
                int halfWidth = width / 2;

                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }

            if (inSampleSize < 1) {
                inSampleSize = 1;
            }

            return inSampleSize;
        } catch (Throwable e) {
            return 1;
        }
    }

    private void recycleWallpaperGroup(FrameLayout group) {
        if (group == null) {
            return;
        }

        try {
            Object tag = group.getTag();
            if (tag instanceof WallpaperHolder) {
                ((WallpaperHolder) tag).recycle();
            }
        } catch (Throwable ignored) {
        }
    }

    private void removeAllOldWallpaperGroupsExcept(FrameLayout keepGroup) {
        if (wallpaperLayer == null) {
            return;
        }

        for (int i = oldWallpaperGroups.size() - 1; i >= 0; i--) {
            FrameLayout group = oldWallpaperGroups.get(i);
            if (group == null || group == keepGroup || group.getParent() == null) {
                oldWallpaperGroups.remove(i);
                continue;
            }

            try {
                group.animate().cancel();
                wallpaperLayer.removeView(group);
            } catch (Throwable ignored) {
            }
            recycleWallpaperGroup(group);
            oldWallpaperGroups.remove(i);
        }
    }

    private static class WallpaperHolder {
        ImageView main;
        Bitmap original;
        boolean recycled;

        WallpaperHolder(ImageView m, Bitmap o) {
            main = m;
            original = o;
        }

        void recycle() {
            if (recycled) {
                return;
            }
            recycled = true;

            try {
                if (main != null) {
                    main.setImageDrawable(null);
                }
            } catch (Throwable ignored) {
            }

            try {
                if (original != null && !original.isRecycled()) {
                    original.recycle();
                }
            } catch (Throwable ignored) {
            }

            main = null;
            original = null;
        }
    }

    private void applyWallpaperForDevice(boolean animate) {
        if (wallpaperLayer == null) return;

        String asset = resolveWallpaperAsset();

        if (asset.equals(currentWallpaperAsset) && currentWallpaperGroup != null) {
            applyWallpaperTransform(animate);
            return;
        }

        final FrameLayout nextGroup = createWallpaperGroup(asset);

        if (nextGroup == null) {
            if (currentWallpaperGroup == null) {
                wallpaperLayer.setBackgroundColor(Color.BLACK);
            }
            return;
        }

        if (!animate || currentWallpaperGroup == null) {
            boolean firstWallpaper = currentWallpaperGroup == null && !homeEntrancePlayed;

            removeAllOldWallpaperGroupsExcept(null);
            if (currentWallpaperGroup != null) {
                recycleWallpaperGroup(currentWallpaperGroup);
            }

            wallpaperLayer.removeAllViews();
            if (firstWallpaper) {
                nextGroup.setAlpha(0.0f);
                wallpaperTransitionRunning = true;
            } else {
                nextGroup.setAlpha(1.0f);
            }

            wallpaperLayer.addView(nextGroup);
            currentWallpaperGroup = nextGroup;
            currentWallpaperAsset = asset;
            applyWallpaperRefs(nextGroup);

            if (firstWallpaper) {
                homeEntrancePlayed = true;
                startHomeEntranceAnimation(nextGroup);
            } else {
                wallpaperTransitionRunning = false;
            }
            return;
        }

        final FrameLayout oldGroup = currentWallpaperGroup;

        removeAllOldWallpaperGroupsExcept(oldGroup);

        oldGroup.animate().cancel();
        nextGroup.animate().cancel();
        nextGroup.setAlpha(0.0f);
        wallpaperLayer.addView(nextGroup);

        final String nextAsset = asset;
        wallpaperTransitionRunning = true;
        currentWallpaperGroup = nextGroup;
        currentWallpaperAsset = nextAsset;
        applyWallpaperRefs(nextGroup);

        if (!oldWallpaperGroups.contains(oldGroup)) {
            oldWallpaperGroups.add(oldGroup);
        }

        oldGroup.animate()
                .alpha(0.0f)
                .setDuration(WALLPAPER_FADE_OUT_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            wallpaperLayer.removeView(oldGroup);
                        } catch (Throwable ignored) {
                        }

                        oldWallpaperGroups.remove(oldGroup);
                        recycleWallpaperGroup(oldGroup);
                    }
                })
                .start();

        nextGroup.animate()
                .alpha(1.0f)
                .setDuration(WALLPAPER_FADE_IN_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        wallpaperTransitionRunning = false;
                    }
                })
                .start();
    }

    private String resolveWallpaperAsset() {
        return isLandscapeOrientation() ? WALLPAPER_LANDSCAPE_ASSET : WALLPAPER_PORTRAIT_ASSET;
    }

    private boolean isLandscapeOrientation() {
        Configuration configuration = getResources().getConfiguration();
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private boolean isTabletOrPc() {
        Configuration configuration = getResources().getConfiguration();

        int screenSize = configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        boolean largeByScreenLayout =
                screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE
                        || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;

        boolean largeBySmallestWidth = configuration.smallestScreenWidthDp >= 600;

        int uiMode = configuration.uiMode & Configuration.UI_MODE_TYPE_MASK;
        boolean desktopLike =
                uiMode == Configuration.UI_MODE_TYPE_DESK
                        || uiMode == Configuration.UI_MODE_TYPE_TELEVISION;

        return largeByScreenLayout || largeBySmallestWidth || desktopLike;
    }

    private FrameLayout createWallpaperGroup(String assetPath) {
        FrameLayout group = new FrameLayout(this);
        group.setClipChildren(false);
        group.setClipToPadding(false);
        group.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        try {
            Bitmap original = decodeWallpaperBitmap(assetPath);
            ImageView main = createWallpaperImageView();

            if (original != null) {
                main.setImageBitmap(original);
            } else {
                main.setBackgroundColor(Color.BLACK);
            }

            wallpaperScale = getCurrentWallpaperScale();
            main.setScaleX(wallpaperScale);
            main.setScaleY(wallpaperScale);
            main.setTranslationX(wallpaperParallaxX);
            main.setTranslationY(wallpaperParallaxY);

            group.addView(main);
            group.setTag(new WallpaperHolder(main, original));

            return group;
        } catch (Throwable e) {
            if (!WALLPAPER_PORTRAIT_ASSET.equals(assetPath)) {
                recycleWallpaperGroup(group);
                return createWallpaperGroup(WALLPAPER_PORTRAIT_ASSET);
            }

            group.setBackgroundColor(Color.BLACK);
            return group;
        }
    }

    private ImageView createWallpaperImageView() {
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setAdjustViewBounds(false);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        return imageView;
    }

    private void applyWallpaperRefs(final FrameLayout group) {
        if (group == null || !(group.getTag() instanceof WallpaperHolder)) {
            return;
        }

        WallpaperHolder holder = (WallpaperHolder) group.getTag();
        mainImage = holder.main;

        if (mainImage != null) {
            mainImage.post(new Runnable() {
                @Override
                public void run() {
                    applyWallpaperTransform(false);
                }
            });
        }
    }

    private void setupRainbowDrawing(FrameLayout rootLayout) {
        rainbowDrawingView = new RainbowDrawingView(this);
        rootLayout.addView(rainbowDrawingView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
    }

    private void checkSignatureAndLaunch() {
        if (!isSignatureValid()) {
            showToast("Invalid signature!");
            finish();
        }
    }

    private static class GpuSmokeTextureView extends TextureView implements TextureView.SurfaceTextureListener {
        private RenderThread renderThread;
        private volatile boolean paused = false;
        private volatile boolean surfaceReady = false;
        private int surfaceWidth = 1;
        private int surfaceHeight = 1;
        private float sceneSeed;
        private final Random seedRandom = new Random();

        public GpuSmokeTextureView(Context context) {
            super(context);
            setSurfaceTextureListener(this);
            setOpaque(false);
            sceneSeed = seedRandom.nextFloat() * 1000.0f;
        }

        public void pauseRenderer() {
            paused = true;
        }

        public void resumeRenderer() {
            paused = false;
            RenderThread thread = renderThread;
            if (thread != null) {
                thread.wakeUp();
            }
        }

        public void resetScene() {
            sceneSeed = seedRandom.nextFloat() * 1000.0f;
            RenderThread thread = renderThread;
            if (thread != null) {
                thread.setSceneSeed(sceneSeed);
                thread.setSurfaceSize(surfaceWidth, surfaceHeight);
                thread.wakeUp();
            }
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            surfaceReady = true;
            surfaceWidth = Math.max(1, width);
            surfaceHeight = Math.max(1, height);
            stopRendererThread();
            renderThread = new RenderThread(surface, surfaceWidth, surfaceHeight, sceneSeed, this);
            renderThread.start();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            surfaceWidth = Math.max(1, width);
            surfaceHeight = Math.max(1, height);
            RenderThread thread = renderThread;
            if (thread != null) {
                thread.setSurfaceSize(surfaceWidth, surfaceHeight);
                thread.wakeUp();
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            surfaceReady = false;
            stopRendererThread();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }

        private boolean isRendererPaused() {
            return paused || !surfaceReady;
        }

        private void stopRendererThread() {
            RenderThread thread = renderThread;
            renderThread = null;
            if (thread != null) {
                thread.requestStop();
                try {
                    thread.join(450L);
                } catch (InterruptedException ignored) {
                }
            }
        }

        private static class RenderThread extends Thread {
            private static final long FRAME_DELAY_MS = 50L;

            private final SurfaceTexture surfaceTexture;
            private final GpuSmokeTextureView owner;
            private volatile boolean running = true;
            private volatile int width;
            private volatile int height;
            private volatile float seed;

            private EGLDisplay eglDisplay = EGL14.EGL_NO_DISPLAY;
            private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
            private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;

            private FloatBuffer vertexBuffer;
            private int program = 0;
            private int positionHandle = -1;
            private int resolutionHandle = -1;
            private int timeHandle = -1;
            private int seedHandle = -1;
            private long startTime;

            RenderThread(SurfaceTexture texture, int w, int h, float s, GpuSmokeTextureView view) {
                super("ridhoae303-gpu-smoke");
                surfaceTexture = texture;
                width = Math.max(1, w);
                height = Math.max(1, h);
                seed = s;
                owner = view;
            }

            void requestStop() {
                running = false;
                interrupt();
            }

            void wakeUp() {
                interrupt();
            }

            void setSurfaceSize(int w, int h) {
                width = Math.max(1, w);
                height = Math.max(1, h);
            }

            void setSceneSeed(float s) {
                seed = s;
            }

            @Override
            public void run() {
                try {
                    if (!initEgl()) {
                        return;
                    }
                    initGlObjects();
                    startTime = System.currentTimeMillis();

                    while (running) {
                        if (owner == null || owner.isRendererPaused()) {
                            safeSleep(120L);
                            continue;
                        }

                        long frameStart = System.currentTimeMillis();
                        drawFrame();
                        EGL14.eglSwapBuffers(eglDisplay, eglSurface);

                        long elapsed = System.currentTimeMillis() - frameStart;
                        long sleep = FRAME_DELAY_MS - elapsed;
                        if (sleep > 0L) {
                            safeSleep(sleep);
                        }
                    }
                } catch (Throwable ignored) {
                } finally {
                    releaseGlObjects();
                    releaseEgl();
                }
            }

            private void safeSleep(long millis) {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException ignored) {
                }
            }

            private boolean initEgl() {
                eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
                if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                    return false;
                }

                int[] version = new int[2];
                if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                    return false;
                }

                int[] configAttribs = {
                        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                        EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                        EGL14.EGL_RED_SIZE, 8,
                        EGL14.EGL_GREEN_SIZE, 8,
                        EGL14.EGL_BLUE_SIZE, 8,
                        EGL14.EGL_ALPHA_SIZE, 8,
                        EGL14.EGL_DEPTH_SIZE, 0,
                        EGL14.EGL_STENCIL_SIZE, 0,
                        EGL14.EGL_NONE
                };

                EGLConfig[] configs = new EGLConfig[1];
                int[] numConfigs = new int[1];
                if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] <= 0) {
                    return false;
                }

                int[] contextAttribs = {
                        EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                        EGL14.EGL_NONE
                };
                eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0);
                if (eglContext == EGL14.EGL_NO_CONTEXT) {
                    return false;
                }

                int[] surfaceAttribs = {
                        EGL14.EGL_NONE
                };
                eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surfaceTexture, surfaceAttribs, 0);
                if (eglSurface == EGL14.EGL_NO_SURFACE) {
                    return false;
                }

                return EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
            }

            private void initGlObjects() {
                float[] vertices = {
                        -1.0f, -1.0f,
                         1.0f, -1.0f,
                        -1.0f,  1.0f,
                         1.0f,  1.0f
                };
                ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
                bb.order(ByteOrder.nativeOrder());
                vertexBuffer = bb.asFloatBuffer();
                vertexBuffer.put(vertices);
                vertexBuffer.position(0);

                String vertexShader =
                        "attribute vec2 aPosition;\n" +
                        "varying vec2 vUv;\n" +
                        "void main() {\n" +
                        "    vUv = aPosition * 0.5 + 0.5;\n" +
                        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
                        "}\n";

                String fragmentShader =
                        "precision mediump float;\n" +
                        "varying vec2 vUv;\n" +
                        "uniform vec2 uResolution;\n" +
                        "uniform float uTime;\n" +
                        "uniform float uSeed;\n" +
                        "float blob(vec2 p, vec2 c, float r) {\n" +
                        "    float d = distance(p, c);\n" +
                        "    return smoothstep(r, 0.0, d);\n" +
                        "}\n" +
                        "void main() {\n" +
                        "    float aspect = max(0.2, uResolution.x / max(1.0, uResolution.y));\n" +
                        "    vec2 p = vUv;\n" +
                        "    p.x = (p.x - 0.5) * aspect + 0.5;\n" +
                        "    float t = uTime * 0.060 + uSeed * 0.013;\n" +
                        "    vec3 col = vec3(0.0);\n" +
                        "    float a = 0.0;\n" +
                        "    vec2 c1 = vec2(0.22 + 0.14*sin(t*1.10), 0.28 + 0.10*cos(t*0.82));\n" +
                        "    vec2 c2 = vec2(0.78 + 0.16*cos(t*0.77), 0.22 + 0.12*sin(t*1.03));\n" +
                        "    vec2 c3 = vec2(0.42 + 0.18*sin(t*0.63), 0.76 + 0.11*cos(t*0.91));\n" +
                        "    vec2 c4 = vec2(0.66 + 0.13*cos(t*1.31), 0.58 + 0.14*sin(t*0.69));\n" +
                        "    vec2 c5 = vec2(0.14 + 0.11*sin(t*0.54), 0.82 + 0.12*cos(t*1.20));\n" +
                        "    vec2 c6 = vec2(0.92 + 0.08*cos(t*0.98), 0.70 + 0.10*sin(t*0.73));\n" +
                        "    float b1 = blob(p, c1, 0.62);\n" +
                        "    float b2 = blob(p, c2, 0.56);\n" +
                        "    float b3 = blob(p, c3, 0.68);\n" +
                        "    float b4 = blob(p, c4, 0.58);\n" +
                        "    float b5 = blob(p, c5, 0.54);\n" +
                        "    float b6 = blob(p, c6, 0.50);\n" +
                        "    col += vec3(0.00, 0.90, 1.00) * b1;\n" +
                        "    col += vec3(0.62, 0.20, 1.00) * b2;\n" +
                        "    col += vec3(1.00, 0.26, 0.82) * b3;\n" +
                        "    col += vec3(0.82, 0.70, 1.00) * b4;\n" +
                        "    col += vec3(0.24, 0.65, 1.00) * b5;\n" +
                        "    col += vec3(1.00, 1.00, 1.00) * b6 * 0.55;\n" +
                        "    a += b1 * 0.22 + b2 * 0.20 + b3 * 0.18 + b4 * 0.15 + b5 * 0.14 + b6 * 0.10;\n" +
                        "    float mist = 0.5 + 0.5*sin((p.x*3.2 + p.y*2.3 + t*0.75) * 3.14159);\n" +
                        "    col += vec3(0.55, 0.95, 1.00) * mist * 0.045;\n" +
                        "    a += mist * 0.030;\n" +
                        "    col = col / max(0.65, b1 + b2 + b3 + b4 + b5 + b6);\n" +
                        "    a = clamp(a, 0.0, 0.42);\n" +
                        "    gl_FragColor = vec4(col, a);\n" +
                        "}\n";

                int vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
                int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
                program = GLES20.glCreateProgram();
                GLES20.glAttachShader(program, vs);
                GLES20.glAttachShader(program, fs);
                GLES20.glLinkProgram(program);
                GLES20.glDeleteShader(vs);
                GLES20.glDeleteShader(fs);

                positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
                resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution");
                timeHandle = GLES20.glGetUniformLocation(program, "uTime");
                seedHandle = GLES20.glGetUniformLocation(program, "uSeed");

                GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            }

            private int compileShader(int type, String source) {
                int shader = GLES20.glCreateShader(type);
                GLES20.glShaderSource(shader, source);
                GLES20.glCompileShader(shader);
                return shader;
            }

            private void drawFrame() {
                int w = Math.max(1, width);
                int h = Math.max(1, height);
                float seconds = (System.currentTimeMillis() - startTime) / 1000.0f;

                GLES20.glViewport(0, 0, w, h);
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                GLES20.glUseProgram(program);
                GLES20.glUniform2f(resolutionHandle, (float) w, (float) h);
                GLES20.glUniform1f(timeHandle, seconds);
                GLES20.glUniform1f(seedHandle, seed);

                vertexBuffer.position(0);
                GLES20.glEnableVertexAttribArray(positionHandle);
                GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
                GLES20.glDisableVertexAttribArray(positionHandle);
            }

            private void releaseGlObjects() {
                try {
                    if (program != 0) {
                        GLES20.glDeleteProgram(program);
                        program = 0;
                    }
                } catch (Throwable ignored) {
                }
            }

            private void releaseEgl() {
                try {
                    if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                        if (eglSurface != EGL14.EGL_NO_SURFACE) {
                            EGL14.eglDestroySurface(eglDisplay, eglSurface);
                        }
                        if (eglContext != EGL14.EGL_NO_CONTEXT) {
                            EGL14.eglDestroyContext(eglDisplay, eglContext);
                        }
                        EGL14.eglTerminate(eglDisplay);
                    }
                } catch (Throwable ignored) {
                }
                eglDisplay = EGL14.EGL_NO_DISPLAY;
                eglSurface = EGL14.EGL_NO_SURFACE;
                eglContext = EGL14.EGL_NO_CONTEXT;
            }
        }
    }

    private class RainbowDrawingView extends View {
        private class TouchTrail {
            Path path;
            Paint paint;
            int colorIndex;
            long lastColorChange;
            ArrayList<SparkleParticle> sparkleParticles = new ArrayList<SparkleParticle>();
            ArrayList<SmokeParticle> smokeParticles = new ArrayList<SmokeParticle>();
            ArrayList<GlowParticle> glowParticles = new ArrayList<GlowParticle>();
            ArrayList<SpectralParticle> spectralParticles = new ArrayList<SpectralParticle>();
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

        private class SmokeParticle {
            float x, y, size;
            int alpha, color;
            float dx, dy;
            long creationTime;
        }

        private class SparkleParticle {
            float x, y, size;
            int alpha, color;
            float speed, angle;
            long creationTime;
        }

        private class GlowParticle {
            float x, y, size;
            int alpha, color;
            long creationTime;
            float lifeTime;
        }

        private class SpectralParticle {
            float x, y, size;
            int alpha, color;
            float speed, angle;
            long creationTime;
            float lifeTime;
        }

        private class Bubble {
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

        private class FreeSparkle {
            float x, y, size;
            int alpha, color;
            float speed, angle;
            long creationTime;
            float lifeTime;
        }

        private class FreeSmoke {
            float x, y, size;
            int alpha, color;
            float dx, dy;
            long creationTime;
            float lifeTime;
        }

        private class Ripple {
            float x, y;
            float radius;
            float maxRadius;
            int alpha;
            long creationTime;
        }

        private final SparseArray<TouchTrail> activeTrails = new SparseArray<TouchTrail>();
        private final ArrayList<TouchTrail> fadingTrails = new ArrayList<TouchTrail>();
        private final ArrayList<Bubble> bubbles = new ArrayList<Bubble>();
        private final ArrayList<FreeSparkle> freeSparkles = new ArrayList<FreeSparkle>();
        private final ArrayList<FreeSmoke> freeSmokes = new ArrayList<FreeSmoke>();
        private final ArrayList<Ripple> ripples = new ArrayList<Ripple>();
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
                Color.rgb(255, 80, 80),
                Color.rgb(255, 120, 90),
                Color.rgb(255, 160, 80),
                Color.rgb(255, 200, 80),
                Color.rgb(255, 230, 120),
                Color.rgb(180, 255, 120),
                Color.rgb(120, 255, 140),
                Color.rgb(80, 255, 180),
                Color.rgb(80, 255, 220),
                Color.rgb(80, 220, 255),
                Color.rgb(80, 180, 255),
                Color.rgb(100, 140, 255),
                Color.rgb(120, 120, 255),
                Color.rgb(150, 100, 255),
                Color.rgb(180, 90, 255),
                Color.rgb(210, 90, 255),
                Color.rgb(255, 90, 255),
                Color.rgb(255, 100, 220),
                Color.rgb(255, 120, 180),
                Color.rgb(255, 140, 160),
                Color.rgb(255, 180, 200),
                Color.rgb(255, 220, 240),
                Color.rgb(180, 240, 255),
                Color.rgb(220, 180, 255),
                Color.rgb(255, 255, 255)
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

        private float parallaxTargetX = 0f, parallaxTargetY = 0f;
        private float parallaxCurrentX = 0f, parallaxCurrentY = 0f;
        private boolean drawingActive = true;
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

        public void resetWallpaperMotion() {
            parallaxTargetX = 0.0f;
            parallaxTargetY = 0.0f;
            parallaxCurrentX = 0.0f;
            parallaxCurrentY = 0.0f;
        }

        @Override
        protected void onDetachedFromWindow() {
            drawingActive = false;
            activeTrails.clear();
            fadingTrails.clear();
            bubbles.clear();
            freeSparkles.clear();
            freeSmokes.clear();
            ripples.clear();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (!drawingActive) {
                return;
            }
            try {
                long currentTime = System.currentTimeMillis();
                float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
                lastUpdateTime = currentTime;

                parallaxCurrentX += (parallaxTargetX - parallaxCurrentX) * Math.min(1f, deltaTime * 10f);
                parallaxCurrentY += (parallaxTargetY - parallaxCurrentY) * Math.min(1f, deltaTime * 10f);

                if (Math.abs(parallaxCurrentX) < 0.05f) parallaxCurrentX = 0f;
                if (Math.abs(parallaxCurrentY) < 0.05f) parallaxCurrentY = 0f;

                MainActivity.this.setWallpaperParallax(parallaxCurrentX, parallaxCurrentY);

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

                if (drawingActive) {
                    if (activeTrails.size() > 0 || fadingTrails.size() > 0 || freeSparkles.size() > 0 || freeSmokes.size() > 0 || bubbles.size() > 0 || ripples.size() > 0) {
                        postInvalidateOnAnimation();
                    } else {
                        postInvalidateDelayed(80);
                    }
                }
            } catch (Throwable e) {
                if (drawingActive) {
                    postInvalidateDelayed(120);
                }
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
            b.x = 50 + random.nextFloat() * Math.max(1, getWidth() - 100);
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
            b.x = x;
            b.y = y;
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
            if (trail == null || trail.smokeParticles.size() > 60) return;
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
            if (trail == null || trail.sparkleParticles.size() > 60) return;
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
            if (trail == null || trail.glowParticles.size() > 40) return;
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
            if (trail == null || !trail.isColorTransitioning || trail.spectralParticles.size() > 60) return;
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
            if (alpha < 0) {
                alpha = 0;
            } else if (alpha > 255) {
                alpha = 255;
            }
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
                        popBubble(x, y, rainbowColors[trail.colorIndex]);
                        MainActivity.this.animateWallpaperZoom(true);

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
                            MainActivity.this.animateWallpaperZoom(false);
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
                        MainActivity.this.animateWallpaperZoom(false);
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
            PackageInfo packageInfo;
            Signature[] signatures;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo = pm.getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                SigningInfo signingInfo = packageInfo.signingInfo;
                if (signingInfo == null) return false;
                if (signingInfo.hasMultipleSigners()) {
                    signatures = signingInfo.getApkContentsSigners();
                } else {
                    signatures = signingInfo.getSigningCertificateHistory();
                }
            } else {
                packageInfo = pm.getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
                signatures = packageInfo.signatures;
            }

            if (signatures == null || signatures.length == 0) return false;

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < signatures.length; i++) {
                String value = bytesToHex(md.digest(signatures[i].toByteArray()));
                if ("e4201e2e32724c1ba1ef1100d35ff9f75c5d3e888a58c68b7747808f4c87607b".equalsIgnoreCase(value)) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private String bytesToHex(byte[] data) {
        char[] table = "0123456789abcdef".toCharArray();
        char[] out = new char[data.length * 2];
        for (int i = 0, j = 0; i < data.length; i++) {
            int v = data[i] & 0xff;
            out[j++] = table[v >>> 4];
            out[j++] = table[v & 15];
        }
        return new String(out);
    }

    @Override
    public void onBackPressed() {
        long now = System.currentTimeMillis();
        if (now - lastBackPressed < 1800) {
            super.onBackPressed();
            return;
        }
        lastBackPressed = now;
        showToast("Tap back again to exit");
    }

    @Override
    protected void onDestroy() {
        try {
            if (smokeTextureView != null) {
                smokeTextureView.pauseRenderer();
            }
        } catch (Throwable ignored) {
        }

        try {
            if (wallpaperLayer != null) {
                for (int i = 0; i < wallpaperLayer.getChildCount(); i++) {
                    View child = wallpaperLayer.getChildAt(i);
                    if (child instanceof FrameLayout) {
                        recycleWallpaperGroup((FrameLayout) child);
                    }
                }
                wallpaperLayer.removeAllViews();
            }
            oldWallpaperGroups.clear();
        } catch (Throwable ignored) {
        }

        super.onDestroy();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}