/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.lint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.ast.AstVisitor;
import lombok.ast.Cast;
import lombok.ast.Expression;
import lombok.ast.MethodInvocation;
import lombok.ast.StrictListAccessor;

/**
 * Detector looking for casts on th result of context.getSystemService which are suspect
 */
public class ServiceCastDetector extends Detector implements Detector.JavaScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ServiceCast", //$NON-NLS-1$
            "Wrong system service casts",

            "When you call `Context#getSystemService()`, the result is typically cast to " +
            "a specific interface. This lint check ensures that the cast is compatible with " +
            "the expected type of the return value.",

            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            new Implementation(
                    ServiceCastDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /** Constructs a new {@link ServiceCastDetector} check */
    public ServiceCastDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    // ---- Implements JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("getSystemService"); //$NON-NLS-1$
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        if (node.getParent() instanceof Cast) {
            Cast cast = (Cast) node.getParent();
            StrictListAccessor<Expression, MethodInvocation> args = node.astArguments();
            if (args.size() == 1) {
                String name = stripPackage(args.first().toString());
                String expectedClass = getExpectedType(name);
                if (expectedClass != null) {
                    String castType = cast.astTypeReference().getTypeName();
                    if (castType.indexOf('.') == -1) {
                        expectedClass = stripPackage(expectedClass);
                    }
                    if (!castType.equals(expectedClass)) {
                        // It's okay to mix and match
                        // android.content.ClipboardManager and android.text.ClipboardManager
                        if (isClipboard(castType) && isClipboard(expectedClass)) {
                            return;
                        }

                        String message = String.format(
                                "Suspicious cast to `%1$s` for a `%2$s`: expected `%3$s`",
                                stripPackage(castType), name, stripPackage(expectedClass));
                        context.report(ISSUE, node, context.getLocation(cast), message);
                    }
                }

            }
        }
    }

    private static boolean isClipboard(String cls) {
        return cls.equals("android.content.ClipboardManager")      //$NON-NLS-1$
                || cls.equals("android.text.ClipboardManager");    //$NON-NLS-1$
    }

    private static String stripPackage(String fqcn) {
        int index = fqcn.lastIndexOf('.');
        if (index != -1) {
            fqcn = fqcn.substring(index + 1);
        }

        return fqcn;
    }

    @Nullable
    private static String getExpectedType(@NonNull String value) {
        return getServiceMap().get(value);
    }

    @NonNull
    private static Map<String, String> getServiceMap() {
        if (sServiceMap == null) {
            final int EXPECTED_SIZE = 49;
            sServiceMap = Maps.newHashMapWithExpectedSize(EXPECTED_SIZE);

            sServiceMap.put("ACCESSIBILITY_SERVICE", "android.view.accessibility.AccessibilityManager");
            sServiceMap.put("ACCOUNT_SERVICE", "android.accounts.AccountManager");
            sServiceMap.put("ACTIVITY_SERVICE", "android.app.ActivityManager");
            sServiceMap.put("ALARM_SERVICE", "android.app.AlarmManager");
            sServiceMap.put("APPWIDGET_SERVICE", "android.appwidget.AppWidgetManager");
            sServiceMap.put("APP_OPS_SERVICE", "android.app.AppOpsManager");
            sServiceMap.put("AUDIO_SERVICE", "android.media.AudioManager");
            sServiceMap.put("BATTERY_SERVICE", "android.os.BatteryManager");
            sServiceMap.put("BLUETOOTH_SERVICE", "android.bluetooth.BluetoothManager");
            sServiceMap.put("CAMERA_SERVICE", "android.hardware.camera2.CameraManager");
            sServiceMap.put("CAPTIONING_SERVICE", "android.view.accessibility.CaptioningManager");
            sServiceMap.put("CLIPBOARD_SERVICE", "android.text.ClipboardManager");
            sServiceMap.put("CONNECTIVITY_SERVICE", "android.net.ConnectivityManager");
            sServiceMap.put("CONSUMER_IR_SERVICE", "android.hardware.ConsumerIrManager");
            sServiceMap.put("DEVICE_POLICY_SERVICE", "android.app.admin.DevicePolicyManager");
            sServiceMap.put("DISPLAY_SERVICE", "android.hardware.display.DisplayManager");
            sServiceMap.put("DOWNLOAD_SERVICE", "android.app.DownloadManager");
            sServiceMap.put("DROPBOX_SERVICE", "android.os.DropBoxManager");
            sServiceMap.put("INPUT_METHOD_SERVICE", "android.view.inputmethod.InputMethodManager");
            sServiceMap.put("INPUT_SERVICE", "android.hardware.input.InputManager");
            sServiceMap.put("JOB_SCHEDULER_SERVICE", "android.app.job.JobScheduler");
            sServiceMap.put("KEYGUARD_SERVICE", "android.app.KeyguardManager");
            sServiceMap.put("LAUNCHER_APPS_SERVICE", "android.content.pm.LauncherApps");
            sServiceMap.put("LAYOUT_INFLATER_SERVICE", "android.view.LayoutInflater");
            sServiceMap.put("LOCATION_SERVICE", "android.location.LocationManager");
            sServiceMap.put("MEDIA_PROJECTION_SERVICE", "android.media.projection.MediaProjectionManager");
            sServiceMap.put("MEDIA_ROUTER_SERVICE", "android.media.MediaRouter");
            sServiceMap.put("MEDIA_SESSION_SERVICE", "android.media.session.MediaSessionManager");
            sServiceMap.put("NFC_SERVICE", "android.nfc.NfcManager");
            sServiceMap.put("NOTIFICATION_SERVICE", "android.app.NotificationManager");
            sServiceMap.put("NSD_SERVICE", "android.net.nsd.NsdManager");
            sServiceMap.put("POWER_SERVICE", "android.os.PowerManager");
            sServiceMap.put("PRINT_SERVICE", "android.print.PrintManager");
            sServiceMap.put("RESTRICTIONS_SERVICE", "android.content.RestrictionsManager");
            sServiceMap.put("SEARCH_SERVICE", "android.app.SearchManager");
            sServiceMap.put("SENSOR_SERVICE", "android.hardware.SensorManager");
            sServiceMap.put("STORAGE_SERVICE", "android.os.storage.StorageManager");
            sServiceMap.put("TELECOM_SERVICE", "android.telecom.TelecomManager");
            sServiceMap.put("TELEPHONY_SERVICE", "android.telephony.TelephonyManager");
            sServiceMap.put("TEXT_SERVICES_MANAGER_SERVICE", "android.view.textservice.TextServicesManager");
            sServiceMap.put("TV_INPUT_SERVICE", "android.media.tv.TvInputManager");
            sServiceMap.put("UI_MODE_SERVICE", "android.app.UiModeManager");
            sServiceMap.put("USB_SERVICE", "android.hardware.usb.UsbManager");
            sServiceMap.put("USER_SERVICE", "android.os.UserManager");
            sServiceMap.put("VIBRATOR_SERVICE", "android.os.Vibrator");
            sServiceMap.put("WALLPAPER_SERVICE", "com.android.server.WallpaperService");
            sServiceMap.put("WIFI_P2P_SERVICE", "android.net.wifi.p2p.WifiP2pManager");
            sServiceMap.put("WIFI_SERVICE", "android.net.wifi.WifiManager");
            sServiceMap.put("WINDOW_SERVICE", "android.view.WindowManager");

            assert sServiceMap.size() == EXPECTED_SIZE : sServiceMap.size();
        }

        return sServiceMap;
    }

    private static Map<String, String> sServiceMap;
}
