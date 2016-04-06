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

import static com.android.SdkConstants.SUPPORT_LIB_ARTIFACT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Project;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;
import com.android.tools.klint.detector.api.TextFormat;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.visitor.UastVisitor;

/**
 * Looks for performance issues in Java files, such as memory allocations during
 * drawing operations and using HashMap instead of SparseArray.
 */
public class JavaPerformanceDetector extends Detector implements UastScanner {

    private static final Implementation
            IMPLEMENTATION = new Implementation(
            JavaPerformanceDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Allocating objects during a paint method */
    public static final Issue PAINT_ALLOC = Issue.create(
            "DrawAllocation", //$NON-NLS-1$
            "Memory allocations within drawing code",

            "You should avoid allocating objects during a drawing or layout operation. These " +
            "are called frequently, so a smooth UI can be interrupted by garbage collection " +
            "pauses caused by the object allocations.\n" +
            "\n" +
            "The way this is generally handled is to allocate the needed objects up front " +
            "and to reuse them for each drawing operation.\n" +
            "\n" +
            "Some methods allocate memory on your behalf (such as `Bitmap.create`), and these " +
            "should be handled in the same way.",

            Category.PERFORMANCE,
            9,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Using HashMaps where SparseArray would be better */
    public static final Issue USE_SPARSE_ARRAY = Issue.create(
            "UseSparseArrays", //$NON-NLS-1$
            "HashMap can be replaced with SparseArray",

            "For maps where the keys are of type integer, it's typically more efficient to " +
            "use the Android `SparseArray` API. This check identifies scenarios where you might " +
            "want to consider using `SparseArray` instead of `HashMap` for better performance.\n" +
            "\n" +
            "This is *particularly* useful when the value types are primitives like ints, " +
            "where you can use `SparseIntArray` and avoid auto-boxing the values from `int` to " +
            "`Integer`.\n" +
            "\n" +
            "If you need to construct a `HashMap` because you need to call an API outside of " +
            "your control which requires a `Map`, you can suppress this warning using for " +
            "example the `@SuppressLint` annotation.",

            Category.PERFORMANCE,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Using {@code new Integer()} instead of the more efficient {@code Integer.valueOf} */
    public static final Issue USE_VALUE_OF = Issue.create(
            "UseValueOf", //$NON-NLS-1$
            "Should use `valueOf` instead of `new`",

            "You should not call the constructor for wrapper classes directly, such as" +
            "`new Integer(42)`. Instead, call the `valueOf` factory method, such as " +
            "`Integer.valueOf(42)`. This will typically use less memory because common integers " +
            "such as 0 and 1 will share a single instance.",

            Category.PERFORMANCE,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    static final String ON_MEASURE = "onMeasure";                           //$NON-NLS-1$
    static final String ON_DRAW = "onDraw";                                 //$NON-NLS-1$
    static final String ON_LAYOUT = "onLayout";                             //$NON-NLS-1$
    private static final String INTEGER = "Integer";                        //$NON-NLS-1$
    private static final String BOOLEAN = "Boolean";                        //$NON-NLS-1$
    private static final String BYTE = "Byte";                              //$NON-NLS-1$
    private static final String LONG = "Long";                              //$NON-NLS-1$
    private static final String CHARACTER = "Character";                    //$NON-NLS-1$
    private static final String DOUBLE = "Double";                          //$NON-NLS-1$
    private static final String FLOAT = "Float";                            //$NON-NLS-1$
    private static final String HASH_MAP = "HashMap";                       //$NON-NLS-1$
    private static final String SPARSE_ARRAY = "SparseArray";               //$NON-NLS-1$
    private static final String CANVAS = "Canvas";                          //$NON-NLS-1$
    private static final String LAYOUT = "layout";                          //$NON-NLS-1$

    /** Constructs a new {@link JavaPerformanceDetector} check */
    public JavaPerformanceDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements UastScanner ----

    @Override
    public UastVisitor createUastVisitor(UastAndroidContext context) {
        return new PerformanceVisitor(context);
    }

    private static class PerformanceVisitor extends UastVisitor {
        private final UastAndroidContext mContext;
        private final boolean mCheckMaps;
        private final boolean mCheckAllocations;
        private final boolean mCheckValueOf;
        /** Whether allocations should be "flagged" in the current method */
        private boolean mFlagAllocations;

        public PerformanceVisitor(UastAndroidContext context) {
            mContext = context;

            JavaContext lintContext = context.getLintContext();
            mCheckAllocations = lintContext.isEnabled(PAINT_ALLOC);
            mCheckMaps = lintContext.isEnabled(USE_SPARSE_ARRAY);
            mCheckValueOf = lintContext.isEnabled(USE_VALUE_OF);
        }

        @Override
        public boolean visitFunction(@NotNull UFunction node) {
            mFlagAllocations = isBlockedAllocationMethod(node);
            return super.visitFunction(node);
        }

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression node) {
            UastCallKind kind = node.getKind();
            if (kind == UastCallKind.CONSTRUCTOR_CALL) {
                visitConstructorInvocation(node);
            } else if (kind == UastCallKind.FUNCTION_CALL) {
                visitFunctionInvocation(node);
            }

            return super.visitCallExpression(node);
        }

        private void visitConstructorInvocation(UCallExpression node) {
            USimpleReferenceExpression classReference = node.getClassReference();
            if (classReference == null) {
                return;
            }

            String typeName = null;
            if (mCheckMaps) {
                UClass clazz = classReference.resolveClass(mContext);
                if (clazz != null) {
                    typeName = clazz.getName();
                    // TODO: Should we handle factory method constructions of HashMaps as well,
                    // e.g. via Guava? This is a bit trickier since we need to infer the type
                    // arguments from the calling context.
                    if (clazz.matchesFqName(HASH_MAP)) {
                        checkHashMap(node);
                    } else if (clazz.matchesFqName(SPARSE_ARRAY)) {
                        checkSparseArray(node);
                    }
                }
            }

            if (mCheckValueOf) {
                if (typeName == null) {
                    typeName = classReference.getIdentifier();
                }
                if ((typeName.equals(INTEGER)
                        || typeName.equals(BOOLEAN)
                        || typeName.equals(FLOAT)
                        || typeName.equals(CHARACTER)
                        || typeName.equals(LONG)
                        || typeName.equals(DOUBLE)
                        || typeName.equals(BYTE))
                        && node.getValueArgumentCount() == 1) {
                    String argument = node.getValueArguments().get(0).renderString();
                    mContext.report(USE_VALUE_OF, node, mContext.getLocation(node), getUseValueOfErrorMessage(
                            typeName, argument));
                }
            }

            if (mFlagAllocations && !(UastUtils.isThrow(node.getParent())) && mCheckAllocations) {
                // Make sure we're still inside the method declaration that marked
                // mInDraw as true, in case we've left it and we're in a static
                // block or something:
                UElement method = node;
                while (method != null) {
                    if (method instanceof UFunction) {
                        break;
                    }
                    method = method.getParent();
                }
                if (method != null && isBlockedAllocationMethod(((UFunction) method))
                        && !isLazilyInitialized(node)) {
                    reportAllocation(node);
                }
            }
        }

        private void reportAllocation(UElement node) {
            mContext.report(PAINT_ALLOC, node, mContext.getLocation(node),
                "Avoid object allocations during draw/layout operations (preallocate and " +
                "reuse instead)");
        }

        private void visitFunctionInvocation(UCallExpression node) {
            UExpression operand = UastUtils.getReceiver(node);

            if (mFlagAllocations && operand != null) {
                // Look for forbidden methods
                String methodName = node.getFunctionName();
                if ("createBitmap".equals(methodName)                              //$NON-NLS-1$
                    || "createScaledBitmap".equals(methodName)) {                  //$NON-NLS-1$
                    String operandText = operand.renderString();
                    if (operandText.equals("Bitmap")                                   //$NON-NLS-1$
                            || operandText.equals("android.graphics.Bitmap")) {        //$NON-NLS-1$
                        if (!isLazilyInitialized(node)) {
                            reportAllocation(node);
                        }
                    }
                } else if (methodName != null && methodName.startsWith("decode")) { //$NON-NLS-1$
                    // decodeFile, decodeByteArray, ...
                    String operandText = operand.renderString();
                    if (operandText.equals("BitmapFactory")                            //$NON-NLS-1$
                            || operandText.equals("android.graphics.BitmapFactory")) { //$NON-NLS-1$
                        if (!isLazilyInitialized(node)) {
                            reportAllocation(node);
                        }
                    }
                } else if ("getClipBounds".equals(methodName)) {                   //$NON-NLS-1$
                    if (node.getValueArgumentCount() == 0) {
                        mContext.report(PAINT_ALLOC, node, mContext.getLocation(node),
                                "Avoid object allocations during draw operations: Use " +
                                "`Canvas.getClipBounds(Rect)` instead of `Canvas.getClipBounds()` " +
                                "which allocates a temporary `Rect`");
                    }
                }
            }
        }

        /**
         * Check whether the given invocation is done as a lazy initialization,
         * e.g. {@code if (foo == null) foo = new Foo();}.
         * <p>
         * This tries to also handle the scenario where the check is on some
         * <b>other</b> variable - e.g.
         * <pre>
         *    if (foo == null) {
         *        foo == init1();
         *        bar = new Bar();
         *    }
         * </pre>
         * or
         * <pre>
         *    if (!initialized) {
         *        initialized = true;
         *        bar = new Bar();
         *    }
         * </pre>
         */
        private static boolean isLazilyInitialized(UElement node) {
            UElement curr = node.getParent();
            while (curr != null) {
                if (curr instanceof UFunction) {
                    return false;
                } else if (curr instanceof UIfExpression) {
                    UIfExpression ifNode = (UIfExpression) curr;
                    // See if the if block represents a lazy initialization:
                    // compute all variable names seen in the condition
                    // (e.g. for "if (foo == null || bar != foo)" the result is "foo,bar"),
                    // and then compute all variables assigned to in the if body,
                    // and if there is an overlap, we'll consider the whole if block
                    // guarded (so lazily initialized and an allocation we won't complain
                    // about.)
                    List<String> assignments = new ArrayList<String>();
                    UExpression thenBranch = ifNode.getThenBranch();
                    if (thenBranch != null) {
                        AssignmentTracker visitor = new AssignmentTracker(assignments);
                        visitor.process(thenBranch);
                    }
                    if (!assignments.isEmpty()) {
                        List<String> references = new ArrayList<String>();
                        addReferencedVariables(references, ifNode.getCondition());
                        if (!references.isEmpty()) {
                            SetView<String> intersection = Sets.intersection(
                                    new HashSet<String>(assignments),
                                    new HashSet<String>(references));
                            return !intersection.isEmpty();
                        }
                    }
                    return false;

                }
                curr = curr.getParent();
            }

            return false;
        }

        /** Adds any variables referenced in the given expression into the given list */
        private static void addReferencedVariables(Collection<String> variables,
                UExpression expression) {
            if (expression instanceof UBinaryExpression) {
                UBinaryExpression binary = (UBinaryExpression) expression;
                addReferencedVariables(variables, binary.getLeftOperand());
                addReferencedVariables(variables, binary.getRightOperand());
            } else if (expression instanceof UUnaryExpression) {
                UUnaryExpression unary = (UUnaryExpression) expression;
                addReferencedVariables(variables, unary.getOperand());
            } else if (expression instanceof USimpleReferenceExpression) {
                USimpleReferenceExpression reference = (USimpleReferenceExpression) expression;
                variables.add(reference.getIdentifier());
            } else if (expression instanceof UQualifiedExpression) {
                UQualifiedExpression select = (UQualifiedExpression) expression;
                if (select.getReceiver() instanceof UThisExpression && select.getSelector() instanceof USimpleReferenceExpression) {
                    variables.add(((USimpleReferenceExpression) select.getSelector()).getIdentifier());
                }
            }
        }

        /**
         * Returns whether the given method declaration represents a method
         * where allocating objects is not allowed for performance reasons
         */
        private static boolean isBlockedAllocationMethod(UFunction node) {
            return isOnDrawMethod(node) || isOnMeasureMethod(node) || isOnLayoutMethod(node)
                    || isLayoutMethod(node);
        }

        /**
         * Returns true if this method looks like it's overriding android.view.View's
         * {@code protected void onDraw(Canvas canvas)}
         */
        private static boolean isOnDrawMethod(UFunction node) {
            if (ON_DRAW.equals(node.getName())) {
                List<UVariable> parameters = node.getValueParameters();
                if (parameters.size() == 1) {
                    UVariable arg0 = parameters.get(0);
                    if (arg0.getType().matchesName(CANVAS)) {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Returns true if this method looks like it's overriding
         * android.view.View's
         * {@code protected void onLayout(boolean changed, int left, int top,
         *      int right, int bottom)}
         */
        private static boolean isOnLayoutMethod(UFunction node) {
            if (ON_LAYOUT.equals(node.getName())) {
                List<UVariable> parameters = node.getValueParameters();
                if (parameters.size() == 5) {
                    Iterator<UVariable> iterator = parameters.iterator();
                    if (!iterator.hasNext()) {
                        return false;
                    }

                    // Ensure that the argument list matches boolean, int, int, int, int
                    UType type = iterator.next().getType();
                    if (!type.isBoolean() || !iterator.hasNext()) {
                        return false;
                    }
                    for (int i = 0; i < 4; i++) {
                        type = iterator.next().getType();
                        if (!type.isInt()) {
                            return false;
                        }
                        if (!iterator.hasNext()) {
                            return i == 3;
                        }
                    }
                }
            }

            return false;
        }

        /**
         * Returns true if this method looks like it's overriding android.view.View's
         * {@code protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)}
         */
        private static boolean isOnMeasureMethod(UFunction node) {
            if (ON_MEASURE.equals(node.getName())) {
                List<UVariable> parameters = node.getValueParameters();
                if (parameters.size() == 2) {
                    UVariable arg0 = parameters.get(0);
                    UVariable arg1 = parameters.get(parameters.size() - 1);
                    return arg0.getType().isInt() && arg1.getType().isInt();
                }
            }

            return false;
        }

        /**
         * Returns true if this method looks like it's overriding android.view.View's
         * {@code public void layout(int l, int t, int r, int b)}
         */
        private static boolean isLayoutMethod(UFunction node) {
            if (LAYOUT.equals(node.getName())) {
                List<UVariable> parameters = node.getValueParameters();
                if (parameters.size() == 4) {
                    Iterator<UVariable> iterator = parameters.iterator();
                    for (int i = 0; i < 4; i++) {
                        if (!iterator.hasNext()) {
                            return false;
                        }
                        UVariable next = iterator.next();
                        if (!next.getType().isInt()) {
                            return false;
                        }
                    }
                    return true;
                }
            }

            return false;
        }


        /**
         * Checks whether the given constructor call and type reference refers
         * to a HashMap constructor call that is eligible for replacement by a
         * SparseArray call instead
         */
        private void checkHashMap(UCallExpression node) {
            // reference.hasTypeArguments returns false where it should not
            List<UType> types = node.getTypeArguments();
            if (types.size() == 2) {
                UType first = types.get(0);
                String typeName = first.getName();

                Project mainProject = mContext.getLintContext().getMainProject();
                int minSdk = mainProject.getMinSdk();

                if (typeName.equals(INTEGER) || typeName.equals(BYTE)) {
                    String valueType = types.get(1).getName();
                    if (valueType.equals(INTEGER)) {
                        mContext.report(USE_SPARSE_ARRAY, node, mContext.getLocation(node),
                            "Use new `SparseIntArray(...)` instead for better performance");
                    } else if (valueType.equals(LONG) && minSdk >= 18) {
                        mContext.report(USE_SPARSE_ARRAY, node, mContext.getLocation(node),
                                "Use `new SparseLongArray(...)` instead for better performance");
                    } else if (valueType.equals(BOOLEAN)) {
                        mContext.report(USE_SPARSE_ARRAY, node, mContext.getLocation(node),
                                "Use `new SparseBooleanArray(...)` instead for better performance");
                    } else {
                        mContext.report(USE_SPARSE_ARRAY, node, mContext.getLocation(node),
                            String.format(
                                "Use `new SparseArray<%1$s>(...)` instead for better performance",
                              valueType));
                    }
                } else if (typeName.equals(LONG) && (minSdk >= 16 ||
                                                     Boolean.TRUE == mainProject.dependsOn(
                                SUPPORT_LIB_ARTIFACT))) {
                    boolean useBuiltin = minSdk >= 16;
                    String message = useBuiltin ?
                            "Use `new LongSparseArray(...)` instead for better performance" :
                            "Use `new android.support.v4.util.LongSparseArray(...)` instead for better performance";
                    mContext.report(USE_SPARSE_ARRAY, node, mContext.getLocation(node),
                            message);
                }
            }
        }

        private void checkSparseArray(UCallExpression node) {
            // reference.hasTypeArguments returns false where it should not
            List<UType> types = node.getTypeArguments();
            if (types.size() == 1) {
                UType first = types.get(0);
                String valueType = first.getName();
                if (valueType.equals(INTEGER)) {
                    mContext.report(USE_SPARSE_ARRAY, node, mContext.getLocation(node),
                        "Use `new SparseIntArray(...)` instead for better performance");
                } else if (valueType.equals(BOOLEAN)) {
                    mContext.report(USE_SPARSE_ARRAY, node, mContext.getLocation(node),
                            "Use `new SparseBooleanArray(...)` instead for better performance");
                }
            }
        }
    }

    private static String getUseValueOfErrorMessage(String typeName, String argument) {
        // Keep in sync with {@link #getReplacedType} below
        return String.format("Use `%1$s.valueOf(%2$s)` instead", typeName, argument);
    }

    /**
     * For an error message for an {@link #USE_VALUE_OF} issue reported by this detector,
     * returns the type being replaced. Intended to use for IDE quickfix implementations.
     */
    @Nullable
    public static String getReplacedType(@NonNull String message, @NonNull TextFormat format) {
        message = format.toText(message);
        int index = message.indexOf('.');
        if (index != -1 && message.startsWith("Use ")) {
            return message.substring(4, index);
        }
        return null;
    }

    /** Visitor which records variable names assigned into */
    private static class AssignmentTracker extends UastVisitor {
        private final Collection<String> mVariables;

        public AssignmentTracker(Collection<String> variables) {
            mVariables = variables;
        }

        @Override
        public boolean visitBinaryExpression(@NotNull UBinaryExpression node) {
            UastBinaryOperator operator = node.getOperator();
            if (operator == UastBinaryOperator.ASSIGN || operator == UastBinaryOperator.OR_ASSIGN) {
                UExpression left = node.getLeftOperand();
                String variable = null;
                if (left instanceof UQualifiedExpression && ((UQualifiedExpression) left).getReceiver() instanceof UThisExpression) {
                    UExpression selector = ((UQualifiedExpression) left).getSelector();
                    if (selector instanceof USimpleReferenceExpression) {
                        variable = ((USimpleReferenceExpression) selector).getIdentifier();
                    }
                } else if (left instanceof USimpleReferenceExpression) {
                    variable = ((USimpleReferenceExpression) left).getIdentifier();
                }
                if (variable != null) {
                    mVariables.add(variable);
                }
            }

            return super.visitBinaryExpression(node);
        }
    }
}
