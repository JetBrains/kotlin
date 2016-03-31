/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static com.android.SdkConstants.ANDROID_VIEW_VIEW;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.ListIterator;

/**
 * Checks that views that override View#onTouchEvent also implement View#performClick
 * and call performClick when click detection occurs.
 */
public class ClickableViewAccessibilityDetector extends Detector implements Detector.ClassScanner {

    public static final Issue ISSUE = Issue.create(
            "ClickableViewAccessibility", //$NON-NLS-1$
            "Accessibility in Custom Views",
            "If a `View` that overrides `onTouchEvent` or uses an `OnTouchListener` does not also "
                    + "implement `performClick` and call it when clicks are detected, the `View` "
                    + "may not handle accessibility actions properly. Logic handling the click "
                    + "actions should ideally be placed in `View#performClick` as some "
                    + "accessibility services invoke `performClick` when a click action "
                    + "should occur.",
            Category.A11Y,
            6,
            Severity.WARNING,
            new Implementation(
                    ClickableViewAccessibilityDetector.class,
                    Scope.CLASS_FILE_SCOPE));

    private static final String ON_TOUCH_EVENT = "onTouchEvent"; //$NON-NLS-1$
    private static final String ON_TOUCH_EVENT_SIG = "(Landroid/view/MotionEvent;)Z"; //$NON-NLS-1$
    private static final String PERFORM_CLICK = "performClick"; //$NON-NLS-1$
    private static final String PERFORM_CLICK_SIG = "()Z"; //$NON-NLS-1$
    private static final String SET_ON_TOUCH_LISTENER = "setOnTouchListener"; //$NON-NLS-1$
    private static final String SET_ON_TOUCH_LISTENER_SIG = "(Landroid/view/View$OnTouchListener;)V"; //$NON-NLS-1$
    private static final String ON_TOUCH = "onTouch"; //$NON-NLS-1$
    private static final String ON_TOUCH_SIG = "(Landroid/view/View;Landroid/view/MotionEvent;)Z"; //$NON-NLS-1$
    private static final String ON_TOUCH_LISTENER = "android/view/View$OnTouchListener";  //$NON-NLS-1$


