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


import static com.android.SdkConstants.ATTR_VALUE;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_ALL_OF;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_ANY_OF;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_CONDITIONAL;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.JavaParser.ResolvedAnnotation;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.JavaContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Node;
import lombok.ast.Select;
import lombok.ast.VariableDefinitionEntry;

/**
 * A permission requirement is a boolean expression of permission names that a
 * caller must satisfy for a given Android API.
 */
public abstract class PermissionRequirement {
    public static final String ATTR_PROTECTION_LEVEL = "protectionLevel"; //$NON-NLS-1$
    public static final String VALUE_DANGEROUS = "dangerous"; //$NON-NLS-1$

    private final ResolvedAnnotation annotation;

    @SuppressWarnings("ConstantConditions")
    public static final PermissionRequirement NONE = new PermissionRequirement(null) {
        @Override
        public boolean isSatisfied(@NonNull PermissionHolder available) {
            return true;
        }

        @Override
        public boolean isConditional() {
            return false;
        }

        @Override
        public boolean isRevocable(@NonNull PermissionHolder revocable) {
            return false;
        }

        @Override
        public String toString() {
            return "None";
        }

        @Override
        protected void addMissingPermissions(@NonNull PermissionHolder available,
          @NonNull Set<String> result) {
        }

        @Override
        protected void addRevocablePermissions(@NonNull Set<String> result,
                @NonNull PermissionHolder revocable) {
        }

        @Nullable
        @Override
        public BinaryOperator getOperator() {
            return null;
        }

        @NonNull
        @Override
        public Iterable<PermissionRequirement> getChildren() {
            return Collections.emptyList();
        }
    };

    private PermissionRequirement(@NonNull ResolvedAnnotation annotation) {
        this.annotation = annotation;
    }

    @NonNull
    public static PermissionRequirement create(
            @Nullable Context context,
            @NonNull ResolvedAnnotation annotation) {
        String value = (String)annotation.getValue(ATTR_VALUE);
        if (value != null && !value.isEmpty()) {
            for (int i = 0, n = value.length(); i < n; i++) {
                char c = value.charAt(i);
                // See if it's a complex expression and if so build it up
                if (c == '&' || c == '|' || c == '^') {
                    return Complex.parse(annotation, context, value);
                }
            }

            return new Single(annotation, value);
        }

        Object v = annotation.getValue(ATTR_ANY_OF);
        if (v != null) {
            if (v instanceof String[]) {
                String[] anyOf = (String[])v;
                if (anyOf.length > 0) {
                    return new Many(annotation, BinaryOperator.LOGICAL_OR, anyOf);
                }
            } else if (v instanceof String) {
                String[] anyOf = new String[] { (String)v };
                return new Many(annotation, BinaryOperator.LOGICAL_OR, anyOf);
            }
        }

        v = annotation.getValue(ATTR_ALL_OF);
        if (v != null) {
            if (v instanceof String[]) {
                String[] allOf = (String[])v;
                if (allOf.length > 0) {
                    return new Many(annotation, BinaryOperator.LOGICAL_AND, allOf);
                }
            } else if (v instanceof String) {
                String[] allOf = new String[] { (String)v };
                return new Many(annotation, BinaryOperator.LOGICAL_AND, allOf);
            }
        }

        return NONE;
    }

    /**
     * Returns whether this requirement is conditional, meaning that there are
     * some circumstances in which the requirement is not necessary. For
     * example, consider
     * {@code android.app.backup.BackupManager.dataChanged(java.lang.String)} .
     * Here the {@code android.permission.BACKUP} is required but only if the
     * argument is not your own package.
     * <p>
     * This is used to handle permissions differently between the "missing" and
     * "unused" checks. When checking for missing permissions, we err on the
     * side of caution: if you are missing a permission, but the permission is
     * conditional, you may not need it so we may not want to complain. However,
     * when looking for unused permissions, we don't want to flag the
     * conditional permissions as unused since they may be required.
     *
     * @return true if this requirement is conditional
     */
    public boolean isConditional() {
        Object o = annotation.getValue(ATTR_CONDITIONAL);
        if (o instanceof Boolean) {
            return (Boolean)o;
        }
        return false;
    }

    /**
     * Returns whether this requirement is for a single permission (rather than
     * a boolean expression such as one permission or another.)
     *
     * @return true if this requirement is just a simple permission name
     */
    public boolean isSingle() {
        return true;
    }

    /**
     * Whether the permission requirement is satisfied given the set of granted permissions
     *
     * @param available the available permissions
     * @return true if all permissions specified by this requirement are available
     */
    public abstract boolean isSatisfied(@NonNull PermissionHolder available);

    /** Describes the missing permissions (e.g. "P1, P2 and P3") */
    public String describeMissingPermissions(@NonNull PermissionHolder available) {
        return "";
    }

