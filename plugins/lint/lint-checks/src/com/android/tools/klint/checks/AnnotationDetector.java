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

import static com.android.SdkConstants.ATTR_VALUE;
import static com.android.SdkConstants.FQCN_SUPPRESS_LINT;
import static com.android.SdkConstants.INT_DEF_ANNOTATION;
import static com.android.SdkConstants.STRING_DEF_ANNOTATION;
import static com.android.SdkConstants.SUPPORT_ANNOTATIONS_PREFIX;
import static com.android.SdkConstants.TYPE_DEF_FLAG_ATTRIBUTE;
import static com.android.tools.klint.checks.PermissionRequirement.getAnnotationBooleanValue;
import static com.android.tools.klint.checks.SupportAnnotationDetector.ATTR_ALL_OF;
import static com.android.tools.klint.checks.SupportAnnotationDetector.ATTR_ANY_OF;
import static com.android.tools.klint.checks.SupportAnnotationDetector.ATTR_FROM;
import static com.android.tools.klint.checks.SupportAnnotationDetector.ATTR_MAX;
import static com.android.tools.klint.checks.SupportAnnotationDetector.ATTR_MIN;
import static com.android.tools.klint.checks.SupportAnnotationDetector.ATTR_MULTIPLE;
import static com.android.tools.klint.checks.SupportAnnotationDetector.ATTR_TO;
import static com.android.tools.klint.checks.SupportAnnotationDetector.CHECK_RESULT_ANNOTATION;
import static com.android.tools.klint.checks.SupportAnnotationDetector.FLOAT_RANGE_ANNOTATION;
import static com.android.tools.klint.checks.SupportAnnotationDetector.INT_RANGE_ANNOTATION;
import static com.android.tools.klint.checks.SupportAnnotationDetector.PERMISSION_ANNOTATION;
import static com.android.tools.klint.checks.SupportAnnotationDetector.PERMISSION_ANNOTATION_READ;
import static com.android.tools.klint.checks.SupportAnnotationDetector.PERMISSION_ANNOTATION_WRITE;
import static com.android.tools.klint.checks.SupportAnnotationDetector.SIZE_ANNOTATION;
import static com.android.tools.klint.checks.SupportAnnotationDetector.filterRelevantAnnotations;
import static com.android.tools.klint.checks.SupportAnnotationDetector.getDoubleAttribute;
import static com.android.tools.klint.checks.SupportAnnotationDetector.getLongAttribute;
import static com.android.tools.klint.client.api.JavaParser.TYPE_DOUBLE;
import static com.android.tools.klint.client.api.JavaParser.TYPE_FLOAT;
import static com.android.tools.klint.client.api.JavaParser.TYPE_INT;
import static com.android.tools.klint.client.api.JavaParser.TYPE_LONG;
import static com.android.tools.klint.client.api.JavaParser.TYPE_STRING;
import static com.android.tools.klint.detector.api.LintUtils.findSubstring;
import static com.android.tools.klint.detector.api.LintUtils.getAutoBoxedType;
import static com.android.tools.klint.detector.api.ResourceEvaluator.COLOR_INT_ANNOTATION;
import static com.android.tools.klint.detector.api.ResourceEvaluator.PX_ANNOTATION;
import static com.android.tools.klint.detector.api.ResourceEvaluator.RES_SUFFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.IssueRegistry;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.ConstantEvaluator;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.TextFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParenthesizedExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiSwitchLabelStatement;
import com.intellij.psi.PsiSwitchStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeCastExpression;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Checks annotations to make sure they are valid
 */
public class AnnotationDetector extends Detector implements JavaPsiScanner {

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

    @Override
    public List<Class<? extends PsiElement>> getApplicablePsiTypes() {
        List<Class<? extends PsiElement>> types = new ArrayList<Class<? extends PsiElement>>(2);
        types.add(PsiAnnotation.class);
        types.add(PsiSwitchStatement.class);
        return types;
    }

    @Nullable
    @Override
    public JavaElementVisitor createPsiVisitor(@NonNull JavaContext context) {
        return new AnnotationChecker(context);
    }

    private class AnnotationChecker extends JavaElementVisitor {
        private final JavaContext mContext;

        public AnnotationChecker(JavaContext context) {
            mContext = context;
        }

