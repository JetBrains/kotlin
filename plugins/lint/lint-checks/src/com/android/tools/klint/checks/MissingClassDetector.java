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

import static com.android.SdkConstants.ANDROID_PKG_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_FRAGMENT;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.CONSTRUCTOR_NAME;
import static com.android.SdkConstants.TAG_ACTIVITY;
import static com.android.SdkConstants.TAG_APPLICATION;
import static com.android.SdkConstants.TAG_HEADER;
import static com.android.SdkConstants.TAG_PROVIDER;
import static com.android.SdkConstants.TAG_RECEIVER;
import static com.android.SdkConstants.TAG_SERVICE;
import static com.android.SdkConstants.TAG_STRING;
import static com.android.SdkConstants.VIEW_FRAGMENT;
import static com.android.SdkConstants.VIEW_TAG;
import static com.android.resources.ResourceFolderType.LAYOUT;
import static com.android.resources.ResourceFolderType.VALUES;
import static com.android.resources.ResourceFolderType.XML;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
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
import com.android.tools.lint.detector.api.TextFormat;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.SdkUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks to ensure that classes referenced in the manifest actually exist and are included
 *
 */
public class MissingClassDetector extends LayoutDetector implements ClassScanner {
    /** Manifest-referenced classes missing from the project or libraries */
    public static final Issue MISSING = Issue.create(
            "MissingRegistered", //$NON-NLS-1$
            "Missing registered class",

            "If a class is referenced in the manifest, it must also exist in the project (or in one " +
            "of the libraries included by the project. This check helps uncover typos in " +
            "registration names, or attempts to rename or move classes without updating the " +
            "manifest file properly.",

            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            new Implementation(
                    MissingClassDetector.class,
                    EnumSet.of(Scope.MANIFEST, Scope.CLASS_FILE,
                            Scope.JAVA_LIBRARIES, Scope.RESOURCE_FILE)))
            .addMoreInfo("http://developer.android.com/guide/topics/manifest/manifest-intro.html"); //$NON-NLS-1$

    /** Are activity, service, receiver etc subclasses instantiatable? */
    public static final Issue INSTANTIATABLE = Issue.create(
            "Instantiatable", //$NON-NLS-1$
            "Registered class is not instantiatable",

            "Activities, services, broadcast receivers etc. registered in the manifest file " +
            "must be \"instantiatable\" by the system, which means that the class must be " +
            "public, it must have an empty public constructor, and if it's an inner class, " +
            "it must be a static inner class.",

            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            new Implementation(
                    MissingClassDetector.class,
                    Scope.CLASS_FILE_SCOPE));

    /** Is the right character used for inner class separators? */
    public static final Issue INNERCLASS = Issue.create(
            "InnerclassSeparator", //$NON-NLS-1$
            "Inner classes should use `$` rather than `.`",

            "When you reference an inner class in a manifest file, you must use '$' instead of '.' " +
            "as the separator character, i.e. Outer$Inner instead of Outer.Inner.\n" +
            "\n" +
            "(If you get this warning for a class which is not actually an inner class, it's " +
            "because you are using uppercase characters in your package name, which is not " +
            "conventional.)",

            Category.CORRECTNESS,
            3,
            Severity.WARNING,
            new Implementation(
                    MissingClassDetector.class,
                    Scope.MANIFEST_SCOPE));

    private Map<String, Location.Handle> mReferencedClasses;
    private Set<String> mCustomViews;
    private boolean mHaveClasses;

    /** Constructs a new {@link MissingClassDetector} */
    public MissingClassDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements XmlScanner ----

