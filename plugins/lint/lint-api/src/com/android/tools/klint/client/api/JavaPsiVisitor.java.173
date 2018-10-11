/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.tools.klint.client.api;

import static com.android.SdkConstants.ANDROID_PKG;
import static com.android.SdkConstants.R_CLASS;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.resources.ResourceType;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.klint.detector.api.Detector.XmlScanner;
import com.android.tools.klint.detector.api.JavaContext;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.ImplicitVariable;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiArrayAccessExpression;
import com.intellij.psi.PsiArrayInitializerExpression;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiAssertStatement;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiBreakStatement;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiCatchSection;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiContinueStatement;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiDoWhileStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEmptyStatement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiEnumConstantInitializer;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiExpressionListStatement;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiIfStatement;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiImportStaticReferenceElement;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiInstanceOfExpression;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiLabeledStatement;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiPackageStatement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiParenthesizedExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiPostfixExpression;
import com.intellij.psi.PsiPrefixExpression;
import com.intellij.psi.PsiReceiverParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiResourceList;
import com.intellij.psi.PsiResourceVariable;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiSuperExpression;
import com.intellij.psi.PsiSwitchLabelStatement;
import com.intellij.psi.PsiSwitchStatement;
import com.intellij.psi.PsiSynchronizedStatement;
import com.intellij.psi.PsiThisExpression;
import com.intellij.psi.PsiThrowStatement;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.PsiTypeCastExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWhileStatement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocTagValue;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.javadoc.PsiInlineDocTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specialized visitor for running detectors on a Java AST.
 * It operates in three phases:
 * <ol>
 *   <li> First, it computes a set of maps where it generates a map from each
 *        significant AST attribute (such as method call names) to a list
 *        of detectors to consult whenever that attribute is encountered.
 *        Examples of "attributes" are method names, Android resource identifiers,
 *        and general AST node types such as "cast" nodes etc. These are
 *        defined on the {@link JavaPsiScanner} interface.
 *   <li> Second, it iterates over the document a single time, delegating to
 *        the detectors found at each relevant AST attribute.
 *   <li> Finally, it calls the remaining visitors (those that need to process a
 *        whole document on their own).
 * </ol>
 * It also notifies all the detectors before and after the document is processed
 * such that they can do pre- and post-processing.
 */
public class JavaPsiVisitor {
    /** Default size of lists holding detectors of the same type for a given node type */
    private static final int SAME_TYPE_COUNT = 8;

    private final Map<String, List<VisitingDetector>> mMethodDetectors =
            Maps.newHashMapWithExpectedSize(80);
    private final Map<String, List<VisitingDetector>> mConstructorDetectors =
            Maps.newHashMapWithExpectedSize(12);
    private final Map<String, List<VisitingDetector>> mReferenceDetectors =
            Maps.newHashMapWithExpectedSize(10);
    private Set<String> mConstructorSimpleNames;
    private final List<VisitingDetector> mResourceFieldDetectors =
            new ArrayList<VisitingDetector>();
    private final List<VisitingDetector> mAllDetectors;
    private final List<VisitingDetector> mFullTreeDetectors;
    private final Map<Class<? extends PsiElement>, List<VisitingDetector>> mNodePsiTypeDetectors =
            new HashMap<Class<? extends PsiElement>, List<VisitingDetector>>(16);
    private final JavaParser mParser;
    private final Map<String, List<VisitingDetector>> mSuperClassDetectors =
            new HashMap<String, List<VisitingDetector>>();

    /**
     * Number of fatal exceptions (internal errors, usually from ECJ) we've
     * encountered; we don't log each and every one to avoid massive log spam
     * in code which triggers this condition
     */
    private static int sExceptionCount;
    /** Max number of logs to include */
    private static final int MAX_REPORTED_CRASHES = 20;

