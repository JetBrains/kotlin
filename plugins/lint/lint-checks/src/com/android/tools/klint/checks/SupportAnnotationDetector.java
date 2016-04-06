/*
 * Copyright (C) 2015 The Android Open Source Project
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
import static com.android.SdkConstants.ATTR_VALUE;
import static com.android.SdkConstants.INT_DEF_ANNOTATION;
import static com.android.SdkConstants.R_CLASS;
import static com.android.SdkConstants.STRING_DEF_ANNOTATION;
import static com.android.SdkConstants.SUPPORT_ANNOTATIONS_PREFIX;
import static com.android.SdkConstants.TAG_PERMISSION;
import static com.android.SdkConstants.TAG_USES_PERMISSION;
import static com.android.SdkConstants.TYPE_DEF_FLAG_ATTRIBUTE;
import static com.android.resources.ResourceType.COLOR;
import static com.android.resources.ResourceType.DRAWABLE;
import static com.android.resources.ResourceType.MIPMAP;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import com.android.tools.klint.client.api.LintClient;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Project;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.utils.XmlUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.java.JavaUFunction;
import org.jetbrains.uast.java.JavaUastCallKinds;
import org.jetbrains.uast.kinds.UastOperator;
import org.jetbrains.uast.visitor.UastVisitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Looks up annotations on method calls and enforces the various things they
 * express, e.g. for {@code @CheckReturn} it makes sure the return value is used,
 * for {@code ColorInt} it ensures that a proper color integer is passed in, etc.
 *
 * TODO: Throw in some annotation usage checks here too; e.g. specifying @Size without parameters,
 * specifying toInclusive without setting to, combining @ColorInt with any @ResourceTypeRes,
 * using @CheckResult on a void method, etc.
 */
public class SupportAnnotationDetector extends Detector implements UastScanner {

    public static final Implementation IMPLEMENTATION
            = new Implementation(SupportAnnotationDetector.class, Scope.JAVA_FILE_SCOPE);

    /** Method result should be used */
    public static final Issue RANGE = Issue.create(
        "Range", //$NON-NLS-1$
        "Outside Range",

        "Some parameters are required to in a particular numerical range; this check " +
        "makes sure that arguments passed fall within the range. For arrays, Strings " +
        "and collections this refers to the size or length.",

        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        IMPLEMENTATION);

    /**
     * Attempting to set a resource id as a color
     */
    public static final Issue RESOURCE_TYPE = Issue.create(
        "ResourceType", //$NON-NLS-1$
        "Wrong Resource Type",

        "Ensures that resource id's passed to APIs are of the right type; for example, " +
        "calling `Resources.getColor(R.string.name)` is wrong.",

        Category.CORRECTNESS,
        7,
        Severity.FATAL,
        IMPLEMENTATION);

    /** Attempting to set a resource id as a color */
    public static final Issue COLOR_USAGE = Issue.create(
        "ResourceAsColor", //$NON-NLS-1$
        "Should pass resolved color instead of resource id",

        "Methods that take a color in the form of an integer should be passed " +
        "an RGB triple, not the actual color resource id. You must call " +
        "`getResources().getColor(resource)` to resolve the actual color value first.",

        Category.CORRECTNESS,
        7,
        Severity.ERROR,
        IMPLEMENTATION);

    /** Passing the wrong constant to an int or String method */
    public static final Issue TYPE_DEF = Issue.create(
        "WrongConstant", //$NON-NLS-1$
        "Incorrect constant",

        "Ensures that when parameter in a method only allows a specific set " +
        "of constants, calls obey those rules.",

        Category.SECURITY,
        6,
        Severity.ERROR,
        IMPLEMENTATION);

    /** Method result should be used */
    public static final Issue CHECK_RESULT = Issue.create(
        "CheckResult", //$NON-NLS-1$
        "Ignoring results",

        "Some methods have no side effects, an calling them without doing something " +
        "without the result is suspicious. ",

        Category.CORRECTNESS,
        6,
        Severity.WARNING,
            IMPLEMENTATION);

    /** Failing to enforce security by just calling check permission */
    public static final Issue CHECK_PERMISSION = Issue.create(
        "UseCheckPermission", //$NON-NLS-1$
        "Using the result of check permission calls",

        "You normally want to use the result of checking a permission; these methods " +
        "return whether the permission is held; they do not throw an error if the permission " +
        "is not granted. Code which does not do anything with the return value probably " +
        "meant to be calling the enforce methods instead, e.g. rather than " +
        "`Context#checkCallingPermission` it should call `Context#enforceCallingPermission`.",

        Category.SECURITY,
        6,
        Severity.WARNING,
        IMPLEMENTATION);

