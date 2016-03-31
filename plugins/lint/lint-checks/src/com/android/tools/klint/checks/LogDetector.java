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

import static com.android.tools.lint.client.api.JavaParser.TYPE_STRING;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ConstantEvaluator;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.BinaryExpression;
import lombok.ast.ClassDeclaration;
import lombok.ast.Expression;
import lombok.ast.If;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Select;
import lombok.ast.StringLiteral;
import lombok.ast.VariableReference;

/**
 * Detector for finding inefficiencies and errors in logging calls.
 */
public class LogDetector extends Detector implements Detector.JavaScanner {
    private static final Implementation IMPLEMENTATION = new Implementation(
          LogDetector.class, Scope.JAVA_FILE_SCOPE);


    /** Log call missing surrounding if */
    public static final Issue CONDITIONAL = Issue.create(
            "LogConditional", //$NON-NLS-1$
            "Unconditional Logging Calls",
            "The BuildConfig class (available in Tools 17) provides a constant, \"DEBUG\", " +
            "which indicates whether the code is being built in release mode or in debug " +
            "mode. In release mode, you typically want to strip out all the logging calls. " +
            "Since the compiler will automatically remove all code which is inside a " +
            "\"if (false)\" check, surrounding your logging calls with a check for " +
            "BuildConfig.DEBUG is a good idea.\n" +
            "\n" +
            "If you *really* intend for the logging to be present in release mode, you can " +
            "suppress this warning with a @SuppressLint annotation for the intentional " +
            "logging calls.",

            Category.PERFORMANCE,
            5,
            Severity.WARNING,
            IMPLEMENTATION).setEnabledByDefault(false);

