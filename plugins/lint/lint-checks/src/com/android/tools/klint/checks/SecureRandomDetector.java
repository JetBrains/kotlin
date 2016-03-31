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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.ClassScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.Collections;
import java.util.List;

/**
 * Checks for hardcoded seeds with random numbers.
 */
public class SecureRandomDetector extends Detector implements ClassScanner {
    /** Unregistered activities and services */
    public static final Issue ISSUE = Issue.create(
            "SecureRandom", //$NON-NLS-1$
            "Using a fixed seed with `SecureRandom`",

            "Specifying a fixed seed will cause the instance to return a predictable sequence " +
            "of numbers. This may be useful for testing but it is not appropriate for secure use.",

            Category.SECURITY,
            9,
            Severity.WARNING,
            new Implementation(
                    SecureRandomDetector.class,
                    Scope.CLASS_FILE_SCOPE))
            .addMoreInfo("http://developer.android.com/reference/java/security/SecureRandom.html");

    private static final String SET_SEED = "setSeed"; //$NON-NLS-1$
    static final String OWNER_SECURE_RANDOM = "java/security/SecureRandom"; //$NON-NLS-1$
    private static final String OWNER_RANDOM = "java/util/Random"; //$NON-NLS-1$
    private static final String VM_SECURE_RANDOM = 'L' + OWNER_SECURE_RANDOM + ';';
    /** Method description for a method that takes a long argument (no return type specified */
    private static final String LONG_ARG = "(J)"; //$NON-NLS-1$

    /** Constructs a new {@link SecureRandomDetector} */
    public SecureRandomDetector() {
    }

    // ---- Implements ClassScanner ----

    @Override
    @Nullable
    public List<String> getApplicableCallNames() {
        return Collections.singletonList(SET_SEED);
    }

    @Override
    public void checkCall(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull MethodInsnNode call) {
        String owner = call.owner;
        String desc = call.desc;
        if (owner.equals(OWNER_SECURE_RANDOM)) {
            if (desc.startsWith(LONG_ARG)) {
                checkValidSetSeed(context, call);
            } else if (desc.startsWith("([B)")) { //$NON-NLS-1$
                // setSeed(byte[]) ...
                // We could do some flow analysis here to see whether the byte array getting
                // passed in appears to be fixed.
                // However, people calling this constructor rather than the simpler one
                // with a fixed integer are probably less likely to make that mistake... right?
            }
        } else if (owner.equals(OWNER_RANDOM) && desc.startsWith(LONG_ARG)) {
            // Called setSeed(long) on an instanceof a Random object. Flag this if the instance
            // is likely a SecureRandom.

            // Track allocations such that we know whether the type of the call
            // is on a SecureRandom rather than a Random
            Analyzer analyzer = new Analyzer(new BasicInterpreter() {
                @Override
                public BasicValue newValue(Type type) {
                    if (type != null && type.getDescriptor().equals(VM_SECURE_RANDOM)) {
                        return new BasicValue(type);
                    }
                    return super.newValue(type);
                }
            });
            try {
                Frame[] frames = analyzer.analyze(classNode.name, method);
                InsnList instructions = method.instructions;
                Frame frame = frames[instructions.indexOf(call)];
                int stackSlot = frame.getStackSize();
                for (Type type : Type.getArgumentTypes(desc)) {
                    stackSlot -= type.getSize();
                }
                BasicValue stackValue = (BasicValue) frame.getStack(stackSlot);
                Type type = stackValue.getType();
                if (type != null && type.getDescriptor().equals(VM_SECURE_RANDOM)) {
                    checkValidSetSeed(context, call);
                }
            } catch (AnalyzerException e) {
                context.log(e, null);
            }
        } else if (owner.equals(OWNER_RANDOM) && desc.startsWith(LONG_ARG)) {
            // Called setSeed(long) on an instanceof a Random object. Flag this if the instance
            // is likely a SecureRandom.
            // TODO
        }
    }

    private static void checkValidSetSeed(ClassContext context, MethodInsnNode call) {
        assert call.name.equals(SET_SEED);

        // Make sure the argument passed is not a literal
        AbstractInsnNode prev = LintUtils.getPrevInstruction(call);
        if (prev == null) {
            return;
        }
        int opcode = prev.getOpcode();
        if (opcode == Opcodes.LCONST_0 || opcode == Opcodes.LCONST_1 || opcode == Opcodes.LDC) {
            context.report(ISSUE, context.getLocation(call),
                    "Do not call `setSeed()` on a `SecureRandom` with a fixed seed: " +
                    "it is not secure. Use `getSeed()`.");
        } else if (opcode == Opcodes.INVOKESTATIC) {
            String methodName = ((MethodInsnNode) prev).name;
            if (methodName.equals("currentTimeMillis") || methodName.equals("nanoTime")) {
                context.report(ISSUE, context.getLocation(call),
                        "It is dangerous to seed `SecureRandom` with the current time because " +
                        "that value is more predictable to an attacker than the default seed.");
            }
        }
    }
}
