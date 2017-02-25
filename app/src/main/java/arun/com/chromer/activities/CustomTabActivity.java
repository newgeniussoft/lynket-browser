package arun.com.chromer.activities;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import arun.com.chromer.R;
import arun.com.chromer.activities.settings.Preferences;
import arun.com.chromer.customtabs.CustomTabs;
import arun.com.chromer.data.website.WebsiteRepository;
import arun.com.chromer.data.website.model.WebSite;
import arun.com.chromer.util.Utils;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.content.Intent.EXTRA_TEXT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.widget.Toast.LENGTH_SHORT;
import static arun.com.chromer.shared.Constants.ACTION_MINIMIZE;
import static arun.com.chromer.shared.Constants.EXTRA_KEY_FROM_WEBHEAD;
import static arun.com.chromer.shared.Constants.EXTRA_KEY_WEBSITE;
import static arun.com.chromer.shared.Constants.NO_COLOR;

public class CustomTabActivity extends AppCompatActivity {
    private boolean isLoaded = false;
    private String baseUrl = "";
    private BroadcastReceiver minimizeReceiver;
    private final CompositeSubscription subscriptions = new CompositeSubscription();

    @TargetApi(LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null || getIntent().getData() == null) {
            Toast.makeText(this, getString(R.string.unsupported_link), LENGTH_SHORT).show();
            finish();
            return;
        }
        baseUrl = getIntent().getDataString();
        final boolean isWebHead = getIntent().getBooleanExtra(EXTRA_KEY_FROM_WEBHEAD, false);
        final WebSite webSite = getIntent().getParcelableExtra(EXTRA_KEY_WEBSITE);
        final int fallbackWebColor = webSite != null && !TextUtils.isEmpty(webSite.themeColor) ? webSite.themeColor() : NO_COLOR;

        CustomTabs.from(this)
                .forUrl(baseUrl)
                .forWebHead(isWebHead)
                .fallbackColor(fallbackWebColor)
                .prepare()
                .launch();
        if (Preferences.get(this).aggressiveLoading()) {
            delayedGoToBack();
        }
        registerMinimizeReceiver();
        beginExtraction(webSite);
    }

    private void beginExtraction(@Nullable WebSite webSite) {
        if (webSite != null && webSite.title != null && webSite.faviconUrl != null) {
            Timber.d("Website info exists, setting description");
            applyDescriptionFromWebsite(webSite);
        } else {
            Timber.d("No info found, beginning parsing");
            final Subscription s = WebsiteRepository.getInstance(this)
                    .getWebsite(baseUrl)
                    .doOnNext(this::applyDescriptionFromWebsite)
                    .doOnError(Timber::e)
                    .subscribe();
            subscriptions.add(s);
        }
    }

    private void registerMinimizeReceiver() {
        minimizeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equalsIgnoreCase(ACTION_MINIMIZE) && intent.hasExtra(EXTRA_TEXT)) {
                    final String url = intent.getStringExtra(EXTRA_TEXT);
                    if (baseUrl.equalsIgnoreCase(url)) {
                        try {
                            Timber.d("Minimized %s", url);
                            moveTaskToBack(true);
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(minimizeReceiver, new IntentFilter(ACTION_MINIMIZE));
    }

    private void delayedGoToBack() {
        new Handler().postDelayed(() -> moveTaskToBack(true), 650);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        isLoaded = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLoaded) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(minimizeReceiver);
        subscriptions.clear();
        super.onDestroy();
    }

    @TargetApi(LOLLIPOP)
    private void applyDescriptionFromWebsite(@NonNull final WebSite webSite) {
        if (Utils.isLollipopAbove()) {
            final String title = webSite.safeLabel();
            final String faviconUrl = webSite.faviconUrl;
            setTaskDescription(new ActivityManager.TaskDescription(title, null, webSite.themeColor()));
            Glide.with(this)
                    .load(faviconUrl)
                    .asBitmap()
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap icon, GlideAnimation<? super Bitmap> glideAnimation) {
                            setTaskDescription(new ActivityManager.TaskDescription(title, icon, webSite.themeColor()));
                        }
                    });
        }
    }
}