    /** Mismatched tags between isLogging and log calls within it */
    public static final Issue WRONG_TAG = Issue.create(
            "LogTagMismatch", //$NON-NLS-1$
            "Mismatched Log Tags",
            "When guarding a `Log.v(tag, ...)` call with `Log.isLoggable(tag)`, the " +
            "tag passed to both calls should be the same. Similarly, the level passed " +
            "in to `Log.isLoggable` should typically match the type of `Log` call, e.g. " +
            "if checking level `Log.DEBUG`, the corresponding `Log` call should be `Log.d`, " +
            "not `Log.i`.",

            Category.CORRECTNESS,
            5,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Log tag is too long */
    public static final Issue LONG_TAG = Issue.create(
            "LongLogTag", //$NON-NLS-1$
            "Too Long Log Tags",
            "Log tags are only allowed to be at most 23 tag characters long.",

            Category.CORRECTNESS,
            5,
            Severity.ERROR,
            IMPLEMENTATION);

    @SuppressWarnings("SpellCheckingInspection")
    private static final String IS_LOGGABLE = "isLoggable";       //$NON-NLS-1$
    private static final String LOG_CLS = "android.util.Log";     //$NON-NLS-1$
    private static final String PRINTLN = "println";              //$NON-NLS-1$

    // ---- Implements Detector.JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList(
                "d",           //$NON-NLS-1$
                "e",           //$NON-NLS-1$
                "i",           //$NON-NLS-1$
                "v",           //$NON-NLS-1$
                "w",           //$NON-NLS-1$
                PRINTLN,
                IS_LOGGABLE);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor, @NonNull MethodInvocation node) {
        ResolvedNode resolved = context.resolve(node);
        if (!(resolved instanceof ResolvedMethod)) {
            return;
        }

        ResolvedMethod method = (ResolvedMethod) resolved;
        if (!method.getContainingClass().matches(LOG_CLS)) {
            return;
        }

        String name = node.astName().astValue();
        boolean withinConditional = IS_LOGGABLE.equals(name) ||
                checkWithinConditional(context, node.getParent(), node);

        // See if it's surrounded by an if statement (and it's one of the non-error, spammy
        // log methods (info, verbose, etc))
        if (("i".equals(name) || "d".equals(name) || "v".equals(name) || PRINTLN.equals(name))
                && !withinConditional
                && performsWork(context, node)
                && context.isEnabled(CONDITIONAL)) {
            String message = String.format("The log call Log.%1$s(...) should be " +
                            "conditional: surround with `if (Log.isLoggable(...))` or " +
                            "`if (BuildConfig.DEBUG) { ... }`",
                    node.astName().toString());
            context.report(CONDITIONAL, node, context.getLocation(node), message);
        }

        // Check tag length
        if (context.isEnabled(LONG_TAG)) {
            int tagArgumentIndex = PRINTLN.equals(name) ? 1 : 0;
            if (method.getArgumentCount() > tagArgumentIndex
                    && method.getArgumentType(tagArgumentIndex).matchesSignature(TYPE_STRING)
                    && node.astArguments().size() == method.getArgumentCount()) {
                Iterator<Expression> iterator = node.astArguments().iterator();
                if (tagArgumentIndex == 1) {
                    iterator.next();
                }
                Node argument = iterator.next();
                String tag = ConstantEvaluator.evaluateString(context, argument, true);
                if (tag != null && tag.length() > 23) {
                    String message = String.format(
                        "The logging tag can be at most 23 characters, was %1$d (%2$s)",
                        tag.length(), tag);
                    context.report(LONG_TAG, node, context.getLocation(node), message);
                }
            }
        }
    }

    /** Returns true if the given logging call performs "work" to compute the message */
    private static boolean performsWork(
            @NonNull JavaContext context,
            @NonNull MethodInvocation node) {
        int messageArgumentIndex = PRINTLN.equals(node.astName().astValue()) ? 2 : 1;
        if (node.astArguments().size() >= messageArgumentIndex) {
            Iterator<Expression> iterator = node.astArguments().iterator();
            Node argument = null;
            for (int i = 0; i <= messageArgumentIndex; i++) {
                argument = iterator.next();
            }
            if (argument == null) {
                return false;
            }
            if (argument instanceof StringLiteral || argument instanceof VariableReference) {
                return false;
            }
            if (argument instanceof BinaryExpression) {
                String string = ConstantEvaluator.evaluateString(context, argument, false);
                //noinspection VariableNotUsedInsideIf
                if (string != null) { // does it resolve to a constant?
                    return false;
                }
            } else if (argument instanceof Select) {
                String string = ConstantEvaluator.evaluateString(context, argument, false);
                //noinspection VariableNotUsedInsideIf
                if (string != null) {
                    return false;
                }
            }

            // Method invocations etc
            return true;
        }

        return false;
    }

    private static boolean checkWithinConditional(
            @NonNull JavaContext context,
            @Nullable Node curr,
            @NonNull MethodInvocation logCall) {
        while (curr != null) {
            if (curr instanceof If) {
                If ifNode = (If) curr;
                if (ifNode.astCondition() instanceof MethodInvocation) {
                    MethodInvocation call = (MethodInvocation) ifNode.astCondition();
                    if (IS_LOGGABLE.equals(call.astName().astValue())) {
                        checkTagConsistent(context, logCall, call);
                    }
                }

                return true;
            } else if (curr instanceof MethodInvocation
                    || curr instanceof ClassDeclaration) { // static block
                break;
            }
            curr = curr.getParent();
        }
        return false;
    }

    /** Checks that the tag passed to Log.s and Log.isLoggable match */
    private static void checkTagConsistent(JavaContext context, MethodInvocation logCall,
            MethodInvocation call) {
        Iterator<Expression> isLogIterator = call.astArguments().iterator();
        Iterator<Expression> logIterator = logCall.astArguments().iterator();
        if (!isLogIterator.hasNext() || !logIterator.hasNext()) {
            return;
        }
        Expression isLoggableTag = isLogIterator.next();
        Expression logTag = logIterator.next();

        //String callName = logCall.astName().astValue();
        String logCallName = logCall.astName().astValue();
        boolean isPrintln = PRINTLN.equals(logCallName);
        if (isPrintln) {
            if (!logIterator.hasNext()) {
                return;
            }
            logTag = logIterator.next();
        }

        if (logTag != null) {
            if (!isLoggableTag.toString().equals(logTag.toString())) {
                ResolvedNode resolved1 = context.resolve(isLoggableTag);
                ResolvedNode resolved2 = context.resolve(logTag);
                if ((resolved1 == null || resolved2 == null || !resolved1.equals(resolved2))
                        && context.isEnabled(WRONG_TAG)) {
                    Location location = context.getLocation(logTag);
                    Location alternate = context.getLocation(isLoggableTag);
                    alternate.setMessage("Conflicting tag");
                    location.setSecondary(alternate);
                    String isLoggableDescription = resolved1 != null ? resolved1
                            .getName()
                            : isLoggableTag.toString();
                    String logCallDescription = resolved2 != null ? resolved2.getName()
                            : logTag.toString();
                    String message = String.format(
                            "Mismatched tags: the `%1$s()` and `isLoggable()` calls typically " +
                                    "should pass the same tag: `%2$s` versus `%3$s`",
                            logCallName,
                            isLoggableDescription,
                            logCallDescription);
                    context.report(WRONG_TAG, call, location, message);
                }
            }
        }

        // Check log level versus the actual log call type (e.g. flag
        //    if (Log.isLoggable(TAG, Log.DEBUG) Log.info(TAG, "something")

        if (logCallName.length() != 1 || !isLogIterator.hasNext()) { // e.g. println
            return;
        }
        Expression isLoggableLevel = isLogIterator.next();
        if (isLoggableLevel == null) {
            return;
        }
        String levelString = isLoggableLevel.toString();
        if (isLoggableLevel instanceof Select) {
            levelString = ((Select)isLoggableLevel).astIdentifier().astValue();
        }
        if (levelString.isEmpty()) {
            return;
        }
        char levelChar = Character.toLowerCase(levelString.charAt(0));
        if (logCallName.charAt(0) == levelChar || !context.isEnabled(WRONG_TAG)) {
            return;
        }
        switch (levelChar) {
            case 'd':
            case 'e':
            case 'i':
            case 'v':
            case 'w':
                break;
            default:
                // Some other char; e.g. user passed in a literal value or some
                // local constant or variable alias
                return;
        }
        String expectedCall = String.valueOf(levelChar);
        String message = String.format(
                "Mismatched logging levels: when checking `isLoggable` level `%1$s`, the " +
                "corresponding log call should be `Log.%2$s`, not `Log.%3$s`",
                levelString, expectedCall, logCallName);
        Location location = context.getLocation(logCall.astName());
        Location alternate = context.getLocation(isLoggableLevel);
        alternate.setMessage("Conflicting tag");
        location.setSecondary(alternate);
        context.report(WRONG_TAG, call, location, message);
    }
}
