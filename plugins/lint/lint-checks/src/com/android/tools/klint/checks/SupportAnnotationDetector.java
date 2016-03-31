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

package com.android.tools.lint.checks;

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
import static com.android.tools.lint.checks.PermissionRequirement.ATTR_PROTECTION_LEVEL;
import static com.android.tools.lint.checks.PermissionRequirement.VALUE_DANGEROUS;
import static com.android.tools.lint.detector.api.JavaContext.findSurroundingMethod;
import static com.android.tools.lint.detector.api.JavaContext.getParentOfType;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import com.android.tools.lint.checks.PermissionHolder.SetPermissionLookup;
import com.android.tools.lint.client.api.JavaParser.ResolvedAnnotation;
import com.android.tools.lint.client.api.JavaParser.ResolvedClass;
import com.android.tools.lint.client.api.JavaParser.ResolvedField;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.client.api.JavaParser.TypeDescriptor;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ConstantEvaluator;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.utils.XmlUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;

import lombok.ast.ArrayCreation;
import lombok.ast.ArrayInitializer;
import lombok.ast.AstVisitor;
import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.Catch;
import lombok.ast.Expression;
import lombok.ast.ExpressionStatement;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.InlineIfExpression;
import lombok.ast.IntegralLiteral;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.NullLiteral;
import lombok.ast.Select;
import lombok.ast.Statement;
import lombok.ast.StringLiteral;
import lombok.ast.Try;
import lombok.ast.TypeReference;
import lombok.ast.UnaryExpression;
import lombok.ast.UnaryOperator;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.VariableReference;

/**
 * Looks up annotations on method calls and enforces the various things they
 * express, e.g. for {@code @CheckReturn} it makes sure the return value is used,
 * for {@code ColorInt} it ensures that a proper color integer is passed in, etc.
 *
 * TODO: Throw in some annotation usage checks here too; e.g. specifying @Size without parameters,
 * specifying toInclusive without setting to, combining @ColorInt with any @ResourceTypeRes,
 * using @CheckResult on a void method, etc.
 */
