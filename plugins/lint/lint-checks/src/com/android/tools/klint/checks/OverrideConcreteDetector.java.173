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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.JavaEvaluator;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.uast.UClass;

import java.util.Collections;
import java.util.List;

/**
 * Checks that subclasses of certain APIs are overriding all methods that were abstract
 * in one or more earlier API levels that are still targeted by the minSdkVersion
 * of this project.
 */
public class OverrideConcreteDetector extends Detector implements Detector.UastScanner {
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

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Collections.singletonList(NOTIFICATION_LISTENER_SERVICE_FQN);
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @NonNull UClass declaration) {
        JavaEvaluator evaluator = context.getEvaluator();
        if (evaluator.isAbstract(declaration)) {
            return;
        }

        int minSdk = Math.max(context.getProject().getMinSdk(), getTargetApi(declaration));
        if (minSdk >= CONCRETE_IN) {
            return;
        }

        String[] methodNames = {ON_NOTIFICATION_POSTED, ON_NOTIFICATION_REMOVED};
        for (String methodName : methodNames) {
            boolean found = false;
            for (PsiMethod method : declaration.findMethodsByName(methodName, true)) {
                // Make sure it's not the base method, but that it's been defined
                // in a subclass, concretely
                PsiClass containingClass = method.getContainingClass();
                if (containingClass == null) {
                    continue;
                }
                if (NOTIFICATION_LISTENER_SERVICE_FQN.equals(containingClass.getQualifiedName())) {
                    continue;
                }
                // Make sure subclass isn't just defining another abstract definition
                // of the method
                if (evaluator.isAbstract(method)) {
                    continue;
                }
                // Make sure it has the exact right signature
                if (method.getParameterList().getParametersCount() != 1) {
                    continue; // Wrong signature
                }
                if (!evaluator.parameterHasType(method, 0, STATUS_BAR_NOTIFICATION_FQN)) {
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
                context.reportUast(ISSUE, declaration, context.getUastNameLocation(declaration), message);
                break;
            }

        }
    }

    private static int getTargetApi(@NonNull PsiClass node) {
        while (node != null) {
            int targetApi = ApiDetector.getTargetApi(node.getModifierList());
            if (targetApi != -1) {
                return targetApi;
            }

            node = PsiTreeUtil.getParentOfType(node, PsiClass.class, true);
        }

        return -1;
    }
}