    @Override
    public Collection<String> getApplicableElements() {
        return ALL;
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == VALUES || folderType == LAYOUT || folderType == XML;
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String pkg = null;
        Node classNameNode;
        String className;
        String tag = element.getTagName();
        ResourceFolderType folderType = context.getResourceFolderType();
        if (folderType == VALUES) {
            if (!tag.equals(TAG_STRING)) {
                return;
            }
            Attr attr = element.getAttributeNode(ATTR_NAME);
            if (attr == null) {
                return;
            }
            className = attr.getValue();
            classNameNode = attr;
        } else if (folderType == LAYOUT) {
            if (tag.indexOf('.') > 0) {
                className = tag;
                classNameNode = element;
            } else if (tag.equals(VIEW_FRAGMENT) || tag.equals(VIEW_TAG)) {
                Attr attr = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
                if (attr == null) {
                    attr = element.getAttributeNode(ATTR_CLASS);
                }
                if (attr == null) {
                    return;
                }
                className = attr.getValue();
                classNameNode = attr;
            } else {
                return;
            }
        } else if (folderType == XML) {
            if (!tag.equals(TAG_HEADER)) {
                return;
            }
            Attr attr = element.getAttributeNodeNS(ANDROID_URI, ATTR_FRAGMENT);
            if (attr == null) {
                return;
            }
            className = attr.getValue();
            classNameNode = attr;
        } else {
            // Manifest file
            if (TAG_APPLICATION.equals(tag)
                    || TAG_ACTIVITY.equals(tag)
                    || TAG_SERVICE.equals(tag)
                    || TAG_RECEIVER.equals(tag)
                    || TAG_PROVIDER.equals(tag)) {
                Attr attr = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
                if (attr == null) {
                    return;
                }
                className = attr.getValue();
                classNameNode = attr;
                pkg = context.getMainProject().getPackage();
            } else {
                return;
            }
        }
        if (className.isEmpty()) {
            return;
        }

        String fqcn;
        int dotIndex = className.indexOf('.');
        if (dotIndex <= 0) {
            if (pkg == null) {
                return; // value file
            }
            if (dotIndex == 0) {
                fqcn = pkg + className;
            } else {
                // According to the <activity> manifest element documentation, this is not
                // valid ( http://developer.android.com/guide/topics/manifest/activity-element.html )
                // but it appears in manifest files and appears to be supported by the runtime
                // so handle this in code as well:
                fqcn = pkg + '.' + className;
            }
        } else { // else: the class name is already a fully qualified class name
            fqcn = className;
            // Only look for fully qualified tracker names in analytics files
            if (folderType == VALUES
                    && !SdkUtils.endsWith(context.file.getPath(), "analytics.xml")) { //$NON-NLS-1$
                return;
            }
        }

        String signature = ClassContext.getInternalName(fqcn);
        if (signature.isEmpty() || signature.startsWith(ANDROID_PKG_PREFIX)) {
            return;
        }

        if (!context.getProject().getReportIssues()) {
            // If this is a library project not being analyzed, ignore it
            return;
        }

        Handle handle = null;
        if (!context.getDriver().isSuppressed(context, MISSING, element)) {
            if (mReferencedClasses == null) {
                mReferencedClasses = Maps.newHashMapWithExpectedSize(16);
                mCustomViews = Sets.newHashSetWithExpectedSize(8);
            }

            handle = context.createLocationHandle(element);
            mReferencedClasses.put(signature, handle);
            if (folderType == LAYOUT && !tag.equals(VIEW_FRAGMENT)) {
                mCustomViews.add(ClassContext.getInternalName(className));
            }
        }

        if (signature.indexOf('$') != -1) {
            checkInnerClass(context, element, pkg, classNameNode, className);

            // The internal name contains a $ which means it's an inner class.
            // The conversion from fqcn to internal name is a bit ambiguous:
            // "a.b.C.D" usually means "inner class D in class C in package a.b".
            // However, it can (see issue 31592) also mean class D in package "a.b.C".
            // To make sure we don't falsely complain that foo/Bar$Baz doesn't exist,
            // in case the user has actually created a package named foo/Bar and a proper
            // class named Baz, we register *both* into the reference map.
            // When generating errors we'll look for these an rip them back out if
            // it looks like one of the two variations have been seen.
            if (handle != null) {
                // Assume that each successive $ is really a capitalized package name
                // instead. In other words, for A$B$C$D (assumed to be class A with
                // inner classes A.B, A.B.C and A.B.C.D) generate the following possible
                // referenced classes A/B$C$D (class B in package A with inner classes C and C.D),
                // A/B/C$D and A/B/C/D
                while (true) {
                    int index = signature.indexOf('$');
                    if (index == -1) {
                        break;
                    }
                    signature = signature.substring(0, index) + '/'
                            + signature.substring(index + 1);
                    mReferencedClasses.put(signature, handle);
                    if (folderType == LAYOUT && !tag.equals(VIEW_FRAGMENT)) {
                        mCustomViews.add(signature);
                    }
                }
            }
        }
    }