        @Override
        public void visitAnnotation(PsiAnnotation annotation) {
            String type = annotation.getQualifiedName();
            if (type == null || type.startsWith("java.lang.")) {
                return;
            }

            if (FQCN_SUPPRESS_LINT.equals(type)) {
                PsiAnnotationOwner owner = annotation.getOwner();
                if (owner == null) {
                    return;
                }
                if (owner instanceof PsiModifierList) {
                    PsiElement parent = ((PsiModifierList) owner).getParent();
                    // Only flag local variables and parameters (not classes, fields and methods)
                    if (!(parent instanceof PsiDeclarationStatement
                          || parent instanceof PsiLocalVariable
                          || parent instanceof PsiParameter)) {
                        return;
                    }
                } else {
                    return;
                }
                PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
                if (attributes.length == 1) {
                    PsiNameValuePair attribute = attributes[0];
                    PsiAnnotationMemberValue value = attribute.getValue();
                    if (value instanceof PsiLiteral) {
                        Object v = ((PsiLiteral) value).getValue();
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
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (type.startsWith(SUPPORT_ANNOTATIONS_PREFIX)) {
                if (CHECK_RESULT_ANNOTATION.equals(type)) {
                    // Check that the return type of this method is not void!
                    if (annotation.getParent() instanceof PsiModifierList
                            && annotation.getParent().getParent() instanceof PsiMethod) {
                        PsiMethod method = (PsiMethod) annotation.getParent().getParent();
                        if (!method.isConstructor()
                                && PsiType.VOID.equals(method.getReturnType())) {
                            mContext.report(ANNOTATION_USAGE, annotation,
                                    mContext.getLocation(annotation),
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
                        mContext.report(ANNOTATION_USAGE, annotation, mContext.getLocation(annotation),
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
                        mContext.report(ANNOTATION_USAGE, annotation, mContext.getLocation(annotation),
                                "Invalid size range: the `min` attribute must be less than "
                                        + "the `max` attribute");
                    } else if (multiple < 1) {
                        mContext.report(ANNOTATION_USAGE, annotation, mContext.getLocation(annotation),
                                "The size multiple must be at least 1");

                    } else if (exact < 0 && exact != unset || min < 0 && min != Long.MIN_VALUE) {
                        mContext.report(ANNOTATION_USAGE, annotation, mContext.getLocation(annotation),
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
                    if (annotation.getParent() instanceof PsiModifierList
                        && annotation.getParent().getParent() instanceof PsiMethod) {
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
                            mContext.report(ANNOTATION_USAGE, annotation,
                                    mContext.getLocation(annotation),
                                    "For methods, permission annotation should specify one "
                                            + "of `value`, `anyOf` or `allOf`");
                        } else if (set > 1) {
                            mContext.report(ANNOTATION_USAGE, annotation,
                                    mContext.getLocation(annotation),
                                    "Only specify one of `value`, `anyOf` or `allOf`");
                        }
                    }

                } else if (type.endsWith(RES_SUFFIX)) {
                    // Check that resource type annotations are on ints
                    checkTargetType(annotation, TYPE_INT, TYPE_LONG, true);
                }
            } else {
                // Look for typedefs (and make sure they're specified on the right type)
                PsiJavaCodeReferenceElement referenceElement = annotation
                        .getNameReferenceElement();
                if (referenceElement != null) {
                    PsiElement resolved = referenceElement.resolve();
                    if (resolved instanceof PsiClass) {
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
            }
        }

        private void checkTargetType(@NonNull PsiAnnotation node, @NonNull String type1,
                @Nullable String type2, boolean allowCollection) {
            PsiAnnotationOwner owner = node.getOwner();
            if (owner instanceof PsiModifierList) {
                PsiElement parent = ((PsiModifierList) owner).getParent();
                PsiType type;
                if (parent instanceof PsiDeclarationStatement) {
                    PsiElement[] elements = ((PsiDeclarationStatement) parent).getDeclaredElements();
                    if (elements.length > 0) {
                        PsiElement element = elements[0];
                        if (element instanceof PsiLocalVariable) {
                            type = ((PsiLocalVariable)element).getType();
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                } else if (parent instanceof PsiMethod) {
                    PsiMethod method = (PsiMethod) parent;
                    type = method.isConstructor()
                            ? mContext.getEvaluator().getClassType(method.getContainingClass())
                            : method.getReturnType();
                } else if (parent instanceof PsiVariable) {
                    // Field or local variable or parameter
                    type = ((PsiVariable)parent).getType();
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
                                mContext.getEvaluator().implementsInterface(resolved,
                                  "java.util.Collection", false)) {
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
                    Location location = mContext.getLocation(node);
                    mContext.report(ANNOTATION_USAGE, node, location, message);
                }
            }
        }

        @Override
        public void visitSwitchStatement(PsiSwitchStatement statement) {
            PsiExpression condition = statement.getExpression();
            if (condition != null && PsiType.INT.equals(condition.getType())) {
                PsiAnnotation annotation = findIntDef(condition);
                if (annotation != null) {
                    checkSwitch(statement, annotation);
                }
            }
        }

        /**
         * Searches for the corresponding @IntDef annotation definition associated
         * with a given node
         */
        @Nullable
        private PsiAnnotation findIntDef(@NonNull PsiElement node) {
            if (node instanceof PsiReferenceExpression) {
                PsiElement resolved = ((PsiReference) node).resolve();
                if (resolved instanceof PsiModifierListOwner) {
                    PsiAnnotation[] annotations = mContext.getEvaluator().getAllAnnotations(
                            (PsiModifierListOwner)resolved, true);
                    PsiAnnotation annotation = SupportAnnotationDetector.findIntDef(
                            filterRelevantAnnotations(annotations));
                    if (annotation != null) {
                        return annotation;
                    }
                }

                if (resolved instanceof PsiLocalVariable) {
                    PsiLocalVariable variable = (PsiLocalVariable) resolved;
                    PsiStatement statement = PsiTreeUtil.getParentOfType(node, PsiStatement.class,
                            false);
                    if (statement != null) {
                        PsiStatement prev = PsiTreeUtil.getPrevSiblingOfType(statement,
                                PsiStatement.class);
                        String targetName = variable.getName();
                        if (targetName == null) {
                            return null;
                        }
                        while (prev != null) {
                            if (prev instanceof PsiDeclarationStatement) {
                                for (PsiElement element : ((PsiDeclarationStatement) prev)
                                        .getDeclaredElements()) {
                                    if (variable.equals(element)) {
                                        PsiExpression initializer = variable.getInitializer();
                                        if (initializer != null) {
                                            return findIntDef(initializer);
                                        }
                                        break;
                                    }
                                }
                            } else if (prev instanceof PsiExpressionStatement) {
                                PsiExpression expression = ((PsiExpressionStatement) prev)
                                        .getExpression();
                                if (expression instanceof PsiAssignmentExpression) {
                                    PsiAssignmentExpression assign
                                            = (PsiAssignmentExpression) expression;
                                    PsiExpression lhs = assign.getLExpression();
                                    if (lhs instanceof PsiReferenceExpression) {
                                        PsiReferenceExpression reference = (PsiReferenceExpression) lhs;
                                        if (targetName.equals(reference.getReferenceName()) &&
                                                reference.getQualifier() == null) {
                                            PsiExpression rExpression = assign.getRExpression();
                                            if (rExpression != null) {
                                                return findIntDef(rExpression);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            prev = PsiTreeUtil.getPrevSiblingOfType(prev,
                                    PsiStatement.class);
                        }
                    }

                }
            } else if (node instanceof PsiMethodCallExpression) {
                PsiMethod method = ((PsiMethodCallExpression) node).resolveMethod();
                if (method != null) {
                    PsiAnnotation[] annotations = mContext.getEvaluator().getAllAnnotations(method, true);
                    PsiAnnotation annotation = SupportAnnotationDetector.findIntDef(
                            filterRelevantAnnotations(annotations));
                    if (annotation != null) {
                        return annotation;
                    }
                }
            } else if (node instanceof PsiConditionalExpression) {
                PsiConditionalExpression expression = (PsiConditionalExpression) node;
                if (expression.getThenExpression() != null) {
                    PsiAnnotation result = findIntDef(expression.getThenExpression());
                    if (result != null) {
                        return result;
                    }
                }
                if (expression.getElseExpression() != null) {
                    PsiAnnotation result = findIntDef(expression.getElseExpression());
                    if (result != null) {
                        return result;
                    }
                }
            } else if (node instanceof PsiTypeCastExpression) {
                PsiTypeCastExpression cast = (PsiTypeCastExpression) node;
                if (cast.getOperand() != null) {
                    return findIntDef(cast.getOperand());
                }
            } else if (node instanceof PsiParenthesizedExpression) {
                PsiParenthesizedExpression expression = (PsiParenthesizedExpression) node;
                if (expression.getExpression() != null) {
                    return findIntDef(expression.getExpression());
                }
            }

            return null;
        }

        private void checkSwitch(@NonNull PsiSwitchStatement node, @NonNull PsiAnnotation annotation) {
            PsiCodeBlock block = node.getBody();
            if (block == null) {
                return;
            }

            PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue(ATTR_VALUE);
            if (value == null) {
                value = annotation.findDeclaredAttributeValue(null);
            }
            if (value == null) {
                return;
            }

            if (!(value instanceof PsiArrayInitializerMemberValue)) {
                return;
            }

            PsiArrayInitializerMemberValue array = (PsiArrayInitializerMemberValue)value;
            PsiAnnotationMemberValue[] allowedValues = array.getInitializers();

            List<PsiElement> fields = Lists.newArrayListWithCapacity(allowedValues.length);
            for (PsiAnnotationMemberValue allowedValue : allowedValues) {
                if (allowedValue instanceof PsiReferenceExpression) {
                    PsiElement resolved = ((PsiReferenceExpression) allowedValue).resolve();
                    if (resolved != null) {
                        fields.add(resolved);
                    }
                } else if (allowedValue instanceof PsiLiteral) {
                    fields.add(allowedValue);
                }
            }


            // Empty switch: arguably we could skip these (since the IDE already warns about
            // empty switches) but it's useful since the quickfix will kick in and offer all
            // the missing ones when you're editing.
            //   if (block.getStatements().length == 0) { return; }

            for (PsiStatement statement : block.getStatements()) {
                if (statement instanceof PsiSwitchLabelStatement) {
                    PsiSwitchLabelStatement caseStatement = (PsiSwitchLabelStatement) statement;
                    PsiExpression expression = caseStatement.getCaseValue();
                    if (expression instanceof PsiLiteral) {
                        // Report warnings if you specify hardcoded constants.
                        // It's the wrong thing to do.
                        List<String> list = computeFieldNames(node, Arrays.asList(allowedValues));
                        // Keep error message in sync with {@link #getMissingCases}
                        String message = "Don't use a constant here; expected one of: " + Joiner
                                .on(", ").join(list);
                        mContext.report(SWITCH_TYPE_DEF, expression,
                                mContext.getLocation(expression), message);
                        return; // Don't look for other missing typedef constants since you might
                        // have aliased with value
                    } else if (expression instanceof PsiReferenceExpression) { // default case can have null expression
                        PsiElement resolved = ((PsiReferenceExpression) expression).resolve();
                        if (resolved == null) {
                            // If there are compilation issues (e.g. user is editing code) we
                            // can't be certain, so don't flag anything.
                            return;
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
                            ListIterator<PsiElement> iterator = fields.listIterator();
                            while (iterator.hasNext()) {
                                PsiElement field = iterator.next();
                                if (field.isEquivalentTo(resolved)) {
                                    iterator.remove();
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                // Look for local alias
                                PsiExpression initializer = ((PsiField) resolved).getInitializer();
                                if (initializer instanceof PsiReferenceExpression) {
                                    resolved = ((PsiReferenceExpression) expression).resolve();
                                    if (resolved instanceof PsiField) {
                                        iterator = fields.listIterator();
                                        while (iterator.hasNext()) {
                                            PsiElement field = iterator.next();
                                            if (field.equals(initializer)) {
                                                iterator.remove();
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            if (!found) {
                                List<String> list = computeFieldNames(node, Arrays.asList(allowedValues));
                                // Keep error message in sync with {@link #getMissingCases}
                                String message = "Unexpected constant; expected one of: " + Joiner
                                        .on(", ").join(list);
                                Location location = mContext.getNameLocation(expression);
                                mContext.report(SWITCH_TYPE_DEF, expression, location, message);
                            }
                        }
                    }
                }
            }
            if (!fields.isEmpty()) {
                List<String> list = computeFieldNames(node, fields);
                // Keep error message in sync with {@link #getMissingCases}
                String message = "Switch statement on an `int` with known associated constant "
                        + "missing case " + Joiner.on(", ").join(list);
                Location location = mContext.getNameLocation(node);
                mContext.report(SWITCH_TYPE_DEF, node, location, message);
            }
        }

        private void ensureUniqueValues(@NonNull PsiAnnotation node) {
            PsiAnnotationMemberValue value = node.findAttributeValue(ATTR_VALUE);
            if (value == null) {
                value = node.findAttributeValue(null);
            }
            if (value == null) {
                return;
            }

            if (!(value instanceof PsiArrayInitializerMemberValue)) {
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
                        PsiElement scope = getAnnotationScope(node);
                        mContext.report(UNIQUE, scope, location, message);
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

        private boolean checkSuppressLint(@NonNull PsiAnnotation node, @NonNull String id) {
            IssueRegistry registry = mContext.getDriver().getRegistry();
            Issue issue = registry.getIssue(id);
            // Special-case the ApiDetector issue, since it does both source file analysis
            // only on field references, and class file analysis on the rest, so we allow
            // annotations outside of methods only on fields
            if (issue != null && !issue.getImplementation().getScope().contains(Scope.JAVA_FILE)
                    || issue == ApiDetector.UNSUPPORTED) {
                // This issue doesn't have AST access: annotations are not
                // available for local variables or parameters
                PsiElement scope = getAnnotationScope(node);
                mContext.report(INSIDE_METHOD, scope, mContext.getLocation(node), String.format(
                    "The `@SuppressLint` annotation cannot be used on a local " +
                    "variable with the lint check '%1$s': move out to the " +
                    "surrounding method", id));
                return false;
            }

            return true;
        }
    }

    @NonNull
    private static List<String> computeFieldNames(@NonNull PsiSwitchStatement node,
            Iterable<?> allowedValues) {
        List<String> list = Lists.newArrayList();
        for (Object o : allowedValues) {
            if (o instanceof PsiReferenceExpression) {
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
                PsiClass clz = PsiTreeUtil.getParentOfType(node, PsiClass.class, true);
                if (clz != null) {
                    PsiClass containingClass = field.getContainingClass();
                    if (containingClass != null && !containingClass.equals(clz)) {

                        //if (Objects.equal(containingClass.getPackage(),
                        //        ((ResolvedClass) resolved).getPackage())) {
                        //    name = containingClass.getSimpleName() + '.' + field.getName();
                        //} else {
                            name = containingClass.getName() + '.' + field.getName();
                        //}
                    }
                }
                list.add('`' + name + '`');
            }
        }
        Collections.sort(list);
        return list;
    }

    /**
     * Given an error message produced by this lint detector for the {@link #SWITCH_TYPE_DEF} issue
     * type, returns the list of missing enum cases. <p> Intended for IDE quickfix implementations.
     *
     * @param errorMessage the error message associated with the error
     * @param format       the format of the error message
     * @return the list of enum cases, or null if not recognized
     */
    @Nullable
    public static List<String> getMissingCases(@NonNull String errorMessage,
            @NonNull TextFormat format) {
        errorMessage = format.toText(errorMessage);

        String substring = findSubstring(errorMessage, " missing case ", null);
        if (substring == null) {
            substring = findSubstring(errorMessage, "expected one of: ", null);
        }
        if (substring != null) {
            return Splitter.on(",").trimResults().splitToList(substring);
        }

        return null;
    }

    /**
     * Returns the node to use as the scope for the given annotation node.
     * You can't annotate an annotation itself (with {@code @SuppressLint}), but
     * you should be able to place an annotation next to it, as a sibling, to only
     * suppress the error on this annotated element, not the whole surrounding class.
     */
    @NonNull
    private static PsiElement getAnnotationScope(@NonNull PsiAnnotation node) {
        PsiElement scope = PsiTreeUtil.getParentOfType(node, PsiAnnotation.class, true);
        if (scope == null) {
            scope = node;
        }
        return scope;
    }
}