public class SupportAnnotationDetector extends Detector implements Detector.JavaScanner {

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
            @NonNull JavaContext context,
            @NonNull ResolvedMethod method,
            @NonNull MethodInvocation node,
            @NonNull ResolvedAnnotation annotation) {
        String signature = annotation.getSignature();
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
            @NonNull JavaContext context,
            @NonNull Node argument,
            @NonNull ResolvedAnnotation annotation) {
        String signature = annotation.getSignature();

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

    private static void checkColor(@NonNull JavaContext context, @NonNull Node argument) {
        if (argument instanceof InlineIfExpression) {
            InlineIfExpression expression = (InlineIfExpression) argument;
            checkColor(context, expression.astIfTrue());
            checkColor(context, expression.astIfFalse());
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
            @NonNull JavaContext context,
            @NonNull MethodInvocation node,
            @NonNull ResolvedMethod method,
            @NonNull ResolvedAnnotation annotation) {
        PermissionRequirement requirement = PermissionRequirement.create(context, annotation);
        if (requirement.isConditional()) {
            return;
        }
        PermissionHolder permissions = getPermissions(context);
        if (!requirement.isSatisfied(permissions)) {
            // See if it looks like we're holding the permission implicitly by @RequirePermission
            // annotations in the surrounding context
            permissions  = addLocalPermissions(context, permissions, node);
            if (!requirement.isSatisfied(permissions)) {
                String name = method.getContainingClass().getSimpleName() + "." + method.getName();
                String message = getMissingPermissionMessage(requirement, name, permissions);
                context.report(MISSING_PERMISSION, node, context.getLocation(node), message);
            }
        } else if (requirement.isRevocable(permissions) &&
                context.getMainProject().getTargetSdkVersion().getFeatureLevel() >= 23) {
            // Ensure that the caller is handling a security exception
            // First check to see if we're inside a try/catch which catches a SecurityException
            // (or some wider exception than that). Check for nested try/catches too.
            boolean handlesMissingPermission = false;
            Node parent = node;
            while (true) {
                Try tryCatch = getParentOfType(parent, Try.class);
                if (tryCatch == null) {
                    break;
                } else {
                    for (Catch aCatch : tryCatch.astCatches()) {
                        TypeReference catchType = aCatch.astExceptionDeclaration().
                                astTypeReference();
                        if (isSecurityException(context,
                                catchType)) {
                            handlesMissingPermission = true;
                            break;
                        }
                    }
                    parent = tryCatch;
                }
            }

            // If not, check to see if the method itself declares that it throws a
            // SecurityException or something wider.
            if (!handlesMissingPermission) {
                MethodDeclaration declaration = getParentOfType(parent, MethodDeclaration.class);
                if (declaration != null) {
                    for (TypeReference typeReference : declaration.astThrownTypeReferences()) {
                        if (isSecurityException(context, typeReference)) {
                            handlesMissingPermission = true;
                            break;
                        }
                    }
                }
            }

            // If not, check to see if the code is deliberately checking to see if the
            // given permission is available.
            if (!handlesMissingPermission) {
                Node methodNode = JavaContext.findSurroundingMethod(node);
                if (methodNode != null) {
                    CheckPermissionVisitor visitor = new CheckPermissionVisitor(node);
                    methodNode.accept(visitor);
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
            @NonNull JavaContext context,
            @NonNull PermissionHolder permissions,
            @NonNull Node node) {
        // Accumulate @RequirePermissions available in the local context
        Node methodNode = JavaContext.findSurroundingMethod(node);
        if (methodNode == null) {
            return permissions;
        }
        ResolvedNode resolved = context.resolve(methodNode);
        if (!(resolved instanceof ResolvedMethod)) {
            return permissions;
        }
        ResolvedMethod method = (ResolvedMethod) resolved;
        ResolvedAnnotation annotation = method.getAnnotation(PERMISSION_ANNOTATION);
        permissions = mergeAnnotationPermissions(context, permissions, annotation);
        annotation = method.getContainingClass().getAnnotation(PERMISSION_ANNOTATION);
        permissions = mergeAnnotationPermissions(context, permissions, annotation);
        return permissions;
    }

    @NonNull
    private static PermissionHolder mergeAnnotationPermissions(
            @NonNull JavaContext context,
            @NonNull PermissionHolder permissions,
            @Nullable ResolvedAnnotation annotation) {
        if (annotation != null) {
            PermissionRequirement requirement = PermissionRequirement.create(context, annotation);
            permissions = SetPermissionLookup.join(permissions, requirement);
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
    private static class CheckPermissionVisitor extends ForwardingAstVisitor {
        private boolean mChecksPermission;
        private boolean mDone;
        private final Node mTarget;

        public CheckPermissionVisitor(@NonNull Node target) {
            mTarget = target;
        }

        @Override
        public boolean visitNode(Node node) {
            return mDone;
        }

        @Override
        public boolean visitMethodInvocation(MethodInvocation node) {
            if (node == mTarget) {
                mDone = true;
            }

            String name = node.astName().astValue();
            if ((name.startsWith("check") || name.startsWith("enforce"))
                    && name.endsWith("Permission")) {
                mChecksPermission = true;
                mDone = true;
            }
            return super.visitMethodInvocation(node);
        }

        public boolean checksPermission() {
            return mChecksPermission;
        }
    }

    private static boolean isSecurityException(
            @NonNull JavaContext context,
            @NonNull TypeReference typeReference) {
        TypeDescriptor type = context.getType(typeReference);
        return type != null && (type.matchesSignature("java.lang.SecurityException") ||
                type.matchesSignature("java.lang.RuntimeException") ||
                type.matchesSignature("java.lang.Exception") ||
                type.matchesSignature("java.lang.Throwable"));
    }

    private PermissionHolder mPermissions;

    private PermissionHolder getPermissions(
            @NonNull JavaContext context) {
        if (mPermissions == null) {
            Set<String> permissions = Sets.newHashSetWithExpectedSize(30);
            Set<String> revocable = Sets.newHashSetWithExpectedSize(4);
            LintClient client = context.getClient();
            // Gather permissions from all projects that contribute to the
            // main project.
            Project mainProject = context.getMainProject();
            for (File manifest : mainProject.getManifestFiles()) {
                addPermissions(client, permissions, revocable, manifest);
            }
            for (Project library : mainProject.getAllLibraries()) {
                for (File manifest : library.getManifestFiles()) {
                    addPermissions(client, permissions, revocable, manifest);
                }
            }

            mPermissions = new SetPermissionLookup(permissions, revocable);
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
            org.w3c.dom.Node item = children.item(i);
            if (item.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
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
                        ATTR_PROTECTION_LEVEL);
                if (VALUE_DANGEROUS.equals(protectionLevel)) {
                    String name = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
                    if (!name.isEmpty()) {
                        revocable.add(name);
                    }
                }
            }
        }
    }

    private static void checkResult(@NonNull JavaContext context, @NonNull MethodInvocation node,
            @NonNull ResolvedAnnotation annotation) {
        if (node.getParent() instanceof ExpressionStatement) {
            String methodName = node.astName().astValue();
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
            @NonNull JavaContext context,
            @NonNull MethodInvocation node,
            @NonNull ResolvedMethod method,
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
    private static String getThreadContext(@NonNull JavaContext context,
            @NonNull MethodInvocation methodCall) {
        Node node = findSurroundingMethod(methodCall);
        if (node != null) {
            ResolvedNode resolved = context.resolve(node);
            if (resolved instanceof ResolvedMethod) {
                ResolvedMethod method = (ResolvedMethod) resolved;
                ResolvedClass cls = method.getContainingClass();

                while (method != null) {
                    for (ResolvedAnnotation annotation : method.getAnnotations()) {
                        String name = annotation.getSignature();
                        if (name.startsWith(SUPPORT_ANNOTATIONS_PREFIX)
                                && name.endsWith(THREAD_SUFFIX)) {
                            return name;
                        }
                    }
                    method = method.getSuperMethod();
                }

                // See if we're extending a class with a known threading context
                while (cls != null) {
                    for (ResolvedAnnotation annotation : cls.getAnnotations()) {
                        String name = annotation.getSignature();
                        if (name.startsWith(SUPPORT_ANNOTATIONS_PREFIX)
                                && name.endsWith(THREAD_SUFFIX)) {
                            return name;
                        }
                    }
                    cls = cls.getSuperClass();
                }
            }
        }

        // In the future, we could also try to infer the threading context using
        // other heuristics. For example, if we're in a method with unknown threading
        // context, but we see that the method is called by another method with a known
        // threading context, we can infer that that threading context is the context for
        // this thread too (assuming the call is direct).

        return null;
    }

    private static boolean isNumber(@NonNull Node argument) {
        return argument instanceof IntegralLiteral || argument instanceof UnaryExpression
                && ((UnaryExpression) argument).astOperator() == UnaryOperator.UNARY_MINUS
                && ((UnaryExpression) argument).astOperand() instanceof IntegralLiteral;
    }

    private static boolean isZero(@NonNull Node argument) {
        return argument instanceof IntegralLiteral
                && ((IntegralLiteral) argument).astIntValue() == 0;
    }

    private static boolean isMinusOne(@NonNull Node argument) {
        return argument instanceof UnaryExpression
                && ((UnaryExpression) argument).astOperator() == UnaryOperator.UNARY_MINUS
                && ((UnaryExpression) argument).astOperand() instanceof IntegralLiteral
                && ((IntegralLiteral) ((UnaryExpression) argument).astOperand()).astIntValue()
                == 1;
    }

    private static void checkResourceType(
            @NonNull JavaContext context,
            @NonNull Node argument,
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
    private static List<ResourceType> getResourceTypes(@NonNull JavaContext context,
            @NonNull Node argument) {
        if (argument instanceof Select) {
            Select node = (Select) argument;
            if (node.astOperand() instanceof Select) {
                Select select = (Select) node.astOperand();
                if (select.astOperand() instanceof Select) { // android.R....
                    Select innerSelect = (Select) select.astOperand();
                    if (innerSelect.astIdentifier().astValue().equals(R_CLASS)) {
                        String typeName = select.astIdentifier().astValue();
                        ResourceType type = ResourceType.getEnum(typeName);
                        return type != null ? Collections.singletonList(type) : null;
                    }
                }
                if (select.astOperand() instanceof VariableReference) {
                    VariableReference reference = (VariableReference) select.astOperand();
                    if (reference.astIdentifier().astValue().equals(R_CLASS)) {
                        String typeName = select.astIdentifier().astValue();
                        ResourceType type = ResourceType.getEnum(typeName);
                        return type != null ? Collections.singletonList(type) : null;
                    }
                }
            }

            // Arbitrary packages -- android.R.type.name, foo.bar.R.type.name
            if (node.astIdentifier().astValue().equals(R_CLASS)) {
                Node parent = node.getParent();
                if (parent instanceof Select) {
                    Node grandParent = parent.getParent();
                    if (grandParent instanceof Select) {
                        Select select = (Select) grandParent;
                        Expression typeOperand = select.astOperand();
                        if (typeOperand instanceof Select) {
                            Select typeSelect = (Select) typeOperand;
                            String typeName = typeSelect.astIdentifier().astValue();
                            ResourceType type = ResourceType.getEnum(typeName);
                            return type != null ? Collections.singletonList(type) : null;
                        }
                    }
                }
            }
        } else if (argument instanceof VariableReference) {
            Statement statement = getParentOfType(argument, Statement.class, false);
            if (statement != null) {
                ListIterator<Node> iterator = statement.getParent().getChildren().listIterator();
                while (iterator.hasNext()) {
                    if (iterator.next() == statement) {
                        if (iterator.hasPrevious()) { // should always be true
                            iterator.previous();
                        }
                        break;
                    }
                }

                String targetName = ((VariableReference)argument).astIdentifier().astValue();
                while (iterator.hasPrevious()) {
                    Node previous = iterator.previous();
                    if (previous instanceof VariableDeclaration) {
                        VariableDeclaration declaration = (VariableDeclaration) previous;
                        VariableDefinition definition = declaration.astDefinition();
                        for (VariableDefinitionEntry entry : definition
                                .astVariables()) {
                            if (entry.astInitializer() != null
                                    && entry.astName().astValue().equals(targetName)) {
                                return getResourceTypes(context, entry.astInitializer());
                            }
                        }
                    } else if (previous instanceof ExpressionStatement) {
                        ExpressionStatement expressionStatement = (ExpressionStatement) previous;
                        Expression expression = expressionStatement.astExpression();
                        if (expression instanceof BinaryExpression &&
                                ((BinaryExpression) expression).astOperator()
                                        == BinaryOperator.ASSIGN) {
                            BinaryExpression binaryExpression = (BinaryExpression) expression;
                            if (targetName.equals(binaryExpression.astLeft().toString())) {
                                return getResourceTypes(context, binaryExpression.astRight());
                            }
                        }
                    }
                }
            }
        } else if (argument instanceof MethodInvocation) {
            ResolvedNode resolved = context.resolve(argument);
            if (resolved != null) {
                for (ResolvedAnnotation annotation : resolved.getAnnotations()) {
                    String signature = annotation.getSignature();
                    if (signature.equals(COLOR_INT_ANNOTATION)) {
                        return Collections.singletonList(COLOR_INT_MARKER_TYPE);
                    }
                    if (signature.endsWith(RES_SUFFIX)
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
            @NonNull JavaContext context,
            @NonNull ResolvedAnnotation annotation,
            @NonNull Node argument) {
        Object object = ConstantEvaluator.evaluate(context, argument);
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
            @NonNull JavaContext context,
            @NonNull ResolvedAnnotation annotation,
            @NonNull Node argument) {
        Object object = ConstantEvaluator.evaluate(context, argument);
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
            boolean fromInclusive, boolean toInclusive, @NonNull Node node) {
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
            if (node instanceof FloatingPointLiteral || node instanceof IntegralLiteral) {
                // Use source text instead to avoid rounding errors involved in conversion, e.g
                //    Error: Value must be > 2.5 (was 2.490000009536743) [Range]
                //    printAtLeastExclusive(2.49f); // ERROR
                //                          ~~~~~
                String str = node.toString();
                if (str.endsWith("f") || str.endsWith("F")) {
                    str = str.substring(0, str.length() - 1);
                }
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
            @NonNull JavaContext context,
            @NonNull ResolvedAnnotation annotation,
            @NonNull Node argument) {
        int actual;
        if (argument instanceof StringLiteral) {
            // Check string length
            StringLiteral literal = (StringLiteral) argument;
            String s = literal.astValue();
            actual = s.length();
        } else if (argument instanceof ArrayCreation) {
            ArrayCreation literal = (ArrayCreation) argument;
            ArrayInitializer initializer = literal.astInitializer();
            if (initializer == null) {
                return;
            }
            actual = initializer.astExpressions().size();
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
        boolean isString = argument instanceof StringLiteral;
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
            @NonNull JavaContext context,
            @NonNull ResolvedAnnotation annotation,
            @NonNull Node argument,
            @Nullable Node errorNode,
            boolean flag) {
        if (argument instanceof NullLiteral) {
            // Accepted for @StringDef
            return;
        }

        if (argument instanceof StringLiteral) {
            StringLiteral string = (StringLiteral) argument;
            checkTypeDefConstant(context, annotation, argument, errorNode, false, string.astValue());
        } else if (argument instanceof IntegralLiteral) {
            IntegralLiteral literal = (IntegralLiteral) argument;
            int value = literal.astIntValue();
            if (flag && value == 0) {
                // Accepted for a flag @IntDef
                return;
            }
            checkTypeDefConstant(context, annotation, argument, errorNode, flag, value);
        } else if (isMinusOne(argument)) {
            // -1 is accepted unconditionally for flags
            if (!flag) {
                reportTypeDef(context, annotation, argument, errorNode);
            }
        } else if (argument instanceof InlineIfExpression) {
            InlineIfExpression expression = (InlineIfExpression) argument;
            if (expression.astIfTrue() != null) {
                checkTypeDefConstant(context, annotation, expression.astIfTrue(), errorNode, flag);
            }
            if (expression.astIfFalse() != null) {
                checkTypeDefConstant(context, annotation, expression.astIfFalse(), errorNode, flag);
            }
        } else if (argument instanceof UnaryExpression) {
            UnaryExpression expression = (UnaryExpression) argument;
            UnaryOperator operator = expression.astOperator();
            if (flag) {
                checkTypeDefConstant(context, annotation, expression.astOperand(), errorNode, true);
            } else if (operator == UnaryOperator.BINARY_NOT) {
                context.report(TYPE_DEF, expression, context.getLocation(expression),
                        "Flag not allowed here");
            }
        } else if (argument instanceof BinaryExpression) {
            // If it's ?: then check both the if and else clauses
            BinaryExpression expression = (BinaryExpression) argument;
            if (flag) {
                checkTypeDefConstant(context, annotation, expression.astLeft(), errorNode, true);
                checkTypeDefConstant(context, annotation, expression.astRight(), errorNode, true);
            } else {
                BinaryOperator operator = expression.astOperator();
                if (operator == BinaryOperator.BITWISE_AND
                        || operator == BinaryOperator.BITWISE_OR
                        || operator == BinaryOperator.BITWISE_XOR) {
                    context.report(TYPE_DEF, expression, context.getLocation(expression),
                            "Flag not allowed here");
                }
            }
        } else {
            ResolvedNode resolved = context.resolve(argument);
            if (resolved instanceof ResolvedField) {
                checkTypeDefConstant(context, annotation, argument, errorNode, flag, resolved);
            } else if (argument instanceof VariableReference) {
                Statement statement = getParentOfType(argument, Statement.class, false);
                if (statement != null) {
                    ListIterator<Node> iterator = statement.getParent().getChildren().listIterator();
                    while (iterator.hasNext()) {
                        if (iterator.next() == statement) {
                            if (iterator.hasPrevious()) { // should always be true
                                iterator.previous();
                            }
                            break;
                        }
                    }

                    String targetName = ((VariableReference)argument).astIdentifier().astValue();
                    while (iterator.hasPrevious()) {
                        Node previous = iterator.previous();
                        if (previous instanceof VariableDeclaration) {
                            VariableDeclaration declaration = (VariableDeclaration) previous;
                            VariableDefinition definition = declaration.astDefinition();
                            for (VariableDefinitionEntry entry : definition
                                    .astVariables()) {
                                if (entry.astInitializer() != null
                                        && entry.astName().astValue().equals(targetName)) {
                                    checkTypeDefConstant(context, annotation,
                                            entry.astInitializer(),
                                            errorNode != null ? errorNode : argument, flag);
                                    return;
                                }
                            }
                        } else if (previous instanceof ExpressionStatement) {
                            ExpressionStatement expressionStatement = (ExpressionStatement) previous;
                            Expression expression = expressionStatement.astExpression();
                            if (expression instanceof BinaryExpression &&
                                    ((BinaryExpression) expression).astOperator()
                                            == BinaryOperator.ASSIGN) {
                                BinaryExpression binaryExpression = (BinaryExpression) expression;
                                if (targetName.equals(binaryExpression.astLeft().toString())) {
                                    checkTypeDefConstant(context, annotation,
                                            binaryExpression.astRight(),
                                            errorNode != null ? errorNode : argument, flag);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void checkTypeDefConstant(@NonNull JavaContext context,
            @NonNull ResolvedAnnotation annotation, @NonNull Node argument,
            @Nullable Node errorNode, boolean flag, Object value) {
        Object allowed = annotation.getValue();
        if (allowed instanceof Object[]) {
            Object[] allowedValues = (Object[]) allowed;
            for (Object o : allowedValues) {
                if (o.equals(value)) {
                    return;
                }
            }
            reportTypeDef(context, argument, errorNode, flag, allowedValues);
        }
    }

    private static void reportTypeDef(@NonNull JavaContext context,
            @NonNull ResolvedAnnotation annotation, @NonNull Node argument,
            @Nullable Node errorNode) {
        Object allowed = annotation.getValue();
        if (allowed instanceof Object[]) {
            Object[] allowedValues = (Object[]) allowed;
            reportTypeDef(context, argument, errorNode, false, allowedValues);
        }
    }

    private static void reportTypeDef(@NonNull JavaContext context, @NonNull Node node,
            @Nullable Node errorNode, boolean flag, @NonNull Object[] allowedValues) {
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

    private static String listAllowedValues(@NonNull Object[] allowedValues) {
        StringBuilder sb = new StringBuilder();
        for (Object allowedValue : allowedValues) {
            String s;
            if (allowedValue instanceof Integer) {
                s = allowedValue.toString();
            } else if (allowedValue instanceof ResolvedNode) {
                ResolvedNode node = (ResolvedNode) allowedValue;
                if (node instanceof ResolvedField) {
                    ResolvedField field = (ResolvedField) node;
                    String containingClassName = field.getContainingClassName();
                    containingClassName = containingClassName.substring(containingClassName.lastIndexOf('.') + 1);
                    s = containingClassName + "." + field.getName();
                } else {
                    s = node.getSignature();
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

    private static double getDoubleAttribute(@NonNull ResolvedAnnotation annotation,
            @NonNull String name, double defaultValue) {
        Object value = annotation.getValue(name);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return defaultValue;
    }

    private static long getLongAttribute(@NonNull ResolvedAnnotation annotation,
            @NonNull String name, long defaultValue) {
        Object value = annotation.getValue(name);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return defaultValue;
    }

    private static boolean getBoolean(@NonNull ResolvedAnnotation annotation,
            @NonNull String name, boolean defaultValue) {
        Object value = annotation.getValue(name);
        if (value instanceof Boolean) {
            return ((Boolean) value);
        }

        return defaultValue;
    }

    @Nullable
    static ResolvedAnnotation getRelevantAnnotation(@NonNull ResolvedAnnotation annotation) {
        String signature = annotation.getSignature();
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
        ResolvedClass type = annotation.getClassType();
        if (type != null) {
            for (ResolvedAnnotation inner : type.getAnnotations()) {
                if (inner.matches(INT_DEF_ANNOTATION)
                        || inner.matches(STRING_DEF_ANNOTATION)
                        || inner.matches(PERMISSION_ANNOTATION)) {
                    return inner;
                }
            }
        }

        return null;
    }

    // ---- Implements JavaScanner ----

    @Override
    public
    List<Class<? extends Node>> getApplicableNodeTypes() {
        return Collections.<Class<? extends Node>>singletonList(MethodInvocation.class);
    }

    @Nullable
    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return new CallVisitor(context);
    }

    private class CallVisitor extends ForwardingAstVisitor {
        private final JavaContext mContext;

        public CallVisitor(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitMethodInvocation(@NonNull MethodInvocation call) {
            ResolvedNode resolved = mContext.resolve(call);
            if (resolved instanceof ResolvedMethod) {
                ResolvedMethod method = (ResolvedMethod) resolved;
                Iterable<ResolvedAnnotation> annotations = method.getAnnotations();
                for (ResolvedAnnotation annotation : annotations) {
                    annotation = getRelevantAnnotation(annotation);
                    if (annotation != null) {
                        checkMethodAnnotation(mContext, method, call, annotation);
                    }
                }

                // Look for annotations on the class as well: these trickle
                // down to all the methods in the class
                ResolvedClass containingClass = method.getContainingClass();
                annotations = containingClass.getAnnotations();
                for (ResolvedAnnotation annotation : annotations) {
                    annotation = getRelevantAnnotation(annotation);
                    if (annotation != null) {
                        checkMethodAnnotation(mContext, method, call, annotation);
                    }
                }

                Iterator<Expression> arguments = call.astArguments().iterator();
                for (int i = 0, n = method.getArgumentCount();
                        i < n && arguments.hasNext();
                        i++) {
                    Expression argument = arguments.next();

                    annotations = method.getParameterAnnotations(i);
                    for (ResolvedAnnotation annotation : annotations) {
                        annotation = getRelevantAnnotation(annotation);
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
