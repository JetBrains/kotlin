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

import static com.android.SdkConstants.ANDROID_APP_ACTIVITY;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.checks.ControlFlowGraph.Node;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.ClassScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

import java.util.Arrays;
import java.util.List;

/**
 * Checks for problems with wakelocks (such as failing to release them)
 * which can lead to unnecessary battery usage.
 */
public class WakelockDetector extends Detector implements ClassScanner {

    /** Problems using wakelocks */
    public static final Issue ISSUE = Issue.create(
            "Wakelock", //$NON-NLS-1$
            "Incorrect `WakeLock` usage",

            "Failing to release a wakelock properly can keep the Android device in " +
            "a high power mode, which reduces battery life. There are several causes " +
            "of this, such as releasing the wake lock in `onDestroy()` instead of in " +
            "`onPause()`, failing to call `release()` in all possible code paths after " +
            "an `acquire()`, and so on.\n" +
            "\n" +
            "NOTE: If you are using the lock just to keep the screen on, you should " +
            "strongly consider using `FLAG_KEEP_SCREEN_ON` instead. This window flag " +
            "will be correctly managed by the platform as the user moves between " +
            "applications and doesn't require a special permission. See " +
            "http://developer.android.com/reference/android/view/WindowManager.LayoutParams.html#FLAG_KEEP_SCREEN_ON.",

            Category.PERFORMANCE,
            9,
            Severity.WARNING,
            new Implementation(
                    WakelockDetector.class,
                    Scope.CLASS_FILE_SCOPE));

    private static final String WAKELOCK_OWNER = "android/os/PowerManager$WakeLock"; //$NON-NLS-1$
    private static final String RELEASE_METHOD = "release"; //$NON-NLS-1$
    private static final String ACQUIRE_METHOD = "acquire"; //$NON-NLS-1$
    private static final String IS_HELD_METHOD = "isHeld"; //$NON-NLS-1$
    private static final String POWER_MANAGER = "android/os/PowerManager"; //$NON-NLS-1$
    private static final String NEW_WAKE_LOCK_METHOD = "newWakeLock"; //$NON-NLS-1$

    /** Print diagnostics during analysis (display flow control graph etc).
     * Make sure you add the asm-debug or asm-util jars to the runtime classpath
     * as well since the opcode integer to string mapping display routine looks for
     * it via reflection. */
    private static final boolean DEBUG = false;