    private static void checkInnerClass(XmlContext context, Element element, String pkg,
            Node classNameNode, String className) {
        if (pkg != null && className.indexOf('$') == -1 && className.indexOf('.', 1) > 0) {
            boolean haveUpperCase = false;
            for (int i = 0, n = pkg.length(); i < n; i++) {
                if (Character.isUpperCase(pkg.charAt(i))) {
                    haveUpperCase = true;
                    break;
                }
            }
            if (!haveUpperCase) {
                String fixed = className.charAt(0) + className.substring(1).replace('.','$');
                String message = "Use '$' instead of '.' for inner classes " +
                        "(or use only lowercase letters in package names); replace \"" +
                        className + "\" with \"" + fixed + "\"";
                Location location = context.getLocation(classNameNode);
                context.report(INNERCLASS, element, location, message);
            }
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (!context.getProject().isLibrary() && mHaveClasses
                && mReferencedClasses != null && !mReferencedClasses.isEmpty()
                && context.getDriver().getScope().contains(Scope.CLASS_FILE)) {
            List<String> classes = new ArrayList<String>(mReferencedClasses.keySet());
            Collections.sort(classes);
            for (String owner : classes) {
                Location.Handle handle = mReferencedClasses.get(owner);
                String fqcn = ClassContext.getFqcn(owner);

                String signature = ClassContext.getInternalName(fqcn);
                if (!signature.equals(owner)) {
                    if (!mReferencedClasses.containsKey(signature)) {
                        continue;
                    }
                } else if (signature.indexOf('$') != -1) {
                    signature = signature.replace('$', '/');
                    if (!mReferencedClasses.containsKey(signature)) {
                        continue;
                    }
                }
                mReferencedClasses.remove(owner);

                // Ignore usages of platform libraries
                if (owner.startsWith("android/")) { //$NON-NLS-1$
                    continue;
                }

                String message = String.format(
                        "Class referenced in the manifest, `%1$s`, was not found in the " +
                                "project or the libraries", fqcn);
                Location location = handle.resolve();
                File parentFile = location.getFile().getParentFile();
                if (parentFile != null) {
                    String parent = parentFile.getName();
                    ResourceFolderType type = ResourceFolderType.getFolderType(parent);
                    if (type == LAYOUT) {
                        message = String.format(
                            "Class referenced in the layout file, `%1$s`, was not found in "
                                + "the project or the libraries", fqcn);
                    } else if (type == XML) {
                        message = String.format(
                                "Class referenced in the preference header file, `%1$s`, was not "
                                        + "found in the project or the libraries", fqcn);

                    } else if (type == VALUES) {
                        message = String.format(
                                "Class referenced in the analytics file, `%1$s`, was not "
                                        + "found in the project or the libraries", fqcn);
                    }
                }

                context.report(MISSING, location, message);
            }
        }
    }

    // ---- Implements ClassScanner ----

    @Override
    public void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode) {
        if (!mHaveClasses && !context.isFromClassLibrary()
                && context.getProject() == context.getMainProject()) {
            mHaveClasses = true;
        }
        String curr = classNode.name;
        if (mReferencedClasses != null && mReferencedClasses.containsKey(curr)) {
            boolean isCustomView = mCustomViews.contains(curr);
            removeReferences(curr);

            // Ensure that the class is public, non static and has a null constructor!

            if ((classNode.access & Opcodes.ACC_PUBLIC) == 0) {
                context.report(INSTANTIATABLE, context.getLocation(classNode), String.format(
                        "This class should be public (%1$s)",
                            ClassContext.createSignature(classNode.name, null, null)));
                return;
            }

            if (classNode.name.indexOf('$') != -1 && !LintUtils.isStaticInnerClass(classNode)) {
                context.report(INSTANTIATABLE, context.getLocation(classNode), String.format(
                        "This inner class should be static (%1$s)",
                            ClassContext.createSignature(classNode.name, null, null)));
                return;
            }

            boolean hasDefaultConstructor = false;
            @SuppressWarnings("rawtypes") // ASM API
            List methodList = classNode.methods;
            for (Object m : methodList) {
                MethodNode method = (MethodNode) m;
                if (method.name.equals(CONSTRUCTOR_NAME)) {
                    if (method.desc.equals("()V")) { //$NON-NLS-1$
                        // The constructor must be public
                        if ((method.access & Opcodes.ACC_PUBLIC) != 0) {
                            hasDefaultConstructor = true;
                        } else {
                            context.report(INSTANTIATABLE, context.getLocation(method, classNode),
                                    "The default constructor must be public");
                            // Also mark that we have a constructor so we don't complain again
                            // below since we've already emitted a more specific error related
                            // to the default constructor
                            hasDefaultConstructor = true;
                        }
                    }
                }
            }

            if (!hasDefaultConstructor && !isCustomView && !context.isFromClassLibrary()
                    && context.getProject().getReportIssues()) {
                context.report(INSTANTIATABLE, context.getLocation(classNode), String.format(
                        "This class should provide a default constructor (a public " +
                        "constructor with no arguments) (%1$s)",
                            ClassContext.createSignature(classNode.name, null, null)));
            }
        }
    }

