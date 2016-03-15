/*
 * Copyright (C) 2012 The Android Open Source Project
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
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;

import java.util.Collections;
import java.util.List;

import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.kinds.UastClassKind;

/**
 * Checks that Handler implementations are top level classes or static.
 * See the corresponding check in the android.os.Handler source code.
 */
public class HandlerDetector extends Detector implements UastScanner {

    /** Potentially leaking handlers */
    public static final Issue ISSUE = Issue.create(
            "HandlerLeak", //$NON-NLS-1$
            "Handler reference leaks",

            "Since this Handler is declared as an inner class, it may prevent the outer " +
            "class from being garbage collected. If the Handler is using a Looper or " +
            "MessageQueue for a thread other than the main thread, then there is no issue. " +
            "If the Handler is using the Looper or MessageQueue of the main thread, you " +
            "need to fix your Handler declaration, as follows: Declare the Handler as a " +
            "static class; In the outer class, instantiate a WeakReference to the outer " +
            "class and pass this object to your Handler when you instantiate the Handler; " +
            "Make all references to members of the outer class using the WeakReference object.",

            Category.PERFORMANCE,
            4,
            Severity.WARNING,
            new Implementation(
                    HandlerDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    private static final String LOOPER_CLS = "android.os.Looper";
    private static final String HANDLER_CLS = "android.os.Handler";

    /** Constructs a new {@link HandlerDetector} */
    public HandlerDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements UastScanner ----


    @Override
    public List<String> getApplicableSuperClasses() {
        return Collections.singletonList(HANDLER_CLS);
    }

    @Override
    public void visitClass(UastAndroidContext context, UClass node) {
        if (UastUtils.isTopLevel(node)) {
            return;
        }

        if (node.hasModifier(UastModifier.STATIC)) {
            return;
        }

        // Only flag handlers using the default looper
        if (hasLooperConstructorParameter(node)) {
            return;
        }

        UElement locationNode = node.getNameElement();
        Location location = UastAndroidUtils.getLocation(locationNode);
        context.report(ISSUE, node, location, String.format(
          "This Handler class should be static or leaks might occur (%1$s)",
          node.getName()));

    }

    private static boolean hasLooperConstructorParameter(@NonNull UClass cls) {
        for (UFunction constructor : cls.getConstructors()) {
            for (int i = 0, n = constructor.getValueParameterCount(); i < n; i++) {
                UType type = constructor.getValueParameters().get(i).getType();
                if (type.matchesFqName(LOOPER_CLS)) {
                    return true;
                }
            }
        }
        return false;
    }
}
