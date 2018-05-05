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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.ExternalReferenceExpression;
import com.android.tools.klint.client.api.IssueRegistry;
import com.android.tools.klint.client.api.UastLintUtils;
import com.android.tools.klint.detector.api.*;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.uast.*;
import org.jetbrains.uast.java.JavaUAnnotation;
import org.jetbrains.uast.java.JavaUTypeCastExpression;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.*;

import static com.android.SdkConstants.*;
import static com.android.tools.klint.checks.PermissionRequirement.getAnnotationBooleanValue;
import static com.android.tools.klint.checks.SupportAnnotationDetector.*;
import static com.android.tools.klint.client.api.JavaParser.*;
import static com.android.tools.klint.detector.api.LintUtils.getAutoBoxedType;
import static com.android.tools.klint.detector.api.ResourceEvaluator.*;

/**
 * Checks annotations to make sure they are valid
 */
public class AnnotationDetector extends Detector implements Detector.UastScanner {

    public static final Implementation IMPLEMENTATION = new Implementation(
            AnnotationDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Placing SuppressLint on a local variable doesn't work for class-file based checks */
    public static final Issue INSIDE_METHOD = Issue.create(
            "LocalSuppress", //$NON-NLS-1$
            "@SuppressLint on invalid element",

            "The `@SuppressAnnotation` is used to suppress Lint warnings in Java files. However, " +
            "while many lint checks analyzes the Java source code, where they can find " +
            "annotations on (for example) local variables, some checks are analyzing the " +
            "`.class` files. And in class files, annotations only appear on classes, fields " +
            "and methods. Annotations placed on local variables disappear. If you attempt " +
            "to suppress a lint error for a class-file based lint check, the suppress " +
            "annotation not work. You must move the annotation out to the surrounding method.",

            Category.CORRECTNESS,
            3,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Incorrectly using a support annotation */
    @SuppressWarnings("WeakerAccess")
    public static final Issue ANNOTATION_USAGE = Issue.create(
            "SupportAnnotationUsage", //$NON-NLS-1$
            "Incorrect support annotation usage",

            "This lint check makes sure that the support annotations (such as " +
            "`@IntDef` and `@ColorInt`) are used correctly. For example, it's an " +
            "error to specify an `@IntRange` where the `from` value is higher than " +
            "the `to` value.",

            Category.CORRECTNESS,
            2,
            Severity.ERROR,
            IMPLEMENTATION);

    /** IntDef annotations should be unique */
    public static final Issue UNIQUE = Issue.create(
            "UniqueConstants", //$NON-NLS-1$
            "Overlapping Enumeration Constants",

            "The `@IntDef` annotation allows you to " +
            "create a light-weight \"enum\" or type definition. However, it's possible to " +
            "accidentally specify the same value for two or more of the values, which can " +
            "lead to hard-to-detect bugs. This check looks for this scenario and flags any " +
            "repeated constants.\n" +
            "\n" +
            "In some cases, the repeated constant is intentional (for example, renaming a " +
            "constant to a more intuitive name, and leaving the old name in place for " +
            "compatibility purposes.)  In that case, simply suppress this check by adding a " +
            "`@SuppressLint(\"UniqueConstants\")` annotation.",

            Category.CORRECTNESS,
            3,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Flags should typically be specified as bit shifts */
    public static final Issue FLAG_STYLE = Issue.create(
            "ShiftFlags", //$NON-NLS-1$
            "Dangerous Flag Constant Declaration",

            "When defining multiple constants for use in flags, the recommended style is " +
            "to use the form `1 << 2`, `1 << 3`, `1 << 4` and so on to ensure that the " +
            "constants are unique and non-overlapping.",

            Category.CORRECTNESS,
            3,
            Severity.WARNING,
            IMPLEMENTATION);

    /** All IntDef constants should be included in switch */
    public static final Issue SWITCH_TYPE_DEF = Issue.create(
            "SwitchIntDef", //$NON-NLS-1$
            "Missing @IntDef in Switch",

            "This check warns if a `switch` statement does not explicitly include all " +
            "the values declared by the typedef `@IntDef` declaration.",

            Category.CORRECTNESS,
            3,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Constructs a new {@link AnnotationDetector} check */
    public AnnotationDetector() {
    }

    // ---- Implements JavaScanner ----

    /**
     * Set of fields we've already warned about {@link #FLAG_STYLE} for; these can
     * be referenced multiple times, so we should only flag them once
     */
    private Set<PsiElement> mWarnedFlags;

    @Nullable
    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        List<Class<? extends UElement>> types = new ArrayList<Class<? extends UElement>>(2);
        types.add(UAnnotation.class);
        types.add(USwitchExpression.class);
        return types;
    }


    @Nullable
    @Override
    public UastVisitor createUastVisitor(@NonNull JavaContext context) {
        return new AnnotationChecker(context);
    }

    private class AnnotationChecker extends AbstractUastVisitor {

        private final JavaContext mContext;

        private AnnotationChecker(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitAnnotation(@NonNull UAnnotation annotation) {
            String type = annotation.getQualifiedName();
            if (type == null || type.startsWith("java.lang.")) {
                return false;
            }

            if (FQCN_SUPPRESS_LINT.equals(type)) {
                UElement parent = annotation.getUastParent();
                if (parent == null) {
                    return false;
                }
                // Only flag local variables and parameters (not classes, fields and methods)
                if (!(parent instanceof UDeclarationsExpression
                      || parent instanceof ULocalVariable
                      || parent instanceof UParameter)) {
                    return false;
                }
                List<UNamedExpression> attributes = annotation.getAttributeValues();
                if (attributes.size() == 1) {
                    UNamedExpression attribute = attributes.get(0);
                    UExpression value = attribute.getExpression();
                    if (value instanceof ULiteralExpression) {
                        Object v = ((ULiteralExpression) value).getValue();
                        if (v instanceof String) {
                            String id = (String) v;
                            checkSuppressLint(annotation, id);
                        }
                    } else if (value instanceof PsiArrayInitializerMemberValue) {
                        PsiArrayInitializerMemberValue initializer =
                                (PsiArrayInitializerMemberValue) value;
                        for (PsiAnnotationMemberValue expression : initializer.getInitializers()) {
                            if (expression instanceof PsiLiteral) {
                                Object v = ((PsiLiteral) expression).getValue();
                                if (v instanceof String) {
                                    String id = (String) v;
                                    if (!checkSuppressLint(annotation, id)) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (type.startsWith(SUPPORT_ANNOTATIONS_PREFIX)) {
                if (CHECK_RESULT_ANNOTATION.equals(type)) {
                    // Check that the return type of this method is not void!
                    if (annotation.getUastParent() instanceof UMethod) {
                        UMethod method = (UMethod) annotation.getUastParent();
                        if (!method.isConstructor()
                            && PsiType.VOID.equals(method.getReturnType())) {
                            mContext.report(ANNOTATION_USAGE, annotation.getPsi(),
                                            mContext.getLocation(annotation.getPsi()),
                                            "@CheckResult should not be specified on `void` methods");
                        }
                    }
                } else if (INT_RANGE_ANNOTATION.equals(type)
                           || FLOAT_RANGE_ANNOTATION.equals(type)) {
                    // Check that the annotated element's type is int or long.
                    // Also make sure that from <= to.
                    boolean invalid;
                    if (INT_RANGE_ANNOTATION.equals(type)) {
                        checkTargetType(annotation, TYPE_INT, TYPE_LONG, true);

                        long from = getLongAttribute(annotation, ATTR_FROM, Long.MIN_VALUE);
                        long to = getLongAttribute(annotation, ATTR_TO, Long.MAX_VALUE);
                        invalid = from > to;
                    } else {
                        checkTargetType(annotation, TYPE_FLOAT, TYPE_DOUBLE, true);

                        double from = getDoubleAttribute(annotation, ATTR_FROM,
                                                         Double.NEGATIVE_INFINITY);
                        double to = getDoubleAttribute(annotation, ATTR_TO,
                                                       Double.POSITIVE_INFINITY);
                        invalid = from > to;
                    }
                    if (invalid) {
                        mContext.reportUast(ANNOTATION_USAGE, annotation, mContext.getUastLocation(annotation),
                                        "Invalid range: the `from` attribute must be less than "
                                        + "the `to` attribute");
                    }
                } else if (SIZE_ANNOTATION.equals(type)) {
                    // Check that the annotated element's type is an array, or a collection
                    // (or at least not an int or long; if so, suggest IntRange)
                    // Make sure the size and the modulo is not negative.
                    int unset = -42;
                    long exact = getLongAttribute(annotation, ATTR_VALUE, unset);
                    long min = getLongAttribute(annotation, ATTR_MIN, Long.MIN_VALUE);
                    long max = getLongAttribute(annotation, ATTR_MAX, Long.MAX_VALUE);
                    long multiple = getLongAttribute(annotation, ATTR_MULTIPLE, 1);
                    if (min > max) {
                        mContext.report(ANNOTATION_USAGE, annotation.getPsi(), mContext.getLocation(annotation.getPsi()),
                                        "Invalid size range: the `min` attribute must be less than "
                                        + "the `max` attribute");
                    } else if (multiple < 1) {
                        mContext.report(ANNOTATION_USAGE, annotation.getPsi(), mContext.getLocation(annotation.getPsi()),
                                        "The size multiple must be at least 1");

                    } else if (exact < 0 && exact != unset || min < 0 && min != Long.MIN_VALUE) {
                        mContext.report(ANNOTATION_USAGE, annotation.getPsi(), mContext.getLocation(annotation.getPsi()),
                                        "The size can't be negative");
                    }
                } else if (COLOR_INT_ANNOTATION.equals(type) || (PX_ANNOTATION.equals(type))) {
                    // Check that ColorInt applies to the right type
                    checkTargetType(annotation, TYPE_INT, TYPE_LONG, true);
                } else if (INT_DEF_ANNOTATION.equals(type)) {
                    // Make sure IntDef constants are unique
                    ensureUniqueValues(annotation);
                } else if (PERMISSION_ANNOTATION.equals(type) ||
                           PERMISSION_ANNOTATION_READ.equals(type) ||
                           PERMISSION_ANNOTATION_WRITE.equals(type)) {
                    // Check that if there are no arguments, this is specified on a parameter,
                    // and conversely, on methods and fields there is a valid argument.
                    if (annotation.getUastParent() instanceof UMethod) {
                        String value = PermissionRequirement.getAnnotationStringValue(annotation, ATTR_VALUE);
                        String[] anyOf = PermissionRequirement.getAnnotationStringValues(annotation, ATTR_ANY_OF);
                        String[] allOf = PermissionRequirement.getAnnotationStringValues(annotation, ATTR_ALL_OF);

                        int set = 0;
                        //noinspection VariableNotUsedInsideIf
                        if (value != null) {
                            set++;
                        }
                        //noinspection VariableNotUsedInsideIf
                        if (allOf != null) {
                            set++;
                        }
                        //noinspection VariableNotUsedInsideIf
                        if (anyOf != null) {
                            set++;
                        }

                        if (set == 0) {
                            mContext.report(ANNOTATION_USAGE, annotation.getPsi(),
                                            mContext.getLocation(annotation.getPsi()),
                                            "For methods, permission annotation should specify one "
                                            + "of `value`, `anyOf` or `allOf`");
                        } else if (set > 1) {
                            mContext.report(ANNOTATION_USAGE, annotation.getPsi(),
                                            mContext.getLocation(annotation.getPsi()),
                                            "Only specify one of `value`, `anyOf` or `allOf`");
                        }
                    }

                } else if (type.endsWith(RES_SUFFIX)) {
                    // Check that resource type annotations are on ints
                    checkTargetType(annotation, TYPE_INT, TYPE_LONG, true);
                }
            } else {
                // Look for typedefs (and make sure they're specified on the right type)
                PsiElement resolved = annotation.resolve();
                if (resolved != null) {
                    PsiClass cls = (PsiClass) resolved;
                    if (cls.isAnnotationType() && cls.getModifierList() != null) {
                        for (PsiAnnotation a : cls.getModifierList().getAnnotations()) {
                            String name = a.getQualifiedName();
                            if (INT_DEF_ANNOTATION.equals(name)) {
                                checkTargetType(annotation, TYPE_INT, TYPE_LONG, true);
                            } else if (STRING_DEF_ANNOTATION.equals(type)) {
                                checkTargetType(annotation, TYPE_STRING, null, true);
                            }
                        }
                    }
                }
            }

            return false;
        }

        private void checkTargetType(@NonNull UAnnotation node, @NonNull String type1,
                @Nullable String type2, boolean allowCollection) {
            UElement parent = node.getUastParent();
            PsiType type;

            if (parent instanceof UDeclarationsExpression) {
                List<UDeclaration> elements = ((UDeclarationsExpression) parent).getDeclarations();
                if (!elements.isEmpty()) {
                    UDeclaration element = elements.get(0);
                    if (element instanceof ULocalVariable) {
                        type = ((ULocalVariable) element).getType();
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            } else if (parent instanceof UMethod) {
                UMethod method = (UMethod) parent;
                type = method.isConstructor()
                       ? mContext.getEvaluator().getClassType(method.getContainingClass())
                       : method.getReturnType();
            } else if (parent instanceof UVariable) {
                // Field or local variable or parameter
                type = ((UVariable) parent).getType();
            } else {
                return;
            }
            if (type == null) {
                return;
            }

            if (allowCollection) {
                if (type instanceof PsiArrayType) {
                    // For example, int[]
                    type = type.getDeepComponentType();
                } else if (type instanceof PsiClassType) {
                    // For example, List<Integer>
                    PsiClassType classType = (PsiClassType)type;
                    if (classType.getParameters().length == 1) {
                        PsiClass resolved = classType.resolve();
                        if (resolved != null &&
                            InheritanceUtil.isInheritor(resolved, false, "java.util.Collection")) {
                            type = classType.getParameters()[0];
                        }
                    }
                }
            }

            String typeName = type.getCanonicalText();
            if (!typeName.equals(type1)
                && (type2 == null || !typeName.equals(type2))) {
                // Autoboxing? You can put @DrawableRes on a java.lang.Integer for example
                if (typeName.equals(getAutoBoxedType(type1))
                    || type2 != null && typeName.equals(getAutoBoxedType(type2))) {
                    return;
                }

                String expectedTypes = type2 == null ? type1 : type1 + " or " + type2;
                if (typeName.equals(TYPE_STRING)) {
                    typeName = "String";
                }
                String message = String.format(
                        "This annotation does not apply for type %1$s; expected %2$s",
                        typeName, expectedTypes);
                Location location = mContext.getUastLocation(node);
                mContext.report(ANNOTATION_USAGE, node, location, message);
            }
        }

        @Override
        public boolean visitSwitchExpression(USwitchExpression switchExpression) {
            UExpression condition = switchExpression.getExpression();
            if (condition != null && PsiType.INT.equals(condition.getExpressionType())) {
                UAnnotation annotation = findIntDefAnnotation(condition);
                if (annotation != null) {
                    UExpression value =
                            annotation.findDeclaredAttributeValue(ATTR_VALUE);
                    if (value == null) {
                        value = annotation.findDeclaredAttributeValue(null);
                    }

                    if (UastExpressionUtils.isArrayInitializer(value)) {
                        List<UExpression> allowedValues =
                                ((UCallExpression) value).getValueArguments();
                        switchExpression.accept(new SwitchChecker(switchExpression, allowedValues));
                    }
                }
            }
            return false;
        }

        @Nullable
        private Integer getConstantValue(@NonNull PsiField intDefConstantRef) {
            Object constant = intDefConstantRef.computeConstantValue();
            if (constant instanceof Number) {
                return ((Number)constant).intValue();
            }

            return null;
        }

        /**
         * Searches for the corresponding @IntDef annotation definition associated
         * with a given node
         */
        @Nullable
        private UAnnotation findIntDefAnnotation(@NonNull UExpression expression) {
            if (expression instanceof UReferenceExpression) {
                PsiElement resolved = ((UReferenceExpression) expression).resolve();

                if (resolved instanceof PsiModifierListOwner) {
                    PsiAnnotation[] annotations = mContext.getEvaluator().getAllAnnotations(
                            (PsiModifierListOwner)resolved);
                    PsiAnnotation[] relevantAnnotations =
                            filterRelevantAnnotations(mContext.getEvaluator(), annotations);
                    UAnnotation annotation = SupportAnnotationDetector.findIntDef(
                            JavaUAnnotation.wrap(relevantAnnotations));
                    if (annotation != null) {
                        return annotation;
                    }
                }

                if (resolved instanceof PsiLocalVariable) {
                    PsiLocalVariable variable = (PsiLocalVariable) resolved;
                    UExpression lastAssignment = UastLintUtils.findLastAssignment(variable,
                                                                                  expression, mContext);

                    if(lastAssignment != null) {
                        return findIntDefAnnotation(lastAssignment);
                    }

                }

            } else if (expression instanceof UCallExpression) {
                PsiMethod method = ((UCallExpression) expression).resolve();
                if (method != null) {
                    PsiAnnotation[] annotations =
                            mContext.getEvaluator().getAllAnnotations(method);
                    PsiAnnotation[] relevantAnnotations =
                            filterRelevantAnnotations(mContext.getEvaluator(), annotations);
                    List<UAnnotation> uAnnotations = JavaUAnnotation.wrap(relevantAnnotations);
                    UAnnotation annotation = SupportAnnotationDetector.findIntDef(uAnnotations);
                    if (annotation != null) {
                        return annotation;
                    }
                }
            } else if (expression instanceof UIfExpression) {
                UIfExpression ifExpression = (UIfExpression) expression;
                if (ifExpression.getThenExpression() != null) {
                    UAnnotation result = findIntDefAnnotation(ifExpression.getThenExpression());
                    if (result != null) {
                        return result;
                    }
                }
                if (ifExpression.getElseExpression() != null) {
                    UAnnotation result = findIntDefAnnotation(ifExpression.getElseExpression());
                    if (result != null) {
                        return result;
                    }
                }
            } else if (expression instanceof JavaUTypeCastExpression) {
                return findIntDefAnnotation(((JavaUTypeCastExpression)expression).getOperand());

            } else if (expression instanceof UParenthesizedExpression) {
                return findIntDefAnnotation(((UParenthesizedExpression) expression).getExpression());
            }

            return null;
        }

        private void ensureUniqueValues(@NonNull UAnnotation node) {
            UExpression value = node.findAttributeValue(ATTR_VALUE);
            if (value == null) {
                value = node.findAttributeValue(null);
            }

            if (!(UastExpressionUtils.isArrayInitializer(value))) {
                return;
            }

            PsiArrayInitializerMemberValue array = (PsiArrayInitializerMemberValue) value;
            PsiAnnotationMemberValue[] initializers = array.getInitializers();
            Map<Number,Integer> valueToIndex =
                    Maps.newHashMapWithExpectedSize(initializers.length);

            boolean flag = getAnnotationBooleanValue(node, TYPE_DEF_FLAG_ATTRIBUTE) == Boolean.TRUE;
            if (flag) {
                ensureUsingFlagStyle(initializers);
            }

            ConstantEvaluator constantEvaluator = new ConstantEvaluator(mContext);
            for (int index = 0; index < initializers.length; index++) {
                PsiAnnotationMemberValue expression = initializers[index];
                Object o = constantEvaluator.evaluate(expression);
                if (o instanceof Number) {
                    Number number = (Number) o;
                    if (valueToIndex.containsKey(number)) {
                        @SuppressWarnings("UnnecessaryLocalVariable")
                        Number repeatedValue = number;

                        Location location;
                        String message;
                        int prevIndex = valueToIndex.get(number);
                        PsiElement prevConstant = initializers[prevIndex];
                        message = String.format(
                                "Constants `%1$s` and `%2$s` specify the same exact "
                                + "value (%3$s); this is usually a cut & paste or "
                                + "merge error",
                                expression.getText(), prevConstant.getText(),
                                repeatedValue.toString());
                        location = mContext.getLocation(expression);
                        Location secondary = mContext.getLocation(prevConstant);
                        secondary.setMessage("Previous same value");
                        location.setSecondary(secondary);
                        UElement scope = getAnnotationScope(node);
                        mContext.reportUast(UNIQUE, scope, location, message);
                        break;
                    }
                    valueToIndex.put(number, index);
                }
            }
        }

        private void ensureUsingFlagStyle(@NonNull PsiAnnotationMemberValue[] constants) {
            if (constants.length < 3) {
                return;
            }

            for (PsiAnnotationMemberValue constant : constants) {
                if (constant instanceof PsiReferenceExpression) {
                    PsiElement resolved = ((PsiReferenceExpression) constant).resolve();
                    if (resolved instanceof PsiField) {
                        PsiExpression initializer = ((PsiField) resolved).getInitializer();
                        if (initializer instanceof PsiLiteral) {
                            PsiLiteral literal = (PsiLiteral) initializer;
                            Object o = literal.getValue();
                            if (!(o instanceof Number)) {
                                continue;
                            }
                            long value = ((Number)o).longValue();
                            // Allow -1, 0 and 1. You can write 1 as "1 << 0" but IntelliJ for
                            // example warns that that's a redundant shift.
                            if (Math.abs(value) <= 1) {
                                continue;
                            }
                            // Only warn if we're setting a specific bit
                            if (Long.bitCount(value) != 1) {
                                continue;
                            }
                            int shift = Long.numberOfTrailingZeros(value);
                            if (mWarnedFlags == null) {
                                mWarnedFlags = Sets.newHashSet();
                            }
                            if (!mWarnedFlags.add(resolved)) {
                                return;
                            }
                            String message = String.format(
                                    "Consider declaring this constant using 1 << %1$d instead",
                                    shift);
                            Location location = mContext.getLocation(initializer);
                            mContext.report(FLAG_STYLE, initializer, location, message);
                        }
                    }
                }
            }
        }

        private boolean checkSuppressLint(@NonNull UAnnotation node, @NonNull String id) {
            IssueRegistry registry = mContext.getDriver().getRegistry();
            Issue issue = registry.getIssue(id);
            // Special-case the ApiDetector issue, since it does both source file analysis
            // only on field references, and class file analysis on the rest, so we allow
            // annotations outside of methods only on fields
            if (issue != null && !issue.getImplementation().getScope().contains(Scope.JAVA_FILE)
                || issue == ApiDetector.UNSUPPORTED) {
                // This issue doesn't have AST access: annotations are not
                // available for local variables or parameters
                UElement scope = getAnnotationScope(node);
                mContext.report(INSIDE_METHOD, scope, mContext.getUastLocation(node), String.format(
                        "The `@SuppressLint` annotation cannot be used on a local " +
                        "variable with the lint check '%1$s': move out to the " +
                        "surrounding method", id));
                return false;
            }

            return true;
        }

        private class SwitchChecker extends AbstractUastVisitor {

            private final USwitchExpression mSwitchExpression;
            private final List<UExpression> mAllowedValues;
            private final List<Object> mFields;
            private final List<Integer> mSeenValues;

            private boolean mReported = false;

            private SwitchChecker(USwitchExpression switchExpression,
                    List<UExpression> allowedValues) {
                mSwitchExpression = switchExpression;
                mAllowedValues = allowedValues;

                mFields = Lists.newArrayListWithCapacity(allowedValues.size());
                for (UExpression allowedValue : allowedValues) {
                    if (allowedValue instanceof ExternalReferenceExpression) {
                        ExternalReferenceExpression externalRef =
                                (ExternalReferenceExpression) allowedValue;

                        PsiElement resolved = UastLintUtils.resolve(externalRef, switchExpression);

                        if (resolved instanceof PsiField) {
                            mFields.add(resolved);
                        }
                    } else if (allowedValue instanceof UReferenceExpression) {
                        PsiElement resolved = ((UReferenceExpression) allowedValue).resolve();
                        if (resolved != null) {
                            mFields.add(resolved);
                        }
                    } else if (allowedValue instanceof ULiteralExpression) {
                        mFields.add(allowedValue);
                    }
                }

                mSeenValues = Lists.newArrayListWithCapacity(allowedValues.size());
            }

            @Override
            public boolean visitSwitchClauseExpression(USwitchClauseExpression node) {
                if (mReported) {
                    return true;
                }

                if (mAllowedValues == null) {
                    return true;
                }

                List<UExpression> caseValues = node.getCaseValues();
                if (caseValues == null) {
                    return true;
                }

                for (UExpression caseValue : caseValues) {
                    if (caseValue instanceof ULiteralExpression) {
                        // Report warnings if you specify hardcoded constants.
                        // It's the wrong thing to do.
                        List<String> list = computeFieldNames(mSwitchExpression,
                                                              Arrays.asList(mAllowedValues));
                        // Keep error message in sync with {@link #getMissingCases}
                        String message = "Don't use a constant here; expected one of: " + Joiner
                                .on(", ").join(list);
                        mContext.report(SWITCH_TYPE_DEF, caseValue,
                                        mContext.getUastLocation(caseValue), message);
                        // Don't look for other missing typedef constants since you might
                        // have aliased with value
                        mReported = true;

                    } else if (caseValue instanceof UReferenceExpression) { // default case can have null expression
                        PsiElement resolved = ((UReferenceExpression) caseValue).resolve();
                        if (resolved == null) {
                            // If there are compilation issues (e.g. user is editing code) we
                            // can't be certain, so don't flag anything.
                            return true;
                        }
                        if (resolved instanceof PsiField) {
                            // We can't just do
                            //    fields.remove(resolved);
                            // since the fields list contains instances of potentially
                            // different types with different hash codes (due to the
                            // external annotations, which are not of the same type as
                            // for example the ECJ based ones.
                            //
                            // The equals method on external field class deliberately handles
                            // this (but it can't make its hash code match what
                            // the ECJ fields do, which is tied to the ECJ binding hash code.)
                            // So instead, manually check for equals. These lists tend to
                            // be very short anyway.
                            boolean found = false;
                            ListIterator<Object> iterator = mFields.listIterator();
                            while (iterator.hasNext()) {
                                Object field = iterator.next();
                                if (field.equals(resolved)) {
                                    iterator.remove();
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                // Look for local alias
                                UExpression initializer = mContext.getUastContext()
                                        .getInitializerBody(((PsiField) resolved));
                                if (initializer instanceof UReferenceExpression) {
                                    resolved = ((UReferenceExpression) initializer).resolve();
                                    if (resolved instanceof PsiField) {
                                        iterator = mFields.listIterator();
                                        while (iterator.hasNext()) {
                                            Object field = iterator.next();
                                            if (field.equals(resolved)) {
                                                iterator.remove();
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            if (found) {
                                Integer cv = getConstantValue((PsiField) resolved);
                                if (cv != null) {
                                    mSeenValues.add(cv);
                                }
                            } else {
                                List<String> list = computeFieldNames(
                                        mSwitchExpression, Collections.singletonList(mAllowedValues));
                                // Keep error message in sync with {@link #getMissingCases}
                                String message = "Unexpected constant; expected one of: " + Joiner
                                        .on(", ").join(list);
                                Location location = mContext.getUastNameLocation(caseValue);
                                mContext.report(SWITCH_TYPE_DEF, caseValue, location, message);
                            }
                        }
                    }
                }
                return true;
            }

            @Override
            public void afterVisitSwitchExpression(USwitchExpression node) {
                reportMissingSwitchCases();
                super.afterVisitSwitchExpression(node);
            }

            private void reportMissingSwitchCases() {
                if (mReported) {
                    return;
                }

                if (mAllowedValues == null) {
                    return;
                }

                // Any missing switch constants? Before we flag them, look to see if any
                // of them have the same values: those can be omitted
                if (!mFields.isEmpty()) {
                    ListIterator<Object> iterator = mFields.listIterator();
                    while (iterator.hasNext()) {
                        Object next = iterator.next();
                        if (next instanceof PsiField) {
                            Integer cv = getConstantValue((PsiField)next);
                            if (mSeenValues.contains(cv)) {
                                iterator.remove();
                            }
                        }
                    }
                }

                if (!mFields.isEmpty()) {
                    List<String> list = computeFieldNames(mSwitchExpression, mFields);
                    // Keep error message in sync with {@link #getMissingCases}
                    String message = "Switch statement on an `int` with known associated constant "
                                     + "missing case " + Joiner.on(", ").join(list);
                    Location location = mContext.getUastLocation(mSwitchExpression.getSwitchIdentifier());
                    mContext.report(SWITCH_TYPE_DEF, mSwitchExpression, location, message);
                }
            }
        }
    }

    private static List<String> computeFieldNames(
            @NonNull USwitchExpression node, Iterable<?> allowedValues) {

        List<String> list = Lists.newArrayList();
        for (Object o : allowedValues) {
            if (o instanceof ExternalReferenceExpression) {
                ExternalReferenceExpression externalRef = (ExternalReferenceExpression) o;
                PsiElement resolved = UastLintUtils.resolve(externalRef, node);
                if (resolved != null) {
                    o = resolved;
                }
            } else if (o instanceof PsiReferenceExpression) {
                PsiElement resolved = ((PsiReferenceExpression) o).resolve();
                if (resolved != null) {
                    o = resolved;
                }
            } else if (o instanceof PsiLiteral) {
                list.add("`" + ((PsiLiteral) o).getValue() + '`');
                continue;
            }

            if (o instanceof PsiField) {
                PsiField field = (PsiField) o;
                // Only include class name if necessary
                String name = field.getName();
                UClass clz = UastUtils.getParentOfType(node, UClass.class, true);
                if (clz != null) {
                    PsiClass containingClass = field.getContainingClass();
                    if (containingClass != null && !containingClass.equals(clz.getPsi())) {
                        name = containingClass.getName() + '.' + field.getName();
                    }
                }
                list.add('`' + name + '`');
            }
        }
        Collections.sort(list);
        return list;
    }

    /**
     * Returns the node to use as the scope for the given annotation node.
     * You can't annotate an annotation itself (with {@code @SuppressLint}), but
     * you should be able to place an annotation next to it, as a sibling, to only
     * suppress the error on this annotated element, not the whole surrounding class.
     */
    @NonNull
    private static UElement getAnnotationScope(@NonNull UAnnotation node) {
        UElement scope = UastUtils.getParentOfType(node, UAnnotation.class, true);
        if (scope == null) {
            scope = node;
        }
        return scope;
    }
}