    /** Returns the missing permissions (e.g. {"P1", "P2", "P3"} */
    public Set<String> getMissingPermissions(@NonNull PermissionHolder available) {
        Set<String> result = Sets.newHashSet();
        addMissingPermissions(available, result);
        return result;
    }

    protected abstract void addMissingPermissions(@NonNull PermissionHolder available,
        @NonNull Set<String> result);

    /** Returns the permissions in the requirement that are revocable */
    public Set<String> getRevocablePermissions(@NonNull PermissionHolder revocable) {
        Set<String> result = Sets.newHashSet();
        addRevocablePermissions(result, revocable);
        return result;
    }

    protected abstract void addRevocablePermissions(@NonNull Set<String> result,
            @NonNull PermissionHolder revocable);

    /**
     * Returns whether this permission is revocable
     *
     * @param revocable the set of revocable permissions
     * @return true if a user can revoke the permission
     */
    public abstract boolean isRevocable(@NonNull PermissionHolder revocable);

    /**
     * For permission requirements that combine children, the operator to combine them with; null
     * for leaf nodes
     */
    @Nullable
    public abstract BinaryOperator getOperator();

    /**
     * Returns nested requirements, combined via {@link #getOperator()}
     */
    @NonNull
    public abstract Iterable<PermissionRequirement> getChildren();

    /** Require a single permission */
    private static class Single extends PermissionRequirement {
        public final String name;

        public Single(@NonNull ResolvedAnnotation annotation, @NonNull String name) {
            super(annotation);
            this.name = name;
        }

        @Override
        public boolean isRevocable(@NonNull PermissionHolder revocable) {
            return revocable.isRevocable(name) || isRevocableSystemPermission(name);
        }

        @Nullable
        @Override
        public BinaryOperator getOperator() {
            return null;
        }

        @NonNull
        @Override
        public Iterable<PermissionRequirement> getChildren() {
            return Collections.emptyList();
        }

        @Override
        public boolean isSingle() {
            return true;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean isSatisfied(@NonNull PermissionHolder available) {
            return available.hasPermission(name);
        }

        @Override
        public String describeMissingPermissions(@NonNull PermissionHolder available) {
            return isSatisfied(available) ? "" : name;
        }

        @Override
        protected void addMissingPermissions(@NonNull PermissionHolder available,
            @NonNull Set<String> missing) {
            if (!isSatisfied(available)) {
                missing.add(name);
            }
        }

        @Override
        protected void addRevocablePermissions(@NonNull Set<String> result,
                @NonNull PermissionHolder revocable) {
            if (isRevocable(revocable)) {
                result.add(name);
            }
        }
    }

    protected static void appendOperator(StringBuilder sb, BinaryOperator operator) {
        sb.append(' ');
        if (operator == BinaryOperator.LOGICAL_AND) {
            sb.append("and");
        } else if (operator == BinaryOperator.LOGICAL_OR) {
            sb.append("or");
        } else {
            assert operator == BinaryOperator.BITWISE_XOR : operator;
            sb.append("xor");
        }
        sb.append(' ');
    }

    /**
     * Require a series of permissions, all with the same operator.
     */
    private static class Many extends PermissionRequirement {
        public final BinaryOperator operator;
        public final List<PermissionRequirement> permissions;

        public Many(
                @NonNull ResolvedAnnotation annotation,
                BinaryOperator operator,
                String[] names) {
            super(annotation);
            assert operator == BinaryOperator.LOGICAL_OR
                    || operator == BinaryOperator.LOGICAL_AND : operator;
            assert names.length >= 2;
            this.operator = operator;
            this.permissions = Lists.newArrayListWithExpectedSize(names.length);
            for (String name : names) {
                permissions.add(new Single(annotation, name));
            }
        }

        @Override
        public boolean isSingle() {
            return false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append(permissions.get(0));

            for (int i = 1; i < permissions.size(); i++) {
                appendOperator(sb, operator);
                sb.append(permissions.get(i));
            }

            return sb.toString();
        }

        @Override
        public boolean isSatisfied(@NonNull PermissionHolder available) {
            if (operator == BinaryOperator.LOGICAL_AND) {
                for (PermissionRequirement requirement : permissions) {
                    if (!requirement.isSatisfied(available)) {
                        return false;
                    }
                }
                return true;
            } else {
                assert operator == BinaryOperator.LOGICAL_OR : operator;
                for (PermissionRequirement requirement : permissions) {
                    if (requirement.isSatisfied(available)) {
                        return true;
                    }
                }
                return false;
            }
        }