    /** Constructs a new {@link ClickableViewAccessibilityDetector} */
    public ClickableViewAccessibilityDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements ClassScanner ----
    @Override
    public void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode) {
        scanForAndCheckSetOnTouchListenerCalls(context, classNode);

        // Ignore abstract classes.
        if ((classNode.access & Opcodes.ACC_ABSTRACT) != 0) {
            return;
        }

        if (context.getDriver().isSubclassOf(classNode, ANDROID_VIEW_VIEW)) {
            checkView(context, classNode);
        }

        if (implementsOnTouchListener(classNode)) {
            checkOnTouchListener(context, classNode);
        }
    }

    @SuppressWarnings("unchecked") // ASM API
    public static void scanForAndCheckSetOnTouchListenerCalls(
            ClassContext context,
            ClassNode classNode) {
        List<MethodNode> methods = classNode.methods;
        for (MethodNode methodNode : methods) {
            ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode abstractInsnNode = iterator.next();
                if (abstractInsnNode.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (methodInsnNode.name.equals(SET_ON_TOUCH_LISTENER)
                            && methodInsnNode.desc.equals(SET_ON_TOUCH_LISTENER_SIG)) {
                        checkSetOnTouchListenerCall(context, methodNode, methodInsnNode);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked") // ASM API
    public static void checkSetOnTouchListenerCall(
            @NonNull ClassContext context,
            @NonNull MethodNode method,
            @NonNull MethodInsnNode call) {
        String owner = call.owner;

        // Ignore the call if it was called on a non-view.
        ClassNode ownerClass = context.getDriver().findClass(context, owner, 0);
        if(ownerClass == null
                || !context.getDriver().isSubclassOf(ownerClass, ANDROID_VIEW_VIEW)) {
            return;
        }

        MethodNode performClick = findMethod(ownerClass.methods, PERFORM_CLICK, PERFORM_CLICK_SIG);
        //noinspection VariableNotUsedInsideIf
        if (performClick == null) {
            String message = String.format(
                    "Custom view `%1$s` has `setOnTouchListener` called on it but does not "
                            + "override `performClick`", ownerClass.name);
            context.report(ISSUE, method, call, context.getLocation(call), message);
        }
    }

    @SuppressWarnings("unchecked") // ASM API
    private static void checkOnTouchListener(ClassContext context, ClassNode classNode) {
        MethodNode onTouchNode =
            findMethod(
                    classNode.methods,
                    ON_TOUCH,
                    ON_TOUCH_SIG);
        if (onTouchNode != null) {
            AbstractInsnNode performClickInsnNode = findMethodCallInstruction(
                    onTouchNode.instructions,
                    ANDROID_VIEW_VIEW,
                    PERFORM_CLICK,
                    PERFORM_CLICK_SIG);
            if (performClickInsnNode == null) {
                String message = String.format(
                        "`%1$s#onTouch` should call `View#performClick` when a click is detected",
                        classNode.name);
                context.report(
                        ISSUE,
                        onTouchNode,
                        null,
                        context.getLocation(onTouchNode, classNode),
                        message);
            }
        }
    }

    @SuppressWarnings("unchecked") // ASM API
    private static void checkView(ClassContext context, ClassNode classNode) {
        MethodNode onTouchEvent = findMethod(classNode.methods, ON_TOUCH_EVENT, ON_TOUCH_EVENT_SIG);
        MethodNode performClick = findMethod(classNode.methods, PERFORM_CLICK, PERFORM_CLICK_SIG);

        // Check if we override onTouchEvent.
        if (onTouchEvent != null) {
            // Ensure that we also override performClick.
            //noinspection VariableNotUsedInsideIf
            if (performClick == null) {
                String message = String.format(
                        "Custom view `%1$s` overrides `onTouchEvent` but not `performClick`",
                        classNode.name);
                context.report(ISSUE, onTouchEvent, null,
                        context.getLocation(onTouchEvent, classNode), message);
            } else {
                // If we override performClick, ensure that it is called inside onTouchEvent.
                AbstractInsnNode performClickInOnTouchEventInsnNode = findMethodCallInstruction(
                        onTouchEvent.instructions,
                        classNode.name,
                        PERFORM_CLICK,
                        PERFORM_CLICK_SIG);
                if (performClickInOnTouchEventInsnNode == null) {
                    String message = String.format(
                            "`%1$s#onTouchEvent` should call `%1$s#performClick` when a click is detected",
                            classNode.name);
                    context.report(ISSUE, onTouchEvent, null,
                            context.getLocation(onTouchEvent, classNode), message);
                }
            }
        }

        // Ensure that, if performClick is implemented, performClick calls super.performClick.
        if (performClick != null) {
            AbstractInsnNode superPerformClickInPerformClickInsnNode = findMethodCallInstruction(
                    performClick.instructions,
                    classNode.superName,
                    PERFORM_CLICK,
                    PERFORM_CLICK_SIG);
            if (superPerformClickInPerformClickInsnNode == null) {
                String message = String.format(
                        "`%1$s#performClick` should call `super#performClick`",
                        classNode.name);
                context.report(ISSUE, performClick, null,
                        context.getLocation(performClick, classNode), message);
            }
        }
    }

    @Nullable
    private static MethodNode findMethod(
            @NonNull List<MethodNode> methods,
            @NonNull String name,
            @NonNull String desc) {
        for (MethodNode method : methods) {
            if (name.equals(method.name)
                    && desc.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked") // ASM API
    @Nullable
    private static AbstractInsnNode findMethodCallInstruction(
            @NonNull InsnList instructions,
            @NonNull String owner,
            @NonNull String name,
            @NonNull String desc) {
        ListIterator<AbstractInsnNode> iterator = instructions.iterator();

        while (iterator.hasNext()) {
            AbstractInsnNode insnNode = iterator.next();
            if (insnNode.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                if ((methodInsnNode.owner.equals(owner))
                        && (methodInsnNode.name.equals(name))
                        && (methodInsnNode.desc.equals(desc))) {
                    return methodInsnNode;
                }
            }
        }

        return null;
    }

    private static boolean implementsOnTouchListener(ClassNode classNode) {
        return (classNode.interfaces != null) && (classNode.interfaces.contains(ON_TOUCH_LISTENER));
    }
}
