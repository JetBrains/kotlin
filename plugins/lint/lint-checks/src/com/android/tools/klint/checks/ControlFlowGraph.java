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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import org.objectweb.asm.util.Printer;

/**
 * A {@linkplain ControlFlowGraph} is a graph containing a node for each
 * instruction in a method, and an edge for each possible control flow; usually
 * just "next" for the instruction following the current instruction, but in the
 * case of a branch such as an "if", multiple edges to each successive location,
 * or with a "goto", a single edge to the jumped-to instruction.
 * <p>
 * It also adds edges for abnormal control flow, such as the possibility of a
 * method call throwing a runtime exception.
 */
public class ControlFlowGraph {
    /** Map from instructions to nodes */
    private Map<AbstractInsnNode, Node> mNodeMap;
    private MethodNode mMethod;

    /**
     * Creates a new {@link ControlFlowGraph} and populates it with the flow
     * control for the given method. If the optional {@code initial} parameter is
     * provided with an existing graph, then the graph is simply populated, not
     * created. This allows subclassing of the graph instance, if necessary.
     *
     * @param initial usually null, but can point to an existing instance of a
     *            {@link ControlFlowGraph} in which that graph is reused (but
     *            populated with new edges)
     * @param classNode the class containing the method to be analyzed
     * @param method the method to be analyzed
     * @return a {@link ControlFlowGraph} with nodes for the control flow in the
     *         given method
     * @throws AnalyzerException if the underlying bytecode library is unable to
     *             analyze the method bytecode
     */
    @NonNull
    public static ControlFlowGraph create(
            @Nullable ControlFlowGraph initial,
            @NonNull ClassNode classNode,
            @NonNull MethodNode method) throws AnalyzerException {
        final ControlFlowGraph graph = initial != null ? initial : new ControlFlowGraph();
        final InsnList instructions = method.instructions;
        graph.mNodeMap = Maps.newHashMapWithExpectedSize(instructions.size());
        graph.mMethod = method;

        // Create a flow control graph using ASM5's analyzer. According to the ASM 4 guide
        // (download.forge.objectweb.org/asm/asm4-guide.pdf) there are faster ways to construct
        // it, but those require a lot more code.
        Analyzer analyzer = new Analyzer(new BasicInterpreter()) {
            @Override
            protected void newControlFlowEdge(int insn, int successor) {
                // Update the information as of whether the this object has been
                // initialized at the given instruction.
                AbstractInsnNode from = instructions.get(insn);
                AbstractInsnNode to = instructions.get(successor);
                graph.add(from, to);
            }

            @Override
            protected boolean newControlFlowExceptionEdge(int insn, TryCatchBlockNode tcb) {
                AbstractInsnNode from = instructions.get(insn);
                graph.exception(from, tcb);
                return super.newControlFlowExceptionEdge(insn, tcb);
            }

            @Override
            protected boolean newControlFlowExceptionEdge(int insn, int successor) {
                AbstractInsnNode from = instructions.get(insn);
                AbstractInsnNode to = instructions.get(successor);
                graph.exception(from, to);
                return super.newControlFlowExceptionEdge(insn, successor);
            }
        };

        analyzer.analyze(classNode.name, method);
        return graph;
    }