    JavaPsiVisitor(@NonNull JavaParser parser, @NonNull List<Detector> detectors) {
        mParser = parser;
        mAllDetectors = new ArrayList<VisitingDetector>(detectors.size());
        mFullTreeDetectors = new ArrayList<VisitingDetector>(detectors.size());

        for (Detector detector : detectors) {
            JavaPsiScanner javaPsiScanner = (JavaPsiScanner) detector;
            VisitingDetector v = new VisitingDetector(detector, javaPsiScanner);
            mAllDetectors.add(v);

            List<String> applicableSuperClasses = detector.applicableSuperClasses();
            if (applicableSuperClasses != null) {
                for (String fqn : applicableSuperClasses) {
                    List<VisitingDetector> list = mSuperClassDetectors.get(fqn);
                    if (list == null) {
                        list = new ArrayList<VisitingDetector>(SAME_TYPE_COUNT);
                        mSuperClassDetectors.put(fqn, list);
                    }
                    list.add(v);
                }
                continue;
            }

            List<Class<? extends PsiElement>> nodePsiTypes = detector.getApplicablePsiTypes();
            if (nodePsiTypes != null) {
                for (Class<? extends PsiElement> type : nodePsiTypes) {
                    List<VisitingDetector> list = mNodePsiTypeDetectors.get(type);
                    if (list == null) {
                        list = new ArrayList<VisitingDetector>(SAME_TYPE_COUNT);
                        mNodePsiTypeDetectors.put(type, list);
                    }
                    list.add(v);
                }
            }

            List<String> names = detector.getApplicableMethodNames();
            if (names != null) {
                // not supported in Java visitors; adding a method invocation node is trivial
                // for that case.
                assert names != XmlScanner.ALL;

                for (String name : names) {
                    List<VisitingDetector> list = mMethodDetectors.get(name);
                    if (list == null) {
                        list = new ArrayList<VisitingDetector>(SAME_TYPE_COUNT);
                        mMethodDetectors.put(name, list);
                    }
                    list.add(v);
                }
            }

            List<String> types = detector.getApplicableConstructorTypes();
            if (types != null) {
                // not supported in Java visitors; adding a method invocation node is trivial
                // for that case.
                assert types != XmlScanner.ALL;
                if (mConstructorSimpleNames == null) {
                    mConstructorSimpleNames = Sets.newHashSet();
                }
                for (String type : types) {
                    List<VisitingDetector> list = mConstructorDetectors.get(type);
                    if (list == null) {
                        list = new ArrayList<VisitingDetector>(SAME_TYPE_COUNT);
                        mConstructorDetectors.put(type, list);
                        mConstructorSimpleNames.add(type.substring(type.lastIndexOf('.')+1));
                    }
                    list.add(v);
                }
            }

            List<String> referenceNames = detector.getApplicableReferenceNames();
            if (referenceNames != null) {
                // not supported in Java visitors; adding a method invocation node is trivial
                // for that case.
                assert referenceNames != XmlScanner.ALL;

                for (String name : referenceNames) {
                    List<VisitingDetector> list = mReferenceDetectors.get(name);
                    if (list == null) {
                        list = new ArrayList<VisitingDetector>(SAME_TYPE_COUNT);
                        mReferenceDetectors.put(name, list);
                    }
                    list.add(v);
                }
            }

            if (detector.appliesToResourceRefs()) {
                mResourceFieldDetectors.add(v);
            } else if ((referenceNames == null || referenceNames.isEmpty())
                    && (nodePsiTypes == null || nodePsiTypes.isEmpty())
                    && (types == null || types.isEmpty())) {
                mFullTreeDetectors.add(v);
            }
        }
    }

    void visitFile(@NonNull final JavaContext context) {
        try {
            final PsiJavaFile javaFile = mParser.parseJavaToPsi(context);
            if (javaFile == null) {
                // No need to log this; the parser should be reporting
                // a full warning (such as IssueRegistry#PARSER_ERROR)
                // with details, location, etc.
                return;
            }
            try {
                context.setJavaFile(javaFile);

                mParser.runReadAction(new Runnable() {
                    @Override
                    public void run() {
                        for (VisitingDetector v : mAllDetectors) {
                            v.setContext(context);
                            v.getDetector().beforeCheckFile(context);
                        }
                    }
                });

                if (!mSuperClassDetectors.isEmpty()) {
                    mParser.runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            SuperclassPsiVisitor visitor = new SuperclassPsiVisitor(context);
                            javaFile.accept(visitor);
                        }
                    });
                }

