package com.passfamily.airesumebuilder.utils;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdHelper {
    private static final String TAG = "AdHelper";

    public static final String BANNER_AD_UNIT_ID = "ca-app-pub-3300125462328011/9810822660";
    public static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3300125462328011/3036696605";

    private static boolean isMobileAdsInitialized = false;

    /**
     * Initialize Google Mobile Ads SDK
     */
    public static void initializeMobileAds(Activity activity) {
        if (!isMobileAdsInitialized) {
            MobileAds.initialize(activity, initializationStatus -> {
                isMobileAdsInitialized = true;
                Log.d(TAG, "Google Mobile Ads initialized");
            });
        }
    }

    /**
     * Load and display banner ad
     */
    public static AdView loadBannerAd(Activity activity, LinearLayout adContainer) {
        if (adContainer == null) {
            Log.e(TAG, "Ad container is null");
            return null;
        }

        AdView adView = new AdView(activity);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(BANNER_AD_UNIT_ID);

        adContainer.addView(adView);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        Log.d(TAG, "Banner ad loaded");
        return adView;
    }

    /**
     * Load interstitial ad
     */
    public static void loadInterstitialAd(Activity activity, InterstitialAdCallback callback) {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(activity, INTERSTITIAL_AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Log.d(TAG, "Interstitial ad loaded");
                        if (callback != null) {
                            callback.onAdLoaded(interstitialAd);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                        if (callback != null) {
                            callback.onAdFailedToLoad(loadAdError.getMessage());
                        }
                    }
                });
    }

    /**
     * Show interstitial ad
     */
    public static void showInterstitialAd(Activity activity, InterstitialAd interstitialAd,
                                          InterstitialAdShowCallback callback) {
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed");
                    if (callback != null) {
                        callback.onAdDismissed();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                    if (callback != null) {
                        callback.onAdFailedToShow(adError.getMessage());
                    }
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed");
                }
            });

            interstitialAd.show(activity);
        } else {
            Log.e(TAG, "Interstitial ad is null");
            if (callback != null) {
                callback.onAdFailedToShow("Ad not loaded");
            }
        }
    }

    // Callback interfaces
    public interface InterstitialAdCallback {
        void onAdLoaded(InterstitialAd interstitialAd);
        void onAdFailedToLoad(String error);
    }

    public interface InterstitialAdShowCallback {
        void onAdDismissed();
        void onAdFailedToShow(String error);
    }
}