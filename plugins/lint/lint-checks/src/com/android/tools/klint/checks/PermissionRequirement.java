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


import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.sdklib.AndroidVersion;
import com.android.tools.klint.detector.api.ConstantEvaluator;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.util.UastExpressionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.android.SdkConstants.ATTR_VALUE;
import static com.android.tools.klint.checks.SupportAnnotationDetector.*;

/**
 * A permission requirement is a boolean expression of permission names that a
 * caller must satisfy for a given Android API.
 */
public abstract class PermissionRequirement {
    public static final String ATTR_PROTECTION_LEVEL = "protectionLevel"; //$NON-NLS-1$
    public static final String VALUE_DANGEROUS = "dangerous"; //$NON-NLS-1$

    protected final UAnnotation annotation;
    private int firstApi;
    private int lastApi;

    @SuppressWarnings("ConstantConditions")
    public static final PermissionRequirement NONE = new PermissionRequirement(null) {
        @Override
        public boolean isSatisfied(@NonNull PermissionHolder available) {
            return true;
        }

        @Override
        public boolean appliesTo(@NonNull PermissionHolder available) {
            return false;
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
        public IElementType getOperator() {
            return null;
        }

        @NonNull
        @Override
        public Iterable<PermissionRequirement> getChildren() {
            return Collections.emptyList();
        }
    };

    private PermissionRequirement(@NonNull UAnnotation annotation) {
        this.annotation = annotation;
    }

    @NonNull
    public static PermissionRequirement create(@NonNull UAnnotation annotation) {
        String value = getAnnotationStringValue(annotation, ATTR_VALUE);
        if (value != null && !value.isEmpty()) {
            return new Single(annotation, value);
        }

        String[] anyOf = getAnnotationStringValues(annotation, ATTR_ANY_OF);
        if (anyOf != null) {
            if (anyOf.length > 1) {
                return new Many(annotation, JavaTokenType.OROR, anyOf);
            } else if (anyOf.length == 1) {
                return new Single(annotation, anyOf[0]);
            }
        }

        String[] allOf = getAnnotationStringValues(annotation, ATTR_ALL_OF);
        if (allOf != null) {
            if (allOf.length > 1) {
                return new Many(annotation, JavaTokenType.ANDAND, allOf);
            } else if (allOf.length == 1) {
                return new Single(annotation, allOf[0]);
            }
        }

        return NONE;
    }

    @Nullable
    public static Boolean getAnnotationBooleanValue(@Nullable UAnnotation annotation,
            @NonNull String name) {
        if (annotation != null) {
            UExpression attributeValue = annotation.findDeclaredAttributeValue(name);
            if (attributeValue == null && ATTR_VALUE.equals(name)) {
                attributeValue = annotation.findDeclaredAttributeValue(null);
            }
            // Use constant evaluator since we want to resolve field references as well
            if (attributeValue != null) {
                Object o = ConstantEvaluator.evaluate(null, attributeValue);
                if (o instanceof Boolean) {
                    return (Boolean) o;
                }
            }
        }

        return null;
    }

    @Nullable
    public static Long getAnnotationLongValue(@Nullable UAnnotation annotation,
            @NonNull String name) {
        if (annotation != null) {
            UExpression attributeValue = annotation.findDeclaredAttributeValue(name);
            if (attributeValue == null && ATTR_VALUE.equals(name)) {
                attributeValue = annotation.findDeclaredAttributeValue(null);
            }
            // Use constant evaluator since we want to resolve field references as well
            if (attributeValue != null) {
                Object o = ConstantEvaluator.evaluate(null, attributeValue);
                if (o instanceof Number) {
                    return ((Number)o).longValue();
                }
            }
        }

        return null;
    }

    @Nullable
    public static Double getAnnotationDoubleValue(@Nullable UAnnotation annotation,
            @NonNull String name) {
        if (annotation != null) {
            UExpression attributeValue = annotation.findDeclaredAttributeValue(name);
            if (attributeValue == null && ATTR_VALUE.equals(name)) {
                attributeValue = annotation.findDeclaredAttributeValue(null);
            }
            // Use constant evaluator since we want to resolve field references as well
            if (attributeValue != null) {
                Object o = ConstantEvaluator.evaluate(null, attributeValue);
                if (o instanceof Number) {
                    return ((Number)o).doubleValue();
                }
            }
        }

        return null;
    }

    @Nullable
    public static String getAnnotationStringValue(@Nullable UAnnotation annotation,
            @NonNull String name) {
        if (annotation != null) {
            UExpression attributeValue = annotation.findDeclaredAttributeValue(name);
            if (attributeValue == null && ATTR_VALUE.equals(name)) {
                attributeValue = annotation.findDeclaredAttributeValue(null);
            }
            // Use constant evaluator since we want to resolve field references as well
            if (attributeValue != null) {
                Object o = ConstantEvaluator.evaluate(null, attributeValue);
                if (o instanceof String) {
                    return (String) o;
                }
            }
        }

        return null;
    }

    @Nullable
    public static String[] getAnnotationStringValues(@Nullable UAnnotation annotation,
            @NonNull String name) {
        if (annotation != null) {
            UExpression attributeValue = annotation.findDeclaredAttributeValue(name);
            if (attributeValue == null && ATTR_VALUE.equals(name)) {
                attributeValue = annotation.findDeclaredAttributeValue(null);
            }
            if (attributeValue == null) {
                return null;
            }
            if (UastExpressionUtils.isArrayInitializer(attributeValue)) {
                List<UExpression> initializers =
                        ((UCallExpression) attributeValue).getValueArguments();
                List<String> result = Lists.newArrayListWithCapacity(initializers.size());
                ConstantEvaluator constantEvaluator = new ConstantEvaluator(null);
                for (UExpression element : initializers) {
                    Object o = constantEvaluator.evaluate(element);
                    if (o instanceof String) {
                        result.add((String)o);
                    }
                }
                if (result.isEmpty()) {
                    return null;
                } else {
                    return result.toArray(new String[0]);
                }
            } else {
                // Use constant evaluator since we want to resolve field references as well
                Object o = ConstantEvaluator.evaluate(null, attributeValue);
                if (o instanceof String) {
                    return new String[]{(String) o};
                } else if (o instanceof String[]) {
                    return (String[])o;
                } else if (o instanceof Object[]) {
                    Object[] array = (Object[]) o;
                    List<String> strings = Lists.newArrayListWithCapacity(array.length);
                    for (Object element : array) {
                        if (element instanceof String) {
                            strings.add((String) element);
                        }
                    }
                    return strings.toArray(new String[0]);
                }
            }
        }

        return null;
    }

    /**
     * Returns false if this permission does not apply given the specified minimum and
     * target sdk versions
     *
     * @param minSdkVersion the minimum SDK version
     * @param targetSdkVersion the target SDK version
     * @return true if this permission requirement applies for the given versions
     */
    /**
     * Returns false if this permission does not apply given the specified minimum and target
     * sdk versions
     *
     * @param available   the permission holder which also knows the min and target versions
     * @return true if this permission requirement applies for the given versions
     */
    protected boolean appliesTo(@NonNull PermissionHolder available) {
        if (firstApi == 0) { // initialized?
            firstApi = -1; // initialized, not specified

            // Not initialized
            String range = getAnnotationStringValue(annotation, "apis");
            if (range != null) {
                // Currently only support the syntax "a..b" where a and b are inclusive end points
                // and where "a" and "b" are optional
                int index = range.indexOf("..");
                if (index != -1) {
                    try {
                        if (index > 0) {
                            firstApi = Integer.parseInt(range.substring(0, index));
                        } else {
                            firstApi = 1;
                        }
                        if (index + 2 < range.length()) {
                            lastApi = Integer.parseInt(range.substring(index + 2));
                        } else {
                            lastApi = Integer.MAX_VALUE;
                        }
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }

        if (firstApi != -1) {
            AndroidVersion minSdkVersion = available.getMinSdkVersion();
            if (minSdkVersion.getFeatureLevel() > lastApi) {
                return false;
            }

            AndroidVersion targetSdkVersion = available.getTargetSdkVersion();
            if (targetSdkVersion.getFeatureLevel() < firstApi) {
                return false;
            }
        }
        return true;
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
        Boolean o = getAnnotationBooleanValue(annotation, ATTR_CONDITIONAL);
        if (o != null) {
            return o;
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
    public abstract IElementType getOperator();

    /**
     * Returns nested requirements, combined via {@link #getOperator()}
     */
    @NonNull
    public abstract Iterable<PermissionRequirement> getChildren();

    /** Require a single permission */
    private static class Single extends PermissionRequirement {
        public final String name;

        public Single(@NonNull UAnnotation annotation, @NonNull String name) {
            super(annotation);
            this.name = name;
        }

        @Override
        public boolean isRevocable(@NonNull PermissionHolder revocable) {
            return revocable.isRevocable(name) || isRevocableSystemPermission(name);
        }

        @Nullable
        @Override
        public IElementType getOperator() {
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
            return available.hasPermission(name) || !appliesTo(available);
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

    protected static void appendOperator(StringBuilder sb, IElementType operator) {
        sb.append(' ');
        if (operator == JavaTokenType.ANDAND) {
            sb.append("and");
        } else if (operator == JavaTokenType.OROR) {
            sb.append("or");
        } else {
            assert operator == JavaTokenType.XOR : operator;
            sb.append("xor");
        }
        sb.append(' ');
    }

    /**
     * Require a series of permissions, all with the same operator.
     */
    private static class Many extends PermissionRequirement {
        public final IElementType operator;
        public final List<PermissionRequirement> permissions;

        public Many(
                @NonNull UAnnotation annotation,
                IElementType operator,
                String[] names) {
            super(annotation);
            assert operator == JavaTokenType.OROR
                    || operator == JavaTokenType.ANDAND : operator;
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
            if (operator == JavaTokenType.ANDAND) {
                for (PermissionRequirement requirement : permissions) {
                    if (!requirement.isSatisfied(available) && requirement.appliesTo(available)) {
                        return false;
                    }
                }
                return true;
            } else {
                assert operator == JavaTokenType.OROR : operator;
                for (PermissionRequirement requirement : permissions) {
                    if (requirement.isSatisfied(available) || !requirement.appliesTo(available)) {
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
            // the operator is JavaTokenType.OROR, only return revocable=true
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
        public IElementType getOperator() {
            return operator;
        }

        @NonNull
        @Override
        public Iterable<PermissionRequirement> getChildren() {
            return permissions;
        }
    }

    /**
     * Returns true if the given permission name is a revocable permission for
     * targetSdkVersion &ge; 23
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
            "android.permission.READ_EXTERNAL_STORAGE",
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
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.WRITE_SETTINGS",
            "android.permission.WRITE_PROFILE",
            "android.permission.WRITE_SOCIAL_STREAM",
            "com.android.voicemail.permission.ADD_VOICEMAIL",
    };
}