                for (final VisitingDetector v : mFullTreeDetectors) {
                    mParser.runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            JavaElementVisitor visitor = v.getVisitor();
                            javaFile.accept(visitor);
                        }
                    });
                }

                if (!mMethodDetectors.isEmpty()
                        || !mResourceFieldDetectors.isEmpty()
                        || !mConstructorDetectors.isEmpty()
                        || !mReferenceDetectors.isEmpty()) {
                    mParser.runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            // TODO: Do we need to break this one up into finer grain
                            // locking units
                            JavaElementVisitor visitor = new DelegatingPsiVisitor(context);
                            javaFile.accept(visitor);
                        }
                    });
                } else {
                    if (!mNodePsiTypeDetectors.isEmpty()) {
                        mParser.runReadAction(new Runnable() {
                            @Override
                            public void run() {
                                // TODO: Do we need to break this one up into finer grain
                                // locking units
                                JavaElementVisitor visitor = new DispatchPsiVisitor();
                                javaFile.accept(visitor);
                            }
                        });
                    }
                }

                mParser.runReadAction(new Runnable() {
                    @Override
                    public void run() {
                        for (VisitingDetector v : mAllDetectors) {
                            v.getDetector().afterCheckFile(context);
                        }
                    }
                });
            } finally {
                mParser.dispose(context, javaFile);
                context.setJavaFile(null);
            }
        } catch (ProcessCanceledException ignore) {
            // Cancelling inspections in the IDE
        } catch (RuntimeException e) {
            if (sExceptionCount++ > MAX_REPORTED_CRASHES) {
                // No need to keep spamming the user that a lot of the files
                // are tripping up ECJ, they get the picture.
                return;
            }

            if (e.getClass().getSimpleName().equals("IndexNotReadyException")) {
                // Attempting to access PSI during startup before indices are ready; ignore these.
                // See http://b.android.com/176644 for an example.
                return;
            }

            // Work around ECJ bugs; see https://code.google.com/p/android/issues/detail?id=172268
            // Don't allow lint bugs to take down the whole build. TRY to log this as a
            // lint error instead!
            StringBuilder sb = new StringBuilder(100);
            sb.append("Unexpected failure during lint analysis of ");
            sb.append(context.file.getName());
            sb.append(" (this is a bug in lint or one of the libraries it depends on)\n");

            sb.append(e.getClass().getSimpleName());
            sb.append(':');
            StackTraceElement[] stackTrace = e.getStackTrace();
            int count = 0;
            for (StackTraceElement frame : stackTrace) {
                if (count > 0) {
                    sb.append("<-");
                }

                String className = frame.getClassName();
                sb.append(className.substring(className.lastIndexOf('.') + 1));
                sb.append('.').append(frame.getMethodName());
                sb.append('(');
                sb.append(frame.getFileName()).append(':').append(frame.getLineNumber());
                sb.append(')');
                count++;
                // Only print the top 3-4 frames such that we can identify the bug
                if (count == 4) {
                    break;
                }
            }
            Throwable throwable = null; // NOT e: this makes for very noisy logs
            //noinspection ConstantConditions
            context.log(throwable, sb.toString());
        }
    }

    /**
     * For testing only: returns the number of exceptions thrown during Java AST analysis
     *
     * @return the number of internal errors found
     */
    @VisibleForTesting
    public static int getCrashCount() {
        return sExceptionCount;
    }

    /**
     * For testing only: clears the crash counter
     */
    @VisibleForTesting
    public static void clearCrashCount() {
        sExceptionCount = 0;
    }

    public void prepare(@NonNull List<JavaContext> contexts) {
        mParser.prepareJavaParse(contexts);
    }

    public void dispose() {
        mParser.dispose();
    }

    @Nullable
    private static Set<String> getInterfaceNames(
            @Nullable Set<String> addTo,
            @NonNull PsiClass cls) {
        for (PsiClass resolvedInterface : cls.getInterfaces()) {
            String name = resolvedInterface.getQualifiedName();
            if (addTo == null) {
                addTo = Sets.newHashSet();
            } else if (addTo.contains(name)) {
                // Superclasses can explicitly implement the same interface,
                // so keep track of visited interfaces as we traverse up the
                // super class chain to avoid checking the same interface
                // more than once.
                continue;
            }
            addTo.add(name);
            getInterfaceNames(addTo, resolvedInterface);
        }

        return addTo;
    }

    private static class VisitingDetector {
        private JavaElementVisitor mVisitor;
        private JavaContext mContext;
        public final Detector mDetector;
        public final JavaPsiScanner mJavaScanner;

        public VisitingDetector(@NonNull Detector detector, @NonNull JavaPsiScanner javaScanner) {
            mDetector = detector;
            mJavaScanner = javaScanner;
        }

        @NonNull
        public Detector getDetector() {
            return mDetector;
        }

        @Nullable
        public JavaPsiScanner getJavaScanner() {
            return mJavaScanner;
        }

        public void setContext(@NonNull JavaContext context) {
            mContext = context;

            // The visitors are one-per-context, so clear them out here and construct
            // lazily only if needed
            mVisitor = null;
        }

        @NonNull
        JavaElementVisitor getVisitor() {
            if (mVisitor == null) {
                mVisitor = mDetector.createPsiVisitor(mContext);
                assert !(mVisitor instanceof JavaRecursiveElementVisitor) :
                        "Your visitor (returned by " + mDetector.getClass().getSimpleName()
                        + "#createPsiVisitor(...) should *not* extend "
                        + " JavaRecursiveElementVisitor; use a plain "
                        + "JavaElementVisitor instead. The lint infrastructure does its own "
                        + "recursion calling *just* your visit methods specified in "
                        + "getApplicablePsiTypes";
                if (mVisitor == null) {
                    mVisitor = new JavaElementVisitor() {
                        @Override
                        public void visitElement(PsiElement element) {
                            // No-op. Workaround for super currently calling
                            //   ProgressIndicatorProvider.checkCanceled();
                        }
                    };
                }
            }
            return mVisitor;
        }
    }

    private class SuperclassPsiVisitor extends JavaRecursiveElementVisitor {
        private JavaContext mContext;

        public SuperclassPsiVisitor(@NonNull JavaContext context) {
            mContext = context;
        }

        @Override
        public void visitClass(@NonNull PsiClass node) {
            super.visitClass(node);
            checkClass(node);
        }

        private void checkClass(@NonNull PsiClass node) {
            if (node instanceof PsiTypeParameter) {
                // Not included: explained in javadoc for JavaPsiScanner#checkClass
                return;
            }

            PsiClass cls = node;
            int depth = 0;
            while (cls != null) {
                List<VisitingDetector> list = mSuperClassDetectors.get(cls.getQualifiedName());
                if (list != null) {
                    for (VisitingDetector v : list) {
                        JavaPsiScanner javaPsiScanner = v.getJavaScanner();
                        if (javaPsiScanner != null) {
                            javaPsiScanner.checkClass(mContext, node);
                        }
                    }
                }

                // Check interfaces too
                Set<String> interfaceNames = getInterfaceNames(null, cls);
                if (interfaceNames != null) {
                    for (String name : interfaceNames) {
                        list = mSuperClassDetectors.get(name);
                        if (list != null) {
                            for (VisitingDetector v : list) {
                                JavaPsiScanner javaPsiScanner = v.getJavaScanner();
                                if (javaPsiScanner != null) {
                                    javaPsiScanner.checkClass(mContext, node);
                                }
                            }
                        }
                    }
                }

                cls = cls.getSuperClass();
                depth++;
                if (depth == 500) {
                    // Shouldn't happen in practice; this prevents the IDE from
                    // hanging if the user has accidentally typed in an incorrect
                    // super class which creates a cycle.
                    break;
                }
            }
        }
    }

    private class DispatchPsiVisitor extends JavaRecursiveElementVisitor {

        @Override
        public void visitAnonymousClass(PsiAnonymousClass node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiAnonymousClass.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAnonymousClass(node);
                }
            }
            super.visitAnonymousClass(node);
        }

        @Override
        public void visitArrayAccessExpression(PsiArrayAccessExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiArrayAccessExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitArrayAccessExpression(node);
                }
            }
            super.visitArrayAccessExpression(node);
        }

        @Override
        public void visitArrayInitializerExpression(PsiArrayInitializerExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors
                    .get(PsiArrayInitializerExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitArrayInitializerExpression(node);
                }
            }
            super.visitArrayInitializerExpression(node);
        }

        @Override
        public void visitAssertStatement(PsiAssertStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiAssertStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAssertStatement(node);
                }
            }
            super.visitAssertStatement(node);
        }

        @Override
        public void visitAssignmentExpression(PsiAssignmentExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiAssignmentExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAssignmentExpression(node);
                }
            }
            super.visitAssignmentExpression(node);
        }

        @Override
        public void visitBinaryExpression(PsiBinaryExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiBinaryExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitBinaryExpression(node);
                }
            }
            super.visitBinaryExpression(node);
        }

        @Override
        public void visitBlockStatement(PsiBlockStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiBlockStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitBlockStatement(node);
                }
            }
            super.visitBlockStatement(node);
        }

        @Override
        public void visitBreakStatement(PsiBreakStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiBreakStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitBreakStatement(node);
                }
            }
            super.visitBreakStatement(node);
        }

        @Override
        public void visitClass(PsiClass node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiClass.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitClass(node);
                }
            }
            super.visitClass(node);
        }

        @Override
        public void visitClassInitializer(PsiClassInitializer node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiClassInitializer.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitClassInitializer(node);
                }
            }
            super.visitClassInitializer(node);
        }

        @Override
        public void visitClassObjectAccessExpression(PsiClassObjectAccessExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors
                    .get(PsiClassObjectAccessExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitClassObjectAccessExpression(node);
                }
            }
            super.visitClassObjectAccessExpression(node);
        }

        @Override
        public void visitCodeBlock(PsiCodeBlock node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiCodeBlock.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitCodeBlock(node);
                }
            }
            super.visitCodeBlock(node);
        }

        @Override
        public void visitConditionalExpression(PsiConditionalExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiConditionalExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitConditionalExpression(node);
                }
            }
            super.visitConditionalExpression(node);
        }

        @Override
        public void visitContinueStatement(PsiContinueStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiContinueStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitContinueStatement(node);
                }
            }
            super.visitContinueStatement(node);
        }

        @Override
        public void visitDeclarationStatement(PsiDeclarationStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiDeclarationStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDeclarationStatement(node);
                }
            }
            super.visitDeclarationStatement(node);
        }

        @Override
        public void visitDocComment(PsiDocComment node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiDocComment.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDocComment(node);
                }
            }
            super.visitDocComment(node);
        }

        @Override
        public void visitDocTag(PsiDocTag node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiDocTag.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDocTag(node);
                }
            }
            super.visitDocTag(node);
        }

        @Override
        public void visitDocTagValue(PsiDocTagValue node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiDocTagValue.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDocTagValue(node);
                }
            }
            super.visitDocTagValue(node);
        }

        @Override
        public void visitDoWhileStatement(PsiDoWhileStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiDoWhileStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDoWhileStatement(node);
                }
            }
            super.visitDoWhileStatement(node);
        }

        @Override
        public void visitEmptyStatement(PsiEmptyStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiEmptyStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitEmptyStatement(node);
                }
            }
            super.visitEmptyStatement(node);
        }

        @Override
        public void visitExpression(PsiExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitExpression(node);
                }
            }
            super.visitExpression(node);
        }

        @Override
        public void visitExpressionList(PsiExpressionList node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiExpressionList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitExpressionList(node);
                }
            }
            super.visitExpressionList(node);
        }

        @Override
        public void visitExpressionListStatement(PsiExpressionListStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors
                    .get(PsiExpressionListStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitExpressionListStatement(node);
                }
            }
            super.visitExpressionListStatement(node);
        }

        @Override
        public void visitExpressionStatement(PsiExpressionStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiExpressionStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitExpressionStatement(node);
                }
            }
            super.visitExpressionStatement(node);
        }

        @Override
        public void visitField(PsiField node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiField.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitField(node);
                }
            }
            super.visitField(node);
        }

        @Override
        public void visitForStatement(PsiForStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiForStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitForStatement(node);
                }
            }
            super.visitForStatement(node);
        }

        @Override
        public void visitForeachStatement(PsiForeachStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiForeachStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitForeachStatement(node);
                }
            }
            super.visitForeachStatement(node);
        }

        @Override
        public void visitIdentifier(PsiIdentifier node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiIdentifier.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitIdentifier(node);
                }
            }
            super.visitIdentifier(node);
        }

        @Override
        public void visitIfStatement(PsiIfStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiIfStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitIfStatement(node);
                }
            }
            super.visitIfStatement(node);
        }

        @Override
        public void visitImportList(PsiImportList node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiImportList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitImportList(node);
                }
            }
            super.visitImportList(node);
        }

        @Override
        public void visitImportStatement(PsiImportStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiImportStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitImportStatement(node);
                }
            }
            super.visitImportStatement(node);
        }

        @Override
        public void visitImportStaticStatement(PsiImportStaticStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiImportStaticStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitImportStaticStatement(node);
                }
            }
            super.visitImportStaticStatement(node);
        }

        @Override
        public void visitInlineDocTag(PsiInlineDocTag node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiInlineDocTag.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitInlineDocTag(node);
                }
            }
            super.visitInlineDocTag(node);
        }

        @Override
        public void visitInstanceOfExpression(PsiInstanceOfExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiInstanceOfExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitInstanceOfExpression(node);
                }
            }
            super.visitInstanceOfExpression(node);
        }

        @Override
        public void visitJavaToken(PsiJavaToken node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiJavaToken.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitJavaToken(node);
                }
            }
            super.visitJavaToken(node);
        }

        @Override
        public void visitKeyword(PsiKeyword node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiKeyword.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitKeyword(node);
                }
            }
            super.visitKeyword(node);
        }

        @Override
        public void visitLabeledStatement(PsiLabeledStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiLabeledStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLabeledStatement(node);
                }
            }
            super.visitLabeledStatement(node);
        }

        @Override
        public void visitLiteralExpression(PsiLiteralExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiLiteralExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLiteralExpression(node);
                }
            }
            super.visitLiteralExpression(node);
        }

        @Override
        public void visitLocalVariable(PsiLocalVariable node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiLocalVariable.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLocalVariable(node);
                }
            }
            super.visitLocalVariable(node);
        }

        @Override
        public void visitMethod(PsiMethod node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiMethod.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitMethod(node);
                }
            }
            super.visitMethod(node);
        }

        @Override
        public void visitMethodCallExpression(PsiMethodCallExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiMethodCallExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitMethodCallExpression(node);
                }
            }
            super.visitMethodCallExpression(node);
        }

        @Override
        public void visitCallExpression(PsiCallExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiCallExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitCallExpression(node);
                }
            }
            super.visitCallExpression(node);
        }

        @Override
        public void visitModifierList(PsiModifierList node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiModifierList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitModifierList(node);
                }
            }
            super.visitModifierList(node);
        }

        @Override
        public void visitNewExpression(PsiNewExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiNewExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitNewExpression(node);
                }
            }
            super.visitNewExpression(node);
        }

        @Override
        public void visitPackage(PsiPackage node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiPackage.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPackage(node);
                }
            }
            super.visitPackage(node);
        }

        @Override
        public void visitPackageStatement(PsiPackageStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiPackageStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPackageStatement(node);
                }
            }
            super.visitPackageStatement(node);
        }

        @Override
        public void visitParameter(PsiParameter node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiParameter.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitParameter(node);
                }
            }
            super.visitParameter(node);
        }

        @Override
        public void visitReceiverParameter(PsiReceiverParameter node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiReceiverParameter.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReceiverParameter(node);
                }
            }
            super.visitReceiverParameter(node);
        }

        @Override
        public void visitParameterList(PsiParameterList node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiParameterList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitParameterList(node);
                }
            }
            super.visitParameterList(node);
        }

        @Override
        public void visitParenthesizedExpression(PsiParenthesizedExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors
                    .get(PsiParenthesizedExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitParenthesizedExpression(node);
                }
            }
            super.visitParenthesizedExpression(node);
        }

        @Override
        public void visitPostfixExpression(PsiPostfixExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiPostfixExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPostfixExpression(node);
                }
            }
            super.visitPostfixExpression(node);
        }

        @Override
        public void visitPrefixExpression(PsiPrefixExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiPrefixExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPrefixExpression(node);
                }
            }
            super.visitPrefixExpression(node);
        }

        @Override
        public void visitReferenceElement(PsiJavaCodeReferenceElement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors
                    .get(PsiJavaCodeReferenceElement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReferenceElement(node);
                }
            }
            super.visitReferenceElement(node);
        }

        @Override
        public void visitImportStaticReferenceElement(PsiImportStaticReferenceElement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors
                    .get(PsiImportStaticReferenceElement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitImportStaticReferenceElement(node);
                }
            }
            super.visitImportStaticReferenceElement(node);
        }

        @Override
        public void visitReferenceExpression(PsiReferenceExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiReferenceExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReferenceExpression(node);
                }
            }
            super.visitReferenceExpression(node);
        }

        @Override
        public void visitMethodReferenceExpression(PsiMethodReferenceExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors
                    .get(PsiMethodReferenceExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitMethodReferenceExpression(node);
                }
            }
            super.visitMethodReferenceExpression(node);
        }

        @Override
        public void visitReferenceList(PsiReferenceList node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiReferenceList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReferenceList(node);
                }
            }
            super.visitReferenceList(node);
        }

        @Override
        public void visitReferenceParameterList(PsiReferenceParameterList node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors
                    .get(PsiReferenceParameterList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReferenceParameterList(node);
                }
            }
            super.visitReferenceParameterList(node);
        }

        @Override
        public void visitTypeParameterList(PsiTypeParameterList node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiTypeParameterList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTypeParameterList(node);
                }
            }
            super.visitTypeParameterList(node);
        }

        @Override
        public void visitReturnStatement(PsiReturnStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiReturnStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReturnStatement(node);
                }
            }
            super.visitReturnStatement(node);
        }

        @Override
        public void visitStatement(PsiStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitStatement(node);
                }
            }
            super.visitStatement(node);
        }

        @Override
        public void visitSuperExpression(PsiSuperExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiSuperExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSuperExpression(node);
                }
            }
            super.visitSuperExpression(node);
        }

        @Override
        public void visitSwitchLabelStatement(PsiSwitchLabelStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiSwitchLabelStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSwitchLabelStatement(node);
                }
            }
            super.visitSwitchLabelStatement(node);
        }

        @Override
        public void visitSwitchStatement(PsiSwitchStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiSwitchStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSwitchStatement(node);
                }
            }
            super.visitSwitchStatement(node);
        }

        @Override
        public void visitSynchronizedStatement(PsiSynchronizedStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiSynchronizedStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSynchronizedStatement(node);
                }
            }
            super.visitSynchronizedStatement(node);
        }

        @Override
        public void visitThisExpression(PsiThisExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiThisExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitThisExpression(node);
                }
            }
            super.visitThisExpression(node);
        }

        @Override
        public void visitThrowStatement(PsiThrowStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiThrowStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitThrowStatement(node);
                }
            }
            super.visitThrowStatement(node);
        }

        @Override
        public void visitTryStatement(PsiTryStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiTryStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTryStatement(node);
                }
            }
            super.visitTryStatement(node);
        }

        @Override
        public void visitCatchSection(PsiCatchSection node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiCatchSection.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitCatchSection(node);
                }
            }
            super.visitCatchSection(node);
        }

        @Override
        public void visitResourceList(PsiResourceList node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiResourceList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitResourceList(node);
                }
            }
            super.visitResourceList(node);
        }

        @Override
        public void visitResourceVariable(PsiResourceVariable node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiResourceVariable.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitResourceVariable(node);
                }
            }
            super.visitResourceVariable(node);
        }

        @Override
        public void visitTypeElement(PsiTypeElement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiTypeElement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTypeElement(node);
                }
            }
            super.visitTypeElement(node);
        }

        @Override
        public void visitTypeCastExpression(PsiTypeCastExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiTypeCastExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTypeCastExpression(node);
                }
            }
            super.visitTypeCastExpression(node);
        }

        @Override
        public void visitVariable(PsiVariable node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiVariable.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitVariable(node);
                }
            }
            super.visitVariable(node);
        }

        @Override
        public void visitWhileStatement(PsiWhileStatement node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiWhileStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitWhileStatement(node);
                }
            }
            super.visitWhileStatement(node);
        }

        @Override
        public void visitJavaFile(PsiJavaFile node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiJavaFile.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitJavaFile(node);
                }
            }
            super.visitJavaFile(node);
        }

        @Override
        public void visitImplicitVariable(ImplicitVariable node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(ImplicitVariable.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitImplicitVariable(node);
                }
            }
            super.visitImplicitVariable(node);
        }

        @Override
        public void visitDocToken(PsiDocToken node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiDocToken.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDocToken(node);
                }
            }
            super.visitDocToken(node);
        }

        @Override
        public void visitTypeParameter(PsiTypeParameter node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiTypeParameter.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTypeParameter(node);
                }
            }
            super.visitTypeParameter(node);
        }

        @Override
        public void visitAnnotation(PsiAnnotation node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiAnnotation.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAnnotation(node);
                }
            }
            super.visitAnnotation(node);
        }

        @Override
        public void visitAnnotationParameterList(PsiAnnotationParameterList node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors
                    .get(PsiAnnotationParameterList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAnnotationParameterList(node);
                }
            }
            super.visitAnnotationParameterList(node);
        }

        @Override
        public void visitAnnotationArrayInitializer(PsiArrayInitializerMemberValue node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors
                    .get(PsiArrayInitializerMemberValue.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAnnotationArrayInitializer(node);
                }
            }
            super.visitAnnotationArrayInitializer(node);
        }

        @Override
        public void visitNameValuePair(PsiNameValuePair node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiNameValuePair.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitNameValuePair(node);
                }
            }
            super.visitNameValuePair(node);
        }

        @Override
        public void visitAnnotationMethod(PsiAnnotationMethod node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiAnnotationMethod.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAnnotationMethod(node);
                }
            }
            super.visitAnnotationMethod(node);
        }

        @Override
        public void visitEnumConstant(PsiEnumConstant node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiEnumConstant.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitEnumConstant(node);
                }
            }
            super.visitEnumConstant(node);
        }

        @Override
        public void visitEnumConstantInitializer(PsiEnumConstantInitializer node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors
                    .get(PsiEnumConstantInitializer.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitEnumConstantInitializer(node);
                }
            }
            super.visitEnumConstantInitializer(node);
        }

        @Override
        public void visitPolyadicExpression(PsiPolyadicExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiPolyadicExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPolyadicExpression(node);
                }
            }
            super.visitPolyadicExpression(node);
        }

        @Override
        public void visitLambdaExpression(PsiLambdaExpression node) {
            List<VisitingDetector> list = mNodePsiTypeDetectors.get(PsiLambdaExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLambdaExpression(node);
                }
            }
            super.visitLambdaExpression(node);
        }
    }

    /** Performs common AST searches for method calls and R-type-field references.
     * Note that this is a specialized form of the {@link DispatchPsiVisitor}. */
    private class DelegatingPsiVisitor extends DispatchPsiVisitor {
        private final JavaContext mContext;
        private final boolean mVisitResources;
        private final boolean mVisitMethods;
        private final boolean mVisitConstructors;
        private final boolean mVisitReferences;

        public DelegatingPsiVisitor(JavaContext context) {
            mContext = context;

            mVisitMethods = !mMethodDetectors.isEmpty();
            mVisitConstructors = !mConstructorDetectors.isEmpty();
            mVisitResources = !mResourceFieldDetectors.isEmpty();
            mVisitReferences = !mReferenceDetectors.isEmpty();
        }

        @Override
        public void visitReferenceElement(PsiJavaCodeReferenceElement element) {
            if (mVisitReferences) {
                String name = element.getReferenceName();
                if (name != null) {
                    List<VisitingDetector> list = mReferenceDetectors.get(name);
                    if (list != null) {
                        PsiElement referenced = element.resolve();
                        if (referenced != null) {
                            for (VisitingDetector v : list) {
                                JavaPsiScanner javaPsiScanner = v.getJavaScanner();
                                if (javaPsiScanner != null) {
                                    javaPsiScanner.visitReference(mContext, v.getVisitor(),
                                            element, referenced);
                                }
                            }
                        }
                    }
                }
            }

            super.visitReferenceElement(element);
        }

        @Override
        public void visitReferenceExpression(PsiReferenceExpression node) {
            if (mVisitResources) {
                // R.type.name
                if (node.getQualifier() instanceof PsiReferenceExpression) {
                    PsiReferenceExpression select = (PsiReferenceExpression) node.getQualifier();
                    if (select.getQualifier() instanceof PsiReferenceExpression) {
                        PsiReferenceExpression reference = (PsiReferenceExpression) select.getQualifier();
                        if (R_CLASS.equals(reference.getReferenceName())) {
                            String typeName = select.getReferenceName();
                            String name = node.getReferenceName();

                            ResourceType type = ResourceType.getEnum(typeName);
                            if (type != null) {
                                boolean isFramework =
                                        reference.getQualifier() instanceof PsiReferenceExpression
                                        && ANDROID_PKG.equals(((PsiReferenceExpression)reference.
                                                getQualifier()).getReferenceName());

                                for (VisitingDetector v : mResourceFieldDetectors) {
                                    JavaPsiScanner detector = v.getJavaScanner();
                                    if (detector != null) {
                                        //noinspection ConstantConditions
                                        detector.visitResourceReference(mContext, v.getVisitor(),
                                                node, type, name, isFramework);
                                    }
                                }
                            }

                            return;
                        }
                    }
                }

                // Arbitrary packages -- android.R.type.name, foo.bar.R.type.name
                if (R_CLASS.equals(node.getReferenceName())) {
                    PsiElement parent = node.getParent();
                    if (parent instanceof PsiReferenceExpression) {
                        PsiElement grandParent = parent.getParent();
                        if (grandParent instanceof PsiReferenceExpression) {
                            PsiReferenceExpression select = (PsiReferenceExpression) grandParent;
                            String name = select.getReferenceName();
                            PsiElement typeOperand = select.getQualifier();
                            if (name != null && typeOperand instanceof PsiReferenceExpression) {
                                PsiReferenceExpression typeSelect =
                                        (PsiReferenceExpression) typeOperand;
                                String typeName = typeSelect.getReferenceName();
                                ResourceType type = typeName != null
                                        ? ResourceType.getEnum(typeName)
                                        : null;
                                if (type != null) {
                                    boolean isFramework = node.getQualifier().getText().equals(
                                            ANDROID_PKG);
                                    for (VisitingDetector v : mResourceFieldDetectors) {
                                        JavaPsiScanner detector = v.getJavaScanner();
                                        if (detector != null) {
                                            detector.visitResourceReference(mContext,
                                                    v.getVisitor(),
                                                    node, type, name, isFramework);
                                        }
                                    }
                                }

                                return;
                            }
                        }
                    }
                }
            }

            super.visitReferenceExpression(node);
        }

        @Override
        public void visitMethodCallExpression(PsiMethodCallExpression node) {
            super.visitMethodCallExpression(node);

            if (mVisitMethods) {
                String methodName = node.getMethodExpression().getReferenceName();
                if (methodName != null) {
                    List<VisitingDetector> list = mMethodDetectors.get(methodName);
                    if (list != null) {
                        PsiMethod method = node.resolveMethod();
                        if (method != null) {
                            for (VisitingDetector v : list) {
                                JavaPsiScanner javaPsiScanner = v.getJavaScanner();
                                if (javaPsiScanner != null) {
                                    javaPsiScanner.visitMethod(mContext, v.getVisitor(), node,
                                            method);
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void visitNewExpression(PsiNewExpression node) {
            super.visitNewExpression(node);

            if (mVisitConstructors) {
                PsiJavaCodeReferenceElement typeReference = node.getClassReference();
                if (typeReference != null) {
                    String type = typeReference.getQualifiedName();
                    if (type != null) {
                        List<VisitingDetector> list = mConstructorDetectors.get(type);
                        if (list != null) {
                            PsiMethod method = node.resolveMethod();
                            if (method != null) {
                                for (VisitingDetector v : list) {
                                    JavaPsiScanner javaPsiScanner = v.getJavaScanner();
                                    if (javaPsiScanner != null) {
                                        javaPsiScanner.visitConstructor(mContext,
                                                v.getVisitor(), node, method);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
