// Created by ridhoae303
// Telegram: @ridhoae303 — https://t.me/ridhoae303

package com.takane.app;

import android.app.Activity;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.media.MediaPlayer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TakaneActivity extends Activity {
    
    // Dialog lifecycle states
    private enum DialogState {
        OPENING, ACTIVE, CLOSING_MANUALLY, CLOSING_AUTO, CLOSED
    }
    private DialogState currentState = DialogState.OPENING;
    
    // Primary dialog view references
    private LinearLayout dialogContainer;
    private LinearLayout contentLayer;
    private ScrollView scrollView;
    private LinearLayout dialogContent;
    private ImageView logoImageView;
    private ImageView appIconImageView;
    private CheckBox dontShowCheckbox;
    private CheckBox timeFreezeCheckbox;
    private TextView timerText;
    private TextView moddedByText;
    private TextView takaneText;
    private TextView descText;
    private TextView welcomeText;
    private TextView packageText;
    private TextView versionText;
    private Button closeBtn;
    private Button waBtn;
    
    // Animation wrappers for checkboxes
    private LinearLayout dontShowWrapper;
    private LinearLayout timeFreezeWrapper;
    
    // Timer and handler system
    private CountDownTimer countDownTimer;
    private long timeRemainingMillis = 15000;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isCheckboxAutoTriggered = false;
    
    // Interactive element states
    private boolean isLogoZoomed = false;
    private boolean isAppIconZoomed = false;
    private Random random = new Random();
    private LinearLayout textInfoLayout;
    
    // Banner media rendering and animation
    private RelativeLayout bannerOuterContainer;
    private RelativeLayout bannerInnerContainer;
    private VideoView bannerVideoView;
    private MediaPlayer bannerVideoPlayer;
    private Movie bannerGifMovie;
    
    // Single instance enforcement
    private static boolean isDialogActive = false;
    
    // SharedPreferences keys for persistence
    private static final String PREF_NAME = "app_pref";
    private static final String DONT_SHOW_KEY = "dont_show";
    private static final String TIMER_REMAINING_KEY = "timer_remaining";
    private static final String IS_FROZEN_KEY = "is_frozen";
    
    // Asset management
    private static final String ASSET_ROOT = "ridhoae303/";
    
    // Root layout reference
    private RelativeLayout rootLayout;
    
    // Logo container for positioning
    private RelativeLayout logoContainer;
    
    // Timer freeze state management
    private boolean isTimerFrozen = false;
    
    // Custom font loading
    private Typeface customFont = null;
    
    // Freeze animation
    private ObjectAnimator freezeGlowAnimator;
    
    // Don't show checkbox text reference
    private TextView dontShowCheckboxText;
    
    // Scroll toast state
    private boolean hasShownScrollToast = false;
    
    // Custom ImageView for GIF rendering
    private class GifImageView extends ImageView {
        private Movie movie;
        private long movieStart;
        private float scale;
        private int measuredWidth, measuredHeight;

        public GifImageView(Context context) {
            super(context);
        }

        public void setMovie(Movie movie) {
            this.movie = movie;
            if (movie != null) {
                setImageBitmap(null);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            measuredWidth = getMeasuredWidth();
            measuredHeight = getMeasuredHeight();
            if (movie != null) {
                int movieWidth = movie.width();
                int movieHeight = movie.height();
                if (movieWidth > 0 && movieHeight > 0) {
                    scale = Math.min((float) measuredWidth / movieWidth, 
                                    (float) measuredHeight / movieHeight);
                } else {
                    scale = 1.0f;
                }
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            long now = android.os.SystemClock.uptimeMillis();
            if (movie != null) {
                if (movieStart == 0) {
                    movieStart = now;
                }
                int dur = movie.duration();
                if (dur == 0) {
                    dur = 1000;
                }
                int relTime = (int) ((now - movieStart) % dur);
                movie.setTime(relTime);
                
                canvas.save();
                canvas.scale(scale, scale);
                movie.draw(canvas, (measuredWidth / scale - movie.width()) / 2,
                          (measuredHeight / scale - movie.height()) / 2);
                canvas.restore();
                
                invalidate();
            }
        }
    }
    
    // Load custom font from assets with fallback
    private void loadCustomFont() {
        try {
            customFont = Typeface.createFromAsset(getAssets(), ASSET_ROOT + "ridhoae303.ttf");
        } catch (Exception e) {
            customFont = null;
        }
    }
    
    // Apply font to TextView with proper fallback handling
    private void applyFontToTextView(TextView textView, int style) {
        if (textView == null) return;
        if (customFont != null) {
            textView.setTypeface(customFont, style);
        } else {
            textView.setTypeface(Typeface.DEFAULT_BOLD, style);
        }
    }
    
    // Load logo media with multiple format support
    private void loadLogoMedia() {
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".mp4", ".webm", ".mov", ".avi"};
        String baseName = "logo";
        
        for (String ext : extensions) {
            try {
                String fullPath = ASSET_ROOT + baseName + ext;
                InputStream testStream = getAssets().open(fullPath);
                testStream.close();
                
                final String fileName = fullPath;
                
                if (ext.equals(".gif")) {
                    loadGifLogo(fileName);
                } else if (ext.equals(".mp4") || ext.equals(".webm") || ext.equals(".mov") || ext.equals(".avi")) {
                    loadVideoLogo(fileName);
                } else {
                    loadImageLogo(fileName);
                }
                return;
            } catch (Exception e) {
                continue;
            }
        }
        
        // Fallback gradient if no logo found
        GradientDrawable fallback = new GradientDrawable();
        fallback.setShape(GradientDrawable.OVAL);
        fallback.setColor(Color.parseColor("#6200EE"));
        if (logoImageView != null && logoImageView.getParent() != null) {
            logoImageView.setBackground(fallback);
        }
    }
    
    private void loadGifLogo(final String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            final Movie gifMovie = Movie.decodeStream(is);
            is.close();
            
            if (gifMovie != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (logoImageView == null || logoContainer == null || 
                            logoImageView.getParent() == null) return;
                        
                        GifImageView gifImageView = new GifImageView(TakaneActivity.this);
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            dpToPx(140), dpToPx(140)
                        );
                        params.addRule(RelativeLayout.CENTER_IN_PARENT);
                        gifImageView.setLayoutParams(params);
                        gifImageView.setMovie(gifMovie);
                        gifImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        
                        logoContainer.removeView(logoImageView);
                        logoImageView = gifImageView;
                        logoContainer.addView(logoImageView);
                        
                        applyOvalClip(logoImageView);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadVideoLogo(final String fileName) {
        try {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (logoContainer == null || logoImageView == null || 
                        logoImageView.getParent() == null) return;
                    
                    logoContainer.removeView(logoImageView);
                    
                    VideoView videoView = new VideoView(TakaneActivity.this);
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        dpToPx(140), dpToPx(140)
                    );
                    params.addRule(RelativeLayout.CENTER_IN_PARENT);
                    videoView.setLayoutParams(params);
                    
                    String filePath = "file:///android_asset/" + fileName;
                    videoView.setVideoURI(Uri.parse(filePath));
                    videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.setLooping(true);
                            mp.setVolume(0, 0);
                            mp.start();
                        }
                    });
                    
                    videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            return true;
                        }
                    });
                    
                    logoContainer.addView(videoView);
                    ImageView newLogoImageView = new ImageView(TakaneActivity.this);
                    newLogoImageView.setLayoutParams(params);
                    logoImageView = newLogoImageView;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadImageLogo(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap originalBitmap = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            
            if (logoImageView != null && logoImageView.getParent() != null) {
                logoImageView.setImageBitmap(originalBitmap);
                logoImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                applyOvalClip(logoImageView);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Load banner media from assets
    private void loadBannerMedia() {
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".mp4", ".webm", ".mov", ".avi"};
        String baseName = "banner";
        
        for (String ext : extensions) {
            try {
                String fullPath = ASSET_ROOT + baseName + ext;
                InputStream is = getAssets().open(fullPath);
                is.close();
                
                final String fileName = fullPath;
                
                if (ext.equals(".gif")) {
                    loadGifBanner(fileName);
                } else if (ext.equals(".mp4") || ext.equals(".webm") || ext.equals(".mov") || ext.equals(".avi")) {
                    loadVideoBanner(fileName);
                } else {
                    loadImageBanner(fileName);
                }
                return;
            } catch (Exception e) {
                continue;
            }
        }
    }
    
    private void loadGifBanner(final String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            bannerGifMovie = Movie.decodeStream(is);
            is.close();
            
            if (bannerGifMovie != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (bannerInnerContainer == null || bannerInnerContainer.getParent() == null) return;
                        
                        bannerInnerContainer.removeAllViews();
                        
                        GifImageView gifView = new GifImageView(TakaneActivity.this);
                        RelativeLayout.LayoutParams gifParams = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT
                        );
                        gifView.setLayoutParams(gifParams);
                        gifView.setMovie(bannerGifMovie);
                        gifView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        
                        bannerInnerContainer.addView(gifView);
                        
                        applyRoundedBanner(bannerOuterContainer, dpToPx(25));
                        applyRoundedBanner(bannerInnerContainer, dpToPx(23));
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadVideoBanner(final String fileName) {
        try {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (bannerInnerContainer == null || bannerInnerContainer.getParent() == null) return;
                    
                    bannerInnerContainer.removeAllViews();
                    
                    VideoView videoView = new VideoView(TakaneActivity.this);
                    RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT
                    );
                    videoView.setLayoutParams(videoParams);
                    
                    String filePath = "file:///android_asset/" + fileName;
                    videoView.setVideoURI(Uri.parse(filePath));
                    videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            bannerVideoPlayer = mp;
                            mp.setLooping(true);
                            mp.setVolume(0, 0);
                            mp.start();
                        }
                    });
                    
                    videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            return true;
                        }
                    });
                    
                    bannerInnerContainer.addView(videoView);
                    bannerVideoView = videoView;
                    
                    applyRoundedBanner(bannerOuterContainer, dpToPx(25));
                    applyRoundedBanner(bannerInnerContainer, dpToPx(23));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadImageBanner(final String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            final BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            final Bitmap originalBitmap = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (bannerInnerContainer == null || bannerInnerContainer.getParent() == null) return;
                    
                    bannerInnerContainer.removeAllViews();
                    
                    ImageView bannerImageView = new ImageView(TakaneActivity.this);
                    RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT
                    );
                    bannerImageView.setLayoutParams(imageParams);
                    bannerImageView.setImageBitmap(originalBitmap);
                    bannerImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    
                    bannerInnerContainer.addView(bannerImageView);
                    
                    applyRoundedBanner(bannerOuterContainer, dpToPx(25));
                    applyRoundedBanner(bannerInnerContainer, dpToPx(23));
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Apply rounded corners to banner containers with null safety
    private void applyRoundedBanner(final View view, final float radius) {
        if (view == null || view.getParent() == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    if (view.getParent() == null) return;
                    view.setClipToOutline(true);
                    view.setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View v, Outline outline) {
                            outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
                        }
                    });
                }
            });
        }
        
        if (view.getBackground() instanceof GradientDrawable) {
            GradientDrawable bg = (GradientDrawable) view.getBackground();
            bg.setCornerRadius(radius);
        }
    }
    
    // Apply oval clip for circular views with null safety
    private void applyOvalClip(final View view) {
        if (view == null || view.getParent() == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    if (view.getParent() == null) return;
                    view.setClipToOutline(true);
                    view.setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View v, Outline outline) {
                            outline.setOval(0, 0, v.getWidth(), v.getHeight());
                        }
                    });
                }
            });
        }
    }
    
    // Logo click animation with null safety
    private void animateLogoClick() {
        if (currentState != DialogState.ACTIVE || logoImageView == null || logoImageView.getParent() == null) return;
        
        logoImageView.animate().cancel();
        logoImageView.setHasTransientState(true);
        logoImageView.setRotation(0f);
        
        float targetScale = isLogoZoomed ? 1.0f : 1.15f;
        isLogoZoomed = !isLogoZoomed;
        
        logoImageView.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(280)
            .setInterpolator(new DecelerateInterpolator())
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (logoImageView != null) {
                        logoImageView.setHasTransientState(false);
                    }
                }
            })
            .start();
        
        logoImageView.animate()
            .rotation(8f)
            .setDuration(100)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (logoImageView != null) {
                        logoImageView.animate()
                            .rotation(-8f)
                            .setDuration(120)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    if (logoImageView != null) {
                                        logoImageView.animate()
                                            .rotation(0f)
                                            .setDuration(100)
                                            .start();
                                    }
                                }
                            })
                            .start();
                    }
                }
            })
            .start();
    }
    
    // Banner click animation with null safety
    private void animateBannerClick() {
        if (currentState != DialogState.ACTIVE || bannerOuterContainer == null || bannerOuterContainer.getParent() == null) return;
        
        bannerOuterContainer.animate().cancel();
        bannerOuterContainer.setHasTransientState(true);
        bannerOuterContainer.setRotation(0f);
        bannerOuterContainer.setScaleX(1f);
        bannerOuterContainer.setScaleY(1f);
        
        bannerOuterContainer.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(100)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (bannerOuterContainer != null) {
                        bannerOuterContainer.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(140)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    if (bannerOuterContainer != null) {
                                        bannerOuterContainer.setHasTransientState(false);
                                    }
                                }
                            })
                            .start();
                    }
                }
            })
            .start();
        
        bannerOuterContainer.animate()
            .rotation(4f)
            .setDuration(90)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (bannerOuterContainer != null) {
                        bannerOuterContainer.animate()
                            .rotation(-4f)
                            .setDuration(120)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    if (bannerOuterContainer != null) {
                                        bannerOuterContainer.animate()
                                            .rotation(0f)
                                            .setDuration(90)
                                            .start();
                                    }
                                }
                            })
                            .start();
                    }
                }
            })
            .start();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Prevent multiple instances
        if (isDialogActive) {
            finish();
            return;
        }
        isDialogActive = true;
        
        // Load custom font early
        loadCustomFont();
        
        // Setup window flags
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, 
                           WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, 
                           WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_FULLSCREEN);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.BLACK);
        }
        
        // Setup root layout
        rootLayout = new RelativeLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootLayout.setBackgroundColor(Color.TRANSPARENT);
        rootLayout.setClipChildren(true);
        
        // Add invisible view to ensure layout is measured
        View tempView = new View(this);
        tempView.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
        tempView.setVisibility(View.INVISIBLE);
        rootLayout.addView(tempView);
        
        setContentView(rootLayout);
        
        // Initialize dialog after layout is ready
        rootLayout.post(new Runnable() {
            @Override
            public void run() {
                initializeDialog();
            }
        });
    }
    
    private void initializeDialog() {
        // Re-enable touch
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        
        // Setup root layout background
        if (rootLayout == null) {
            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            if (rootView instanceof ViewGroup) {
                rootLayout = new RelativeLayout(this);
                rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));
                ((ViewGroup) rootView).removeAllViews();
                ((ViewGroup) rootView).addView(rootLayout);
            } else {
                rootLayout = new RelativeLayout(this);
                setContentView(rootLayout);
            }
        } else {
            rootLayout.removeAllViews();
        }
        
        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setColor(Color.argb(230, 0, 0, 0));
        rootLayout.setBackground(bgDrawable);
        rootLayout.setClipChildren(true);
        rootLayout.setClipToPadding(false);
        
        // Content layer for slide animation
        contentLayer = new LinearLayout(this);
        RelativeLayout.LayoutParams contentParams = new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        contentParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        contentParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
        contentLayer.setLayoutParams(contentParams);
        contentLayer.setOrientation(LinearLayout.VERTICAL);
        contentLayer.setClipChildren(true);
        contentLayer.setClipToPadding(false);
        contentLayer.setElevation(25f);
        
        // Main dialog container
        dialogContainer = new LinearLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            dpToPx(420),
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.gravity = Gravity.CENTER_HORIZONTAL;
        dialogContainer.setLayoutParams(containerParams);
        dialogContainer.setOrientation(LinearLayout.VERTICAL);
        dialogContainer.setMinimumHeight(dpToPx(380));
        
        // Initial state for entrance animation
        dialogContainer.setAlpha(0f);
        dialogContainer.setScaleX(0.85f);
        dialogContainer.setScaleY(0.85f);
        
        // Dialog background
        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setColor(Color.parseColor("#1A1A2E"));
        dialogBg.setCornerRadius(dpToPx(25));
        dialogBg.setStroke(dpToPx(3), Color.parseColor("#00ADB5"));
        dialogContainer.setBackground(dialogBg);
        dialogContainer.setClipChildren(true);
        dialogContainer.setClipToPadding(false);
        
        // Clip outline for rounded corners on API 21+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialogContainer.setClipToOutline(true);
            dialogContainer.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(25));
                }
            });
        }
        
        // Scrollable content area with safe clipping
        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        ));
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setClipChildren(false);  // Fix for banner clipping
        scrollView.setClipToPadding(false); // Allow content to draw outside padding
        
        // Dialog content container
        dialogContent = new LinearLayout(this);
        dialogContent.setLayoutParams(new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT,
            ScrollView.LayoutParams.WRAP_CONTENT
        ));
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setPadding(dpToPx(25), dpToPx(30), dpToPx(25), dpToPx(10));
        dialogContent.setClipChildren(false);  // Fix for banner clipping
        dialogContent.setClipToPadding(false); // Allow content to draw outside padding
        
        // Header layout with banner and logo
        RelativeLayout headerLayout = new RelativeLayout(this);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        headerLayout.setClipChildren(false);
        headerLayout.setClipToPadding(false);
        
        // Banner container
        bannerOuterContainer = new RelativeLayout(this);
        RelativeLayout.LayoutParams bannerParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, dpToPx(180)
        );
        bannerParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        bannerOuterContainer.setLayoutParams(bannerParams);
        bannerOuterContainer.setClipChildren(false);
        bannerOuterContainer.setClipToPadding(false);
        bannerOuterContainer.setAlpha(0f);
        
        // Banner border
        GradientDrawable bannerBorder = new GradientDrawable();
        bannerBorder.setShape(GradientDrawable.RECTANGLE);
        bannerBorder.setStroke(dpToPx(2), Color.parseColor("#FFD700"));
        bannerBorder.setColor(Color.TRANSPARENT);
        bannerBorder.setCornerRadius(dpToPx(25));
        bannerOuterContainer.setBackground(bannerBorder);
        
        // Inner banner container for media
        bannerInnerContainer = new RelativeLayout(this);
        RelativeLayout.LayoutParams innerParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        );
        innerParams.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        bannerInnerContainer.setLayoutParams(innerParams);
        bannerInnerContainer.setClipChildren(true);
        bannerInnerContainer.setClipToPadding(true);
        bannerOuterContainer.addView(bannerInnerContainer);
        
        // Logo container
        logoContainer = new RelativeLayout(this);
        logoContainer.setId(View.generateViewId());
        RelativeLayout.LayoutParams logoContainerParams = new RelativeLayout.LayoutParams(
            dpToPx(150), dpToPx(150)
        );
        logoContainerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        logoContainerParams.topMargin = dpToPx(95);
        logoContainer.setLayoutParams(logoContainerParams);
        logoContainer.setClipChildren(false);
        
        // Logo image view
        logoImageView = new ImageView(this);
        RelativeLayout.LayoutParams logoParams = new RelativeLayout.LayoutParams(
            dpToPx(140), dpToPx(140)
        );
        logoParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        logoImageView.setLayoutParams(logoParams);
        logoImageView.setAlpha(0f);
        logoImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        // Logo border
        RelativeLayout logoBorderContainer = new RelativeLayout(this);
        RelativeLayout.LayoutParams logoBorderParams = new RelativeLayout.LayoutParams(
            dpToPx(150), dpToPx(150)
        );
        logoBorderParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        logoBorderContainer.setLayoutParams(logoBorderParams);

        GradientDrawable logoBorder = new GradientDrawable();
        logoBorder.setShape(GradientDrawable.OVAL);
        logoBorder.setStroke(dpToPx(3), Color.parseColor("#00ADB5"));
        logoBorder.setColor(Color.TRANSPARENT);
        logoBorderContainer.setBackground(logoBorder);
        
        logoContainer.addView(logoBorderContainer);
        logoContainer.addView(logoImageView);
        
        // App info layout
        LinearLayout appInfoLayout = new LinearLayout(this);
        RelativeLayout.LayoutParams appInfoParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        appInfoParams.addRule(RelativeLayout.BELOW, logoContainer.getId());
        appInfoParams.topMargin = dpToPx(20);
        appInfoLayout.setLayoutParams(appInfoParams);
        appInfoLayout.setOrientation(LinearLayout.HORIZONTAL);
        appInfoLayout.setGravity(Gravity.CENTER);
        appInfoLayout.setPadding(0, dpToPx(20), 0, dpToPx(20));
        appInfoLayout.setClipChildren(false);
        
        // App icon container
        final RelativeLayout appIconContainer = new RelativeLayout(this);
        LinearLayout.LayoutParams appIconContainerParams = new LinearLayout.LayoutParams(
            dpToPx(70), dpToPx(70)
        );
        appIconContainerParams.rightMargin = dpToPx(15);
        appIconContainer.setLayoutParams(appIconContainerParams);
        appIconContainer.setClipChildren(false);
        
        // App icon image
        appIconImageView = new ImageView(this);
        RelativeLayout.LayoutParams appIconParams = new RelativeLayout.LayoutParams(
            dpToPx(58), dpToPx(58)
        );
        appIconParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        appIconImageView.setLayoutParams(appIconParams);
        appIconImageView.setAlpha(0f);
        
        try {
            android.graphics.drawable.Drawable appIcon = getPackageManager().getApplicationIcon(getPackageName());
            appIconImageView.setImageDrawable(appIcon);
            appIconImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            applyOvalClip(appIconImageView);
        } catch (Exception e) {
            GradientDrawable fallback = new GradientDrawable();
            fallback.setShape(GradientDrawable.OVAL);
            fallback.setColor(Color.parseColor("#00ADB5"));
            appIconImageView.setBackground(fallback);
        }
        
        // App icon border
        GradientDrawable appIconBorder = new GradientDrawable();
        appIconBorder.setShape(GradientDrawable.OVAL);
        appIconBorder.setStroke(dpToPx(3), Color.parseColor("#FF6B9D"));
        appIconBorder.setColor(Color.TRANSPARENT);
        appIconContainer.setBackground(appIconBorder);
        appIconContainer.addView(appIconImageView);
        
        // Text info layout
        textInfoLayout = new LinearLayout(this);
        textInfoLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textInfoLayout.setOrientation(LinearLayout.VERTICAL);
        textInfoLayout.setAlpha(0f);
        
        // Welcome text
        welcomeText = new TextView(this);
        welcomeText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        welcomeText.setText("Welcome to ");
        welcomeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        welcomeText.setTextColor(Color.WHITE);
        welcomeText.setGravity(Gravity.CENTER);
        applyFontToTextView(welcomeText, Typeface.BOLD);
        welcomeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentState != DialogState.ACTIVE) return;
                Toast.makeText(TakaneActivity.this, getAppName(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Package text
        packageText = new TextView(this);
        packageText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        packageText.setText("");
        packageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        packageText.setTextColor(Color.parseColor("#888888"));
        packageText.setGravity(Gravity.CENTER);
        packageText.setPadding(0, dpToPx(2), 0, 0);
        applyFontToTextView(packageText, Typeface.NORMAL);
        packageText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentState != DialogState.ACTIVE) return;
                String pkg = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
                }
            }
        });
        
        // Version text
        versionText = new TextView(this);
        versionText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        versionText.setText("Version ");
        versionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        versionText.setTextColor(Color.parseColor("#00ADB5"));
        versionText.setGravity(Gravity.CENTER);
        applyFontToTextView(versionText, Typeface.BOLD);
        versionText.setPadding(0, dpToPx(4), 0, 0);
        versionText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentState != DialogState.ACTIVE) return;
                Toast.makeText(TakaneActivity.this, "App Version: " + getAppVersion(), Toast.LENGTH_SHORT).show();
            }
        });
        
        textInfoLayout.addView(welcomeText);
        textInfoLayout.addView(packageText);
        textInfoLayout.addView(versionText);
        
        appInfoLayout.addView(appIconContainer);
        appInfoLayout.addView(textInfoLayout);
        
        // Click listeners
        View.OnClickListener logoClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentState != DialogState.ACTIVE) return;
                animateLogoClick();
            }
        };
        
        View.OnClickListener bannerClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentState != DialogState.ACTIVE) return;
                animateBannerClick();
            }
        };
        
        View.OnClickListener appIconClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentState != DialogState.ACTIVE) return;
                
                if (appIconImageView == null || appIconContainer == null) return;
                
                appIconImageView.animate().cancel();
                appIconContainer.animate().cancel();
                appIconImageView.setHasTransientState(true);
                appIconContainer.setHasTransientState(true);
                
                float targetScale = isAppIconZoomed ? 1.0f : 1.15f;
                isAppIconZoomed = !isAppIconZoomed;
                
                appIconImageView.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (appIconImageView != null) {
                                appIconImageView.setHasTransientState(false);
                            }
                        }
                    })
                    .start();
                
                appIconContainer.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(120)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (appIconContainer != null) {
                                appIconContainer.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(180)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (appIconContainer != null) {
                                                appIconContainer.setHasTransientState(false);
                                            }
                                        }
                                    })
                                    .start();
                            }
                        }
                    })
                    .start();
            }
        };
        
        logoImageView.setOnClickListener(logoClickListener);
        logoBorderContainer.setOnClickListener(logoClickListener);
        bannerOuterContainer.setOnClickListener(bannerClickListener);
        appIconContainer.setOnClickListener(appIconClickListener);
        appIconImageView.setOnClickListener(appIconClickListener);
        
        // Divider
        View divider = new View(this);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(2)
        );
        dividerParams.setMargins(0, dpToPx(10), 0, dpToPx(10));
        divider.setLayoutParams(dividerParams);
        
        GradientDrawable dividerBg = new GradientDrawable();
        dividerBg.setColor(Color.parseColor("#00ADB5"));
        dividerBg.setCornerRadius(dpToPx(1));
        divider.setBackground(dividerBg);
        
        // Modded by text
        moddedByText = new TextView(this);
        LinearLayout.LayoutParams moddedParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        moddedParams.gravity = Gravity.CENTER;
        moddedParams.topMargin = dpToPx(15);
        moddedByText.setLayoutParams(moddedParams);
        moddedByText.setText("Modded by ridhoae303 👻");
        moddedByText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        moddedByText.setTextColor(Color.parseColor("#00FFFF"));
        applyFontToTextView(moddedByText, Typeface.BOLD);
        moddedByText.setShadowLayer(dpToPx(1), 0, 0, Color.parseColor("#0097A7"));
        moddedByText.setAlpha(0f);
        
        moddedByText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (currentState != DialogState.ACTIVE || v == null) return;
                
                v.animate().cancel();
                v.setHasTransientState(true);
                v.setScaleX(1f);
                v.setScaleY(1f);
                
                v.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(120)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (v != null) {
                                v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(180)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (v != null) {
                                                v.setHasTransientState(false);
                                            }
                                        }
                                    })
                                    .start();
                            }
                        }
                    })
                    .start();
                
                final int originalColor = Color.parseColor("#00FFFF");
                int tappedColor = Color.parseColor("#9C27B0");
                moddedByText.setTextColor(tappedColor);
                moddedByText.setShadowLayer(dpToPx(3), 0, 0, tappedColor);
                
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (moddedByText != null) {
                            moddedByText.setTextColor(originalColor);
                            moddedByText.setShadowLayer(dpToPx(1), 0, 0, Color.parseColor("#0097A7"));
                        }
                    }
                }, 300);
                
                Toast.makeText(TakaneActivity.this, "Modded with ❤️ by ridhoae303", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Takane text
        takaneText = new TextView(this);
        LinearLayout.LayoutParams takaneParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        takaneParams.gravity = Gravity.CENTER;
        takaneParams.topMargin = dpToPx(6);
        takaneParams.bottomMargin = dpToPx(10);
        takaneText.setLayoutParams(takaneParams);
        takaneText.setText("Miyoshi Takane best girl 💕");
        takaneText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        takaneText.setTextColor(Color.parseColor("#FF5C8D"));
        takaneText.setGravity(Gravity.CENTER);
        applyFontToTextView(takaneText, Typeface.NORMAL);
        takaneText.setAlpha(0f);
        
        takaneText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (currentState != DialogState.ACTIVE || v == null) return;
                
                v.animate().cancel();
                v.setHasTransientState(true);
                v.setScaleX(1f);
                v.setScaleY(1f);
                
                v.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(100)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (v != null) {
                                v.animate()
                                    .scaleX(1.08f)
                                    .scaleY(1.08f)
                                    .setDuration(120)
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (v != null) {
                                                v.animate()
                                                    .scaleX(1f)
                                                    .scaleY(1f)
                                                    .setDuration(150)
                                                    .setInterpolator(new DecelerateInterpolator())
                                                    .withEndAction(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (v != null) {
                                                                v.setHasTransientState(false);
                                                            }
                                                        }
                                                    })
                                                    .start();
                                            }
                                        }
                                    })
                                    .start();
                            }
                        }
                    })
                    .start();
                
                GradientDrawable glow = new GradientDrawable();
                glow.setCornerRadius(dpToPx(16));
                glow.setColor(Color.parseColor("#55FF5C8D"));
                glow.setStroke(dpToPx(2), Color.parseColor("#FF5C8D"));
                takaneText.setBackground(glow);
                
                takaneText.animate()
                    .alpha(0.7f)
                    .setDuration(150)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (takaneText != null) {
                                takaneText.animate()
                                    .alpha(1f)
                                    .setDuration(250)
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (takaneText != null) {
                                                takaneText.setBackground(null);
                                            }
                                        }
                                    })
                                    .start();
                            }
                        }
                    })
                    .start();
                
                Toast.makeText(TakaneActivity.this, "タカネ 💕", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Description text
        descText = new TextView(this);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.gravity = Gravity.CENTER;
        descParams.topMargin = dpToPx(15);
        descParams.bottomMargin = dpToPx(25);
        descText.setLayoutParams(descParams);
        descText.setText("Join our WhatsApp Community to get mods and other updates.");
        descText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        descText.setTextColor(Color.parseColor("#FFA500"));
        descText.setGravity(Gravity.CENTER);
        applyFontToTextView(descText, Typeface.NORMAL);
        descText.setMaxWidth(dpToPx(350));
        descText.setLineSpacing(dpToPx(4), 1.2f);
        descText.setAlpha(0f);
        
        descText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (currentState != DialogState.ACTIVE || v == null) return;
                
                v.animate().cancel();
                v.setHasTransientState(true);
                v.setScaleX(1f);
                v.setScaleY(1f);
                
                v.animate()
                    .translationY(-dpToPx(3))
                    .setDuration(150)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (v != null) {
                                v.animate()
                                    .translationY(dpToPx(3))
                                    .setDuration(150)
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (v != null) {
                                                v.animate()
                                                    .translationY(0)
                                                    .setDuration(150)
                                                    .withEndAction(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (v != null) {
                                                                v.setHasTransientState(false);
                                                            }
                                                        }
                                                    })
                                                    .start();
                                            }
                                        }
                                    })
                                    .start();
                            }
                        }
                    })
                    .start();
                
                final int originalColor = Color.parseColor("#FFA500");
                final int tappedColor = Color.parseColor("#4CAF50");
                
                ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), originalColor, tappedColor);
                colorAnim.setDuration(300);
                colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        if (descText != null) {
                            descText.setTextColor((int) animator.getAnimatedValue());
                        }
                    }
                });
                colorAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (descText != null) {
                            descText.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (descText != null) {
                                        descText.setTextColor(originalColor);
                                    }
                                }
                            }, 200);
                        }
                    }
                });
                colorAnim.start();
                
                Toast.makeText(TakaneActivity.this, "Let's join the community! 🚀", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Checkbox and timer layout
        LinearLayout checkboxTimerLayout = new LinearLayout(this);
        checkboxTimerLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        checkboxTimerLayout.setOrientation(LinearLayout.VERTICAL);
        checkboxTimerLayout.setGravity(Gravity.END);
        checkboxTimerLayout.setPadding(0, dpToPx(10), 0, dpToPx(20));
        
        LinearLayout timerLayout = new LinearLayout(this);
        timerLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        timerLayout.setOrientation(LinearLayout.HORIZONTAL);
        timerLayout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        timerLayout.setPadding(0, 0, 0, dpToPx(10));
        
        timerText = new TextView(this);
        LinearLayout.LayoutParams timerTextParams = new LinearLayout.LayoutParams(
            dpToPx(70), LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timerText.setLayoutParams(timerTextParams);
        timerText.setText("15s");
        timerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        timerText.setTextColor(Color.parseColor("#FFD700"));
        applyFontToTextView(timerText, Typeface.BOLD);
        timerText.setGravity(Gravity.CENTER);
        timerLayout.addView(timerText);
        
        LinearLayout checkboxesContainer = new LinearLayout(this);
        checkboxesContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        checkboxesContainer.setOrientation(LinearLayout.VERTICAL);
        checkboxesContainer.setGravity(Gravity.END);
        checkboxesContainer.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        checkboxesContainer.setClipChildren(false);
        checkboxesContainer.setClipToPadding(false);
        
        final LinearLayout dontShowContainer = new LinearLayout(this);
        dontShowContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        dontShowContainer.setOrientation(LinearLayout.HORIZONTAL);
        dontShowContainer.setGravity(Gravity.CENTER_VERTICAL);
        dontShowContainer.setPadding(0, 0, 0, dpToPx(8));
        dontShowContainer.setClipChildren(false);
        
        dontShowWrapper = new LinearLayout(this);
        dontShowWrapper.setLayoutParams(new LinearLayout.LayoutParams(
            dpToPx(36), dpToPx(36)
        ));
        dontShowWrapper.setGravity(Gravity.CENTER);
        dontShowWrapper.setClipChildren(false);
        dontShowWrapper.setClipToPadding(false);
        
        dontShowCheckbox = new CheckBox(this);
        LinearLayout.LayoutParams checkboxParams = new LinearLayout.LayoutParams(
            dpToPx(24), dpToPx(24)
        );
        dontShowCheckbox.setLayoutParams(checkboxParams);
        dontShowCheckbox.setButtonDrawable(null);
        
        GradientDrawable uncheckedBg = new GradientDrawable();
        uncheckedBg.setShape(GradientDrawable.OVAL);
        uncheckedBg.setSize(dpToPx(20), dpToPx(20));
        uncheckedBg.setStroke(dpToPx(2), Color.parseColor("#00ADB5"));
        uncheckedBg.setColor(Color.TRANSPARENT);
        
        GradientDrawable checkedBg = new GradientDrawable();
        checkedBg.setShape(GradientDrawable.OVAL);
        checkedBg.setSize(dpToPx(20), dpToPx(20));
        checkedBg.setStroke(dpToPx(2), Color.parseColor("#00ADB5"));
        checkedBg.setColor(Color.parseColor("#00ADB5"));
        
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_checked}, checkedBg);
        states.addState(new int[]{}, uncheckedBg);
        
        dontShowCheckbox.setBackground(states);
        dontShowWrapper.addView(dontShowCheckbox);
        
        dontShowCheckboxText = new TextView(this);
        dontShowCheckboxText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        dontShowCheckboxText.setText("Don't show again");
        dontShowCheckboxText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        dontShowCheckboxText.setTextColor(Color.parseColor("#CCCCCC"));
        applyFontToTextView(dontShowCheckboxText, Typeface.NORMAL);
        
        dontShowContainer.addView(dontShowWrapper);
        dontShowContainer.addView(dontShowCheckboxText);
        
        final LinearLayout timeFreezeContainer = new LinearLayout(this);
        timeFreezeContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        timeFreezeContainer.setOrientation(LinearLayout.HORIZONTAL);
        timeFreezeContainer.setGravity(Gravity.CENTER_VERTICAL);
        timeFreezeContainer.setClipChildren(false);
        
        timeFreezeWrapper = new LinearLayout(this);
        timeFreezeWrapper.setLayoutParams(new LinearLayout.LayoutParams(
            dpToPx(36), dpToPx(36)
        ));
        timeFreezeWrapper.setGravity(Gravity.CENTER);
        timeFreezeWrapper.setClipChildren(false);
        timeFreezeWrapper.setClipToPadding(false);
        
        timeFreezeCheckbox = new CheckBox(this);
        LinearLayout.LayoutParams timeFreezeParams = new LinearLayout.LayoutParams(
            dpToPx(24), dpToPx(24)
        );
        timeFreezeCheckbox.setLayoutParams(timeFreezeParams);
        timeFreezeCheckbox.setButtonDrawable(null);
        
        GradientDrawable timeFreezeUncheckedBg = new GradientDrawable();
        timeFreezeUncheckedBg.setShape(GradientDrawable.OVAL);
        timeFreezeUncheckedBg.setSize(dpToPx(20), dpToPx(20));
        timeFreezeUncheckedBg.setStroke(dpToPx(2), Color.parseColor("#FFD700"));
        timeFreezeUncheckedBg.setColor(Color.TRANSPARENT);
        
        GradientDrawable timeFreezeCheckedBg = new GradientDrawable();
        timeFreezeCheckedBg.setShape(GradientDrawable.OVAL);
        timeFreezeCheckedBg.setSize(dpToPx(20), dpToPx(20));
        timeFreezeCheckedBg.setStroke(dpToPx(2), Color.parseColor("#FFD700"));
        timeFreezeCheckedBg.setColor(Color.parseColor("#FFD700"));
        
        android.graphics.drawable.StateListDrawable timeFreezeStates = new android.graphics.drawable.StateListDrawable();
        timeFreezeStates.addState(new int[]{android.R.attr.state_checked}, timeFreezeCheckedBg);
        timeFreezeStates.addState(new int[]{}, timeFreezeUncheckedBg);
        
        timeFreezeCheckbox.setBackground(timeFreezeStates);
        timeFreezeWrapper.addView(timeFreezeCheckbox);
        
        final TextView timeFreezeText = new TextView(this);
        timeFreezeText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        timeFreezeText.setText("Freeze Time");
        timeFreezeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        timeFreezeText.setTextColor(Color.parseColor("#CCCCCC"));
        applyFontToTextView(timeFreezeText, Typeface.NORMAL);
        
        timeFreezeContainer.addView(timeFreezeWrapper);
        timeFreezeContainer.addView(timeFreezeText);
        
        checkboxesContainer.addView(dontShowContainer);
        checkboxesContainer.addView(timeFreezeContainer);
        checkboxTimerLayout.addView(timerLayout);
        checkboxTimerLayout.addView(checkboxesContainer);
        
        View.OnClickListener checkboxAreaClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentState != DialogState.ACTIVE) return;
                
                if (v == dontShowContainer || v == dontShowCheckboxText) {
                    boolean newState = !dontShowCheckbox.isChecked();
                    dontShowCheckbox.setChecked(newState);
                    animateCheckboxWrapper(dontShowWrapper);
                } else if (v == timeFreezeContainer || v == timeFreezeText) {
                    boolean newState = !timeFreezeCheckbox.isChecked();
                    timeFreezeCheckbox.setChecked(newState);
                    animateCheckboxWrapper(timeFreezeWrapper);
                }
            }
        };
        
        dontShowContainer.setOnClickListener(checkboxAreaClickListener);
        dontShowCheckboxText.setOnClickListener(checkboxAreaClickListener);
        timeFreezeContainer.setOnClickListener(checkboxAreaClickListener);
        timeFreezeText.setOnClickListener(checkboxAreaClickListener);
        
        // Button container
        LinearLayout buttonContainer = new LinearLayout(this);
        LinearLayout.LayoutParams buttonContainerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonContainerParams.topMargin = dpToPx(10);
        buttonContainer.setLayoutParams(buttonContainerParams);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setWeightSum(2);
        buttonContainer.setPadding(dpToPx(25), 0, dpToPx(25), dpToPx(25));
        
        closeBtn = new Button(this);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        closeParams.rightMargin = dpToPx(10);
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setText("CLOSE");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setBackground(createButtonBg(false));
        closeBtn.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16));
        applyFontToTextView(closeBtn, Typeface.BOLD);
        
        waBtn = new Button(this);
        LinearLayout.LayoutParams waParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        waParams.leftMargin = dpToPx(10);
        waBtn.setLayoutParams(waParams);
        waBtn.setText("WHATSAPP");
        waBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        waBtn.setTextColor(Color.WHITE);
        waBtn.setBackground(createButtonBg(true));
        waBtn.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16));
        applyFontToTextView(waBtn, Typeface.BOLD);
        
        // Assemble views
        headerLayout.addView(bannerOuterContainer);
        headerLayout.addView(logoContainer);
        headerLayout.addView(appInfoLayout);
        
        buttonContainer.addView(closeBtn);
        buttonContainer.addView(waBtn);
        
        dialogContent.addView(headerLayout);
        dialogContent.addView(divider);
        dialogContent.addView(moddedByText);
        dialogContent.addView(takaneText);
        dialogContent.addView(descText);
        dialogContent.addView(checkboxTimerLayout);
        
        scrollView.addView(dialogContent);
        dialogContainer.addView(scrollView);
        dialogContainer.addView(buttonContainer);
        contentLayer.addView(dialogContainer);
        rootLayout.addView(contentLayer);
        
        // Load media
        loadLogoMedia();
        loadBannerMedia();
        ensureClipSystem();
        
        // Start entrance animation
        dialogContainer.post(new Runnable() {
            @Override
            public void run() {
                startSmoothAnimation();
            }
        });
    }
    
    @Override
    protected void onStart() {
        super.onStart();
    }
    
    private void animateCheckboxWrapper(final LinearLayout wrapper) {
        if (currentState != DialogState.ACTIVE || wrapper == null || wrapper.getParent() == null) return;
        
        wrapper.animate().cancel();
        wrapper.setHasTransientState(true);
        
        wrapper.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
            .setDuration(150)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (wrapper != null) {
                        wrapper.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    if (wrapper != null) {
                                        wrapper.setHasTransientState(false);
                                    }
                                }
                            })
                            .start();
                    }
                }
            })
            .start();
    }
    
    // Optimized entrance animation with centralized timeline
    private void startSmoothAnimation() {
        final int displayHeight = getDisplayHeight();
        contentLayer.setTranslationY(displayHeight);
        
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dialogContainer == null || contentLayer == null) return;
                
                // Use AnimatorSet for coordinated animation
                AnimatorSet entranceSet = new AnimatorSet();
                List<Animator> animators = new ArrayList<>();
                
                // Dialog container animations
                ObjectAnimator dialogAlpha = ObjectAnimator.ofFloat(dialogContainer, "alpha", 0f, 1f);
                ObjectAnimator dialogScaleX = ObjectAnimator.ofFloat(dialogContainer, "scaleX", 0.85f, 1f);
                ObjectAnimator dialogScaleY = ObjectAnimator.ofFloat(dialogContainer, "scaleY", 0.85f, 1f);
                
                animators.add(dialogAlpha);
                animators.add(dialogScaleX);
                animators.add(dialogScaleY);
                
                // Content layer slide up
                ObjectAnimator slideUp = ObjectAnimator.ofFloat(contentLayer, "translationY", displayHeight, 0);
                slideUp.setDuration(550);
                slideUp.setInterpolator(new DecelerateInterpolator(1.5f));
                animators.add(slideUp);
                
                // Play all animations together
                entranceSet.playTogether(animators);
                entranceSet.setDuration(450);
                entranceSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        startLogoAndBannerAnimations();
                    }
                    
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        currentState = DialogState.ACTIVE;
                        startTextAnimations();
                        startMetadataAnimations();
                        setupListeners();
                        loadPreferences();
                        // Start timer only if not frozen
                        if (!isTimerFrozen) {
                            startAutoCloseTimer(timeRemainingMillis);
                        }
                        
                        if (!hasShownScrollToast) {
                            int orientation = getResources().getConfiguration().orientation;
                            if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                                Toast.makeText(TakaneActivity.this, 
                                    "You can scroll the dialog to see more content.", 
                                    Toast.LENGTH_LONG).show();
                                hasShownScrollToast = true;
                            }
                        }
                    }
                });
                entranceSet.start();
            }
        }, 100);
    }
    
    private void startLogoAndBannerAnimations() {
        // Logo animation
        if (logoImageView != null && logoImageView.getParent() != null) {
            logoImageView.setAlpha(1f);
            logoImageView.setScaleX(0.5f);
            logoImageView.setScaleY(0.5f);
            logoImageView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }
        
        // App icon animation
        if (appIconImageView != null && appIconImageView.getParent() != null) {
            appIconImageView.setAlpha(1f);
            appIconImageView.setScaleX(0.5f);
            appIconImageView.setScaleY(0.5f);
            appIconImageView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }
        
        // Banner animation
        if (bannerOuterContainer != null && bannerOuterContainer.getParent() != null) {
            bannerOuterContainer.setAlpha(0f);
            bannerOuterContainer.setScaleX(0.85f);
            bannerOuterContainer.setScaleY(0.85f);
            bannerOuterContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new DecelerateInterpolator())
                .rotationBy(360f)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (bannerOuterContainer != null) {
                            bannerOuterContainer.setRotation(0f);
                        }
                    }
                })
                .start();
        }
    }
    
    private void setupListeners() {
        if (closeBtn != null) {
            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentState == DialogState.ACTIVE) {
                        closeDialogManually();
                    }
                }
            });
        }
        
        if (waBtn != null) {
            waBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentState == DialogState.ACTIVE) {
                        openCommunityLink();
                    }
                }
            });
        }
        
        if (dontShowCheckbox != null) {
            dontShowCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isCheckboxAutoTriggered) {
                        isCheckboxAutoTriggered = false;
                        savePreference(DONT_SHOW_KEY, isChecked);
                        return;
                    }
                    
                    animateCheckboxWrapper(dontShowWrapper);
                    
                    if (isChecked) {
                        animateDontShowOn(dontShowWrapper, dontShowCheckboxText);
                    } else {
                        animateDontShowOff(dontShowWrapper, dontShowCheckboxText);
                    }
                    
                    savePreference(DONT_SHOW_KEY, isChecked);
                }
            });
        }
        
        if (timeFreezeCheckbox != null) {
            timeFreezeCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    animateCheckboxWrapper(timeFreezeWrapper);
                    
                    if (isChecked) {
                        isTimerFrozen = true;
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }
                        if (timerText != null) {
                            timerText.setText("FROZEN");
                            timerText.setTextColor(Color.parseColor("#00ADB5"));
                        }
                        // Apply freeze animation
                        TextView freezeText = (TextView) ((LinearLayout) timeFreezeWrapper.getParent()).getChildAt(1);
                        if (freezeText != null) {
                            animateFreezeOn(freezeText);
                        }
                        saveTimerState();
                    } else {
                        isTimerFrozen = false;
                        // Remove freeze animation
                        TextView freezeText = (TextView) ((LinearLayout) timeFreezeWrapper.getParent()).getChildAt(1);
                        if (freezeText != null) {
                            animateFreezeOff(freezeText);
                        }
                        startAutoCloseTimer(timeRemainingMillis);
                    }
                }
            });
        }
    }
    
    private void animateDontShowOn(View box, TextView text) {
        if (box != null) {
            box.animate()
                .alpha(0.55f)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }
        if (text != null) {
            text.animate()
                .alpha(0.6f)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }
    }
    
    private void animateDontShowOff(View box, TextView text) {
        if (box != null) {
            box.animate()
                .alpha(1f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }
        if (text != null) {
            text.animate()
                .alpha(1f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }
    }
    
    private void saveTimerState() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(TIMER_REMAINING_KEY, timeRemainingMillis);
        editor.putBoolean(IS_FROZEN_KEY, isTimerFrozen);
        editor.apply();
    }
    
    // Reset timer completely when dialog is closed
    private void resetTimerCompletely() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        timeRemainingMillis = 15000;
        isTimerFrozen = false;
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit()
            .remove(TIMER_REMAINING_KEY)
            .remove(IS_FROZEN_KEY)
            .apply();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        saveTimerState();
    }
    
    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean dontShow = prefs.getBoolean(DONT_SHOW_KEY, false);
        
        // No real-time subtraction - timer only runs in active session
        isTimerFrozen = prefs.getBoolean(IS_FROZEN_KEY, false);
        timeRemainingMillis = prefs.getLong(TIMER_REMAINING_KEY, 15000);
        
        if (dontShowCheckbox != null) {
            dontShowCheckbox.setChecked(dontShow);
        }
        if (timeFreezeCheckbox != null) {
            timeFreezeCheckbox.setChecked(isTimerFrozen);
        }
        
        if (timerText != null) {
            if (isTimerFrozen) {
                timerText.setText("FROZEN");
                timerText.setTextColor(Color.parseColor("#00ADB5"));
            } else {
                long seconds = (long) Math.ceil(timeRemainingMillis / 1000.0);
                timerText.setText(seconds + "s");
            }
        }
    }
    
    private void savePreference(String key, boolean value) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
    }
    
    private void startMetadataAnimations() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (textInfoLayout != null && textInfoLayout.getParent() != null) {
                    textInfoLayout.animate()
                        .alpha(1f)
                        .setDuration(800)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                    
                    startWelcomeTextAnimation();
                }
            }
        }, 300);
    }
    
    private void startWelcomeTextAnimation() {
        final String appName = getAppName();
        
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (welcomeText != null && welcomeText.getParent() != null) {
                    welcomeText.setText("Welcome to ");
                    welcomeText.animate()
                        .alpha(1f)
                        .setDuration(600)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                    
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animateTextTyping(welcomeText, appName, 50, new Runnable() {
                                @Override
                                public void run() {
                                    startPackageTextAnimation();
                                }
                            });
                        }
                    }, 400);
                }
            }
        }, 200);
    }
    
    private void startPackageTextAnimation() {
        final String packageName = getPackageName();
        
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (packageText != null && packageText.getParent() != null) {
                    packageText.setText("Package: ");
                    packageText.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                    
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animateTextTyping(packageText, packageName, 30, new Runnable() {
                                @Override
                                public void run() {
                                    startVersionTextAnimation();
                                }
                            });
                        }
                    }, 200);
                }
            }
        }, 100);
    }
    
    private void startVersionTextAnimation() {
        final String realVersion = getAppVersion();
        
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (versionText != null && versionText.getParent() != null) {
                    versionText.setText("Version ");
                    versionText.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                    
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animateRandomVersionNumbers(realVersion);
                        }
                    }, 200);
                }
            }
        }, 100);
    }
    
    private void animateRandomVersionNumbers(final String realVersion) {
        final int[] count = {0};
        final int maxCount = 12;
        
        Runnable versionRunnable = new Runnable() {
            @Override
            public void run() {
                if (count[0] < maxCount && versionText != null && versionText.getParent() != null) {
                    float randomMajor = 1 + random.nextFloat() * 9;
                    float randomMinor = random.nextFloat() * 9;
                    String randomVersion = String.format("Version %.1f", randomMajor + randomMinor / 10);
                    versionText.setText(randomVersion);
                    count[0]++;
                    mainHandler.postDelayed(this, 80);
                } else if (versionText != null) {
                    versionText.setText("Version " + realVersion);
                }
            }
        };
        
        mainHandler.postDelayed(versionRunnable, 50);
    }
    
    private void animateTextTyping(final TextView textView, final String text, 
                                  final int delay, final Runnable onComplete) {
        final int[] index = {0};
        
        if (textView == null || textView.getParent() == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        Runnable typingRunnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < text.length() && textView != null && textView.getParent() != null) {
                    String currentText = textView.getText().toString() + text.charAt(index[0]);
                    textView.setText(currentText);
                    index[0]++;
                    mainHandler.postDelayed(this, delay);
                } else if (onComplete != null) {
                    onComplete.run();
                }
            }
        };
        
        mainHandler.postDelayed(typingRunnable, 50);
    }
    
    private void startTextAnimations() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (moddedByText != null && moddedByText.getParent() != null) {
                    moddedByText.animate()
                        .alpha(1f)
                        .setDuration(600)
                        .translationY(0)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                    
                    if (takaneText != null && takaneText.getParent() != null) {
                        takaneText.animate()
                            .alpha(1f)
                            .setDuration(600)
                            .start();
                    }
                    
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startTypingAnimation();
                        }
                    }, 300);
                }
            }
        }, 200);
    }
    
    private void startTypingAnimation() {
        if (descText == null || descText.getParent() == null) return;
        
        final String fullText = descText.getText().toString();
        descText.setText("");
        descText.setAlpha(1f);
        
        final int[] index = {0};
        
        Runnable typingRunnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] <= fullText.length() && descText != null && descText.getParent() != null) {
                    descText.setText(fullText.substring(0, index[0]));
                    index[0]++;
                    mainHandler.postDelayed(this, 40);
                }
            }
        };
        
        mainHandler.postDelayed(typingRunnable, 100);
    }
    
    private GradientDrawable createButtonBg(boolean isPrimary) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(12));
        
        if (isPrimary) {
            bg.setColor(Color.parseColor("#25D366"));
            bg.setStroke(dpToPx(2), Color.parseColor("#1DA851"));
        } else {
            bg.setColor(Color.parseColor("#E74C3C"));
            bg.setStroke(dpToPx(2), Color.parseColor("#C0392B"));
        }
        
        return bg;
    }
    
    // Freeze animation effects
    private void animateFreezeOn(final TextView tv) {
        if (tv == null) return;
        
        GradientDrawable ice = new GradientDrawable();
        ice.setCornerRadius(dpToPx(12));
        ice.setColor(Color.parseColor("#3300E5FF"));
        ice.setStroke(dpToPx(1), Color.parseColor("#88BFFBFF"));
        tv.setBackground(ice);
        
        tv.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .alpha(0.85f)
            .setDuration(400)
            .setInterpolator(new DecelerateInterpolator())
            .start();
        
        // Glow animation for icy effect
        freezeGlowAnimator = ObjectAnimator.ofInt(ice, "alpha", 50, 180);
        freezeGlowAnimator.setEvaluator(new ArgbEvaluator());
        freezeGlowAnimator.setDuration(600);
        freezeGlowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        freezeGlowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        freezeGlowAnimator.start();
    }
    
    private void animateFreezeOff(final TextView tv) {
        if (tv == null) return;
        
        if (freezeGlowAnimator != null) {
            freezeGlowAnimator.cancel();
            freezeGlowAnimator = null;
        }
        
        tv.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(350)
            .setInterpolator(new DecelerateInterpolator())
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    tv.setBackground(null);
                }
            })
            .start();
    }
    
    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * displayMetrics.density);
    }
    
    private int getDisplayHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }
    
    private String getAppVersion() {
        try {
            android.content.pm.PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }
    
    private String getAppName() {
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            android.content.pm.PackageInfo pInfo = pm.getPackageInfo(getPackageName(), 0);
            CharSequence label = pm.getApplicationLabel(pInfo.applicationInfo);
            return label.toString();
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "App";
        }
    }
    
    // Auto close timer with optimized saving
    private void startAutoCloseTimer(long startMillis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        if (startMillis <= 0) {
            autoClose();
            return;
        }
        
        timeRemainingMillis = startMillis;
        
        countDownTimer = new CountDownTimer(timeRemainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isTimerFrozen) {
                    cancel();
                    return;
                }
                
                timeRemainingMillis = millisUntilFinished;
                long seconds = (long) Math.ceil(millisUntilFinished / 1000.0);
                
                if (timerText != null && timerText.getParent() != null) {
                    timerText.setText(seconds + "s");
                    
                    if (seconds <= 3) {
                        timerText.setTextColor(Color.parseColor("#FF4444"));
                        timerText.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
                    } else {
                        timerText.setTextColor(Color.parseColor("#FFD700"));
                        timerText.setScaleX(1f);
                        timerText.setScaleY(1f);
                    }
                }
            }
            
            @Override
            public void onFinish() {
                timeRemainingMillis = 0;
                autoClose();
            }
        };
        
        if (!isTimerFrozen) {
            countDownTimer.start();
        }
    }
    
    // Handle timer finish
    private void autoClose() {
        if (currentState != DialogState.ACTIVE || isTimerFrozen) {
            return;
        }
        
        if (timerText != null) {
            timerText.setText("0s");
            timerText.setTextColor(Color.parseColor("#FF4444"));
        }
        
        isCheckboxAutoTriggered = true;
        if (dontShowCheckbox != null) {
            dontShowCheckbox.setChecked(true);
            savePreference(DONT_SHOW_KEY, true);
        }
        
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentState == DialogState.ACTIVE) {
                    closeDialogAuto();
                }
            }
        }, 1000);
    }
    
    // Manual close with animation
    private void closeDialogManually() {
        if (currentState != DialogState.ACTIVE) return;
        currentState = DialogState.CLOSING_MANUALLY;
        
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        resetTimerCompletely();
        
        final int displayHeight = getDisplayHeight();
        
        contentLayer.animate()
            .translationY(displayHeight)
            .setDuration(420)
            .setInterpolator(new AccelerateInterpolator(1.5f))
            .withStartAction(new Runnable() {
                @Override
                public void run() {
                    if (logoImageView != null && logoImageView.getParent() != null) {
                        logoImageView.animate()
                            .scaleX(0.5f)
                            .scaleY(0.5f)
                            .alpha(0f)
                            .setDuration(300)
                            .start();
                    }
                        
                    if (appIconImageView != null && appIconImageView.getParent() != null) {
                        appIconImageView.animate()
                            .scaleX(0.5f)
                            .scaleY(0.5f)
                            .alpha(0f)
                            .setDuration(300)
                            .start();
                    }
                        
                    if (bannerOuterContainer != null && bannerOuterContainer.getParent() != null) {
                        bannerOuterContainer.animate()
                            .scaleX(0.85f)
                            .scaleY(0.85f)
                            .alpha(0f)
                            .rotationBy(-180f)
                            .setDuration(320)
                            .start();
                    }
                }
            })
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    currentState = DialogState.CLOSED;
                    isDialogActive = false;
                    hasShownScrollToast = false;
                    
                    if (bannerVideoPlayer != null) {
                        bannerVideoPlayer.release();
                        bannerVideoPlayer = null;
                    }
                    
                    if (bannerVideoView != null) {
                        bannerVideoView.stopPlayback();
                        bannerVideoView = null;
                    }
                    
                    finish();
                    overridePendingTransition(0, 0);
                }
            })
            .start();
    }
    
    // Auto close with animation
    private void closeDialogAuto() {
        if (currentState != DialogState.ACTIVE) return;
        currentState = DialogState.CLOSING_AUTO;
        
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        resetTimerCompletely();
        
        final int displayHeight = getDisplayHeight();
        
        contentLayer.animate()
            .translationY(displayHeight)
            .alpha(0f)
            .setDuration(600)
            .setInterpolator(new DecelerateInterpolator())
            .withStartAction(new Runnable() {
                @Override
                public void run() {
                    if (bannerOuterContainer != null && bannerOuterContainer.getParent() != null) {
                        bannerOuterContainer.animate()
                            .alpha(0f)
                            .rotationBy(-360f)
                            .setDuration(500)
                            .start();
                    }
                }
            })
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    currentState = DialogState.CLOSED;
                    isDialogActive = false;
                    hasShownScrollToast = false;
                    
                    if (bannerVideoPlayer != null) {
                        bannerVideoPlayer.release();
                        bannerVideoPlayer = null;
                    }
                    
                    if (bannerVideoView != null) {
                        bannerVideoView.stopPlayback();
                        bannerVideoView = null;
                    }
                    
                    finish();
                    overridePendingTransition(0, 0);
                }
            })
            .start();
    }
    
    // Community link opener with fallback
    private void openCommunityLink() {
        try {
            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
            whatsappIntent.setData(Uri.parse("https://chat.whatsapp.com/DcA3oplpxcbDr5vVqIfvE6"));
            whatsappIntent.setPackage("com.whatsapp");
            
            Intent telegramIntent = new Intent(Intent.ACTION_VIEW);
            telegramIntent.setData(Uri.parse("https://t.me/ridhoae303"));
            telegramIntent.setPackage("org.telegram.messenger");
            
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse("https://chat.whatsapp.com/DcA3oplpxcbDr5vVqIfvE6"));
            
            try {
                startActivity(whatsappIntent);
            } catch (Exception e1) {
                try {
                    startActivity(telegramIntent);
                } catch (Exception e2) {
                    startActivity(browserIntent);
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null && !isTimerFrozen) {
            countDownTimer.cancel();
        }
        
        if (bannerVideoPlayer != null && bannerVideoPlayer.isPlaying()) {
            bannerVideoPlayer.pause();
        }
        
        if (bannerVideoView != null && bannerVideoView.isPlaying()) {
            bannerVideoView.pause();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        if (currentState != DialogState.ACTIVE) {
            return;
        }
        
        if (!isTimerFrozen) {
            startAutoCloseTimer(timeRemainingMillis);
        } else {
            if (timerText != null) {
                timerText.setText("FROZEN");
                timerText.setTextColor(Color.parseColor("#00ADB5"));
            }
        }
        
        if (bannerVideoPlayer != null && !bannerVideoPlayer.isPlaying()) {
            bannerVideoPlayer.start();
        }
        
        if (bannerVideoView != null && !bannerVideoView.isPlaying()) {
            bannerVideoView.start();
        }
    }
    
    @Override
    public void onBackPressed() {
        // Disable back button
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentState = DialogState.CLOSED;
        isDialogActive = false;
        hasShownScrollToast = false;
        
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        mainHandler.removeCallbacksAndMessages(null);
        
        if (freezeGlowAnimator != null) {
            freezeGlowAnimator.cancel();
            freezeGlowAnimator = null;
        }
        
        if (bannerVideoPlayer != null) {
            bannerVideoPlayer.release();
            bannerVideoPlayer = null;
        }
        
        if (bannerVideoView != null) {
            bannerVideoView.stopPlayback();
            bannerVideoView = null;
        }
        
        if (bannerGifMovie != null) {
            bannerGifMovie = null;
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("timeRemainingMillis", timeRemainingMillis);
        outState.putBoolean("isTimerFrozen", isTimerFrozen);
        if (timeFreezeCheckbox != null) {
            outState.putBoolean("timeFreezeChecked", timeFreezeCheckbox.isChecked());
        }
        if (dontShowCheckbox != null) {
            outState.putBoolean("dontShowChecked", dontShowCheckbox.isChecked());
        }
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            timeRemainingMillis = savedInstanceState.getLong("timeRemainingMillis", 15000);
            isTimerFrozen = savedInstanceState.getBoolean("isTimerFrozen", false);
            if (timeFreezeCheckbox != null) {
                timeFreezeCheckbox.setChecked(savedInstanceState.getBoolean("timeFreezeChecked", false));
            }
            if (dontShowCheckbox != null) {
                dontShowCheckbox.setChecked(savedInstanceState.getBoolean("dontShowChecked", false));
            }
        }
    }
    
    // Static method to launch dialog from anywhere
    public static void atsuko(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean dontShow = prefs.getBoolean(DONT_SHOW_KEY, false);
        
        if (!dontShow) {
            Intent intent = new Intent(context, TakaneActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
        }
    }
    
    // Helper to ensure clip system is applied consistently
    private void ensureClipSystem() {
        if (logoImageView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            logoImageView.post(new Runnable() {
                @Override
                public void run() {
                    if (logoImageView != null && logoImageView.getParent() != null) {
                        logoImageView.setClipToOutline(true);
                        logoImageView.setOutlineProvider(new ViewOutlineProvider() {
                            @Override
                            public void getOutline(View v, Outline outline) {
                                outline.setOval(0, 0, v.getWidth(), v.getHeight());
                            }
                        });
                        logoImageView.invalidateOutline();
                    }
                }
            });
        }
        
        if (appIconImageView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appIconImageView.post(new Runnable() {
                @Override
                public void run() {
                    if (appIconImageView != null && appIconImageView.getParent() != null) {
                        appIconImageView.setClipToOutline(true);
                        appIconImageView.setOutlineProvider(new ViewOutlineProvider() {
                            @Override
                            public void getOutline(View v, Outline outline) {
                                outline.setOval(0, 0, v.getWidth(), v.getHeight());
                            }
                        });
                        appIconImageView.invalidateOutline();
                    }
                }
            });
        }
    }
}
