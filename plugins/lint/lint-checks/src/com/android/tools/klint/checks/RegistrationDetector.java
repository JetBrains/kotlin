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

import static com.android.SdkConstants.ANDROID_APP_ACTIVITY;
import static com.android.SdkConstants.ANDROID_APP_SERVICE;
import static com.android.SdkConstants.ANDROID_CONTENT_BROADCAST_RECEIVER;
import static com.android.SdkConstants.ANDROID_CONTENT_CONTENT_PROVIDER;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.TAG_ACTIVITY;
import static com.android.SdkConstants.TAG_PROVIDER;
import static com.android.SdkConstants.TAG_RECEIVER;
import static com.android.SdkConstants.TAG_SERVICE;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Detector.ClassScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map.Entry;

/**
 * Checks for missing manifest registrations for activities, services etc
 * and also makes sure that they are registered with the correct tag
 * <p>
 * TODO: Rewrite as Java visitor!
 */
public class RegistrationDetector extends LayoutDetector implements ClassScanner {
    /** Unregistered activities and services */
    public static final Issue ISSUE = Issue.create(
            "Registered", //$NON-NLS-1$
            "Class is not registered in the manifest",

            "Activities, services and content providers should be registered in the " +
            "`AndroidManifest.xml` file using `<activity>`, `<service>` and `<provider>` tags.\n" +
            "\n" +
            "If your activity is simply a parent class intended to be subclassed by other " +
            "\"real\" activities, make it an abstract class.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    RegistrationDetector.class,
                    EnumSet.of(Scope.MANIFEST, Scope.CLASS_FILE)))
            .addMoreInfo(
            "http://developer.android.com/guide/topics/manifest/manifest-intro.html"); //$NON-NLS-1$

    protected Multimap<String, String> mManifestRegistrations;

    /** Constructs a new {@link RegistrationDetector} */
    public RegistrationDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements XmlScanner ----

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(sTags);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String fqcn = getFqcn(context, element);
        String tag = element.getTagName();
        String frameworkClass = tagToClass(tag);
        if (frameworkClass != null) {
            String signature = ClassContext.getInternalName(fqcn);
            if (mManifestRegistrations == null) {
                mManifestRegistrations = ArrayListMultimap.create(4, 8);
            }
            mManifestRegistrations.put(frameworkClass, signature);
            if (signature.indexOf('$') != -1) {
                // The internal name contains a $ which means it's an inner class.
                // The conversion from fqcn to internal name is a bit ambiguous:
                // "a.b.C.D" usually means "inner class D in class C in package a.b".
                // However, it can (see issue 31592) also mean class D in package "a.b.C".
                // Place *both* of these possibilities in the registered map, since this
                // is only used to check that an activity is registered, not the other way
                // (so it's okay to have entries there that do not correspond to real classes).
                signature = signature.replace('$', '/');
                mManifestRegistrations.put(frameworkClass, signature);
            }
        }
    }

    /**
     * Returns the fully qualified class name for a manifest entry element that
     * specifies a name attribute
     *
     * @param context the query context providing the project
     * @param element the element
     * @return the fully qualified class name
     */
    @NonNull
    private static String getFqcn(@NonNull XmlContext context, @NonNull Element element) {
        String className = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
        if (className.startsWith(".")) { //$NON-NLS-1$
            return context.getMainProject().getPackage() + className;
        } else if (className.indexOf('.') == -1) {
            // According to the <activity> manifest element documentation, this is not
            // valid ( http://developer.android.com/guide/topics/manifest/activity-element.html )
            // but it appears in manifest files and appears to be supported by the runtime
            // so handle this in code as well:
            return context.getMainProject().getPackage() + '.' + className;
        } // else: the class name is already a fully qualified class name

        return className;
    }

    // ---- Implements ClassScanner ----

    @Override
    public void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode) {
        // Abstract classes do not need to be registered
        if ((classNode.access & Opcodes.ACC_ABSTRACT) != 0) {
            return;
        }
        String curr = classNode.name;

        int lastIndex = curr.lastIndexOf('$');
        if (lastIndex != -1 && lastIndex < curr.length() - 1) {
            if (Character.isDigit(curr.charAt(lastIndex+1))) {
                // Anonymous inner class, doesn't need to be registered
                return;
            }
        }

        while (curr != null) {
            for (String s : sClasses) {
                if (curr.equals(s)) {
                    Collection<String> registered = mManifestRegistrations != null ?
                            mManifestRegistrations.get(curr) : null;
                    if (registered == null || !registered.contains(classNode.name)) {
                        report(context, classNode, curr);
                    }

                }
            }

            curr = context.getDriver().getSuperClass(curr);
        }
    }

    private void report(ClassContext context, ClassNode classNode, String curr) {
        String tag = classToTag(curr);
        String className = ClassContext.createSignature(classNode.name, null, null);

        String wrongClass = null; // The framework class this class actually extends
        if (mManifestRegistrations != null) {
            Collection<Entry<String,String>> entries =
                    mManifestRegistrations.entries();
            for (Entry<String,String> entry : entries) {
                if (entry.getValue().equals(classNode.name)) {
                    wrongClass = entry.getKey();
                    break;
                }
            }
        }
        if (wrongClass != null) {
            Location location = context.getLocation(classNode);
            context.report(
                    ISSUE,
                    location,
                    String.format(
                            "`%1$s` is a `<%2$s>` but is registered in the manifest as a `<%3$s>`",
                            className, tag, classToTag(wrongClass)));
        } else if (!TAG_RECEIVER.equals(tag)) { // don't need to be registered
            if (context.getMainProject().isGradleProject()) {
                // Disabled for now; we need to formalize the difference between
                // the *manifest* package and the variant package, since in some contexts
                // (such as manifest registrations) we should be using the manifest package,
                // not the gradle package
                return;
            }
            Location location = context.getLocation(classNode);
            context.report(
                    ISSUE,
                    location,
                    String.format(
                            "The `<%1$s> %2$s` is not registered in the manifest",
                            tag, className));
        }
    }

    /** The manifest tags we care about */
    private static final String[] sTags = new String[] {
        TAG_ACTIVITY,
        TAG_SERVICE,
        TAG_RECEIVER,
        TAG_PROVIDER,
        // Keep synchronized with {@link #sClasses}
    };

    /** The corresponding framework classes that the tags in {@link #sTags} should extend */
    private static final String[] sClasses = new String[] {
            ANDROID_APP_ACTIVITY,
            ANDROID_APP_SERVICE,
            ANDROID_CONTENT_BROADCAST_RECEIVER,
            ANDROID_CONTENT_CONTENT_PROVIDER,
            // Keep synchronized with {@link #sTags}
    };

    /** Looks up the corresponding framework class a given manifest tag's class should extend */
    private static String tagToClass(String tag) {
        for (int i = 0, n = sTags.length; i < n; i++) {
            if (sTags[i].equals(tag)) {
                return sClasses[i];
            }
        }

        return null;
    }

    /** Looks up the tag a given framework class should be registered with */
    protected static String classToTag(String className) {
        for (int i = 0, n = sClasses.length; i < n; i++) {
            if (sClasses[i].equals(className)) {
                return sTags[i];
            }
        }

        return null;
    }
}
