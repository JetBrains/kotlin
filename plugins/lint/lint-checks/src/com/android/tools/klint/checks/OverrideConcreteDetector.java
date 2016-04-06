/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.tools.klint.checks;

import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;

import java.util.Collections;
import java.util.List;

import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastScanner;

/**
 * Checks that subclasses of certain APIs are overriding all methods that were abstract
 * in one or more earlier API levels that are still targeted by the minSdkVersion
 * of this project.
 */
public class OverrideConcreteDetector extends Detector implements UastScanner {
    /** Are previously-abstract methods all overridden? */
    public static final Issue ISSUE = Issue.create(
        "OverrideAbstract", //$NON-NLS-1$
        "Not overriding abstract methods on older platforms",

        "To improve the usability of some APIs, some methods that used to be `abstract` have " +
        "been made concrete by adding default implementations. This means that when compiling " +
        "with new versions of the SDK, your code does not have to override these methods.\n" +
        "\n" +
        "However, if your code is also targeting older versions of the platform where these " +
        "methods were still `abstract`, the code will crash. You must override all methods " +
        "that used to be abstract in any versions targeted by your application's " +
        "`minSdkVersion`.",

        Category.CORRECTNESS,
        6,
        Severity.FATAL,
        new Implementation(
                OverrideConcreteDetector.class,
                Scope.JAVA_FILE_SCOPE)
    );

    // This check is currently hardcoded for the specific case of the
    // NotificationListenerService change in API 21. We should consider
    // attempting to infer this information automatically from changes in
    // the API current.txt file and making this detector more database driven,
    // like the API detector.

    private static final String NOTIFICATION_LISTENER_SERVICE_FQN
            = "android.service.notification.NotificationListenerService";
    public static final String STATUS_BAR_NOTIFICATION_FQN
            = "android.service.notification.StatusBarNotification";
    private static final String ON_NOTIFICATION_POSTED = "onNotificationPosted";
    private static final String ON_NOTIFICATION_REMOVED = "onNotificationRemoved";
    private static final int CONCRETE_IN = 21;

    /** Constructs a new {@link OverrideConcreteDetector} */
    public OverrideConcreteDetector() {
    }

    // ---- Implements UastScanner ----


    @Override
    public List<String> getApplicableSuperClasses() {
        return Collections.singletonList(NOTIFICATION_LISTENER_SERVICE_FQN);
    }

    @Override
    public void visitClass(UastAndroidContext context, UClass node) {
        if (node == null) {
            return;
        }
        if (node.hasModifier(UastModifier.ABSTRACT)) {
            return;
        }

        int minSdk = Math.max(context.getLintContext().getProject().getMinSdk(), getTargetApi(node));
        if (minSdk >= CONCRETE_IN) {
            return;
        }

        String[] methodNames = {ON_NOTIFICATION_POSTED, ON_NOTIFICATION_REMOVED};
        for (String methodName : methodNames) {
            boolean found = false;
            List<UFunction> allFunctions = UastUtils.getAllFunctions(node, context);
            for (UFunction method : allFunctions) {
                if (!method.matchesName(methodName)) {
                    continue;
                }

                // Make sure it's not the base method, but that it's been defined
                // in a subclass, concretely
                UClass containingClass = UastUtils.getContainingClassOrEmpty(method);
                if (containingClass.matchesFqName(NOTIFICATION_LISTENER_SERVICE_FQN)) {
                    continue;
                }
                // Make sure subclass isn't just defining another abstract definition
                // of the method
                if (method.hasModifier(UastModifier.ABSTRACT)) {
                    continue;
                }
                // Make sure it has the exact right signature
                if (method.getValueParameterCount() != 1) {
                    continue; // Wrong signature
                }
                if (!method.getValueParameters().get(0).getType().matchesFqName(STATUS_BAR_NOTIFICATION_FQN)) {
                    continue;
                }

                found = true;
                break;
            }

            if (!found) {
                String message = String.format(
                  "Must override `%1$s.%2$s(%3$s)`: Method was abstract until %4$d, and your `minSdkVersion` is %5$d",
                  NOTIFICATION_LISTENER_SERVICE_FQN, methodName,
                  STATUS_BAR_NOTIFICATION_FQN, CONCRETE_IN, minSdk);
                context.report(ISSUE, node, context.getLocation(node.getNameElement()),
                               message);
                break;
            }

        }
    }

    private static int getTargetApi(UClass node) {
        while (node != null) {
            int targetApi = ApiDetector.getTargetApi(node.getAnnotations());
            if (targetApi != -1) {
                return targetApi;
            }

            node = UastUtils.getContainingClass(node);
        }

        return -1;
    }
}
