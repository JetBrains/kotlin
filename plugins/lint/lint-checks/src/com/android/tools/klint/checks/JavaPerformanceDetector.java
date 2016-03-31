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

import static com.android.SdkConstants.SUPPORT_LIB_ARTIFACT;
import static com.android.tools.lint.client.api.JavaParser.TYPE_BOOLEAN;
import static com.android.tools.lint.client.api.JavaParser.TYPE_INT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.TextFormat;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.If;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Select;
import lombok.ast.StrictListAccessor;
import lombok.ast.This;
import lombok.ast.Throw;
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.UnaryExpression;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableReference;

/**
 * Looks for performance issues in Java files, such as memory allocations during
 * drawing operations and using HashMap instead of SparseArray.
 */
public class JavaPerformanceDetector extends Detector implements Detector.JavaScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
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

    // ---- Implements JavaScanner ----

    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        List<Class<? extends Node>> types = new ArrayList<Class<? extends Node>>(3);
        types.add(ConstructorInvocation.class);
        types.add(MethodDeclaration.class);
        types.add(MethodInvocation.class);
        return types;
    }

    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return new PerformanceVisitor(context);
    }

    private static class PerformanceVisitor extends ForwardingAstVisitor {
        private final JavaContext mContext;
        private final boolean mCheckMaps;
        private final boolean mCheckAllocations;
        private final boolean mCheckValueOf;
        /** Whether allocations should be "flagged" in the current method */
        private boolean mFlagAllocations;

        public PerformanceVisitor(JavaContext context) {
            mContext = context;

            mCheckAllocations = context.isEnabled(PAINT_ALLOC);
            mCheckMaps = context.isEnabled(USE_SPARSE_ARRAY);
            mCheckValueOf = context.isEnabled(USE_VALUE_OF);
        }

        @Override
        public boolean visitMethodDeclaration(MethodDeclaration node) {
            mFlagAllocations = isBlockedAllocationMethod(node);

            return super.visitMethodDeclaration(node);
        }

        @Override
        public boolean visitConstructorInvocation(ConstructorInvocation node) {
            String typeName = null;
            if (mCheckMaps) {
                TypeReference reference = node.astTypeReference();
                typeName = reference.astParts().last().astIdentifier().astValue();
                // TODO: Should we handle factory method constructions of HashMaps as well,
                // e.g. via Guava? This is a bit trickier since we need to infer the type
                // arguments from the calling context.
                if (typeName.equals(HASH_MAP)) {
                    checkHashMap(node, reference);
                } else if (typeName.equals(SPARSE_ARRAY)) {
                    checkSparseArray(node, reference);
                }
            }

            if (mCheckValueOf) {
                if (typeName == null) {
                    TypeReference reference = node.astTypeReference();
                    typeName = reference.astParts().last().astIdentifier().astValue();
                }
                if ((typeName.equals(INTEGER)
                        || typeName.equals(BOOLEAN)
                        || typeName.equals(FLOAT)
                        || typeName.equals(CHARACTER)
                        || typeName.equals(LONG)
                        || typeName.equals(DOUBLE)
                        || typeName.equals(BYTE))
                        && node.astTypeReference().astParts().size() == 1
                        && node.astArguments().size() == 1) {
                    String argument = node.astArguments().first().toString();
                    mContext.report(USE_VALUE_OF, node, mContext.getLocation(node), getUseValueOfErrorMessage(
                            typeName, argument));
                }
            }

            if (mFlagAllocations && !(node.getParent() instanceof Throw) && mCheckAllocations) {
                // Make sure we're still inside the method declaration that marked
                // mInDraw as true, in case we've left it and we're in a static
                // block or something:
                Node method = node;
                while (method != null) {
                    if (method instanceof MethodDeclaration) {
                        break;
                    }
                    method = method.getParent();
                }
                if (method != null && isBlockedAllocationMethod(((MethodDeclaration) method))
                        && !isLazilyInitialized(node)) {
                    reportAllocation(node);
                }
            }

            return super.visitConstructorInvocation(node);
        }

        private void reportAllocation(Node node) {
            mContext.report(PAINT_ALLOC, node, mContext.getLocation(node),
                "Avoid object allocations during draw/layout operations (preallocate and " +
                "reuse instead)");
        }

        @Override
        public boolean visitMethodInvocation(MethodInvocation node) {
            if (mFlagAllocations && node.astOperand() != null) {
                // Look for forbidden methods
                String methodName = node.astName().astValue();
                if (methodName.equals("createBitmap")                              //$NON-NLS-1$
                        || methodName.equals("createScaledBitmap")) {              //$NON-NLS-1$
                    String operand = node.astOperand().toString();
                    if (operand.equals("Bitmap")                                   //$NON-NLS-1$
                            || operand.equals("android.graphics.Bitmap")) {        //$NON-NLS-1$
                        if (!isLazilyInitialized(node)) {
                            reportAllocation(node);
                        }
                    }
                } else if (methodName.startsWith("decode")) {                      //$NON-NLS-1$
                    // decodeFile, decodeByteArray, ...
                    String operand = node.astOperand().toString();
                    if (operand.equals("BitmapFactory")                            //$NON-NLS-1$
                            || operand.equals("android.graphics.BitmapFactory")) { //$NON-NLS-1$
                        if (!isLazilyInitialized(node)) {
                            reportAllocation(node);
                        }
                    }
                } else if (methodName.equals("getClipBounds")) {                   //$NON-NLS-1$
                    if (node.astArguments().isEmpty()) {
                        mContext.report(PAINT_ALLOC, node, mContext.getLocation(node),
                                "Avoid object allocations during draw operations: Use " +
                                "`Canvas.getClipBounds(Rect)` instead of `Canvas.getClipBounds()` " +
                                "which allocates a temporary `Rect`");
                    }
                }
            }

            return super.visitMethodInvocation(node);
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
        private static boolean isLazilyInitialized(Node node) {
            Node curr = node.getParent();
            while (curr != null) {
                if (curr instanceof MethodDeclaration) {
                    return false;
                } else if (curr instanceof If) {
                    If ifNode = (If) curr;
                    // See if the if block represents a lazy initialization:
                    // compute all variable names seen in the condition
                    // (e.g. for "if (foo == null || bar != foo)" the result is "foo,bar"),
                    // and then compute all variables assigned to in the if body,
                    // and if there is an overlap, we'll consider the whole if block
                    // guarded (so lazily initialized and an allocation we won't complain
                    // about.)
                    List<String> assignments = new ArrayList<String>();
                    AssignmentTracker visitor = new AssignmentTracker(assignments);
                    ifNode.astStatement().accept(visitor);
                    if (!assignments.isEmpty()) {
                        List<String> references = new ArrayList<String>();
                        addReferencedVariables(references, ifNode.astCondition());
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
                Expression expression) {
            if (expression instanceof BinaryExpression) {
                BinaryExpression binary = (BinaryExpression) expression;
                addReferencedVariables(variables, binary.astLeft());
                addReferencedVariables(variables, binary.astRight());
            } else if (expression instanceof UnaryExpression) {
                UnaryExpression unary = (UnaryExpression) expression;
                addReferencedVariables(variables, unary.astOperand());
            } else if (expression instanceof VariableReference) {
                VariableReference reference = (VariableReference) expression;
                variables.add(reference.astIdentifier().astValue());
            } else if (expression instanceof Select) {
                Select select = (Select) expression;
                if (select.astOperand() instanceof This) {
                    variables.add(select.astIdentifier().astValue());
                }
            }
        }

        /**
         * Returns whether the given method declaration represents a method
         * where allocating objects is not allowed for performance reasons
         */
        private static boolean isBlockedAllocationMethod(MethodDeclaration node) {
            return isOnDrawMethod(node) || isOnMeasureMethod(node) || isOnLayoutMethod(node)
                    || isLayoutMethod(node);
        }

        /**
         * Returns true if this method looks like it's overriding android.view.View's
         * {@code protected void onDraw(Canvas canvas)}
         */
        private static boolean isOnDrawMethod(MethodDeclaration node) {
            if (ON_DRAW.equals(node.astMethodName().astValue())) {
                StrictListAccessor<VariableDefinition, MethodDeclaration> parameters =
                        node.astParameters();
                if (parameters != null && parameters.size() == 1) {
                    VariableDefinition arg0 = parameters.first();
                    TypeReferencePart type = arg0.astTypeReference().astParts().last();
                    String typeName = type.getTypeName();
                    if (typeName.equals(CANVAS)) {
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
        private static boolean isOnLayoutMethod(MethodDeclaration node) {
            if (ON_LAYOUT.equals(node.astMethodName().astValue())) {
                StrictListAccessor<VariableDefinition, MethodDeclaration> parameters =
                        node.astParameters();
                if (parameters != null && parameters.size() == 5) {
                    Iterator<VariableDefinition> iterator = parameters.iterator();
                    if (!iterator.hasNext()) {
                        return false;
                    }

                    // Ensure that the argument list matches boolean, int, int, int, int
                    TypeReferencePart type = iterator.next().astTypeReference().astParts().last();
                    if (!type.getTypeName().equals(TYPE_BOOLEAN) || !iterator.hasNext()) {
                        return false;
                    }
                    for (int i = 0; i < 4; i++) {
                        type = iterator.next().astTypeReference().astParts().last();
                        if (!type.getTypeName().equals(TYPE_INT)) {
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
        private static boolean isOnMeasureMethod(MethodDeclaration node) {
            if (ON_MEASURE.equals(node.astMethodName().astValue())) {
                StrictListAccessor<VariableDefinition, MethodDeclaration> parameters =
                        node.astParameters();
                if (parameters != null && parameters.size() == 2) {
                    VariableDefinition arg0 = parameters.first();
                    VariableDefinition arg1 = parameters.last();
                    TypeReferencePart type1 = arg0.astTypeReference().astParts().last();
                    TypeReferencePart type2 = arg1.astTypeReference().astParts().last();
                    return TYPE_INT.equals(type1.getTypeName())
                            && TYPE_INT.equals(type2.getTypeName());
                }
            }

            return false;
        }

        /**
         * Returns true if this method looks like it's overriding android.view.View's
         * {@code public void layout(int l, int t, int r, int b)}
         */
        private static boolean isLayoutMethod(MethodDeclaration node) {
            if (LAYOUT.equals(node.astMethodName().astValue())) {
                StrictListAccessor<VariableDefinition, MethodDeclaration> parameters =
                        node.astParameters();
                if (parameters != null && parameters.size() == 4) {
                    Iterator<VariableDefinition> iterator = parameters.iterator();
                    for (int i = 0; i < 4; i++) {
                        if (!iterator.hasNext()) {
                            return false;
                        }
                        VariableDefinition next = iterator.next();
                        TypeReferencePart type = next.astTypeReference().astParts().last();
                        if (!TYPE_INT.equals(type.getTypeName())) {
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
        private void checkHashMap(ConstructorInvocation node, TypeReference reference) {
            // reference.hasTypeArguments returns false where it should not
            StrictListAccessor<TypeReference, TypeReference> types = reference.getTypeArguments();
            if (types != null && types.size() == 2) {
                TypeReference first = types.first();
                String typeName = first.getTypeName();
                int minSdk = mContext.getMainProject().getMinSdk();
                if (typeName.equals(INTEGER) || typeName.equals(BYTE)) {
                    String valueType = types.last().getTypeName();
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
                        Boolean.TRUE == mContext.getMainProject().dependsOn(
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

        private void checkSparseArray(ConstructorInvocation node, TypeReference reference) {
            // reference.hasTypeArguments returns false where it should not
            StrictListAccessor<TypeReference, TypeReference> types = reference.getTypeArguments();
            if (types != null && types.size() == 1) {
                TypeReference first = types.first();
                String valueType = first.getTypeName();
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
    private static class AssignmentTracker extends ForwardingAstVisitor {
        private final Collection<String> mVariables;

        public AssignmentTracker(Collection<String> variables) {
            mVariables = variables;
        }

        @Override
        public boolean visitBinaryExpression(BinaryExpression node) {
            BinaryOperator operator = node.astOperator();
            if (operator == BinaryOperator.ASSIGN || operator == BinaryOperator.OR_ASSIGN) {
                Expression left = node.astLeft();
                String variable;
                if (left instanceof Select && ((Select) left).astOperand() instanceof This) {
                    variable = ((Select) left).astIdentifier().astValue();
                } else {
                    variable = left.toString();
                }
                mVariables.add(variable);
            }

            return super.visitBinaryExpression(node);
        }
    }
}
