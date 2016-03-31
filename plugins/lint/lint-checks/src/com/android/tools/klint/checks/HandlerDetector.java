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

package com.android.tools.lint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser.ResolvedClass;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.TypeDescriptor;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import lombok.ast.ClassDeclaration;
import lombok.ast.Node;

/**
 * Checks that Handler implementations are top level classes or static.
 * See the corresponding check in the android.os.Handler source code.
 */
public class HandlerDetector extends Detector implements Detector.JavaScanner {

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

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Collections.singletonList(HANDLER_CLS);
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @Nullable ClassDeclaration declaration,
            @NonNull Node node, @NonNull ResolvedClass cls) {
        if (!isInnerClass(declaration)) {
            return;
        }

        if (isStaticClass(declaration)) {
            return;
        }

        // Only flag handlers using the default looper
        if (hasLooperConstructorParameter(cls)) {
            return;
        }

        Node locationNode = node instanceof ClassDeclaration
                ? ((ClassDeclaration) node).astName() : node;
        Location location = context.getLocation(locationNode);
        context.report(ISSUE, location, String.format(
                "This Handler class should be static or leaks might occur (%1$s)",
                cls.getName()));
    }

    private static boolean isInnerClass(@Nullable ClassDeclaration node) {
        return node == null || // null class declarations means anonymous inner class
                JavaContext.getParentOfType(node, ClassDeclaration.class, true) != null;
    }

    private static boolean isStaticClass(@Nullable ClassDeclaration node) {
        if (node == null) {
            // A null class declaration means anonymous inner class, and these can't be static
            return false;
        }

        int flags = node.astModifiers().getEffectiveModifierFlags();
        return (flags & Modifier.STATIC) != 0;
    }

    private static boolean hasLooperConstructorParameter(@NonNull ResolvedClass cls) {
        for (ResolvedMethod constructor : cls.getConstructors()) {
            for (int i = 0, n = constructor.getArgumentCount(); i < n; i++) {
                TypeDescriptor type = constructor.getArgumentType(i);
                if (type.matchesSignature(LOOPER_CLS)) {
                    return true;
                }
            }
        }
        return false;
    }
}