    /** Method result should be used */
    public static final Issue MISSING_PERMISSION = Issue.create(
            "MissingPermission", //$NON-NLS-1$
            "Missing Permissions",

            "This check scans through your code and libraries and looks at the APIs being used, " +
            "and checks this against the set of permissions required to access those APIs. If " +
            "the code using those APIs is called at runtime, then the program will crash.\n" +
            "\n" +
            "Furthermore, for permissions that are revocable (with targetSdkVersion 23), client " +
            "code must also be prepared to handle the calls throwing an exception if the user " +
            "rejects the request for permission at runtime.",

            Category.CORRECTNESS,
            9,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Passing the wrong constant to an int or String method */
    public static final Issue THREAD = Issue.create(
            "WrongThread", //$NON-NLS-1$
            "Wrong Thread",

            "Ensures that a method which expects to be called on a specific thread, is actually " +
            "called from that thread. For example, calls on methods in widgets should always " +
            "be made on the UI thread.",

            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPLEMENTATION)
            .addMoreInfo(
                    "http://developer.android.com/guide/components/processes-and-threads.html#Threads");

    public static final String CHECK_RESULT_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "CheckResult"; //$NON-NLS-1$
    public static final String COLOR_INT_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "ColorInt"; //$NON-NLS-1$
    public static final String INT_RANGE_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "IntRange"; //$NON-NLS-1$
    public static final String FLOAT_RANGE_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "FloatRange"; //$NON-NLS-1$
    public static final String SIZE_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "Size"; //$NON-NLS-1$
    public static final String PERMISSION_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "RequiresPermission"; //$NON-NLS-1$
    public static final String UI_THREAD_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "UiThread"; //$NON-NLS-1$
    public static final String MAIN_THREAD_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "MainThread"; //$NON-NLS-1$
    public static final String WORKER_THREAD_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "WorkerThread"; //$NON-NLS-1$
    public static final String BINDER_THREAD_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "BinderThread"; //$NON-NLS-1$

    public static final String RES_SUFFIX = "Res";
    public static final String THREAD_SUFFIX = "Thread";
    public static final String ATTR_SUGGEST = "suggest";
    public static final String ATTR_TO = "to";
    public static final String ATTR_FROM = "from";
    public static final String ATTR_FROM_INCLUSIVE = "fromInclusive";
    public static final String ATTR_TO_INCLUSIVE = "toInclusive";
    public static final String ATTR_MULTIPLE = "multiple";
    public static final String ATTR_MIN = "min";
    public static final String ATTR_MAX = "max";
    public static final String ATTR_ALL_OF = "allOf";
    public static final String ATTR_ANY_OF = "anyOf";
    public static final String ATTR_CONDITIONAL = "conditional";

    /**
     * Marker ResourceType used to signify that an expression is of type {@code @ColorInt},
     * which isn't actually a ResourceType but one we want to specifically compare with.
     * We're using {@link ResourceType#PUBLIC} because that one won't appear in the R
     * class (and ResourceType is an enum we can't just create new constants for.)
     */
    public static final ResourceType COLOR_INT_MARKER_TYPE = ResourceType.PUBLIC;

    /**
     * Constructs a new {@link SupportAnnotationDetector} check
     */
    public SupportAnnotationDetector() {
    }

    private void checkMethodAnnotation(
            @NonNull UastAndroidContext context,
            @NonNull UFunction method,
            @NonNull UCallExpression node,
            @NonNull UAnnotation annotation) {
        String signature = annotation.getFqName();
        if (signature == null) {
            return;
        }

        if (CHECK_RESULT_ANNOTATION.equals(signature)
                || signature.endsWith(".CheckReturnValue")) { // support findbugs annotation too
            checkResult(context, node, annotation);
        } else if (signature.equals(PERMISSION_ANNOTATION)) {
            checkPermission(context, node, method, annotation);
        } else if (signature.endsWith(THREAD_SUFFIX)
                && signature.startsWith(SUPPORT_ANNOTATIONS_PREFIX)) {
            checkThreading(context, node, method, signature);
        }
    }

    private static void checkParameterAnnotation(
            @NonNull UastAndroidContext context,
            @NonNull UElement argument,
            @NonNull UAnnotation annotation) {
        String signature = annotation.getFqName();
        if (signature == null) {
            return;
        }

        if (COLOR_INT_ANNOTATION.equals(signature)) {
            checkColor(context, argument);
        } else if (signature.equals(INT_RANGE_ANNOTATION)) {
            checkIntRange(context, annotation, argument);
        } else if (signature.equals(FLOAT_RANGE_ANNOTATION)) {
            checkFloatRange(context, annotation, argument);
        } else if (signature.equals(SIZE_ANNOTATION)) {
            checkSize(context, annotation, argument);
        } else {
            // We only run @IntDef, @StringDef and @<Type>Res checks if we're not
            // running inside Android Studio / IntelliJ where there are already inspections
            // covering the same warnings (using IntelliJ's own data flow analysis); we
            // don't want to (a) create redundant warnings or (b) work harder than we
            // have to
            if (signature.equals(INT_DEF_ANNOTATION)) {
                boolean flag = annotation.getValue(TYPE_DEF_FLAG_ATTRIBUTE) == Boolean.TRUE;
                checkTypeDefConstant(context, annotation, argument, null, flag);
            } else if (signature.equals(STRING_DEF_ANNOTATION)) {
                checkTypeDefConstant(context, annotation, argument, null, false);
            } else if (signature.endsWith(RES_SUFFIX)) {
                String typeString = signature.substring(SUPPORT_ANNOTATIONS_PREFIX.length(),
                        signature.length() - RES_SUFFIX.length()).toLowerCase(Locale.US);
                ResourceType type = ResourceType.getEnum(typeString);
                if (type != null) {
                    checkResourceType(context, argument, type);
                } else if (typeString.equals("any")) { // @AnyRes
                    checkResourceType(context, argument, null);
                }
            }
        }
    }

    private static void checkColor(@NonNull UastAndroidContext context, @NonNull UElement argument) {
        if (argument instanceof UIfExpression) {
            UIfExpression expression = (UIfExpression) argument;
            if (expression.isTernary()) {
                checkColor(context, expression.getThenBranch());
                checkColor(context, expression.getElseBranch());
            }
            return;
        }

        List<ResourceType> types = getResourceTypes(context, argument);

        if (types != null && types.contains(ResourceType.COLOR)) {
            String message = String.format(
                    "Should pass resolved color instead of resource id here: " +
                            "`getResources().getColor(%1$s)`", argument.toString());
            context.report(COLOR_USAGE, argument, context.getLocation(argument), message);
        }
    }

    private void checkPermission(
            @NonNull UastAndroidContext context,
            @NonNull UCallExpression node,
            @NonNull UFunction method,
            @NonNull UAnnotation annotation) {
        PermissionRequirement requirement = PermissionRequirement.create(context, annotation);
        if (requirement.isConditional()) {
            return;
        }
        PermissionHolder permissions = getPermissions(context.getLintContext());
        if (!requirement.isSatisfied(permissions)) {
            // See if it looks like we're holding the permission implicitly by @RequirePermission
            // annotations in the surrounding context
            permissions  = addLocalPermissions(context, permissions, node);
            if (!requirement.isSatisfied(permissions)) {
                UClass containingClass = UastUtils.getContainingClass(method);
                if (containingClass != null) {
                    String name = containingClass.getName() + "." + method.getName();
                    String message = getMissingPermissionMessage(requirement, name, permissions);
                    context.report(MISSING_PERMISSION, node, context.getLocation(node), message);
                }
            }
        } else if (requirement.isRevocable(permissions) &&
                context.getLintContext().getMainProject().getTargetSdkVersion().getFeatureLevel() >= 23) {
            // Ensure that the caller is handling a security exception
            // First check to see if we're inside a try/catch which catches a SecurityException
            // (or some wider exception than that). Check for nested try/catches too.
            boolean handlesMissingPermission = false;
            UElement parent = node;
            while (true) {
                UTryExpression tryCatch = UastUtils.getParentOfType(parent, UTryExpression.class);
                if (tryCatch == null) {
                    break;
                } else {
                    for (UCatchClause aCatch : tryCatch.getCatchClauses()) {
                        for (UType catchType : aCatch.getTypes()) {
                            if (isSecurityException(catchType)) {
                                handlesMissingPermission = true;
                                break;
                            }
                        }
                    }
                    parent = tryCatch;
                }
            }

            // If not, check to see if the method itself declares that it throws a
            // SecurityException or something wider.
            if (!handlesMissingPermission) {
                UFunction declaration = UastUtils.getParentOfType(parent, UFunction.class);
                if (declaration instanceof JavaUFunction) {
                    List<UType> thrownExceptions = ((JavaUFunction)declaration).getThrownExceptions();
                    for (UType typeReference : thrownExceptions) {
                        if (isSecurityException(typeReference)) {
                            handlesMissingPermission = true;
                            break;
                        }
                    }
                }
            }

            // If not, check to see if the code is deliberately checking to see if the
            // given permission is available.
            if (!handlesMissingPermission) {
                UFunction methodNode = UastUtils.getContainingFunction(node);
                if (methodNode != null) {
                    CheckPermissionVisitor visitor = new CheckPermissionVisitor(node);
                    visitor.process(methodNode);
                    handlesMissingPermission = visitor.checksPermission();
                }
            }

            if (!handlesMissingPermission) {
                String message = getUnhandledPermissionMessage();
                context.report(MISSING_PERMISSION, node, context.getLocation(node), message);
            }
        }
    }

    @NonNull
    private static PermissionHolder addLocalPermissions(
            @NonNull UastAndroidContext context,
            @NonNull PermissionHolder permissions,
            @NonNull UElement node) {
        // Accumulate @RequirePermissions available in the local context
        UFunction method = UastUtils.getContainingFunction(node);
        if (method == null) {
            return permissions;
        }
        UAnnotation annotation = UastUtils.findAnnotation(method, PERMISSION_ANNOTATION);
        permissions = mergeAnnotationPermissions(context, permissions, annotation);
        annotation = UastUtils.findAnnotation(UastUtils.getContainingClassOrEmpty(method), PERMISSION_ANNOTATION);
        permissions = mergeAnnotationPermissions(context, permissions, annotation);
        return permissions;
    }

    @NonNull
    private static PermissionHolder mergeAnnotationPermissions(
            @NonNull UastAndroidContext context,
            @NonNull PermissionHolder permissions,
            @Nullable UAnnotation annotation) {
        if (annotation != null) {
            PermissionRequirement requirement = PermissionRequirement.create(context, annotation);
            permissions = PermissionHolder.SetPermissionLookup.join(permissions, requirement);
        }

        return permissions;
    }

    /** Returns the error message shown when a given call is missing one or more permissions */
    public static String getMissingPermissionMessage(@NonNull PermissionRequirement requirement,
            @NonNull String callName, @NonNull PermissionHolder permissions) {
        return String.format("Missing permissions required by %1$s: %2$s", callName,
                requirement.describeMissingPermissions(permissions));
    }

    /** Returns the error message shown when a revocable permission call is not properly handled */
    public static String getUnhandledPermissionMessage() {
        return "Call requires permission which may be rejected by user: code should explicitly "
                + "check to see if permission is available (with `checkPermission`) or handle "
                + "a potential `SecurityException`";
    }

    /**
     * Visitor which looks through a method, up to a given call (the one requiring a
     * permission) and checks whether it's preceeded by a call to checkPermission or
     * checkCallingPermission or enforcePermission etc.
     * <p>
     * Currently it only looks for the presence of this check; it does not perform
     * flow analysis to determine whether the check actually affects program flow
     * up to the permission call, or whether the check permission is checking for
     * permissions sufficient to satisfy the permission requirement of the target call,
     * or whether the check return value (== PERMISSION_GRANTED vs != PERMISSION_GRANTED)
     * is handled correctly, etc.
     */
    private static class CheckPermissionVisitor extends UastVisitor {
        private boolean mChecksPermission;
        private boolean mDone;
        private final UElement mTarget;

        public CheckPermissionVisitor(@NonNull UElement target) {
            mTarget = target;
        }

        @Override
        public void process(@NotNull UElement element) {
            if (!mDone) {
                super.process(element);
            }
        }

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression node) {
            if (node.getKind() == UastCallKind.FUNCTION_CALL) {
                if (node == mTarget) {
                    mDone = true;
                }

                String name = node.getFunctionName();
                if (name != null &&
                    (name.startsWith("check") || name.startsWith("enforce"))
                    && name.endsWith("Permission")) {
                    mChecksPermission = true;
                    mDone = true;
                }
            }

            return super.visitCallExpression(node);
        }

        public boolean checksPermission() {
            return mChecksPermission;
        }
    }