    /** Constructs a new {@link WakelockDetector} */
    public WakelockDetector() {
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mHasAcquire && !mHasRelease && context.getDriver().getPhase() == 1) {
            // Gather positions of the acquire calls
            context.getDriver().requestRepeat(this, Scope.CLASS_FILE_SCOPE);
        }
    }

    // ---- Implements ClassScanner ----

    /** Whether any {@code acquire()} calls have been encountered */
    private boolean mHasAcquire;

    /** Whether any {@code release()} calls have been encountered */
    private boolean mHasRelease;

    @Override
    @Nullable
    public List<String> getApplicableCallNames() {
        return Arrays.asList(ACQUIRE_METHOD, RELEASE_METHOD, NEW_WAKE_LOCK_METHOD);
    }

    @Override
    public void checkCall(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull MethodInsnNode call) {
        if (!context.getProject().getReportIssues()) {
            // If this is a library project not being analyzed, ignore it
            return;
        }

        if (call.owner.equals(WAKELOCK_OWNER)) {
            String name = call.name;
            if (name.equals(ACQUIRE_METHOD)) {
                if (call.desc.equals("(J)V")) { // acquire(long timeout) does not require a corresponding release
                    return;
                }
                mHasAcquire = true;

                if (context.getDriver().getPhase() == 2) {
                    assert !mHasRelease;
                    context.report(ISSUE, method, call, context.getLocation(call),
                        "Found a wakelock `acquire()` but no `release()` calls anywhere");
                } else {
                    assert context.getDriver().getPhase() == 1;
                    // Perform flow analysis in this method to see if we're
                    // performing an acquire/release block, where there are code paths
                    // between the acquire and release which can result in the
                    // release call not getting reached.
                    checkFlow(context, classNode, method, call);
                }
            } else if (name.equals(RELEASE_METHOD)) {
                mHasRelease = true;

                // See if the release is happening in an onDestroy method, in an
                // activity.
                if ("onDestroy".equals(method.name) //$NON-NLS-1$
                        && context.getDriver().isSubclassOf(
                                classNode, ANDROID_APP_ACTIVITY)) {
                    context.report(ISSUE, method, call, context.getLocation(call),
                        "Wakelocks should be released in `onPause`, not `onDestroy`");
                }
            }
        } else if (call.owner.equals(POWER_MANAGER)) {
            if (call.name.equals(NEW_WAKE_LOCK_METHOD)) {
                AbstractInsnNode prev = LintUtils.getPrevInstruction(call);
                if (prev == null) {
                    return;
                }
                prev = LintUtils.getPrevInstruction(prev);
                if (prev == null || prev.getOpcode() != Opcodes.LDC) {
                    return;
                }
                LdcInsnNode ldc = (LdcInsnNode) prev;
                Object constant = ldc.cst;
                if (constant instanceof Integer) {
                    int flag = ((Integer) constant).intValue();
                    // Constant values are copied into the bytecode so we have to compare
                    // values; however, that means the values are part of the API
                    final int PARTIAL_WAKE_LOCK = 0x00000001;
                    final int ACQUIRE_CAUSES_WAKEUP = 0x10000000;
                    final int both = PARTIAL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP;
                    if ((flag & both) == both) {
                        context.report(ISSUE, method, call, context.getLocation(call),
                                "Should not set both `PARTIAL_WAKE_LOCK` and `ACQUIRE_CAUSES_WAKEUP`. "
                                        + "If you do not want the screen to turn on, get rid of "
                                        + "`ACQUIRE_CAUSES_WAKEUP`");
                    }
                }

            }
        }
    }

    private static void checkFlow(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull MethodInsnNode acquire) {
        // Track allocations such that we know whether the type of the call
        // is on a SecureRandom rather than a Random
        final InsnList instructions = method.instructions;
        MethodInsnNode release = null;

        // Find release call
        for (int i = 0, n = instructions.size(); i < n; i++) {
            AbstractInsnNode instruction = instructions.get(i);
            int type = instruction.getType();
            if (type == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.name.equals(RELEASE_METHOD) &&
                        call.owner.equals(WAKELOCK_OWNER)) {
                    release = call;
                    break;
                }
            }
        }

        if (release == null) {
            // Didn't find both acquire and release in this method; no point in doing
            // local flow analysis
            return;
        }

        try {
            MyGraph graph = new MyGraph();
            ControlFlowGraph.create(graph, classNode, method);

            if (DEBUG) {
                // Requires util package
                //ClassNode clazz = classNode;
                //clazz.accept(new TraceClassVisitor(new PrintWriter(System.out)));
                System.out.println(graph.toString(graph.getNode(acquire)));
            }

            int status = dfs(graph.getNode(acquire));
            if ((status & SEEN_RETURN) != 0) {
                String message;
                if ((status & SEEN_EXCEPTION) != 0) {
                    message = "The `release()` call is not always reached (via exceptional flow)";
                } else {
                    message = "The `release()` call is not always reached";
                }

                context.report(ISSUE, method, acquire,
                        context.getLocation(release), message);
            }
        } catch (AnalyzerException e) {
            context.log(e, null);
        }
    }

    private static final int SEEN_TARGET = 1;
    private static final int SEEN_BRANCH = 2;
    private static final int SEEN_EXCEPTION = 4;
    private static final int SEEN_RETURN = 8;

    /** TODO RENAME */
    private static class MyGraph extends ControlFlowGraph {
        @Override
        protected void add(@NonNull AbstractInsnNode from, @NonNull AbstractInsnNode to) {
            if (from.getOpcode() == Opcodes.IFNULL) {
                JumpInsnNode jump = (JumpInsnNode) from;
                if (jump.label == to) {
                    // Skip jump targets on null if it's surrounding the release call
                    //
                    //  if (lock != null) {
                    //      lock.release();
                    //  }
                    //
                    // The above shouldn't be considered a scenario where release() may not
                    // be called.
                    AbstractInsnNode next = LintUtils.getNextInstruction(from);
                    if (next != null && next.getType() == AbstractInsnNode.VAR_INSN) {
                        next = LintUtils.getNextInstruction(next);
                        if (next != null && next.getType() == AbstractInsnNode.METHOD_INSN) {
                            MethodInsnNode method = (MethodInsnNode) next;
                            if (method.name.equals(RELEASE_METHOD) &&
                                    method.owner.equals(WAKELOCK_OWNER)) {
                                // This isn't entirely correct; this will also trigger
                                // for "if (lock == null) { lock.release(); }" but that's
                                // not likely (and caught by other null checking in tools)
                                return;
                            }
                        }
                    }
                }
            } else if (from.getOpcode() == Opcodes.IFEQ) {
                JumpInsnNode jump = (JumpInsnNode) from;
                if (jump.label == to) {
                    AbstractInsnNode prev = LintUtils.getPrevInstruction(from);
                    if (prev != null && prev.getType() == AbstractInsnNode.METHOD_INSN) {
                        MethodInsnNode method = (MethodInsnNode) prev;
                        if (method.name.equals(IS_HELD_METHOD) &&
                                method.owner.equals(WAKELOCK_OWNER)) {
                            AbstractInsnNode next = LintUtils.getNextInstruction(from);
                            if (next != null) {
                                super.add(from, next);
                                return;
                            }
                        }
                    }
                }
            }

            super.add(from, to);
        }
    }

    /** Search from the given node towards the target; return false if we reach
     * an exit point such as a return or a call on the way there that is not within
     * a try/catch clause.
     *
     * @param node the current node
     * @return true if the target was reached
     *    XXX RETURN VALUES ARE WRONG AS OF RIGHT NOW
     */
    protected static int dfs(ControlFlowGraph.Node node) {
        AbstractInsnNode instruction = node.instruction;
        if (instruction.getType() == AbstractInsnNode.JUMP_INSN) {
            int opcode = instruction.getOpcode();
            if (opcode == Opcodes.RETURN || opcode == Opcodes.ARETURN
                    || opcode == Opcodes.LRETURN || opcode == Opcodes.IRETURN
                    || opcode == Opcodes.DRETURN || opcode == Opcodes.FRETURN
                    || opcode == Opcodes.ATHROW) {
                if (DEBUG) {
                    System.out.println("Found exit via explicit return: " //$NON-NLS-1$
                            + node.toString(false));
                }
                return SEEN_RETURN;
            }
        }

        if (!DEBUG) {
            // There are no cycles, so no *NEED* for this, though it does avoid
            // researching shared labels. However, it makes debugging harder (no re-entry)
            // so this is only done when debugging is off
            if (node.visit != 0) {
                return 0;
            }
            node.visit = 1;
        }

        // Look for the target. This is any method call node which is a release on the
        // lock (later also check it's the same instance, though that's harder).
        // This is because finally blocks tend to be inlined so from a single try/catch/finally
        // with a release() in the finally, the bytecode can contain multiple repeated
        // (inlined) release() calls.
        if (instruction.getType() == AbstractInsnNode.METHOD_INSN) {
            MethodInsnNode method = (MethodInsnNode) instruction;
            if (method.name.equals(RELEASE_METHOD) && method.owner.equals(WAKELOCK_OWNER)) {
                return SEEN_TARGET;
            } else if (method.name.equals(ACQUIRE_METHOD) && method.owner.equals(WAKELOCK_OWNER)) {
                // OK
            } else if (method.name.equals(IS_HELD_METHOD) && method.owner.equals(WAKELOCK_OWNER)) {
                // OK
            } else {
                // Some non acquire/release method call: if this is not associated with a
                // try-catch block, it would mean the exception would exit the method,
                // which would be an error
                if (node.exceptions == null || node.exceptions.isEmpty()) {
                    // Look up the corresponding frame, if any
                    AbstractInsnNode curr = method.getPrevious();
                    boolean foundFrame = false;
                    while (curr != null) {
                        if (curr.getType() == AbstractInsnNode.FRAME) {
                            foundFrame = true;
                            break;
                        }
                        curr = curr.getPrevious();
                    }

                    if (!foundFrame) {
                        if (DEBUG) {
                            System.out.println("Found exit via unguarded method call: " //$NON-NLS-1$
                                    + node.toString(false));
                        }
                        return SEEN_RETURN;
                    }
                }
            }
        }

        // if (node.instruction is a call, and the call is not caught by
        // a try/catch block (provided the release is not inside the try/catch block)
        // then return false
        int status = 0;

        boolean implicitReturn = true;
        List<Node> successors = node.successors;
        List<Node> exceptions = node.exceptions;
        if (exceptions != null) {
            if (!exceptions.isEmpty()) {
                implicitReturn = false;
            }
            for (Node successor : exceptions) {
                status = dfs(successor) | status;
                if ((status & SEEN_RETURN) != 0) {
                    if (DEBUG) {
                        System.out.println("Found exit via exception: " //$NON-NLS-1$
                                + node.toString(false));
                    }
                    return status;
                }
            }

            if (status != 0) {
                status |= SEEN_EXCEPTION;
            }
        }

        if (successors != null) {
            if (!successors.isEmpty()) {
                implicitReturn = false;
                if (successors.size() > 1) {
                    status |= SEEN_BRANCH;
                }
            }
            for (Node successor : successors) {
                status = dfs(successor) | status;
                if ((status & SEEN_RETURN) != 0) {
                    if (DEBUG) {
                        System.out.println("Found exit via branches: " //$NON-NLS-1$
                                + node.toString(false));
                    }
                    return status;
                }
            }
        }

        if (implicitReturn) {
            status |= SEEN_RETURN;
            if (DEBUG) {
                System.out.println("Found exit: via implicit return: " //$NON-NLS-1$
                        + node.toString(false));
            }
        }

        return status;
    }
}
