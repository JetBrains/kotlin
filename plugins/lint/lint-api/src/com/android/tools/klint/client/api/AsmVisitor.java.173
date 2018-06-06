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

package com.android.tools.klint.client.api;

import com.android.annotations.NonNull;
import com.android.tools.klint.detector.api.ClassContext;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Detector.ClassScanner;
import com.google.common.annotations.Beta;

import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.InsnList;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specialized visitor for running detectors on a class object model.
 * <p>
 * It operates in two phases:
 * <ol>
 *   <li> First, it computes a set of maps where it generates a map from each
 *        significant method name to a list of detectors to consult for that method
 *        name. The set of method names that a detector is interested in is provided
 *        by the detectors themselves.
 *   <li> Second, it iterates over the DOM a single time. For each method call it finds,
 *        it dispatches to any check that has registered interest in that method name.
 *   <li> Finally, it runs a full check on those class scanners that do not register
 *        specific method names to be checked. This is intended for those detectors
 *        that do custom work, not related specifically to method calls.
 * </ol>
 * It also notifies all the detectors before and after the document is processed
 * such that they can do pre- and post-processing.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
class AsmVisitor {
    /**
     * Number of distinct node types specified in {@link AbstractInsnNode}. Sadly
     * there isn't a max-constant there, so update this along with ASM library
     * updates.
     */
    private static final int TYPE_COUNT = AbstractInsnNode.LINE + 1;
    private final Map<String, List<ClassScanner>> mMethodNameToChecks =
            new HashMap<String, List<ClassScanner>>();
    private final Map<String, List<ClassScanner>> mMethodOwnerToChecks =
            new HashMap<String, List<ClassScanner>>();
    private final List<Detector> mFullClassChecks = new ArrayList<Detector>();

    private final List<? extends Detector> mAllDetectors;
    private List<ClassScanner>[] mNodeTypeDetectors;

    // Really want this:
    //<T extends List<Detector> & Detector.ClassScanner> ClassVisitor(T xmlDetectors) {
    // but it makes client code tricky and ugly.
    @SuppressWarnings("unchecked")
    AsmVisitor(@NonNull LintClient client, @NonNull List<? extends Detector> classDetectors) {
        mAllDetectors = classDetectors;

        // TODO: Check appliesTo() for files, and find a quick way to enable/disable
        // rules when running through a full project!
        for (Detector detector : classDetectors) {
            Detector.ClassScanner scanner = (Detector.ClassScanner) detector;

            boolean checkFullClass = true;

            Collection<String> names = scanner.getApplicableCallNames();
            if (names != null) {
                checkFullClass = false;
                for (String element : names) {
                    List<Detector.ClassScanner> list = mMethodNameToChecks.get(element);
                    if (list == null) {
                        list = new ArrayList<Detector.ClassScanner>();
                        mMethodNameToChecks.put(element, list);
                    }
                    list.add(scanner);
                }
            }

            Collection<String> owners = scanner.getApplicableCallOwners();
            if (owners != null) {
                checkFullClass = false;
                for (String element : owners) {
                    List<Detector.ClassScanner> list = mMethodOwnerToChecks.get(element);
                    if (list == null) {
                        list = new ArrayList<Detector.ClassScanner>();
                        mMethodOwnerToChecks.put(element, list);
                    }
                    list.add(scanner);
                }
            }

            int[] types = scanner.getApplicableAsmNodeTypes();
            if (types != null) {
                checkFullClass = false;
                for (int type : types) {
                    if (type < 0 || type >= TYPE_COUNT) {
                        // Can't support this node type: looks like ASM wasn't updated correctly.
                        client.log(null, "Out of range node type %1$d from detector %2$s",
                                type, scanner);
                        continue;
                    }
                    if (mNodeTypeDetectors == null) {
                        mNodeTypeDetectors = new List[TYPE_COUNT];
                    }
                    List<ClassScanner> checks = mNodeTypeDetectors[type];
                    if (checks == null) {
                        checks = new ArrayList<ClassScanner>();
                        mNodeTypeDetectors[type] = checks;
                    }
                    checks.add(scanner);
                }
            }

            if (checkFullClass) {
                mFullClassChecks.add(detector);
            }
        }
    }

    @SuppressWarnings("rawtypes") // ASM API uses raw types
    void runClassDetectors(ClassContext context) {
        ClassNode classNode = context.getClassNode();

        for (Detector detector : mAllDetectors) {
            detector.beforeCheckFile(context);
        }

        for (Detector detector : mFullClassChecks) {
            Detector.ClassScanner scanner = (Detector.ClassScanner) detector;
            scanner.checkClass(context, classNode);
            detector.afterCheckFile(context);
        }

        if (!mMethodNameToChecks.isEmpty() || !mMethodOwnerToChecks.isEmpty() ||
                mNodeTypeDetectors != null && mNodeTypeDetectors.length > 0) {
            List methodList = classNode.methods;
            for (Object m : methodList) {
                MethodNode method = (MethodNode) m;
                InsnList nodes = method.instructions;
                for (int i = 0, n = nodes.size(); i < n; i++) {
                    AbstractInsnNode instruction = nodes.get(i);
                    int type = instruction.getType();
                    if (type == AbstractInsnNode.METHOD_INSN) {
                        MethodInsnNode call = (MethodInsnNode) instruction;

                        String owner = call.owner;
                        List<ClassScanner> scanners = mMethodOwnerToChecks.get(owner);
                        if (scanners != null) {
                            for (ClassScanner scanner : scanners) {
                                scanner.checkCall(context, classNode, method, call);
                            }
                        }

                        String name = call.name;
                        scanners = mMethodNameToChecks.get(name);
                        if (scanners != null) {
                            for (ClassScanner scanner : scanners) {
                                scanner.checkCall(context, classNode, method, call);
                            }
                        }
                    }

                    if (mNodeTypeDetectors != null && type < mNodeTypeDetectors.length) {
                        List<ClassScanner> scanners = mNodeTypeDetectors[type];
                        if (scanners != null) {
                            for (ClassScanner scanner : scanners) {
                                scanner.checkInstruction(context, classNode, method, instruction);
                            }
                        }
                    }
                }
            }
        }

        for (Detector detector : mAllDetectors) {
            detector.afterCheckFile(context);
        }
    }
}
