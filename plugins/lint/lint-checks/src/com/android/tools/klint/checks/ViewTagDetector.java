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
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.SdkInfo;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.ClassScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.Collections;
import java.util.List;

/**
 * Checks for missing view tag detectors
 */
public class ViewTagDetector extends Detector implements ClassScanner {
    /** Using setTag and leaking memory */
    public static final Issue ISSUE = Issue.create(
            "ViewTag", //$NON-NLS-1$
            "Tagged object leaks",

            "Prior to Android 4.0, the implementation of `View.setTag(int, Object)` would " +
            "store the objects in a static map, where the values were strongly referenced. " +
            "This means that if the object contains any references pointing back to the " +
            "context, the context (which points to pretty much everything else) will leak. " +
            "If you pass a view, the view provides a reference to the context " +
            "that created it. Similarly, view holders typically contain a view, and cursors " +
            "are sometimes also associated with views.",

            Category.PERFORMANCE,
            6,
            Severity.WARNING,
            new Implementation(
                    ViewTagDetector.class,
                    Scope.CLASS_FILE_SCOPE));

    /** Constructs a new {@link ViewTagDetector} */
    public ViewTagDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements ClassScanner ----

    @Override
    @Nullable
    public List<String> getApplicableCallNames() {
        return Collections.singletonList("setTag"); //$NON-NLS-1$
    }

    @Override
    public void checkCall(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull MethodInsnNode call) {
        // The leak behavior is fixed in ICS:
        // http://code.google.com/p/android/issues/detail?id=18273
        if (context.getMainProject().getMinSdk() >= 14) {
            return;
        }

        String owner = call.owner;
        String desc = call.desc;
        if (owner.equals("android/view/View")                 //$NON-NLS-1$
                && desc.equals("(ILjava/lang/Object;)V")) {   //$NON-NLS-1$
            Analyzer analyzer = new Analyzer(new BasicInterpreter() {
                @Override
                public BasicValue newValue(Type type) {
                    if (type == null) {
                        return BasicValue.UNINITIALIZED_VALUE;
                    } else if (type.getSort() == Type.VOID) {
                        return null;
                    } else {
                        return new BasicValue(type);
                    }
                }
            });
            try {
                Frame[] frames = analyzer.analyze(classNode.name, method);
                InsnList instructions = method.instructions;
                Frame frame = frames[instructions.indexOf(call)];
                if (frame.getStackSize() < 3) {
                    return;
                }
                BasicValue stackValue = (BasicValue) frame.getStack(2);
                Type type = stackValue.getType();
                if (type == null) {
                    return;
                }

                String internalName = type.getInternalName();
                String className = type.getClassName();
                LintDriver driver = context.getDriver();

                SdkInfo sdkInfo = context.getClient().getSdkInfo(context.getMainProject());
                String objectType = null;
                while (className != null) {
                    if (className.equals("android.view.View")) {         //$NON-NLS-1$
                        objectType = "views";
                        break;
                    } else if (className.endsWith("ViewHolder")) {       //$NON-NLS-1$
                        objectType = "view holders";
                        break;
                    } else if (className.endsWith("Cursor")              //$NON-NLS-1$
                                && className.startsWith("android.")) {   //$NON-NLS-1$
                        objectType = "cursors";
                        break;
                    }

                    // TBD: Bitmaps, drawables? That's tricky, because as explained in
                    // http://android-developers.blogspot.com/2009/01/avoiding-memory-leaks.html
                    // apparently these are used along with nulling out the callbacks,
                    // and that's harder to detect here

                    String parent = sdkInfo.getParentViewClass(className);
                    if (parent == null) {
                        if (internalName == null) {
                            internalName = className.replace('.', '/');
                        }
                        assert internalName != null;
                        parent = driver.getSuperClass(internalName);
                    }
                    className = parent;
                    internalName = null;
                }

                if (objectType != null) {
                    Location location = context.getLocation(call);
                    String message = String.format("Avoid setting %1$s as values for `setTag`: " +
                        "Can lead to memory leaks in versions older than Android 4.0",
                        objectType);
                    context.report(ISSUE, method, call, location, message);
                }
            } catch (AnalyzerException e) {
                context.log(e, null);
            }
        }
    }
}