        @Override
        public String describeMissingPermissions(@NonNull PermissionHolder available) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (PermissionRequirement requirement : permissions) {
                if (!requirement.isSatisfied(available)) {
                    if (first) {
                        first = false;
                    } else {
                        appendOperator(sb, operator);
                    }
                    sb.append(requirement.describeMissingPermissions(available));
                }
            }
            return sb.toString();
        }

        @Override
        protected void addMissingPermissions(@NonNull PermissionHolder available,
          @NonNull Set<String> missing) {
            for (PermissionRequirement requirement : permissions) {
                if (!requirement.isSatisfied(available)) {
                    requirement.addMissingPermissions(available, missing);
                }
            }
        }

        @Override
        protected void addRevocablePermissions(@NonNull Set<String> result,
                @NonNull PermissionHolder revocable) {
            for (PermissionRequirement requirement : permissions) {
                requirement.addRevocablePermissions(result, revocable);
            }
        }

        @Override
        public boolean isRevocable(@NonNull PermissionHolder revocable) {
            // TODO: Pass in the available set of permissions here, and if
            // the operator is BinaryOperator.LOGICAL_OR, only return revocable=true
            // if an unsatisfied permission is also revocable. In other words,
            // if multiple permissions are allowed, and some of them are satisfied and
            // not revocable the overall permission requirement is not revocable.
            for (PermissionRequirement requirement : permissions) {
                if (requirement.isRevocable(revocable)) {
                    return true;
                }
            }
            return false;
        }

        @Nullable
        @Override
        public BinaryOperator getOperator() {
            return operator;
        }

        @NonNull
        @Override
        public Iterable<PermissionRequirement> getChildren() {
            return permissions;
        }
    }

    /**
     * Require multiple permissions. This is a group of permissions with some
     * associated boolean logic, such as "B or (C and (D or E))".
     */
    private static class Complex extends PermissionRequirement {
        public final BinaryOperator operator;
        public final PermissionRequirement left;
        public final PermissionRequirement right;