    private static boolean isSecurityException(@NonNull UType type) {
        return type != null && (type.matchesFqName("java.lang.SecurityException") ||
                type.matchesFqName("java.lang.RuntimeException") ||
                type.matchesFqName("java.lang.Exception") ||
                type.matchesFqName("java.lang.Throwable"));
    }

    private PermissionHolder mPermissions;

    private PermissionHolder getPermissions(
            @NonNull UastAndroidContext context) {
        if (mPermissions == null) {
            Set<String> permissions = Sets.newHashSetWithExpectedSize(30);
            Set<String> revocable = Sets.newHashSetWithExpectedSize(4);
            JavaContext lintContext = context.getLintContext();
            LintClient client = lintContext.getClient();
            // Gather permissions from all projects that contribute to the
            // main project.
            Project mainProject = lintContext.getMainProject();
            for (File manifest : mainProject.getManifestFiles()) {
                addPermissions(client, permissions, revocable, manifest);
            }
            for (Project library : mainProject.getAllLibraries()) {
                for (File manifest : library.getManifestFiles()) {
                    addPermissions(client, permissions, revocable, manifest);
                }
            }

            mPermissions = new PermissionHolder.SetPermissionLookup(permissions, revocable);
        }

        return mPermissions;
    }

    private static void addPermissions(@NonNull LintClient client,
            @NonNull Set<String> permissions,
            @NonNull Set<String> revocable,
            @NonNull File manifest) {
        Document document = XmlUtils.parseDocumentSilently(client.readFile(manifest), true);
        if (document == null) {
            return;
        }
        Element root = document.getDocumentElement();
        if (root == null) {
            return;
        }
        NodeList children = root.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node item = children.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String nodeName = item.getNodeName();
            if (nodeName.equals(TAG_USES_PERMISSION)) {
                Element element = (Element)item;
                String name = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
                if (!name.isEmpty()) {
                    permissions.add(name);
                }
            } else if (nodeName.equals(TAG_PERMISSION)) {
                Element element = (Element)item;
                String protectionLevel = element.getAttributeNS(ANDROID_URI,
                                                                PermissionRequirement.ATTR_PROTECTION_LEVEL);
                if (PermissionRequirement.VALUE_DANGEROUS.equals(protectionLevel)) {
                    String name = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
                    if (!name.isEmpty()) {
                        revocable.add(name);
                    }
                }
            }
        }
    }

    private static void checkResult(
            @NonNull UastAndroidContext context,
            @NonNull UCallExpression node,
            @NonNull UAnnotation annotation) {
        if (node.getParent() instanceof UExpression) {
            String methodName = node.getFunctionName();
            assert methodName != null;
            Object suggested = annotation.getValue(ATTR_SUGGEST);

            // Failing to check permissions is a potential security issue (and had an existing
            // dedicated issue id before which people may already have configured with a
            // custom severity in their LintOptions etc) so continue to use that issue
            // (which also has category Security rather than Correctness) for these:
            Issue issue = CHECK_RESULT;
            if (methodName.startsWith("check") && methodName.contains("Permission")) {
                issue = CHECK_PERMISSION;
            }

            String message = String.format("The result of `%1$s` is not used",
                    methodName);
            if (suggested != null) {
                // TODO: Resolve suggest attribute (e.g. prefix annotation class if it starts
                // with "#" etc?
                message = String.format(
                        "The result of `%1$s` is not used; did you mean to call `%2$s`?",
                        methodName, suggested.toString());
            }
            context.report(issue, node, context.getLocation(node), message);
        }
    }

    private static void checkThreading(
            @NonNull UastAndroidContext context,
            @NonNull UCallExpression node,
            @NonNull UFunction method,
            @NonNull String annotation) {
        String threadContext = getThreadContext(context, node);
        if (threadContext != null && !isCompatibleThread(threadContext, annotation)) {
            String message = String.format("Method %1$s must be called from the `%2$s` thread, currently inferred thread is `%3$s` thread",
                    method.getName(), describeThread(annotation), describeThread(threadContext));
            context.report(THREAD, node, context.getLocation(node), message);
        }
    }

    @NonNull
    public static String describeThread(@NonNull String annotation) {
        if (UI_THREAD_ANNOTATION.equals(annotation)) {
            return "UI";
        }
        else if (MAIN_THREAD_ANNOTATION.equals(annotation)) {
            return "main";
        }
        else if (BINDER_THREAD_ANNOTATION.equals(annotation)) {
            return "binder";
        }
        else if (WORKER_THREAD_ANNOTATION.equals(annotation)) {
            return "worker";
        } else {
            return "other";
        }
    }

    /** returns true if the two threads are compatible */
    public static boolean isCompatibleThread(@NonNull String thread1, @NonNull String thread2) {
        if (thread1.equals(thread2)) {
            return true;
        }

        // Allow @UiThread and @MainThread to be combined
        if (thread1.equals(UI_THREAD_ANNOTATION)) {
            if (thread2.equals(MAIN_THREAD_ANNOTATION)) {
                return true;
            }
        } else if (thread1.equals(MAIN_THREAD_ANNOTATION)) {
            if (thread2.equals(UI_THREAD_ANNOTATION)) {
                return true;
            }
        }

        return false;
    }

    /** Attempts to infer the current thread context at the site of the given method call */
    @Nullable
    private static String getThreadContext(@NonNull UastAndroidContext context,
            @NonNull UCallExpression methodCall) {
        UFunction method = UastUtils.getContainingFunction(methodCall);
        if (method != null) {
            UClass cls = UastUtils.getContainingClass(method);

            while (method != null) {
                for (UAnnotation annotation : method.getAnnotations()) {
                    String name = annotation.getFqName();
                    if (name != null
                        && name.startsWith(SUPPORT_ANNOTATIONS_PREFIX)
                        && name.endsWith(THREAD_SUFFIX)) {
                        return name;
                    }
                }
                List<UFunction> superFunctions = method.getSuperFunctions(context);
                if (superFunctions.isEmpty()) {
                    method = null;
                } else {
                    method = superFunctions.get(0);
                }
            }

            // See if we're extending a class with a known threading context
            while (cls != null) {
                for (UAnnotation annotation : cls.getAnnotations()) {
                    String name = annotation.getFqName();
                    if (name != null
                        && name.startsWith(SUPPORT_ANNOTATIONS_PREFIX)
                        && name.endsWith(THREAD_SUFFIX)) {
                        return name;
                    }
                }
                cls = cls.getSuperClass(context);
            }
        }

        // In the future, we could also try to infer the threading context using
        // other heuristics. For example, if we're in a method with unknown threading
        // context, but we see that the method is called by another method with a known
        // threading context, we can infer that that threading context is the context for
        // this thread too (assuming the call is direct).

        return null;
    }

    private static boolean isNumber(@NonNull UElement argument) {
        return UastLiteralUtils.isIntegralLiteral(argument) || argument instanceof UPrefixExpression
                && ((UPrefixExpression) argument).getOperator() == UastPrefixOperator.UNARY_MINUS
                && UastLiteralUtils.isIntegralLiteral(((UPrefixExpression) argument).getOperand());
    }

    private static boolean isZero(@NonNull UElement argument) {
        return UastLiteralUtils.isIntegralLiteral(argument)
                && UastLiteralUtils.getLongValue((ULiteralExpression) argument) == 0;
    }

    private static boolean isMinusOne(@NonNull UElement argument) {
        return argument instanceof UUnaryExpression
                && ((UUnaryExpression) argument).getOperator() == UastPrefixOperator.UNARY_MINUS
                && UastLiteralUtils.isIntegralLiteral(((UUnaryExpression) argument).getOperand())
                && UastLiteralUtils.getLongValue((ULiteralExpression) ((UUnaryExpression) argument).getOperand())
                   == 1;
    }

    private static void checkResourceType(
            @NonNull UastAndroidContext context,
            @NonNull UElement argument,
            @Nullable ResourceType expectedType) {
        List<ResourceType> actual = getResourceTypes(context, argument);
        if (actual == null && (!isNumber(argument) || isZero(argument) || isMinusOne(argument)) ) {
            return;
        } else if (actual != null && (expectedType == null
                || actual.contains(expectedType)
                || expectedType == DRAWABLE && (actual.contains(COLOR) || actual.contains(MIPMAP)))) {
            return;
        }

        String message;
        if (actual != null && actual.size() == 1 && actual.get(0) == COLOR_INT_MARKER_TYPE) {
            message = "Expected a color resource id (`R.color.`) but received an RGB integer";
        } else if (expectedType == COLOR_INT_MARKER_TYPE) {
            message = String.format("Should pass resolved color instead of resource id here: " +
                    "`getResources().getColor(%1$s)`", argument.toString());
        } else if (expectedType != null) {
            message = String.format(
                    "Expected resource of type %1$s", expectedType.getName());
        } else {
            message = "Expected resource identifier (`R`.type.`name`)";
        }
        context.report(RESOURCE_TYPE, argument, context.getLocation(argument), message);
    }

    @Nullable
    private static List<ResourceType> getResourceTypes(@NonNull UastAndroidContext context,
            @NonNull UElement argument) {
        if (argument instanceof UQualifiedExpression) {
            UQualifiedExpression node = (UQualifiedExpression) argument;
            if (node.getReceiver() instanceof UQualifiedExpression) {
                UQualifiedExpression select = (UQualifiedExpression) node.getReceiver();
                if (select.getReceiver() instanceof UQualifiedExpression) { // android.R....
                    UQualifiedExpression innerSelect = (UQualifiedExpression) select.getReceiver();
                    if (innerSelect.getSelector().renderString().equals(R_CLASS)) {
                        String typeName = select.getSelector().renderString();
                        ResourceType type = ResourceType.getEnum(typeName);
                        return type != null ? Collections.singletonList(type) : null;
                    }
                }
                if (select.getReceiver() instanceof USimpleReferenceExpression) {
                    USimpleReferenceExpression reference = (USimpleReferenceExpression) select.getReceiver();
                    if (reference.getIdentifier().equals(R_CLASS)) {
                        String typeName = select.getSelector().renderString();
                        ResourceType type = ResourceType.getEnum(typeName);
                        return type != null ? Collections.singletonList(type) : null;
                    }
                }
            }

            // Arbitrary packages -- android.R.type.name, foo.bar.R.type.name
            if (node.getSelector().renderString().equals(R_CLASS)) {
                UElement parent = node.getParent();
                if (parent instanceof UQualifiedExpression) {
                    UElement grandParent = parent.getParent();
                    if (grandParent instanceof UQualifiedExpression) {
                        UQualifiedExpression select = (UQualifiedExpression) grandParent;
                        UExpression typeOperand = select.getReceiver();
                        if (typeOperand instanceof UQualifiedExpression) {
                            UQualifiedExpression typeSelect = (UQualifiedExpression) typeOperand;
                            String typeName = typeSelect.getSelector().renderString();
                            ResourceType type = ResourceType.getEnum(typeName);
                            return type != null ? Collections.singletonList(type) : null;
                        }
                    }
                }
            }
        } else if (argument instanceof UCallExpression) {
            UDeclaration resolved = ((UCallExpression)argument).resolve(context);
            //noinspection ConstantConditions
            if (resolved instanceof UAnnotated) {
                List<UAnnotation> annotations = ((UAnnotated) resolved).getAnnotations();
                for (UAnnotation annotation : annotations) {
                    String signature = annotation.getFqName();
                    if (COLOR_INT_ANNOTATION.equals(signature)) {
                        return Collections.singletonList(COLOR_INT_MARKER_TYPE);
                    }
                    if (signature != null
                            && signature.endsWith(RES_SUFFIX)
                            && signature.startsWith(SUPPORT_ANNOTATIONS_PREFIX)) {
                        String typeString = signature.substring(SUPPORT_ANNOTATIONS_PREFIX.length(),
                                signature.length() - RES_SUFFIX.length()).toLowerCase(Locale.US);
                        ResourceType type = ResourceType.getEnum(typeString);
                        if (type != null) {
                            return Collections.singletonList(type);
                        } else if (typeString.equals("any")) { // @AnyRes
                            ResourceType[] types = ResourceType.values();
                            List<ResourceType> result = Lists.newArrayListWithExpectedSize(
                                    types.length);
                            for (ResourceType t : types) {
                                if (t != COLOR_INT_MARKER_TYPE) {
                                    result.add(t);
                                }
                            }

                            return result;
                        }
                    }
                }
            }
        }

        return null;
    }

    private static void checkIntRange(
            @NonNull UastAndroidContext context,
            @NonNull UAnnotation annotation,
            @NonNull UElement argument) {
        Object object = null;
        if (argument instanceof UExpression) {
            object = ((UExpression) argument).evaluate();
        }
        if (!(object instanceof Number)) {
            return;
        }
        long value = ((Number)object).longValue();
        long from = getLongAttribute(annotation, ATTR_FROM, Long.MIN_VALUE);
        long to = getLongAttribute(annotation, ATTR_TO, Long.MAX_VALUE);

        String message = getIntRangeError(value, from, to);
        if (message != null) {
            context.report(RANGE, argument, context.getLocation(argument), message);
        }
    }

    /**
     * Checks whether a given integer value is in the allowed range, and if so returns
     * null; otherwise returns a suitable error message.
     */
    private static String getIntRangeError(long value, long from, long to) {
        String message = null;
        if (value < from || value > to) {
            StringBuilder sb = new StringBuilder(20);
            if (value < from) {
                sb.append("Value must be \u2265 ");
                sb.append(Long.toString(from));
            } else {
                assert value > to;
                sb.append("Value must be \u2264 ");
                sb.append(Long.toString(to));
            }
            sb.append(" (was ").append(value).append(')');
            message = sb.toString();
        }
        return message;
    }

    private static void checkFloatRange(
            @NonNull UastAndroidContext context,
            @NonNull UAnnotation annotation,
            @NonNull UElement argument) {
        Object object = null;
        if (argument instanceof UExpression) {
            object = ((UExpression) argument).evaluate();
        }
        if (!(object instanceof Number)) {
            return;
        }
        double value = ((Number)object).doubleValue();
        double from = getDoubleAttribute(annotation, ATTR_FROM, Double.NEGATIVE_INFINITY);
        double to = getDoubleAttribute(annotation, ATTR_TO, Double.POSITIVE_INFINITY);
        boolean fromInclusive = getBoolean(annotation, ATTR_FROM_INCLUSIVE, true);
        boolean toInclusive = getBoolean(annotation, ATTR_TO_INCLUSIVE, true);

        String message = getFloatRangeError(value, from, to, fromInclusive, toInclusive, argument);
        if (message != null) {
            context.report(RANGE, argument, context.getLocation(argument), message);
        }
    }

    /**
     * Checks whether a given floating point value is in the allowed range, and if so returns
     * null; otherwise returns a suitable error message.
     */
    @Nullable
    private static String getFloatRangeError(double value, double from, double to,
            boolean fromInclusive, boolean toInclusive, @NonNull UElement node) {
        if (!((fromInclusive && value >= from || !fromInclusive && value > from) &&
                (toInclusive && value <= to || !toInclusive && value < to))) {
            StringBuilder sb = new StringBuilder(20);
            if (from != Double.NEGATIVE_INFINITY) {
                if (to != Double.POSITIVE_INFINITY) {
                    if (fromInclusive && value < from || !fromInclusive && value <= from) {
                        sb.append("Value must be ");
                        if (fromInclusive) {
                            sb.append('\u2265'); // >= sign
                        } else {
                            sb.append('>');
                        }
                        sb.append(' ');
                        sb.append(Double.toString(from));
                    } else {
                        assert toInclusive && value > to || !toInclusive && value >= to;
                        sb.append("Value must be ");
                        if (toInclusive) {
                            sb.append('\u2264'); // <= sign
                        } else {
                            sb.append('<');
                        }
                        sb.append(' ');
                        sb.append(Double.toString(to));
                    }
                } else {
                    sb.append("Value must be ");
                    if (fromInclusive) {
                        sb.append('\u2265'); // >= sign
                    } else {
                        sb.append('>');
                    }
                    sb.append(' ');
                    sb.append(Double.toString(from));
                }
            } else if (to != Double.POSITIVE_INFINITY) {
                sb.append("Value must be ");
                if (toInclusive) {
                    sb.append('\u2264'); // <= sign
                } else {
                    sb.append('<');
                }
                sb.append(' ');
                sb.append(Double.toString(to));
            }
            sb.append(" (was ");
            if (UastLiteralUtils.isNumberLiteral(node)) {
                // Use source text instead to avoid rounding errors involved in conversion, e.g
                //    Error: Value must be > 2.5 (was 2.490000009536743) [Range]
                //    printAtLeastExclusive(2.49f); // ERROR
                //                          ~~~~~
                //noinspection ConstantConditions
                String str = ((ULiteralExpression)node).getValue().toString();
                sb.append(str);
            } else {
                sb.append(value);
            }
            sb.append(')');
            return sb.toString();
        }
        return null;
    }

    private static void checkSize(
            @NonNull UastAndroidContext context,
            @NonNull UAnnotation annotation,
            @NonNull UElement argument) {
        int actual;
        if (UastLiteralUtils.isStringLiteral(argument)) {
            // Check string length
            ULiteralExpression literal = (ULiteralExpression) argument;
            String s = (String) literal.getValue();
            assert s != null;
            actual = s.length();
        } else if (argument instanceof UCallExpression
                   && ((UCallExpression)argument).getKind() == JavaUastCallKinds.ARRAY_INITIALIZER) {
            UCallExpression initializer = (UCallExpression) argument;
            actual = initializer.getValueArgumentCount();
        } else {
            // TODO: Collections syntax, e.g. Arrays.asList => param count, emptyList=0, singleton=1, etc
            // TODO: Flow analysis
            // No flow analysis for this check yet, only checking literals passed in as parameters
            return;
        }
        long exact = getLongAttribute(annotation, ATTR_VALUE, -1);
        long min = getLongAttribute(annotation, ATTR_MIN, Long.MIN_VALUE);
        long max = getLongAttribute(annotation, ATTR_MAX, Long.MAX_VALUE);
        long multiple = getLongAttribute(annotation, ATTR_MULTIPLE, 1);

        String unit;
        boolean isString = argument instanceof ULiteralExpression;
        if (isString) {
            unit = "length";
        } else {
            unit = "size";
        }
        String message = getSizeError(actual, exact, min, max, multiple, unit);
        if (message != null) {
            context.report(RANGE, argument, context.getLocation(argument), message);
        }
    }

    /**
     * Checks whether a given size follows the given constraints, and if so returns
     * null; otherwise returns a suitable error message.
     */
    private static String getSizeError(long actual, long exact, long min, long max, long multiple,
            @NonNull String unit) {
        String message = null;
        if (exact != -1) {
            if (exact != actual) {
                message = String.format("Expected %1$s %2$d (was %3$d)",
                        unit, exact, actual);
            }
        } else if (actual < min || actual > max) {
            StringBuilder sb = new StringBuilder(20);
            if (actual < min) {
                sb.append("Expected ").append(unit).append(" \u2265 ");
                sb.append(Long.toString(min));
            } else {
                assert actual > max;
                sb.append("Expected ").append(unit).append(" \u2264 ");
                sb.append(Long.toString(max));
            }
            sb.append(" (was ").append(actual).append(')');
            message = sb.toString();
        } else if (actual % multiple != 0) {
            message = String.format("Expected %1$s to be a multiple of %2$d (was %3$d "
                            + "and should be either %4$d or %5$d)",
                    unit, multiple, actual, (actual / multiple) * multiple,
                    (actual / multiple + 1) * multiple);
        }
        return message;
    }

    private static void checkTypeDefConstant(
            @NonNull UastAndroidContext context,
            @NonNull UAnnotation annotation,
            @NonNull UElement argument,
            @Nullable UElement errorNode,
            boolean flag) {
        if (UastLiteralUtils.isNullLiteral(argument)) {
            // Accepted for @StringDef
            return;
        }

        if (UastLiteralUtils.isStringLiteral(argument)) {
            ULiteralExpression string = (ULiteralExpression) argument;
            checkTypeDefConstant(context, annotation, argument, errorNode, false, string.getValue());
        } else if (UastLiteralUtils.isIntegralLiteral(argument)) {
            ULiteralExpression literal = (ULiteralExpression) argument;
            long value = UastLiteralUtils.getLongValue(literal);
            if (flag && value == 0) {
                // Accepted for a flag @IntDef
                return;
            }
            checkTypeDefConstant(context, annotation, argument, errorNode, flag, (int) value);
        } else if (isMinusOne(argument)) {
            // -1 is accepted unconditionally for flags
            if (!flag) {
                reportTypeDef(context, annotation, argument, errorNode);
            }
        } else if (argument instanceof UIfExpression && ((UIfExpression) argument).isTernary()) {
            UIfExpression expression = (UIfExpression) argument;
            if (expression.getThenBranch() != null) {
                checkTypeDefConstant(context, annotation, expression.getThenBranch(), errorNode, flag);
            }
            if (expression.getElseBranch() != null) {
                checkTypeDefConstant(context, annotation, expression.getElseBranch(), errorNode, flag);
            }
        } else if (argument instanceof UUnaryExpression) {
            UUnaryExpression expression = (UUnaryExpression) argument;
            UastOperator operator = expression.getOperator();
            if (flag) {
                checkTypeDefConstant(context, annotation, expression.getOperand(), errorNode, true);
            } else if (operator == UastPrefixOperator.BITWISE_NOT) {
                context.report(TYPE_DEF, expression, context.getLocation(expression),
                        "Flag not allowed here");
            }
        } else if (argument instanceof UBinaryExpression) {
            // If it's ?: then check both the if and else clauses
            UBinaryExpression expression = (UBinaryExpression) argument;
            if (flag) {
                checkTypeDefConstant(context, annotation, expression.getLeftOperand(), errorNode, true);
                checkTypeDefConstant(context, annotation, expression.getRightOperand(), errorNode, true);
            } else {
                UastOperator operator = expression.getOperator();
                if (operator == UastBinaryOperator.BITWISE_AND
                        || operator == UastBinaryOperator.BITWISE_OR
                        || operator == UastBinaryOperator.BITWISE_XOR) {
                    context.report(TYPE_DEF, expression, context.getLocation(expression),
                            "Flag not allowed here");
                }
            }
        } else if (argument instanceof UResolvable) {
            UDeclaration resolved = ((UResolvable)argument).resolve(context);
            if (resolved instanceof UVariable) {
                checkTypeDefConstant(context, annotation, argument, errorNode, flag, resolved);
            }
        }
    }

    private static void checkTypeDefConstant(@NonNull UastAndroidContext context,
            @NonNull UAnnotation annotation, @NonNull UElement argument,
            @Nullable UElement errorNode, boolean flag, Object value) {
        List<Object> valueArguments = annotation.getValues();
        for (Object o : valueArguments) {
            if (o.equals(value)) {
                return;
            }
        }
        reportTypeDef(context, argument, errorNode, flag, valueArguments);
    }

    private static void reportTypeDef(@NonNull UastAndroidContext context,
            @NonNull UAnnotation annotation, @NonNull UElement argument,
            @Nullable UElement errorNode) {
        List<Object> allowed = annotation.getValues();
        reportTypeDef(context, argument, errorNode, false, allowed);
    }

    private static void reportTypeDef(@NonNull UastAndroidContext context, @NonNull UElement node,
            @Nullable UElement errorNode, boolean flag, @NonNull List<Object> allowedValues) {
        String values = listAllowedValues(allowedValues);
        String message;
        if (flag) {
            message = "Must be one or more of: " + values;
        } else {
            message = "Must be one of: " + values;
        }
        if (errorNode == null) {
            errorNode = node;
        }
        context.report(TYPE_DEF, errorNode, context.getLocation(errorNode), message);
    }

    private static String listAllowedValues(@NonNull List<Object> allowedValues) {
        StringBuilder sb = new StringBuilder();
        for (Object allowedValue : allowedValues) {
            String s;
            if (allowedValue instanceof Integer) {
                s = allowedValue.toString();
            } else if (allowedValue instanceof UVariable) {
                UVariable variable = (UVariable) allowedValue;
                UClass containingClass = UastUtils.getContainingClassOrEmpty(variable);
                String containingClassName = containingClass.getFqName();
                if (containingClassName == null) {
                    continue;
                }
                containingClassName = containingClassName.substring(containingClassName.lastIndexOf('.') + 1);
                s = containingClassName + "." + variable.getName();
            } else if (allowedValue instanceof UFqNamed) {
                String fqName = ((UFqNamed)allowedValue).getFqName();
                if (fqName != null) {
                    s = fqName;
                } else {
                    continue;
                }
            } else {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    private static double getDoubleAttribute(@NonNull UAnnotation annotation,
            @NonNull String name, double defaultValue) {
        Object value = annotation.getValue(name);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return defaultValue;
    }

    private static long getLongAttribute(@NonNull UAnnotation annotation,
            @NonNull String name, long defaultValue) {
        Object value = annotation.getValue(name);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return defaultValue;
    }

    private static boolean getBoolean(@NonNull UAnnotation annotation,
            @NonNull String name, boolean defaultValue) {
        Object value = annotation.getValue(name);
        if (value instanceof Boolean) {
            return ((Boolean) value);
        }

        return defaultValue;
    }

    @Nullable
    static UAnnotation getRelevantAnnotation(
            @NonNull UAnnotation annotation,
            @NonNull UastAndroidContext context
    ) {
        String signature = annotation.getFqName();
        if (signature == null) {
            return null;
        }

        if (signature.startsWith(SUPPORT_ANNOTATIONS_PREFIX)) {
            // Bail on the nullness annotations early since they're the most commonly
            // defined ones. They're not analyzed in lint yet.
            if (signature.endsWith(".Nullable") || signature.endsWith(".NonNull")) {
                return null;
            }


            return annotation;
        }

        if (signature.startsWith("java.")) {
            // @Override, @SuppressWarnings etc. Ignore
            return null;
        }

        // Special case @IntDef and @StringDef: These are used on annotations
        // themselves. For example, you create a new annotation named @foo.bar.Baz,
        // annotate it with @IntDef, and then use @foo.bar.Baz in your signatures.
        // Here we want to map from @foo.bar.Baz to the corresponding int def.
        // Don't need to compute this if performing @IntDef or @StringDef lookup
        UClass type = annotation.resolve(context);
        if (type != null) {
            for (UAnnotation inner : type.getAnnotations()) {
                if (inner.matchesFqName(INT_DEF_ANNOTATION)
                        || inner.matchesFqName(STRING_DEF_ANNOTATION)
                        || inner.matchesFqName(PERMISSION_ANNOTATION)) {
                    return inner;
                }
            }
        }

        return null;
    }

    @Nullable
    static UAnnotation getRelevantAnnotation(
            @NonNull UastAndroidContext context,
            @NonNull UAnnotation annotation
    ) {
        String signature = annotation.getFqName();
        if (signature != null && signature.startsWith(SUPPORT_ANNOTATIONS_PREFIX)) {
            // Bail on the nullness annotations early since they're the most commonly
            // defined ones. They're not analyzed in lint yet.
            if (signature.endsWith(".Nullable") || signature.endsWith(".NonNull")) {
                return null;
            }


            return annotation;
        }

        if (signature != null && signature.startsWith("java.")) {
            // @Override, @SuppressWarnings etc. Ignore
            return null;
        }

        // Special case @IntDef and @StringDef: These are used on annotations
        // themselves. For example, you create a new annotation named @foo.bar.Baz,
        // annotate it with @IntDef, and then use @foo.bar.Baz in your signatures.
        // Here we want to map from @foo.bar.Baz to the corresponding int def.
        // Don't need to compute this if performing @IntDef or @StringDef lookup
        UClass type = annotation.resolve(context);
        if (type != null) {
            for (UAnnotation inner : type.getAnnotations()) {
                if (INT_DEF_ANNOTATION.equals(inner.getFqName())
                    || STRING_DEF_ANNOTATION.equals(inner.getFqName())
                    || PERMISSION_ANNOTATION.equals(inner.getFqName())) {
                    return inner;
                }
            }
        }

        return null;
    }

    // ---- Implements UastScanner ----

    @Override
    public UastVisitor createUastVisitor(UastAndroidContext context) {
        return new CallVisitor(context);
    }

    private class CallVisitor extends UastVisitor {
        private final UastAndroidContext mContext;

        public CallVisitor(UastAndroidContext context) {
            mContext = context;
        }

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression node) {
            if (node.getKind() == UastCallKind.FUNCTION_CALL) {
                visitFunctionInvocation(node);
            }

            return super.visitCallExpression(node);
        }

        private boolean visitFunctionInvocation(@NonNull UCallExpression call) {
            UFunction method = call.resolve(mContext);
            if (method != null) {
                List<UAnnotation> annotations = method.getAnnotations();
                for (UAnnotation annotation : annotations) {
                    annotation = getRelevantAnnotation(annotation, mContext);
                    if (annotation != null) {
                        checkMethodAnnotation(mContext, method, call, annotation);
                    }
                }

                // Look for annotations on the class as well: these trickle
                // down to all the methods in the class
                UClass containingClass = UastUtils.getContainingClass(method);
                if (containingClass != null) {
                    annotations = containingClass.getAnnotations();
                    for (UAnnotation annotation : annotations) {
                        annotation = getRelevantAnnotation(annotation, mContext);
                        if (annotation != null) {
                            checkMethodAnnotation(mContext, method, call, annotation);
                        }
                    }
                }

                Iterator<UExpression> arguments = call.getValueArguments().iterator();
                for (int i = 0, n = method.getValueParameterCount();
                        i < n && arguments.hasNext();
                        i++) {
                    UExpression argument = arguments.next();

                    annotations = method.getValueParameters().get(i).getAnnotations();
                    for (UAnnotation annotation : annotations) {
                        annotation = getRelevantAnnotation(annotation, mContext);
                        if (annotation != null) {
                            checkParameterAnnotation(mContext, argument, annotation);
                        }
                    }
                }
            }

            return false;
        }
    }
}
