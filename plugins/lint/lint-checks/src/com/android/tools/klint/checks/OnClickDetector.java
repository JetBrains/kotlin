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

import static com.android.SdkConstants.ATTR_ON_CLICK;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;

import com.android.annotations.NonNull;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector.ClassScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Location.Handle;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.base.Joiner;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks for missing onClick handlers
 */
public class OnClickDetector extends LayoutDetector implements ClassScanner {
    /** Missing onClick handlers */
    public static final Issue ISSUE = Issue.create(
            "OnClick", //$NON-NLS-1$
            "`onClick` method does not exist",

            "The `onClick` attribute value should be the name of a method in this View's context " +
            "to invoke when the view is clicked. This name must correspond to a public method " +
            "that takes exactly one parameter of type `View`.\n" +
            "\n" +
            "Must be a string value, using '\\;' to escape characters such as '\\n' or " +
            "'\\uxxxx' for a unicode character.",
            Category.CORRECTNESS,
            10,
            Severity.ERROR,
            new Implementation(
                    OnClickDetector.class,
                    Scope.CLASS_AND_ALL_RESOURCE_FILES));

    private Map<String, Location.Handle> mNames;
    private Map<String, List<String>> mSimilar;
    private boolean mHaveBytecode;

    /** Constructs a new {@link OnClickDetector} */
    public OnClickDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mNames != null && !mNames.isEmpty() && mHaveBytecode) {
            List<String> names = new ArrayList<String>(mNames.keySet());
            Collections.sort(names);
            LintDriver driver = context.getDriver();
            for (String name : names) {
                Handle handle = mNames.get(name);

                Object clientData = handle.getClientData();
                if (clientData instanceof Node) {
                    if (driver.isSuppressed(null, ISSUE, (Node) clientData)) {
                        continue;
                    }
                }

                Location location = handle.resolve();
                String message = String.format(
                    "Corresponding method handler '`public void %1$s(android.view.View)`' not found",
                    name);
                List<String> similar = mSimilar != null ? mSimilar.get(name) : null;
                if (similar != null) {
                    Collections.sort(similar);
                  message += String.format(" (did you mean `%1$s` ?)", Joiner.on(", ").join(similar));
                }
                context.report(ISSUE, location, message);
            }
        }
    }

    // ---- Implements XmlScanner ----

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_ON_CLICK);
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        String value = attribute.getValue();
        if (value.isEmpty() || value.trim().isEmpty()) {
            context.report(ISSUE, attribute, context.getLocation(attribute),
                    "`onClick` attribute value cannot be empty");
        } else if (!value.equals(value.trim())) {
            context.report(ISSUE, attribute, context.getLocation(attribute),
                    "There should be no whitespace around attribute values");
        } else if (!value.startsWith(PREFIX_RESOURCE_REF)) { // Not resolved
            if (!context.getProject().getReportIssues()) {
                // If this is a library project not being analyzed, ignore it
                return;
            }

            if (mNames == null) {
                mNames = new HashMap<String, Location.Handle>();
            }
            Handle handle = context.createLocationHandle(attribute);
            handle.setClientData(attribute);

            // Replace unicode characters with the actual value since that's how they
            // appear in the ASM signatures
            if (value.contains("\\u")) { //$NON-NLS-1$
                Pattern pattern = Pattern.compile("\\\\u(\\d\\d\\d\\d)"); //$NON-NLS-1$
                Matcher matcher = pattern.matcher(value);
                StringBuilder sb = new StringBuilder(value.length());
                int remainder = 0;
                while (matcher.find()) {
                    sb.append(value.substring(0, matcher.start()));
                    String unicode = matcher.group(1);
                    int hex = Integer.parseInt(unicode, 16);
                    sb.append((char) hex);
                    remainder = matcher.end();
                }
                sb.append(value.substring(remainder));
                value = sb.toString();
            }

            mNames.put(value, handle);
        }
    }

    // ---- Implements ClassScanner ----

    @SuppressWarnings("rawtypes")
    @Override
    public void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode) {
        if (mNames == null) {
            // No onClick attributes in the XML files
            return;
        }

        mHaveBytecode = true;

        List methodList = classNode.methods;
        for (Object m : methodList) {
            MethodNode method = (MethodNode) m;
            boolean rightArguments = method.desc.equals("(Landroid/view/View;)V"); //$NON-NLS-1$
            if (!mNames.containsKey(method.name)) {
                if (rightArguments) {
                    // See if there's a possible typo instead
                    for (String n : mNames.keySet()) {
                        if (LintUtils.editDistance(n, method.name) <= 2) {
                            recordSimilar(n, classNode, method);
                            break;
                        }
                    }
                }
                continue;
            }

            // TODO: Validate class hierarchy: should extend a context method
            // Longer term, also validate that it's in a layout that corresponds to
            // the given activity

            if (rightArguments){
                // Found: remove from list to be checked
                mNames.remove(method.name);

                // Make sure the method is public
                if ((method.access & Opcodes.ACC_PUBLIC) == 0) {
                    Location location = context.getLocation(method, classNode);
                    String message = String.format(
                            "On click handler `%1$s(View)` must be public",
                            method.name);
                    context.report(ISSUE, location, message);
                } else if ((method.access & Opcodes.ACC_STATIC) != 0) {
                    Location location = context.getLocation(method, classNode);
                    String message = String.format(
                            "On click handler `%1$s(View)` should not be static",
                            method.name);
                    context.report(ISSUE, location, message);
                }

                if (mNames.isEmpty()) {
                    mNames = null;
                    return;
                }
            }
        }
    }

    private void recordSimilar(String name, ClassNode classNode, MethodNode method) {
        if (mSimilar == null) {
            mSimilar = new HashMap<String, List<String>>();
        }
        List<String> list = mSimilar.get(name);
        if (list == null) {
            list = new ArrayList<String>();
            mSimilar.put(name, list);
        }

        String signature = ClassContext.createSignature(classNode.name, method.name, method.desc);
        list.add(signature);
    }
}
