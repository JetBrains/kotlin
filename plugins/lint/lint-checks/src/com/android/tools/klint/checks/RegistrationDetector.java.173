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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.CLASS_ACTIVITY;
import static com.android.SdkConstants.CLASS_APPLICATION;
import static com.android.SdkConstants.CLASS_BROADCASTRECEIVER;
import static com.android.SdkConstants.CLASS_CONTENTPROVIDER;
import static com.android.SdkConstants.CLASS_SERVICE;
import static com.android.SdkConstants.TAG_ACTIVITY;
import static com.android.SdkConstants.TAG_APPLICATION;
import static com.android.SdkConstants.TAG_PROVIDER;
import static com.android.SdkConstants.TAG_RECEIVER;
import static com.android.SdkConstants.TAG_SERVICE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.ProductFlavorContainer;
import com.android.builder.model.SourceProviderContainer;
import com.android.tools.klint.client.api.JavaEvaluator;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.LayoutDetector;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.XmlContext;
import com.android.utils.SdkUtils;
import com.google.common.collect.Maps;
import com.intellij.psi.PsiClass;

import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.uast.UClass;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Checks for missing manifest registrations for activities, services etc
 * and also makes sure that they are registered with the correct tag
 */
public class RegistrationDetector extends LayoutDetector implements Detector.UastScanner {
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
                    EnumSet.of(Scope.MANIFEST, Scope.JAVA_FILE)))
            .addMoreInfo(
            "http://developer.android.com/guide/topics/manifest/manifest-intro.html"); //$NON-NLS-1$

    protected Map<String, String> mManifestRegistrations;

    /** Constructs a new {@link RegistrationDetector} */
    public RegistrationDetector() {
    }

    // ---- Implements XmlScanner ----

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(sTags);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (!element.hasAttributeNS(ANDROID_URI, ATTR_NAME)) {
            // For example, application appears in manifest and doesn't always have a name
            return;
        }
        String fqcn = getFqcn(context, element);
        String tag = element.getTagName();
        String frameworkClass = tagToClass(tag);
        if (frameworkClass != null) {
            String signature = fqcn;
            if (mManifestRegistrations == null) {
                mManifestRegistrations = Maps.newHashMap();
            }
            mManifestRegistrations.put(signature, frameworkClass);
            if (signature.indexOf('$') != -1) {
                signature = signature.replace('$', '.');
                mManifestRegistrations.put(signature, frameworkClass);
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
            return context.getProject().getPackage() + className;
        } else if (className.indexOf('.') == -1) {
            // According to the <activity> manifest element documentation, this is not
            // valid ( http://developer.android.com/guide/topics/manifest/activity-element.html )
            // but it appears in manifest files and appears to be supported by the runtime
            // so handle this in code as well:
            return context.getProject().getPackage() + '.' + className;
        } // else: the class name is already a fully qualified class name

        return className;
    }

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Arrays.asList(
                // Common super class for Activity, ContentProvider, Service, Application
                // (as well as some other classes not registered in the manifest, such as
                // Fragment and VoiceInteractionSession)
                "android.content.ComponentCallbacks2",
                CLASS_BROADCASTRECEIVER);
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @NonNull UClass cls) {
        if (cls.getName() == null) {
            // anonymous class; can't be registered
            return;
        }

        JavaEvaluator evaluator = context.getEvaluator();
        if (evaluator.isAbstract(cls) || evaluator.isPrivate(cls)) {
            // Abstract classes do not need to be registered, and
            // private classes are clearly not intended to be registered
            return;
        }

        String rightTag = getTag(evaluator, cls);
        if (rightTag == null) {
            // some non-registered Context, such as a BackupAgent
            return;
        }
        String className = cls.getQualifiedName();
        if (className == null) {
            return;
        }
        if (mManifestRegistrations != null) {
            String framework = mManifestRegistrations.get(className);
            if (framework == null) {
                reportMissing(context, cls, className, rightTag);
            } else if (!InheritanceUtil.isInheritor(cls, false, framework)) {
                reportWrongTag(context, cls, rightTag, className, framework);
            }
        } else {
            reportMissing(context, cls, className, rightTag);
        }
    }

    private static void reportWrongTag(
            @NonNull JavaContext context,
            @NonNull PsiClass node,
            @NonNull String rightTag,
            @NonNull String className,
            @NonNull String framework) {
        String wrongTag = classToTag(framework);
        if (wrongTag == null) {
            return;
        }
        Location location = context.getNameLocation(node);
        String message = String.format("`%1$s` is %2$s but is registered "
                        + "in the manifest as %3$s", className, describeTag(rightTag),
                describeTag(wrongTag));
        context.report(ISSUE, node, location, message);
    }

    private static String describeTag(@NonNull String tag) {
        String article = tag.startsWith("a") ? "an" : "a"; // an for activity and application
        return String.format("%1$s `<%2$s>`", article, tag);
    }

    private static void reportMissing(
            @NonNull JavaContext context,
            @NonNull PsiClass node,
            @NonNull String className,
            @NonNull String tag) {
        if (tag.equals(TAG_RECEIVER)) {
            // Receivers can be registered in code; don't flag these.
            return;
        }

        // Don't flag activities registered in test source sets
        if (context.getProject().isGradleProject()) {
            AndroidProject model = context.getProject().getGradleProjectModel();
            if (model != null) {
                String javaSource = context.file.getPath();
                // Test source set?

                for (SourceProviderContainer extra : model.getDefaultConfig().getExtraSourceProviders()) {
                    String artifactName = extra.getArtifactName();
                    if (AndroidProject.ARTIFACT_ANDROID_TEST.equals(artifactName)) {
                        for (File file : extra.getSourceProvider().getJavaDirectories()) {
                            if (SdkUtils.startsWithIgnoreCase(javaSource, file.getPath())) {
                                return;
                            }
                        }
                    }
                }

                for (ProductFlavorContainer container : model.getProductFlavors()) {
                    for (SourceProviderContainer extra : container.getExtraSourceProviders()) {
                        String artifactName = extra.getArtifactName();
                        if (AndroidProject.ARTIFACT_ANDROID_TEST.equals(artifactName)) {
                            for (File file : extra.getSourceProvider().getJavaDirectories()) {
                                if (SdkUtils.startsWithIgnoreCase(javaSource, file.getPath())) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        Location location = context.getNameLocation(node);
        String message = String.format("The `<%1$s> %2$s` is not registered in the manifest",
                tag, className);
        context.report(ISSUE, node, location, message);
    }

    private static String getTag(@NonNull JavaEvaluator evaluator, @NonNull PsiClass cls) {
        String tag = null;
        for (String s : sClasses) {
            if (InheritanceUtil.isInheritor(cls, false, s)) {
                tag = classToTag(s);
                break;
            }
        }
        return tag;
    }

    /** The manifest tags we care about */
    private static final String[] sTags = new String[] {
        TAG_ACTIVITY,
        TAG_SERVICE,
        TAG_RECEIVER,
        TAG_PROVIDER,
        TAG_APPLICATION
        // Keep synchronized with {@link #sClasses}
    };

    /** The corresponding framework classes that the tags in {@link #sTags} should extend */
    private static final String[] sClasses = new String[] {
            CLASS_ACTIVITY,
            CLASS_SERVICE,
            CLASS_BROADCASTRECEIVER,
            CLASS_CONTENTPROVIDER,
            CLASS_APPLICATION
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
