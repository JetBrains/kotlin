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
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Looks for usages of Java packages that are not included in Android.
 */
public class InvalidPackageDetector extends Detector implements Detector.ClassScanner {
    /** Accessing an invalid package */
    public static final Issue ISSUE = Issue.create(
            "InvalidPackage", //$NON-NLS-1$
            "Package not included in Android",

            "This check scans through libraries looking for calls to APIs that are not included " +
            "in Android.\n" +
            "\n" +
            "When you create Android projects, the classpath is set up such that you can only " +
            "access classes in the API packages that are included in Android. However, if you " +
            "add other projects to your libs/ folder, there is no guarantee that those .jar " +
            "files were built with an Android specific classpath, and in particular, they " +
            "could be accessing unsupported APIs such as java.applet.\n" +
            "\n" +
            "This check scans through library jars and looks for references to API packages " +
            "that are not included in Android and flags these. This is only an error if your " +
            "code calls one of the library classes which wind up referencing the unsupported " +
            "package.",

            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(
                    InvalidPackageDetector.class,
                    Scope.JAVA_LIBRARY_SCOPE));

    private static final String JAVA_PKG_PREFIX = "java/";    //$NON-NLS-1$
    private static final String JAVAX_PKG_PREFIX = "javax/";  //$NON-NLS-1$

    private ApiLookup mApiDatabase;

    /**
     * List of candidates that are potential package violations. These are
     * recorded as candidates rather than flagged immediately such that we can
     * filter out hits for classes that are also defined as libraries (possibly
     * encountered later in the library traversal).
     */
    private List<Candidate> mCandidates;
    /**
     * Set of Java packages defined in the libraries; this means that if the
     * user has added libraries in this package namespace (such as the
     * null annotations jars) we don't flag these.
     */
    private final Set<String> mJavaxLibraryClasses = Sets.newHashSetWithExpectedSize(64);

