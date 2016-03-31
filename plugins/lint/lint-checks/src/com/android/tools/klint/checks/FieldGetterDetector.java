/*
 * Copyright (C) 2011 The Android Open Source Project
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
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.google.common.collect.Maps;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Looks for getter calls within the same class that could be replaced by
 * direct field references instead.
 */
public class FieldGetterDetector extends Detector implements Detector.ClassScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "FieldGetter", //$NON-NLS-1$
            "Using getter instead of field",

            "Accessing a field within the class that defines a getter for that field is " +
            "at least 3 times faster than calling the getter. For simple getters that do " +
            "nothing other than return the field, you might want to just reference the " +
            "local field directly instead.\n" +
            "\n" +
            "*NOTE*: As of Android 2.3 (Gingerbread), this optimization is performed " +
            "automatically by Dalvik, so there is no need to change your code; this is " +
            "only relevant if you are targeting older versions of Android.",

            Category.PERFORMANCE,
            4,
            Severity.WARNING,
            new Implementation(
                    FieldGetterDetector.class,
                    Scope.CLASS_FILE_SCOPE)).
            // This is a micro-optimization: not enabled by default
            setEnabledByDefault(false).
            addMoreInfo(
            "http://developer.android.com/guide/practices/design/performance.html#internal_get_set"); //$NON-NLS-1$
    private ArrayList<Entry> mPendingCalls;

    /** Constructs a new {@link FieldGetterDetector} check */
    public FieldGetterDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements ClassScanner ----

    @Override
    public int[] getApplicableAsmNodeTypes() {
        return new int[] { AbstractInsnNode.METHOD_INSN };
    }

    @Override
    public void checkInstruction(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull AbstractInsnNode instruction) {
        // As of Gingerbread/API 9, Dalvik performs this optimization automatically
        if (context.getProject().getMinSdk() >= 9) {
            return;
        }

        if ((method.access & Opcodes.ACC_STATIC) != 0) {
            // Not an instance method
            return;
        }

        if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) {
            return;
        }

        MethodInsnNode node = (MethodInsnNode) instruction;
        String name = node.name;
        String owner = node.owner;

        AbstractInsnNode prev = LintUtils.getPrevInstruction(instruction);
        if (prev == null || prev.getOpcode() != Opcodes.ALOAD) {
            return;
        }
        VarInsnNode prevVar = (VarInsnNode) prev;
        if (prevVar.var != 0) { // Not on "this", variable 0 in instance methods?
            return;
        }

        if (((name.startsWith("get") && name.length() > 3     //$NON-NLS-1$
                && Character.isUpperCase(name.charAt(3)))
            || (name.startsWith("is") && name.length() > 2    //$NON-NLS-1$
                && Character.isUpperCase(name.charAt(2))))
                && owner.equals(classNode.name)) {
            // Calling a potential getter method on self. We now need to
            // investigate the method body of the getter call and make sure
            // it's really a plain getter, not just a method which happens
            // to have a method name like a getter, or a method which not
            // only returns a field but possibly computes it or performs
            // other initialization or side effects. This is done in a
            // second pass over the bytecode, initiated by the finish()
            // method.
            if (mPendingCalls == null) {
                mPendingCalls = new ArrayList<Entry>();
            }

            mPendingCalls.add(new Entry(name, node, method));
        }

        super.checkInstruction(context, classNode, method, instruction);
    }

    @Override
    public void afterCheckFile(@NonNull Context c) {
        ClassContext context = (ClassContext) c;

        if (mPendingCalls != null) {
            Set<String> names = new HashSet<String>(mPendingCalls.size());
            for (Entry entry : mPendingCalls) {
                names.add(entry.name);
            }

            Map<String, String> getters = checkMethods(context.getClassNode(), names);
            if (!getters.isEmpty()) {
                for (String getter : getters.keySet()) {
                    for (Entry entry : mPendingCalls) {
                        String name = entry.name;
                        // There can be more than one reference to the same name:
                        // one for each call site
                        if (name.equals(getter)) {
                            Location location = context.getLocation(entry.call);
                            String fieldName = getters.get(getter);
                            if (fieldName == null) {
                                fieldName = "";
                            }
                            context.report(ISSUE, entry.method, entry.call, location,
                                String.format(
                                "Calling getter method `%1$s()` on self is " +
                                "slower than field access (`%2$s`)", getter, fieldName));
                        }
                    }
                }
            }
        }

        mPendingCalls = null;
    }

    // Holder class for getters to be checked
    private static class Entry {
        public final String name;
        public final MethodNode method;
        public final MethodInsnNode call;

        public Entry(String name, MethodInsnNode call, MethodNode method) {
            super();
            this.name = name;
            this.call = call;
            this.method = method;
        }
    }

    // Validate that these getter methods are really just simple field getters
    // like these int and String getters:
    // public int getFoo();
    //   Code:
    //    0:   aload_0
    //    1:   getfield    #21; //Field mFoo:I
    //    4:   ireturn
    //
    // public java.lang.String getBar();
    //   Code:
    //    0:   aload_0
    //    1:   getfield    #25; //Field mBar:Ljava/lang/String;
    //    4:   areturn
    //
    // Returns a map of valid getters as keys, and if the field name is found, the field name
    // for each getter as its value.
    private static Map<String, String> checkMethods(ClassNode classNode, Set<String> names) {
        Map<String, String> validGetters = Maps.newHashMap();
        @SuppressWarnings("rawtypes")
        List methods = classNode.methods;
        String fieldName = null;
        checkMethod:
        for (Object methodObject : methods) {
            MethodNode method = (MethodNode) methodObject;
            if (names.contains(method.name)
                    && method.desc.startsWith("()")) { //$NON-NLS-1$ // (): No arguments
                InsnList instructions = method.instructions;
                int mState = 1;
                for (AbstractInsnNode curr = instructions.getFirst();
                        curr != null;
                        curr = curr.getNext()) {
                    switch (curr.getOpcode()) {
                        case -1:
                            // Skip label and line number nodes
                            continue;
                        case Opcodes.ALOAD:
                            if (mState == 1) {
                                fieldName = null;
                                mState = 2;
                            } else {
                                continue checkMethod;
                            }
                            break;
                        case Opcodes.GETFIELD:
                            if (mState == 2) {
                                FieldInsnNode field = (FieldInsnNode) curr;
                                fieldName = field.name;
                                mState = 3;
                            } else {
                                continue checkMethod;
                            }
                            break;
                        case Opcodes.ARETURN:
                        case Opcodes.FRETURN:
                        case Opcodes.IRETURN:
                        case Opcodes.DRETURN:
                        case Opcodes.LRETURN:
                        case Opcodes.RETURN:
                            if (mState == 3) {
                                validGetters.put(method.name, fieldName);
                            }
                            continue checkMethod;
                        default:
                            continue checkMethod;
                    }
                }
            }
        }

        return validGetters;
    }
}