        public Complex(
                @NonNull ResolvedAnnotation annotation,
                BinaryOperator operator,
                PermissionRequirement left,
                PermissionRequirement right) {
            super(annotation);
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean isSingle() {
            return false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            boolean needsParentheses = left instanceof Complex &&
                    ((Complex) left).operator != BinaryOperator.LOGICAL_AND;
            if (needsParentheses) {
                sb.append('(');
            }
            sb.append(left.toString());
            if (needsParentheses) {
                sb.append(')');
            }

            appendOperator(sb, operator);

            needsParentheses = right instanceof Complex &&
                    ((Complex) right).operator != BinaryOperator.LOGICAL_AND;
            if (needsParentheses) {
                sb.append('(');
            }
            sb.append(right.toString());
            if (needsParentheses) {
                sb.append(')');
            }

            return sb.toString();
        }

        @Override
        public boolean isSatisfied(@NonNull PermissionHolder available) {
            boolean satisfiedLeft = left.isSatisfied(available);
            boolean satisfiedRight = right.isSatisfied(available);
            if (operator == BinaryOperator.LOGICAL_AND) {
                return satisfiedLeft && satisfiedRight;
            } else if (operator == BinaryOperator.LOGICAL_OR) {
                return satisfiedLeft || satisfiedRight;
            } else {
                assert operator == BinaryOperator.BITWISE_XOR : operator;
                return satisfiedLeft ^ satisfiedRight;
            }
        }

        @Override
        public String describeMissingPermissions(@NonNull PermissionHolder available) {
            boolean satisfiedLeft = left.isSatisfied(available);
            boolean satisfiedRight = right.isSatisfied(available);
            if (operator == BinaryOperator.LOGICAL_AND || operator == BinaryOperator.LOGICAL_OR) {
                if (satisfiedLeft) {
                    if (satisfiedRight) {
                        return "";
                    }
                    return right.describeMissingPermissions(available);
                } else if (satisfiedRight) {
                    return left.describeMissingPermissions(available);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(left.describeMissingPermissions(available));
                    appendOperator(sb, operator);
                    sb.append(right.describeMissingPermissions(available));
                    return sb.toString();
                }
            } else {
                assert operator == BinaryOperator.BITWISE_XOR : operator;
                return toString();
            }
        }

        @Override
        protected void addMissingPermissions(@NonNull PermissionHolder available,
          @NonNull Set<String> missing) {
            boolean satisfiedLeft = left.isSatisfied(available);
            boolean satisfiedRight = right.isSatisfied(available);
            if (operator == BinaryOperator.LOGICAL_AND || operator == BinaryOperator.LOGICAL_OR) {
                if (satisfiedLeft) {
                    if (satisfiedRight) {
                        return;
                    }
                    right.addMissingPermissions(available, missing);
                } else if (satisfiedRight) {
                    left.addMissingPermissions(available, missing);
                } else {
                    left.addMissingPermissions(available, missing);
                    right.addMissingPermissions(available, missing);
                }
            } else {
                assert operator == BinaryOperator.BITWISE_XOR : operator;
                left.addMissingPermissions(available, missing);
                right.addMissingPermissions(available, missing);
            }
        }

        @Override
        protected void addRevocablePermissions(@NonNull Set<String> result,
                @NonNull PermissionHolder revocable) {
            left.addRevocablePermissions(result, revocable);
            right.addRevocablePermissions(result, revocable);
        }

        @Override
        public boolean isRevocable(@NonNull PermissionHolder revocable) {
            // TODO: If operator == BinaryOperator.LOGICAL_OR only return
            // revocable the there isn't a non-revocable term which is also satisfied.
            return left.isRevocable(revocable) || right.isRevocable(revocable);
        }

        @NonNull
        public static PermissionRequirement parse(@NonNull ResolvedAnnotation annotation,
                @Nullable Context context, @NonNull final String value) {
            // Parse an expression of the form (A op1 B op2 C) op3 (D op4 E) etc.
            // We'll just use the Java parser to handle this to ensure that operator
            // precedence etc is correct.
            if (context == null) {
                return NONE;
            }
            JavaParser javaParser = context.getClient().getJavaParser(null);
            if (javaParser == null) {
                return NONE;
            }
            try {
                JavaContext javaContext = new JavaContext(context.getDriver(),
                        context.getProject(), context.getMainProject(), context.file,
                        javaParser) {
                    @Nullable
                    @Override
                    public String getContents() {
                        return ""
                                + "class Test { void test() {\n"
                                + "boolean result=" + value
                                + ";\n}\n}";
                    }
                };
                Node node = javaParser.parseJava(javaContext);
                if (node != null) {
                    final AtomicReference<Expression> reference = new AtomicReference<Expression>();
                    node.accept(new ForwardingAstVisitor() {
                        @Override
                        public boolean visitVariableDefinitionEntry(VariableDefinitionEntry node) {
                            reference.set(node.astInitializer());
                            return true;
                        }
                    });
                    Expression expression = reference.get();
                    if (expression != null) {
                        return parse(annotation, expression);
                    }
                }

                return NONE;
            } finally {
                javaParser.dispose();
            }
        }

        private static PermissionRequirement parse(
                @NonNull ResolvedAnnotation annotation,
                @NonNull Expression expression) {
            if (expression instanceof Select) {
                return new Single(annotation, expression.toString());
            } else if (expression instanceof BinaryExpression) {
                BinaryExpression binaryExpression = (BinaryExpression) expression;
                BinaryOperator operator = binaryExpression.astOperator();
                if (operator == BinaryOperator.LOGICAL_AND
                        || operator == BinaryOperator.LOGICAL_OR
                        || operator == BinaryOperator.BITWISE_XOR) {
                    PermissionRequirement left = parse(annotation, binaryExpression.astLeft());
                    PermissionRequirement right = parse(annotation, binaryExpression.astRight());
                    return new Complex(annotation, operator, left, right);
                }
            }
            return NONE;
        }

        @Nullable
        @Override
        public BinaryOperator getOperator() {
            return operator;
        }

        @NonNull
        @Override
        public Iterable<PermissionRequirement> getChildren() {
            return Arrays.asList(left, right);
        }
    }

    /**
     * Returns true if the given permission name is a revocable permission for
     * targetSdkVersion >= 23
     *
     * @param name permission name
     * @return true if this is a revocable permission
     */
    public static boolean isRevocableSystemPermission(@NonNull String name) {
        return Arrays.binarySearch(REVOCABLE_PERMISSION_NAMES, name) >= 0;
    }

    @VisibleForTesting
    static final String[] REVOCABLE_PERMISSION_NAMES = new String[] {
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.BODY_SENSORS",
            "android.permission.CALL_PHONE",
            "android.permission.CAMERA",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.READ_CALENDAR",
            "android.permission.READ_CALL_LOG",
            "android.permission.READ_CELL_BROADCASTS",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_PROFILE",
            "android.permission.READ_SMS",
            "android.permission.READ_SOCIAL_STREAM",
            "android.permission.RECEIVE_MMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.RECEIVE_WAP_PUSH",
            "android.permission.RECORD_AUDIO",
            "android.permission.SEND_SMS",
            "android.permission.USE_FINGERPRINT",
            "android.permission.USE_SIP",
            "android.permission.WRITE_CALENDAR",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.WRITE_CONTACTS",
            "android.permission.WRITE_PROFILE",
            "android.permission.WRITE_SOCIAL_STREAM",
            "com.android.voicemail.permission.ADD_VOICEMAIL",
    };
}