    /** Constructs a new package check */
    public InvalidPackageDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.SLOW;
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        mApiDatabase = ApiLookup.get(context.getClient());
    }

    // ---- Implements ClassScanner ----

    @SuppressWarnings("rawtypes") // ASM API
    @Override
    public void checkClass(@NonNull final ClassContext context, @NonNull ClassNode classNode) {
        if (!context.isFromClassLibrary() || shouldSkip(context.file)) {
            return;
        }

        if (mApiDatabase == null) {
            return;
        }

        if ((classNode.access & Opcodes.ACC_ANNOTATION) != 0
                || classNode.superName.startsWith("javax/annotation/")) {
            // Don't flag references from annotations and annotation processors
            return;
        }
        if (classNode.name.startsWith(JAVAX_PKG_PREFIX)) {
            mJavaxLibraryClasses.add(classNode.name);
        }

        List methodList = classNode.methods;
        for (Object m : methodList) {
            MethodNode method = (MethodNode) m;

            InsnList nodes = method.instructions;

            // Check return type
            // The parameter types are already handled as local variables so we can skip
            // right to the return type.
            // Check types in parameter list
            String signature = method.desc;
            if (signature != null) {
                int args = signature.indexOf(')');
                if (args != -1 && signature.charAt(args + 1) == 'L') {
                    String type = signature.substring(args + 2, signature.length() - 1);
                    if (isInvalidPackage(type)) {
                        AbstractInsnNode first = nodes.size() > 0 ? nodes.get(0) : null;
                        record(context, method, first, type);
                    }
                }
            }

            for (int i = 0, n = nodes.size(); i < n; i++) {
                AbstractInsnNode instruction = nodes.get(i);
                int type = instruction.getType();
                if (type == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode node = (MethodInsnNode) instruction;
                    String owner = node.owner;

                    // No need to check methods in this local class; we know they
                    // won't be an API match
                    if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                            && owner.equals(classNode.name)) {
                        owner = classNode.superName;
                    }

                    while (owner != null) {
                        if (isInvalidPackage(owner)) {
                            record(context, method, instruction, owner);
                        }

                        // For virtual dispatch, walk up the inheritance chain checking
                        // each inherited method
                        if (owner.startsWith("android/")           //$NON-NLS-1$
                                || owner.startsWith(JAVA_PKG_PREFIX)
                                || owner.startsWith(JAVAX_PKG_PREFIX)) {
                            owner = null;
                        } else if (node.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                            owner = context.getDriver().getSuperClass(owner);
                        } else if (node.getOpcode() == Opcodes.INVOKESTATIC) {
                            // Inherit through static classes as well
                            owner = context.getDriver().getSuperClass(owner);
                        } else {
                            owner = null;
                        }
                    }
                } else if (type == AbstractInsnNode.FIELD_INSN) {
                    FieldInsnNode node = (FieldInsnNode) instruction;
                    String owner = node.owner;
                    if (isInvalidPackage(owner)) {
                        record(context, method, instruction, owner);
                    }
                } else if (type == AbstractInsnNode.LDC_INSN) {
                    LdcInsnNode node = (LdcInsnNode) instruction;
                    if (node.cst instanceof Type) {
                        Type t = (Type) node.cst;
                        String className = t.getInternalName();
                        if (isInvalidPackage(className)) {
                            record(context, method, instruction, className);
                        }
                    }
                }
            }
        }
    }

    private boolean isInvalidPackage(String owner) {
        if (owner.startsWith(JAVA_PKG_PREFIX)) {
            return !mApiDatabase.isValidJavaPackage(owner);
        }

        if (owner.startsWith(JAVAX_PKG_PREFIX)) {
            // Annotations-related code is usually fine; these tend to be for build time
            // jars, such as dagger
            //noinspection SimplifiableIfStatement
            if (owner.startsWith("javax/annotation/") || owner.startsWith("javax/lang/model")) {
                return false;
            }

            return !mApiDatabase.isValidJavaPackage(owner);
        }

        return false;
    }

    private void record(ClassContext context, MethodNode method,
            AbstractInsnNode instruction, String owner) {
        if (owner.indexOf('$') != -1) {
            // Don't report inner classes too; there will pretty much always be an outer class
            // reference as well
            return;
        }

        if (mCandidates == null) {
            mCandidates = Lists.newArrayList();
        }
        mCandidates.add(new Candidate(owner, context.getClassNode().name, context.getJarFile()));
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mCandidates == null) {
            return;
        }

        Set<String> seen = Sets.newHashSet();

        for (Candidate candidate : mCandidates) {
            String type = candidate.mClass;
            if (mJavaxLibraryClasses.contains(type)) {
                continue;
            }
            File jarFile = candidate.mJarFile;
            String referencedIn = candidate.mReferencedIn;

            Location location = Location.create(jarFile);
            String pkg = getPackageName(type);
            if (seen.contains(pkg)) {
                continue;
            }
            seen.add(pkg);

            if (pkg.equals("javax.inject")) {
                String name = jarFile.getName();
                //noinspection SpellCheckingInspection
                if (name.startsWith("dagger-") || name.startsWith("guice-")) {
                    // White listed
                    return;
                }
            }

            String message = String.format(
                    "Invalid package reference in library; not included in Android: `%1$s`. " +
                    "Referenced from `%2$s`.", pkg, ClassContext.getFqcn(referencedIn));
            context.report(ISSUE, location, message);
        }
    }

    private static String getPackageName(String owner) {
        String pkg = owner;
        int index = pkg.lastIndexOf('/');
        if (index != -1) {
            pkg = pkg.substring(0, index);
        }

        return ClassContext.getFqcn(pkg);
    }

    private static boolean shouldSkip(File file) {
        // No need to do work on this library, which is included in pretty much all new ADT
        // projects
        return file.getPath().endsWith("android-support-v4.jar");

    }

    private static class Candidate {
        private final String mReferencedIn;
        private final File mJarFile;
        private final String mClass;

        public Candidate(String className, String referencedIn, File jarFile) {
            mClass = className;
            mReferencedIn = referencedIn;
            mJarFile = jarFile;
        }
    }
}