    private void removeReferences(String curr) {
        mReferencedClasses.remove(curr);

        // Since "A.B.C" is ambiguous whether it's referencing a class in package A.B or
        // an inner class C in package A, we insert multiple possible references when we
        // encounter the A.B.C reference; now that we've seen the actual class we need to
        // remove all the possible permutations we've added such that the permutations
        // don't count as unreferenced classes.
        int index = curr.lastIndexOf('/');
        if (index == -1) {
            return;
        }
        boolean hasCapitalizedPackageName = false;
        for (int i = index - 1; i >= 0; i--) {
            char c = curr.charAt(i);
            if (Character.isUpperCase(c)) {
                hasCapitalizedPackageName = true;
                break;
            }
        }
        if (!hasCapitalizedPackageName) {
            // No path ambiguity
            return;
        }

        while (true) {
            index = curr.lastIndexOf('/');
            if (index == -1) {
                break;
            }
            curr = curr.substring(0, index) + '$' + curr.substring(index + 1);
            mReferencedClasses.remove(curr);
        }
    }

    /**
     * Given an error message produced by this lint detector for the given issue type,
     * returns the old value to be replaced in the source code.
     * <p>
     * Intended for IDE quickfix implementations.
     *
     * @param issue the corresponding issue
     * @param errorMessage the error message associated with the error
     * @param format the format of the error message
     * @return the corresponding old value, or null if not recognized
     */
    @Nullable
    public static String getOldValue(@NonNull Issue issue, @NonNull String errorMessage,
            @NonNull TextFormat format) {
        if (issue == INNERCLASS) {
            errorMessage = format.toText(errorMessage);
            return LintUtils.findSubstring(errorMessage, " replace \"", "\"");
        }

        return null;
    }

    /**
     * Given an error message produced by this lint detector for the given issue type,
     * returns the new value to be put into the source code.
     * <p>
     * Intended for IDE quickfix implementations.
     *
     * @param issue the corresponding issue
     * @param errorMessage the error message associated with the error
     * @param format the format of the error message
     * @return the corresponding new value, or null if not recognized
     */
    @Nullable
    public static String getNewValue(@NonNull Issue issue, @NonNull String errorMessage,
            @NonNull TextFormat format) {
        if (issue == INNERCLASS) {
            errorMessage = format.toText(errorMessage);
            return LintUtils.findSubstring(errorMessage, " with \"", "\"");
        }
        return null;
    }
}
