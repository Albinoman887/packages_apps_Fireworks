package com.spark.settings.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.spark.SparkUtils;
import com.android.internal.util.spark.ThemeUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.development.OverlayCategoryPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.preferences.CustomSeekBarPreference;
import com.android.settings.preferences.SystemSettingEditTextPreference;
import com.android.settings.preferences.SystemSettingListPreference;
import com.android.settings.preferences.SystemSettingSwitchPreference;
import com.spark.settings.preferences.SystemSettingListPreference;
import com.spark.settings.preferences.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.om.IOverlayManager;
import static android.os.UserHandle.USER_CURRENT;
import static android.os.UserHandle.USER_SYSTEM;
import lineageos.providers.LineageSettings;

@SearchIndexable
public class ThemeSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    // Quick settings constants
    private static final String KEY_SHOW_AUTO_BRIGHTNESS = "qs_show_auto_brightness";
    private static final String KEY_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";
    private static final String KEY_BRIGHTNESS_SLIDER_POSITION = "qs_brightness_slider_position";
    private static final String PREF_TILE_ANIM_DURATION = "qs_tile_animation_duration";
    private static final String PREF_TILE_ANIM_INTERPOLATOR = "qs_tile_animation_interpolator";
    private static final String PREF_TILE_ANIM_STYLE = "qs_tile_animation_style";
    private static final String QS_PANEL_STYLE = "qs_panel_style";
    private static final String QS_PAGE_TRANSITIONS = "custom_transitions_page_tile";

    // Heads-up notification constants
    private static final String HEADS_UP_TIMEOUT_PREF = "heads_up_timeout";

    // Volume panel constants
    private static final String KEY_VOLUME_PANEL_LEFT = "volume_panel_on_left";

    // Settings constants
    private static final String ABOUT_PHONE_STYLE = "about_card_style";
    private static final String HIDE_USER_CARD = "hide_user_card";
    private static final String SETTINGS_CONTEXTUAL_MESSAGES = "settings_contextual_messages";
    private static final String SETTINGS_DASHBOARD_STYLE = "settings_dashboard_style";
    private static final String SETTINGS_HEADER_IMAGE = "settings_header_image";
    private static final String SETTINGS_HEADER_IMAGE_RANDOM = "settings_header_image_random";
    private static final String SETTINGS_HEADER_TEXT = "settings_header_text";
    private static final String SETTINGS_HEADER_TEXT_ENABLED = "settings_header_text_enabled";
    private static final String USE_STOCK_LAYOUT = "use_stock_layout";

    private Handler mHandler;
    private ThemeUtils mThemeUtils;
    private IOverlayManager mOverlayManager;
    private IOverlayManager mOverlayService;

    private Preference mSettingsHeaderImage;
    private Preference mSettingsHeaderImageRandom;
    private Preference mSettingsMessage;

    private SystemSettingEditTextPreference mSettingsHeaderText;
    private SystemSettingSwitchPreference mSettingsHeaderTextEnabled;

    private SystemSettingSwitchPreference mUseStockLayout;
    private SystemSettingSwitchPreference mHideUserCard;

    private ListPreference mTileAnimationStyle;
    private ListPreference mTileAnimationDuration;
    private ListPreference mTileAnimationInterpolator;

    private SystemSettingListPreference mQsStyle;
    private SystemSettingListPreference mSettingsDashBoardStyle;
    private SystemSettingListPreference mAboutPhoneStyle;

    private SwitchPreference mVolumePanelLeft;
    private CustomSeekBarPreference mHeadsUpTimeOut;
    private ListPreference mShowBrightnessSlider;
    private ListPreference mBrightnessSliderPosition;
    private SwitchPreference mShowAutoBrightness;
    private SystemSettingListPreference mPageTransitions;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.spark_settings_themes);
        final Context mContext = getActivity().getApplicationContext();
        final ContentResolver resolver = mContext.getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mShowBrightnessSlider = findPreference(KEY_SHOW_BRIGHTNESS_SLIDER);
        mShowBrightnessSlider.setOnPreferenceChangeListener(this);
        boolean showSlider = LineageSettings.Secure.getIntForUser(resolver,
                LineageSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1, UserHandle.USER_CURRENT) > 0;

        mBrightnessSliderPosition = findPreference(KEY_BRIGHTNESS_SLIDER_POSITION);
        mBrightnessSliderPosition.setEnabled(showSlider);

        mShowAutoBrightness = findPreference(KEY_SHOW_AUTO_BRIGHTNESS);
        boolean automaticAvailable = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
        if (automaticAvailable) {
            mShowAutoBrightness.setEnabled(showSlider);
        } else {
            prefScreen.removePreference(mShowAutoBrightness);
        }

        mHeadsUpTimeOut = (CustomSeekBarPreference)
                            prefScreen.findPreference(HEADS_UP_TIMEOUT_PREF);
        mHeadsUpTimeOut.setDefaultValue(getDefaultDecay(mContext));


        boolean isAudioPanelOnLeft = LineageSettings.Secure.getIntForUser(resolver,
                LineageSettings.Secure.VOLUME_PANEL_ON_LEFT, isAudioPanelOnLeftSide(getActivity()) ? 1 : 0,
                UserHandle.USER_CURRENT) != 0;

        mVolumePanelLeft = (SwitchPreference) prefScreen.findPreference(KEY_VOLUME_PANEL_LEFT);
        mVolumePanelLeft.setChecked(isAudioPanelOnLeft);

        mThemeUtils = new ThemeUtils(getActivity());
        mCustomSettingsObserver.observe();

        mSettingsDashBoardStyle = (SystemSettingListPreference) findPreference(SETTINGS_DASHBOARD_STYLE);
        mSettingsDashBoardStyle.setOnPreferenceChangeListener(this);
        mSettingsHeaderImageRandom = findPreference(SETTINGS_HEADER_IMAGE_RANDOM);
        mSettingsHeaderImageRandom.setOnPreferenceChangeListener(this);
        mSettingsMessage = findPreference(SETTINGS_CONTEXTUAL_MESSAGES);
        mSettingsMessage.setOnPreferenceChangeListener(this);
        mSettingsHeaderImage = findPreference(SETTINGS_HEADER_IMAGE);
        mSettingsHeaderImage.setOnPreferenceChangeListener(this);
        mUseStockLayout = (SystemSettingSwitchPreference) findPreference(USE_STOCK_LAYOUT);
        mUseStockLayout.setOnPreferenceChangeListener(this);
        mAboutPhoneStyle = (SystemSettingListPreference) findPreference(ABOUT_PHONE_STYLE);
        mAboutPhoneStyle.setOnPreferenceChangeListener(this);
        mHideUserCard = (SystemSettingSwitchPreference) findPreference(HIDE_USER_CARD);
        mHideUserCard.setOnPreferenceChangeListener(this);
        mSettingsHeaderText = (SystemSettingEditTextPreference) findPreference(SETTINGS_HEADER_TEXT);
        mSettingsHeaderText.setOnPreferenceChangeListener(this);
        mSettingsHeaderTextEnabled = (SystemSettingSwitchPreference) findPreference(SETTINGS_HEADER_TEXT_ENABLED);
        mSettingsHeaderTextEnabled.setOnPreferenceChangeListener(this);

        mTileAnimationStyle = (ListPreference) findPreference(PREF_TILE_ANIM_STYLE);
        int tileAnimationStyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_ANIMATION_STYLE, 0, UserHandle.USER_CURRENT);
        mTileAnimationStyle.setValue(String.valueOf(tileAnimationStyle));
        updateTileAnimationStyleSummary(tileAnimationStyle);
        updateAnimTileStyle(tileAnimationStyle);
        mTileAnimationStyle.setOnPreferenceChangeListener(this);

        mTileAnimationDuration = (ListPreference) findPreference(PREF_TILE_ANIM_DURATION);
        int tileAnimationDuration = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_ANIMATION_DURATION, 2000, UserHandle.USER_CURRENT);
        mTileAnimationDuration.setValue(String.valueOf(tileAnimationDuration));
        updateTileAnimationDurationSummary(tileAnimationDuration);
        mTileAnimationDuration.setOnPreferenceChangeListener(this);

        mTileAnimationInterpolator = (ListPreference) findPreference(PREF_TILE_ANIM_INTERPOLATOR);
        int tileAnimationInterpolator = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.QS_TILE_ANIMATION_INTERPOLATOR, 0, UserHandle.USER_CURRENT);
        mTileAnimationInterpolator.setValue(String.valueOf(tileAnimationInterpolator));
        updateTileAnimationInterpolatorSummary(tileAnimationInterpolator);
        mTileAnimationInterpolator.setOnPreferenceChangeListener(this);

        mOverlayService = IOverlayManager.Stub
        .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));

        mQsStyle = (SystemSettingListPreference) findPreference(QS_PANEL_STYLE);
        mCustomSettingsObserver.observe();

        mPageTransitions = (SystemSettingListPreference) findPreference(QS_PAGE_TRANSITIONS);
        mPageTransitions.setOnPreferenceChangeListener(this);
        int customTransitions = Settings.System.getIntForUser(resolver,
                Settings.System.CUSTOM_TRANSITIONS_KEY,
                0, UserHandle.USER_CURRENT);
        mPageTransitions.setValue(String.valueOf(customTransitions));
        mPageTransitions.setSummary(mPageTransitions.getEntry());
    }

    private static boolean isAudioPanelOnLeftSide(Context context) {
        try {
            Context con = context.createPackageContext("org.lineageos.lineagesettings", 0);
            int id = con.getResources().getIdentifier("def_volume_panel_on_left",
                    "bool", "org.lineageos.lineagesettings");
            return con.getResources().getBoolean(id);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mShowBrightnessSlider) {
            int value = Integer.parseInt((String) newValue);
            mBrightnessSliderPosition.setEnabled(value > 0);
            if (mShowAutoBrightness != null)
                mShowAutoBrightness.setEnabled(value > 0);
            return true;
        } else if (preference == mSettingsDashBoardStyle) {
            mCustomSettingsObserver.observe();
            return true;
        } else if (preference == mUseStockLayout) {
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mHideUserCard) {
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mAboutPhoneStyle) {
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mSettingsHeaderImage) {
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mSettingsHeaderImageRandom) {
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mSettingsMessage) {
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mSettingsHeaderTextEnabled) {
            boolean enable = (Boolean) newValue;
            SystemProperties.set("persist.sys.settings.header_text_enabled", enable ? "true" : "false");
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mSettingsHeaderText) {
            String value = (String) newValue;
            SystemProperties.set("persist.sys.settings.header_text", value);
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
         } else if (preference == mTileAnimationStyle) {
            int tileAnimationStyle = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(resolver, Settings.System.QS_TILE_ANIMATION_STYLE,
                    tileAnimationStyle, UserHandle.USER_CURRENT);
            updateTileAnimationStyleSummary(tileAnimationStyle);
            updateAnimTileStyle(tileAnimationStyle);
            return true;
        } else if (preference == mTileAnimationDuration) {
            int tileAnimationDuration = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(resolver, Settings.System.QS_TILE_ANIMATION_DURATION,
                    tileAnimationDuration, UserHandle.USER_CURRENT);
            updateTileAnimationDurationSummary(tileAnimationDuration);
            return true;
        } else if (preference == mTileAnimationInterpolator) {
            int tileAnimationInterpolator = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(resolver, Settings.System.QS_TILE_ANIMATION_INTERPOLATOR,
                    tileAnimationInterpolator, UserHandle.USER_CURRENT);
            updateTileAnimationInterpolatorSummary(tileAnimationInterpolator);
            return true;
        } else if (preference == mQsStyle) {
            mCustomSettingsObserver.observe();
            return true;
        } else if (preference == mPageTransitions) {
            int customTransitions = Integer.parseInt(((String) newValue).toString());
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.CUSTOM_TRANSITIONS_KEY, customTransitions, UserHandle.USER_CURRENT);
            int index = mPageTransitions.findIndexOfValue((String) newValue);
            mPageTransitions.setSummary(
                    mPageTransitions.getEntries()[index]);
            return true;
        }
         return false;
    }

    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            Context mContext = getContext();
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SETTINGS_DASHBOARD_STYLE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_PANEL_STYLE),
                    false, this, UserHandle.USER_ALL);
        }


    @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(Settings.System.SETTINGS_DASHBOARD_STYLE))) {
                updateSettingsStyle();
            } else if (uri.equals(Settings.System.getUriFor(Settings.System.QS_PANEL_STYLE))) {
                updateQsStyle();
            }
        }
    }

    private void updateSettingsStyle() {
        ContentResolver resolver = getActivity().getContentResolver();

        int settingsPanelStyle = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SETTINGS_DASHBOARD_STYLE, 0, UserHandle.USER_CURRENT);

	// reset all overlays before applying
        mThemeUtils.setOverlayEnabled("android.theme.customization.icon_pack.settings", "com.android.settings", "com.android.settings");

	if (settingsPanelStyle == 0) return;

        switch (settingsPanelStyle) {
            case 1:
              setSettingsStyle("com.android.system.settings.rui");
              break;
            case 2:
              setSettingsStyle("com.android.system.settings.arc");
              break;
            case 3:
              setSettingsStyle("com.android.system.settings.aosp");
              break;
            case 4:
              setSettingsStyle("com.android.system.settings.mt");
              break;
            case 5:
              setSettingsStyle("com.android.system.settings.card");
              break;
            default:
              break;
        }
    }

    public void setSettingsStyle(String overlayName) {
       mThemeUtils.setOverlayEnabled("android.theme.customization.icon_pack.settings", overlayName, "com.android.settings");
    }

    private static int getDefaultDecay(Context context) {
        int defaultHeadsUpTimeOut = 5;
        Resources systemUiResources;
        try {
            systemUiResources = context.getPackageManager().getResourcesForApplication("com.android.systemui");
            defaultHeadsUpTimeOut = systemUiResources.getInteger(systemUiResources.getIdentifier(
                    "com.android.systemui:integer/heads_up_notification_decay", null, null)) / 1000;
        } catch (Exception e) {
        }
        return defaultHeadsUpTimeOut;
    }

    private void updateQsStyle() {
          ContentResolver resolver = getActivity().getContentResolver();

          int qsPanelStyle = Settings.System.getIntForUser(getContext().getContentResolver(),
                  Settings.System.QS_PANEL_STYLE , 0, UserHandle.USER_CURRENT);

          switch (qsPanelStyle) {
              case 0:
                setQsStyle("com.android.systemui");
                break;
              case 1:
                setQsStyle("com.android.system.qs.outline");
                break;
              case 2:
              case 3:
                setQsStyle("com.android.system.qs.twotoneaccent");
                break;
              case 4:
                setQsStyle("com.android.system.qs.shaded");
                break;
              case 5:
                setQsStyle("com.android.system.qs.cyberpunk");
                break;
              case 6:
                setQsStyle("com.android.system.qs.neumorph");
                break;
              case 7:
                setQsStyle("com.android.system.qs.reflected");
                break;
              case 8:
                setQsStyle("com.android.system.qs.surround");
                break;
              case 9:
                setQsStyle("com.android.system.qs.thin");
                break;
              case 10:
                setQsStyle("com.android.system.qs.twotoneaccenttrans");
                break;
              default:
                break;
          }
      }

      public void setQsStyle(String overlayName) {
          mThemeUtils.setOverlayEnabled("android.theme.customization.qs_panel", overlayName, "com.android.systemui");
      }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SPARK_SETTINGS;
    }

    private void updateTileAnimationStyleSummary(int tileAnimationStyle) {
        String prefix = (String) mTileAnimationStyle.getEntries()[mTileAnimationStyle.findIndexOfValue(String
                .valueOf(tileAnimationStyle))];
        mTileAnimationStyle.setSummary(getResources().getString(R.string.qs_set_animation_style, prefix));
    }

     private void updateTileAnimationDurationSummary(int tileAnimationDuration) {
        String prefix = (String) mTileAnimationDuration.getEntries()[mTileAnimationDuration.findIndexOfValue(String
                .valueOf(tileAnimationDuration))];
        mTileAnimationDuration.setSummary(getResources().getString(R.string.qs_set_animation_duration, prefix));
    }

    private void updateTileAnimationInterpolatorSummary(int tileAnimationInterpolator) {
        String prefix = (String) mTileAnimationInterpolator.getEntries()[mTileAnimationInterpolator.findIndexOfValue(String
                .valueOf(tileAnimationInterpolator))];
        mTileAnimationInterpolator.setSummary(getResources().getString(R.string.qs_set_animation_interpolator, prefix));
    }

    private void updateAnimTileStyle(int tileAnimationStyle) {
        if (mTileAnimationDuration != null) {
            if (tileAnimationStyle == 0) {
                mTileAnimationDuration.setSelectable(false);
                mTileAnimationInterpolator.setSelectable(false);
            } else {
                mTileAnimationDuration.setSelectable(true);
                mTileAnimationInterpolator.setSelectable(true);
            }
        }
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.spark_settings_themes);
}
