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

import static com.android.tools.klint.client.api.JavaParser.TYPE_STRING;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastScanner;

/**
 * Detector for finding inefficiencies and errors in logging calls.
 */
public class LogDetector extends Detector implements UastScanner {
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

    // ---- Implements UastScanner ----


    @Override
    public List<String> getApplicableFunctionNames() {
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
    public void visitFunctionCall(UastAndroidContext context, UCallExpression node) {
        UFunction method = node.resolve(context);
        if (method == null) {
            return;
        }

        if (!UastUtils.getContainingClassOrEmpty(method).matchesFqName(LOG_CLS)) {
            return;
        }

        String name = node.getFunctionName();
        boolean withinConditional = IS_LOGGABLE.equals(name) ||
                                    checkWithinConditional(context, node.getParent(), node);

        // See if it's surrounded by an if statement (and it's one of the non-error, spammy
        // log methods (info, verbose, etc))
        if (("i".equals(name) || "d".equals(name) || "v".equals(name) || PRINTLN.equals(name))
            && !withinConditional
            && performsWork(node)
            && context.getLintContext().isEnabled(CONDITIONAL)) {
            String message = String.format("The log call Log.%1$s(...) should be " +
                                           "conditional: surround with `if (Log.isLoggable(...))` or " +
                                           "`if (BuildConfig.DEBUG) { ... }`",
                                           node.getFunctionName());
            context.report(CONDITIONAL, node, UastAndroidUtils.getLocation(node), message);
        }

        // Check tag length
        if (context.getLintContext().isEnabled(LONG_TAG)) {
            int tagArgumentIndex = PRINTLN.equals(name) ? 1 : 0;
            if (method.getValueParameterCount() > tagArgumentIndex
                && method.getValueParameters().get(tagArgumentIndex).getType().matchesFqName(TYPE_STRING)
                && node.getValueArgumentCount() == method.getValueParameterCount()) {
                Iterator<UExpression> iterator = node.getValueArguments().iterator();
                if (tagArgumentIndex == 1) {
                    iterator.next();
                }
                UExpression argument = iterator.next();
                String tag = argument.evaluateString();
                if (tag != null && tag.length() > 23) {
                    String message = String.format(
                      "The logging tag can be at most 23 characters, was %1$d (%2$s)",
                      tag.length(), tag);
                    context.report(LONG_TAG, node, UastAndroidUtils.getLocation(node), message);
                }
            }
        }

    }

    /** Returns true if the given logging call performs "work" to compute the message */
    private static boolean performsWork(
            @NonNull UCallExpression node) {
        int messageArgumentIndex = PRINTLN.equals(node.getFunctionName()) ? 2 : 1;
        if (node.getValueArgumentCount() >= messageArgumentIndex) {
            Iterator<UExpression> iterator = node.getValueArguments().iterator();
            UExpression argument = null;
            for (int i = 0; i <= messageArgumentIndex; i++) {
                argument = iterator.next();
            }
            if (argument == null) {
                return false;
            }
            if (UastLiteralUtils.isStringLiteral(argument) || argument instanceof USimpleReferenceExpression) {
                return false;
            }
            if (argument instanceof UBinaryExpression) {
                String string = argument.evaluateString();
                //noinspection VariableNotUsedInsideIf
                if (string != null) { // does it resolve to a constant?
                    return false;
                }
            } else if (argument instanceof UQualifiedExpression) {
                String string = argument.evaluateString();
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
            @NonNull UastAndroidContext context,
            @Nullable UElement curr,
            @NonNull UCallExpression logCall) {
        while (curr != null) {
            if (curr instanceof UIfExpression) {
                UIfExpression ifNode = (UIfExpression) curr;
                if (ifNode.getCondition() instanceof UCallExpression) {
                    UCallExpression call = (UCallExpression) ifNode.getCondition();
                    if (IS_LOGGABLE.equals(call.getFunctionName())) {
                        checkTagConsistent(context, logCall, call);
                    }
                }

                return true;
            } else if (curr instanceof UCallExpression
                    || curr instanceof UClass) { // static block
                break;
            }
            curr = curr.getParent();
        }
        return false;
    }

    /** Checks that the tag passed to Log.s and Log.isLoggable match */
    private static void checkTagConsistent(UastAndroidContext context, UCallExpression logCall,
            UCallExpression call) {
        Iterator<UExpression> isLogIterator = call.getValueArguments().iterator();
        Iterator<UExpression> logIterator = logCall.getValueArguments().iterator();
        if (!isLogIterator.hasNext() || !logIterator.hasNext()) {
            return;
        }
        UExpression isLoggableTag = isLogIterator.next();
        UExpression logTag = logIterator.next();

        String logCallName = logCall.getFunctionName();
        if (logCallName == null) {
            return;
        }

        boolean isPrintln = PRINTLN.equals(logCallName);
        if (isPrintln) {
            if (!logIterator.hasNext()) {
                return;
            }
            logTag = logIterator.next();
        }

        JavaContext lintContext = context.getLintContext();
        if (logTag != null) {
            if (!isLoggableTag.toString().equals(logTag.toString()) &&
                    isLoggableTag instanceof UResolvable &&
                    logTag instanceof UResolvable) {
                UDeclaration resolved1 = ((UResolvable) isLoggableTag).resolve(context);
                UDeclaration resolved2 = ((UResolvable) logTag).resolve(context);
                if ((resolved1 == null || resolved2 == null || !resolved1.equals(resolved2))
                    && lintContext.isEnabled(WRONG_TAG)) {
                    Location location = UastAndroidUtils.getLocation(logTag);
                    Location alternate = UastAndroidUtils.getLocation(isLoggableTag);
                    if (location != null && alternate != null) {
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
        }

        // Check log level versus the actual log call type (e.g. flag
        //    if (Log.isLoggable(TAG, Log.DEBUG) Log.info(TAG, "something")

        if (logCallName.length() != 1 || !isLogIterator.hasNext()) { // e.g. println
            return;
        }
        UExpression isLoggableLevel = isLogIterator.next();
        if (isLoggableLevel == null) {
            return;
        }
        String levelString = isLoggableLevel.toString();
        if (isLoggableLevel instanceof UQualifiedExpression) {
            levelString = ((UQualifiedExpression)isLoggableLevel).getSelector().renderString();
        }
        if (levelString.isEmpty()) {
            return;
        }
        char levelChar = Character.toLowerCase(levelString.charAt(0));
        if (logCallName.charAt(0) == levelChar || !lintContext.isEnabled(WRONG_TAG)) {
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
        Location location = UastAndroidUtils.getLocation(logCall.getFunctionNameElement());
        Location alternate = UastAndroidUtils.getLocation(isLoggableLevel);
        if (location != null && alternate != null) {
            alternate.setMessage("Conflicting tag");
            location.setSecondary(alternate);
            context.report(WRONG_TAG, call, location, message);
        }
    }
}