    /**
     * Checks whether there is a path from the given source node to the given
     * destination node
     */
    @SuppressWarnings("MethodMayBeStatic")
    private boolean isConnected(@NonNull Node from,
            @NonNull Node to, @NonNull Set<Node> seen) {
        if (from == to) {
            return true;
        } else if (seen.contains(from)) {
            return false;
        }
        seen.add(from);

        List<Node> successors = from.successors;
        List<Node> exceptions = from.exceptions;
        if (exceptions != null) {
            for (Node successor : exceptions) {
                if (isConnected(successor, to, seen)) {
                    return true;
                }
            }
        }

        if (successors != null) {
            for (Node successor : successors) {
                if (isConnected(successor, to, seen)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks whether there is a path from the given source node to the given
     * destination node
     */
    public boolean isConnected(@NonNull Node from, @NonNull Node to) {
        return isConnected(from, to, Sets.<Node>newIdentityHashSet());
    }

    /**
     * Checks whether there is a path from the given instruction to the given
     * instruction node
     */
    public boolean isConnected(@NonNull AbstractInsnNode from, @NonNull AbstractInsnNode to) {
        return isConnected(getNode(from), getNode(to));
    }

    /** A {@link Node} is a node in the control flow graph for a method, pointing to
     * the instruction and its possible successors */
    public static class Node {
        /** The instruction */
        public final AbstractInsnNode instruction;
        /** Any normal successors (e.g. following instruction, or goto or conditional flow) */
        public final List<Node> successors = new ArrayList<Node>(2);
        /** Any abnormal successors (e.g. the handler to go to following an exception) */
        public final List<Node> exceptions = new ArrayList<Node>(1);

        /** A tag for use during depth-first-search iteration of the graph etc */
        public int visit;

        /**
         * Constructs a new control graph node
         *
         * @param instruction the instruction to associate with this node
         */
        public Node(@NonNull AbstractInsnNode instruction) {
            this.instruction = instruction;
        }

        void addSuccessor(@NonNull Node node) {
            if (!successors.contains(node)) {
                successors.add(node);
            }
        }

        void addExceptionPath(@NonNull Node node) {
            if (!exceptions.contains(node)) {
                exceptions.add(node);
            }
        }

        /**
         * Represents this instruction as a string, for debugging purposes
         *
         * @param includeAdjacent whether it should include a display of
         *            adjacent nodes as well
         * @return a string representation
         */
        @NonNull
        public String toString(boolean includeAdjacent) {
            StringBuilder sb = new StringBuilder(100);

            sb.append(getId(instruction));
            sb.append(':');

            if (instruction instanceof LabelNode) {
                //LabelNode l = (LabelNode) instruction;
                //sb.append('L' + l.getLabel().getOffset() + ":");
                //sb.append('L' + l.getLabel().info + ":");
                sb.append("LABEL");
            } else if (instruction instanceof LineNumberNode) {
                sb.append("LINENUMBER ").append(((LineNumberNode)instruction).line);
            } else if (instruction instanceof FrameNode) {
                sb.append("FRAME");
            } else {
                int opcode = instruction.getOpcode();
                String opcodeName = getOpcodeName(opcode);
                sb.append(opcodeName);
                if (instruction.getType() == AbstractInsnNode.METHOD_INSN) {
                    sb.append('(').append(((MethodInsnNode)instruction).name).append(')');
                }
            }

            if (includeAdjacent) {
                if (successors != null && !successors.isEmpty()) {
                    sb.append(" Next:");
                    for (Node successor : successors) {
                        sb.append(' ');
                        sb.append(successor.toString(false));
                    }
                }

                if (exceptions != null && !exceptions.isEmpty()) {
                    sb.append(" Exceptions:");
                    for (Node exception : exceptions) {
                        sb.append(' ');
                        sb.append(exception.toString(false));
                    }
                }
                sb.append('\n');
            }

            return sb.toString();
        }
    }

    /** Adds an exception flow to this graph */
    protected void add(@NonNull AbstractInsnNode from, @NonNull AbstractInsnNode to) {
        getNode(from).addSuccessor(getNode(to));
    }

    /** Adds an exception flow to this graph */
    protected void exception(@NonNull AbstractInsnNode from, @NonNull AbstractInsnNode to) {
        // For now, these edges appear useless; we also get more specific
        // information via the TryCatchBlockNode which we use instead.
        //getNode(from).addExceptionPath(getNode(to));
    }

    /** Adds an exception try block node to this graph */
    protected void exception(@NonNull AbstractInsnNode from, @NonNull TryCatchBlockNode tcb) {
        // Add tcb's to all instructions in the range
        LabelNode start = tcb.start;
        LabelNode end = tcb.end; // exclusive

        // Add exception edges for all method calls in the range
        AbstractInsnNode curr = start;
        Node handlerNode = getNode(tcb.handler);
        while (curr != end && curr != null) {
            if (curr.getType() == AbstractInsnNode.METHOD_INSN) {
                // Method call; add exception edge to handler
                if (tcb.type == null) {
                    // finally block: not an exception path
                    getNode(curr).addSuccessor(handlerNode);
                }
                getNode(curr).addExceptionPath(handlerNode);
            }
            curr = curr.getNext();
        }
    }

    /**
     * Looks up (and if necessary) creates a graph node for the given instruction
     *
     * @param instruction the instruction
     * @return the control flow graph node corresponding to the given
     *         instruction
     */
    @NonNull
    public Node getNode(@NonNull AbstractInsnNode instruction) {
        Node node = mNodeMap.get(instruction);
        if (node == null) {
            node = new Node(instruction);
            mNodeMap.put(instruction, node);
        }

        return node;
    }

    /**
     * Creates a human readable version of the graph
     *
     * @param start the starting instruction, or null if not known or to use the
     *            first instruction
     * @return a string version of the graph
     */
    @NonNull
    public String toString(@Nullable Node start) {
        StringBuilder sb = new StringBuilder(400);

        AbstractInsnNode curr;
        if (start != null) {
            curr = start.instruction;
        } else {
            if (mNodeMap.isEmpty()) {
                return "<empty>";
            } else {
                curr = mNodeMap.keySet().iterator().next();
                while (curr.getPrevious() != null) {
                    curr = curr.getPrevious();
                }
            }
        }

        while (curr != null) {
            Node node = mNodeMap.get(curr);
            if (node != null) {
                sb.append(node.toString(true));
            }
            curr = curr.getNext();
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return toString(null);
    }

    // ---- For debugging only ----

    private static Map<Object, String> sIds = null;
    private static int sNextId = 1;
    private static String getId(Object object) {
        if (sIds == null) {
            sIds = Maps.newHashMap();
        }
        String id = sIds.get(object);
        if (id == null) {
            id = Integer.toString(sNextId++);
            sIds.put(object, id);
        }
        return id;
    }

    /**
     * Generates dot output of the graph. This can be used with
     * graphwiz to visualize the graph. For example, if you
     * save the output as graph1.gv you can run
     * <pre>
     * $ dot -Tps graph1.gv -o graph1.ps
     * </pre>
     * to generate a postscript file, which you can then view
     * with "gv graph1.ps".
     *
     * (There are also some online web sites where you can
     * paste in dot graphs and see the visualization right
     * there in the browser.)
     *
     * @return a dot description of this control flow graph,
     *    useful for debugging
     */
    public String toDot(@Nullable Set<Node> highlight) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");


        AbstractInsnNode instruction = mMethod.instructions.getFirst();

        // Special start node
        sb.append("  start -> ").append(getId(mNodeMap.get(instruction))).append(";\n");
        sb.append("  start [shape=plaintext];\n");

        while (instruction != null) {
            Node node = mNodeMap.get(instruction);
            if (node != null) {
                if (node.successors != null) {
                    for (Node to : node.successors) {
                        sb.append("  ").append(getId(node)).append(" -> ").append(getId(to));
                        if (node.instruction instanceof JumpInsnNode) {
                            sb.append(" [label=\"");
                            if (((JumpInsnNode)node.instruction).label == to.instruction) {
                                sb.append("yes");
                            } else {
                                sb.append("no");
                            }
                            sb.append("\"]");
                        }
                        sb.append(";\n");
                    }
                }
                if (node.exceptions != null) {
                    for (Node to : node.exceptions) {
                        sb.append(getId(node)).append(" -> ").append(getId(to));
                        sb.append(" [label=\"exception\"];\n");
                    }
                }
            }

            instruction = instruction.getNext();
        }


        // Labels
        sb.append("\n");
        for (Node node : mNodeMap.values()) {
            instruction = node.instruction;
            sb.append("  ").append(getId(node)).append(" ");
            sb.append("[label=\"").append(dotDescribe(node)).append("\"");
            if (highlight != null && highlight.contains(node)) {
                sb.append(",shape=box,style=filled");
            } else if (instruction instanceof LineNumberNode ||
              instruction instanceof LabelNode ||
              instruction instanceof FrameNode) {
                sb.append(",shape=oval,style=dotted");
            } else {
                sb.append(",shape=box");
            }
            sb.append("];\n");
        }

        sb.append("}");
        return sb.toString();
    }

    private static String dotDescribe(Node node) {
        AbstractInsnNode instruction = node.instruction;
        if (instruction instanceof LabelNode) {
            return "Label";
        } else if (instruction instanceof LineNumberNode) {
            LineNumberNode lineNode = (LineNumberNode)instruction;
            return "Line " + lineNode.line;
        } else if (instruction instanceof FrameNode) {
            return "Stack Frame";
        } else if (instruction instanceof MethodInsnNode) {
            MethodInsnNode method = (MethodInsnNode)instruction;
            String cls = method.owner.substring(method.owner.lastIndexOf('/') + 1);
            cls = cls.replace('$','.');
            return "Call " + cls + "#" + method.name;
        } else if (instruction instanceof FieldInsnNode) {
            FieldInsnNode field = (FieldInsnNode) instruction;
            String cls = field.owner.substring(field.owner.lastIndexOf('/') + 1);
            cls = cls.replace('$','.');
            return "Field " + cls + "#" + field.name;
        } else if (instruction instanceof TypeInsnNode && instruction.getOpcode() == Opcodes.NEW) {
            return "New " + ((TypeInsnNode)instruction).desc;
        }
        StringBuilder sb = new StringBuilder();
        String opcodeName = getOpcodeName(instruction.getOpcode());
        sb.append(opcodeName);

        if (instruction instanceof IntInsnNode) {
            IntInsnNode in = (IntInsnNode) instruction;
            sb.append(" ").append(Integer.toString(in.operand));
        } else if (instruction instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) instruction;
            sb.append(" ");
            if (ldc.cst instanceof String) {
                sb.append("\\\"");
            }
            sb.append(ldc.cst);
            if (ldc.cst instanceof String) {
                sb.append("\\\"");
            }
        }
        return sb.toString();
    }

    private static String getOpcodeName(int opcode) {
        if (sOpcodeNames == null) {
            sOpcodeNames = new String[255];
            try {
                Field[] fields = Opcodes.class.getDeclaredFields();
                for (Field field : fields) {
                    if (field.getType() == int.class) {
                        String name = field.getName();
                        if (name.startsWith("ASM") || name.startsWith("V1_") ||
                            name.startsWith("ACC_") || name.startsWith("T_") ||
                            name.startsWith("H_") || name.startsWith("F_")) {
                            continue;
                        }
                        int val = field.getInt(null);
                        if (val >= 0 && val < sOpcodeNames.length) {
                            sOpcodeNames[val] = field.getName();
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (opcode >= 0 && opcode < sOpcodeNames.length) {
            String name = sOpcodeNames[opcode];
            if (name != null) {
                return name;
            }
        }

        return Integer.toString(opcode);
    }

    private static String[] sOpcodeNames;
}

