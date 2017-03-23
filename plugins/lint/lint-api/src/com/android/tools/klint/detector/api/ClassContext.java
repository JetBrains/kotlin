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

package com.android.tools.klint.detector.api;

import static com.android.SdkConstants.CONSTRUCTOR_NAME;
import static com.android.SdkConstants.DOT_CLASS;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.tools.klint.detector.api.Location.SearchDirection.BACKWARD;
import static com.android.tools.klint.detector.api.Location.SearchDirection.EOL_BACKWARD;
import static com.android.tools.klint.detector.api.Location.SearchDirection.FORWARD;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.LintDriver;
import com.android.tools.klint.detector.api.Location.SearchDirection;
import com.android.tools.klint.detector.api.Location.SearchHints;
import com.android.utils.AsmUtils;
import com.google.common.annotations.Beta;
import com.google.common.base.Splitter;

import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.FieldNode;
import org.jetbrains.org.objectweb.asm.tree.LineNumberNode;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.util.List;

/**
 * A {@link Context} used when checking .class files.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class ClassContext extends Context {
    private final File mBinDir;
    /** The class file DOM root node */
    private final ClassNode mClassNode;
    /** The class file byte data */
    private final byte[] mBytes;
    /** The source file, if known/found */
    private File mSourceFile;
    /** The contents of the source file, if source file is known/found */
    private String mSourceContents;
    /** Whether we've searched for the source file (used to avoid repeated failed searches) */
    private boolean mSearchedForSource;
    /** If the file is a relative path within a jar file, this is the jar file, otherwise null */
    private final File mJarFile;
    /** Whether this class is part of a library (rather than corresponding to one of the
     * source files in this project */
    private final boolean mFromLibrary;

    /**
     * Construct a new {@link ClassContext}
     *
     * @param driver the driver running through the checks
     * @param project the project containing the file being checked
     * @param main the main project if this project is a library project, or
     *            null if this is not a library project. The main project is the
     *            root project of all library projects, not necessarily the
     *            directly including project.
     * @param file the file being checked
     * @param jarFile If the file is a relative path within a jar file, this is
     *            the jar file, otherwise null
     * @param binDir the root binary directory containing this .class file.
     * @param bytes the bytecode raw data
     * @param classNode the bytecode object model
     * @param fromLibrary whether this class is from a library rather than part
     *            of this project
     * @param sourceContents initial contents of the Java source, if known, or
     *            null
     */
    public ClassContext(
            @NonNull LintDriver driver,
            @NonNull Project project,
            @Nullable Project main,
            @NonNull File file,
            @Nullable File jarFile,
            @NonNull File binDir,
            @NonNull byte[] bytes,
            @NonNull ClassNode classNode,
            boolean fromLibrary,
            @Nullable String sourceContents) {
        super(driver, project, main, file);
        mJarFile = jarFile;
        mBinDir = binDir;
        mBytes = bytes;
        mClassNode = classNode;
        mFromLibrary = fromLibrary;
        mSourceContents = sourceContents;
    }

    /**
     * Returns the raw bytecode data for this class file
     *
     * @return the byte array containing the bytecode data
     */
    @NonNull
    public byte[] getBytecode() {
        return mBytes;
    }

    /**
     * Returns the bytecode object model
     *
     * @return the bytecode object model, never null
     */
    @NonNull
    public ClassNode getClassNode() {
        return mClassNode;
    }

    /**
     * Returns the jar file, if any. If this is null, the .class file is a real file
     * on disk, otherwise it represents a relative path within the jar file.
     *
     * @return the jar file, or null
     */
    @Nullable
    public File getJarFile() {
        return mJarFile;
    }

    /**
     * Returns whether this class is part of a library (not this project).
     *
     * @return true if this class is part of a library
     */
    public boolean isFromClassLibrary() {
        return mFromLibrary;
    }

    /**
     * Returns the source file for this class file, if possible.
     *
     * @return the source file, or null
     */
    @Nullable
    public File getSourceFile() {
        if (mSourceFile == null && !mSearchedForSource) {
            mSearchedForSource = true;

            String source = mClassNode.sourceFile;
            if (source == null) {
                source = file.getName();
                if (source.endsWith(DOT_CLASS)) {
                    source = source.substring(0, source.length() - DOT_CLASS.length()) + ".kt";
                }
                int index = source.indexOf('$');
                if (index != -1) {
                    source = source.substring(0, index) + ".kt";
                }
            }
            if (source != null) {
                if (mJarFile != null) {
                    String relative = file.getParent() + File.separator + source;
                    List<File> sources = getProject().getJavaSourceFolders();
                    for (File dir : sources) {
                        File sourceFile = new File(dir, relative);
                        if (sourceFile.exists()) {
                            mSourceFile = sourceFile;
                            break;
                        }
                    }
                } else {
                    // Determine package
                    String topPath = mBinDir.getPath();
                    String parentPath = file.getParentFile().getPath();
                    if (parentPath.startsWith(topPath)) {
                        int start = topPath.length() + 1;
                        String relative = start > parentPath.length() ? // default package?
                                "" : parentPath.substring(start);
                        List<File> sources = getProject().getJavaSourceFolders();
                        for (File dir : sources) {
                            File sourceFile = new File(dir, relative + File.separator + source);
                            if (sourceFile.exists()) {
                                mSourceFile = sourceFile;
                                break;
                            }
                        }
                    }
                }
            }
        }

        return mSourceFile;
    }

    /**
     * Returns the contents of the source file for this class file, if found.
     *
     * @return the source contents, or ""
     */
    @NonNull
    public String getSourceContents() {
        if (mSourceContents == null) {
            File sourceFile = getSourceFile();
            if (sourceFile != null) {
                mSourceContents = getClient().readFile(mSourceFile);
            }

            if (mSourceContents == null) {
                mSourceContents = "";
            }
        }

        return mSourceContents;
    }

    /**
     * Returns the contents of the source file for this class file, if found. If
     * {@code read} is false, do not read the source contents if it has not
     * already been read. (This is primarily intended for the lint
     * infrastructure; most client code would call {@link #getSourceContents()}
     * .)
     *
     * @param read whether to read the source contents if it has not already
     *            been initialized
     * @return the source contents, which will never be null if {@code read} is
     *         true, or null if {@code read} is false and the source contents
     *         hasn't already been read.
     */
    @Nullable
    public String getSourceContents(boolean read) {
        if (read) {
            return getSourceContents();
        } else {
            return mSourceContents;
        }
    }

    /**
     * Returns a location for the given source line number in this class file's
     * source file, if available.
     *
     * @param line the line number (1-based, which is what ASM uses)
     * @param patternStart optional pattern to search for in the source for
     *            range start
     * @param patternEnd optional pattern to search for in the source for range
     *            end
     * @param hints additional hints about the pattern search (provided
     *            {@code patternStart} is non null)
     * @return a location, never null
     */
    @NonNull
    public Location getLocationForLine(int line, @Nullable String patternStart,
            @Nullable String patternEnd, @Nullable SearchHints hints) {
        File sourceFile = getSourceFile();
        if (sourceFile != null) {
            // ASM line numbers are 1-based, and lint line numbers are 0-based
            if (line != -1) {
                return Location.create(sourceFile, getSourceContents(), line - 1,
                        patternStart, patternEnd, hints);
            } else {
                return Location.create(sourceFile);
            }
        }

        return Location.create(file);
    }

    /**
     * Reports an issue.
     * <p>
     * Detectors should only call this method if an error applies to the whole class
     * scope and there is no specific method or field that applies to the error.
     * If so, use
     * {@link #report(Issue, MethodNode, AbstractInsnNode, Location, String)} or
     * {@link #report(Issue, FieldNode, Location, String)}, such that
     * suppress annotations are checked.
     *
     * @param issue the issue to report
     * @param location the location of the issue, or null if not known
     * @param message the message for this warning
     */
    @Override
    public void report(
            @NonNull Issue issue,
            @NonNull Location location,
            @NonNull String message) {
        if (mDriver.isSuppressed(issue, mClassNode)) {
            return;
        }
        ClassNode curr = mClassNode;
        while (curr != null) {
            ClassNode prev = curr;
            curr = mDriver.getOuterClassNode(curr);
            if (curr != null) {
                if (prev.outerMethod != null) {
                    @SuppressWarnings("rawtypes") // ASM API
                    List methods = curr.methods;
                    for (Object m : methods) {
                        MethodNode method = (MethodNode) m;
                        if (method.name.equals(prev.outerMethod)
                                && method.desc.equals(prev.outerMethodDesc)) {
                            // Found the outer method for this anonymous class; continue
                            // reporting on it (which will also work its way up the parent
                            // class hierarchy)
                            if (method != null && mDriver.isSuppressed(issue, mClassNode, method,
                                    null)) {
                                return;
                            }
                            break;
                        }
                    }
                }
                if (mDriver.isSuppressed(issue, curr)) {
                    return;
                }
            }
        }

        super.report(issue, location, message);
    }

    // Unfortunately, ASMs nodes do not extend a common DOM node type with parent
    // pointers, so we have to have multiple methods which pass in each type
    // of node (class, method, field) to be checked.

    /**
     * Reports an issue applicable to a given method node.
     *
     * @param issue the issue to report
     * @param method the method scope the error applies to. The lint
     *            infrastructure will check whether there are suppress
     *            annotations on this method (or its enclosing class) and if so
     *            suppress the warning without involving the client.
     * @param instruction the instruction within the method the error applies
     *            to. You cannot place annotations on individual method
     *            instructions (for example, annotations on local variables are
     *            allowed, but are not kept in the .class file). However, this
     *            instruction is needed to handle suppressing errors on field
     *            initializations; in that case, the errors may be reported in
     *            the {@code <clinit>} method, but the annotation is found not
     *            on that method but for the {@link FieldNode}'s.
     * @param location the location of the issue, or null if not known
     * @param message the message for this warning
     */
    public void report(
            @NonNull Issue issue,
            @Nullable MethodNode method,
            @Nullable AbstractInsnNode instruction,
            @NonNull Location location,
            @NonNull String message) {
        if (method != null && mDriver.isSuppressed(issue, mClassNode, method, instruction)) {
            return;
        }
        report(issue, location, message); // also checks the class node
    }

    /**
     * Reports an issue applicable to a given method node.
     *
     * @param issue the issue to report
     * @param field the scope the error applies to. The lint infrastructure
     *    will check whether there are suppress annotations on this field (or its enclosing
     *    class) and if so suppress the warning without involving the client.
     * @param location the location of the issue, or null if not known
     * @param message the message for this warning
     */
    public void report(
            @NonNull Issue issue,
            @Nullable FieldNode field,
            @NonNull Location location,
            @NonNull String message) {
        if (field != null && mDriver.isSuppressed(issue, field)) {
            return;
        }
        report(issue, location, message); // also checks the class node
    }

    /**
     * Report an error.
     * Like {@link #report(Issue, MethodNode, AbstractInsnNode, Location, String)} but with
     * a now-unused data parameter at the end.
     *
     * @deprecated Use {@link #report(Issue, FieldNode, Location, String)} instead;
     *    this method is here for custom rule compatibility
     */
    @SuppressWarnings("UnusedDeclaration") // Potentially used by external existing custom rules
    @Deprecated
    public void report(
            @NonNull Issue issue,
            @Nullable MethodNode method,
            @Nullable AbstractInsnNode instruction,
            @NonNull Location location,
            @NonNull String message,
            @SuppressWarnings("UnusedParameters") @Nullable Object data) {
        report(issue, method, instruction, location, message);
    }

    /**
     * Report an error.
     * Like {@link #report(Issue, FieldNode, Location, String)} but with
     * a now-unused data parameter at the end.
     *
     * @deprecated Use {@link #report(Issue, FieldNode, Location, String)} instead;
     *    this method is here for custom rule compatibility
     */
    @SuppressWarnings("UnusedDeclaration") // Potentially used by external existing custom rules
    @Deprecated
    public void report(
            @NonNull Issue issue,
            @Nullable FieldNode field,
            @NonNull Location location,
            @NonNull String message,
            @SuppressWarnings("UnusedParameters") @Nullable Object data) {
        report(issue, field, location, message);
    }

    /**
     * Finds the line number closest to the given node
     *
     * @param node the instruction node to get a line number for
     * @return the closest line number, or -1 if not known
     */
    public static int findLineNumber(@NonNull AbstractInsnNode node) {
        AbstractInsnNode curr = node;

        // First search backwards
        while (curr != null) {
            if (curr.getType() == AbstractInsnNode.LINE) {
                return ((LineNumberNode) curr).line;
            }
            curr = curr.getPrevious();
        }

        // Then search forwards
        curr = node;
        while (curr != null) {
            if (curr.getType() == AbstractInsnNode.LINE) {
                return ((LineNumberNode) curr).line;
            }
            curr = curr.getNext();
        }

        return -1;
    }

    /**
     * Finds the line number closest to the given method declaration
     *
     * @param node the method node to get a line number for
     * @return the closest line number, or -1 if not known
     */
    public static int findLineNumber(@NonNull MethodNode node) {
        if (node.instructions != null && node.instructions.size() > 0) {
            return findLineNumber(node.instructions.get(0));
        }

        return -1;
    }

    /**
     * Finds the line number closest to the given class declaration
     *
     * @param node the method node to get a line number for
     * @return the closest line number, or -1 if not known
     */
    public static int findLineNumber(@NonNull ClassNode node) {
        if (node.methods != null && !node.methods.isEmpty()) {
            MethodNode firstMethod = getFirstRealMethod(node);
            if (firstMethod != null) {
                return findLineNumber(firstMethod);
            }
        }

        return -1;
    }

    /**
     * Returns a location for the given {@link ClassNode}, where class node is
     * either the top level class, or an inner class, in the current context.
     *
     * @param classNode the class in the current context
     * @return a location pointing to the class declaration, or as close to it
     *         as possible
     */
    @NonNull
    public Location getLocation(@NonNull ClassNode classNode) {
        // Attempt to find a proper location for this class. This is tricky
        // since classes do not have line number entries in the class file; we need
        // to find a method, look up the corresponding line number then search
        // around it for a suitable tag, such as the class name.
        String pattern;
        if (isAnonymousClass(classNode.name)) {
            pattern = classNode.superName;
        } else {
            pattern = classNode.name;
        }
        int index = pattern.lastIndexOf('$');
        if (index != -1) {
            pattern = pattern.substring(index + 1);
        }
        index = pattern.lastIndexOf('/');
        if (index != -1) {
            pattern = pattern.substring(index + 1);
        }

        return getLocationForLine(findLineNumber(classNode), pattern, null,
                SearchHints.create(BACKWARD).matchJavaSymbol());
    }

    @Nullable
    private static MethodNode getFirstRealMethod(@NonNull ClassNode classNode) {
        // Return the first method in the class for line number purposes. Skip <init>,
        // since it's typically not located near the real source of the method.
        if (classNode.methods != null) {
            @SuppressWarnings("rawtypes") // ASM API
            List methods = classNode.methods;
            for (Object m : methods) {
                MethodNode method = (MethodNode) m;
                if (method.name.charAt(0) != '<') {
                    return method;
                }
            }

            if (!classNode.methods.isEmpty()) {
                return (MethodNode) classNode.methods.get(0);
            }
        }

        return null;
    }

    /**
     * Returns a location for the given {@link MethodNode}.
     *
     * @param methodNode the class in the current context
     * @param classNode the class containing the method
     * @return a location pointing to the class declaration, or as close to it
     *         as possible
     */
    @NonNull
    public Location getLocation(@NonNull MethodNode methodNode,
            @NonNull ClassNode classNode) {
        // Attempt to find a proper location for this class. This is tricky
        // since classes do not have line number entries in the class file; we need
        // to find a method, look up the corresponding line number then search
        // around it for a suitable tag, such as the class name.
        String pattern;
        SearchDirection searchMode;
        if (methodNode.name.equals(CONSTRUCTOR_NAME)) {
            searchMode = EOL_BACKWARD;
            if (isAnonymousClass(classNode.name)) {
                pattern = classNode.superName.substring(classNode.superName.lastIndexOf('/') + 1);
            } else {
                pattern = classNode.name.substring(classNode.name.lastIndexOf('$') + 1);
            }
        } else {
            searchMode = BACKWARD;
            pattern = methodNode.name;
        }

        return getLocationForLine(findLineNumber(methodNode), pattern, null,
                SearchHints.create(searchMode).matchJavaSymbol());
    }

    /**
     * Returns a location for the given {@link AbstractInsnNode}.
     *
     * @param instruction the instruction to look up the location for
     * @return a location pointing to the instruction, or as close to it
     *         as possible
     */
    @NonNull
    public Location getLocation(@NonNull AbstractInsnNode instruction) {
        SearchHints hints = SearchHints.create(FORWARD).matchJavaSymbol();
        String pattern = null;
        if (instruction instanceof MethodInsnNode) {
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.name.equals(CONSTRUCTOR_NAME)) {
                pattern = call.owner;
                hints = hints.matchConstructor();
            } else {
                pattern = call.name;
            }
            int index = pattern.lastIndexOf('$');
            if (index != -1) {
                pattern = pattern.substring(index + 1);
            }
            index = pattern.lastIndexOf('/');
            if (index != -1) {
                pattern = pattern.substring(index + 1);
            }
        }

        int line = findLineNumber(instruction);
        return getLocationForLine(line, pattern, null, hints);
    }

    private static boolean isAnonymousClass(@NonNull String fqcn) {
        int lastIndex = fqcn.lastIndexOf('$');
        if (lastIndex != -1 && lastIndex < fqcn.length() - 1) {
            if (Character.isDigit(fqcn.charAt(lastIndex + 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts from a VM owner name (such as foo/bar/Foo$Baz) to a
     * fully qualified class name (such as foo.bar.Foo.Baz).
     *
     * @param owner the owner name to convert
     * @return the corresponding fully qualified class name
     */
    @NonNull
    public static String getFqcn(@NonNull String owner) {
        return owner.replace('/', '.').replace('$','.');
    }

    /**
     * Computes a user-readable type signature from the given class owner, name
     * and description. For example, for owner="foo/bar/Foo$Baz", name="foo",
     * description="(I)V", it returns "void foo.bar.Foo.Bar#foo(int)".
     *
     * @param owner the class name
     * @param name the method name
     * @param desc the method description
     * @return a user-readable string
     */
    public static String createSignature(String owner, String name, String desc) {
        StringBuilder sb = new StringBuilder(100);

        if (desc != null) {
            Type returnType = Type.getReturnType(desc);
            sb.append(getTypeString(returnType));
            sb.append(' ');
        }

        if (owner != null) {
            sb.append(getFqcn(owner));
        }
        if (name != null) {
            sb.append('#');
            sb.append(name);
            if (desc != null) {
                Type[] argumentTypes = Type.getArgumentTypes(desc);
                if (argumentTypes != null && argumentTypes.length > 0) {
                    sb.append('(');
                    boolean first = true;
                    for (Type type : argumentTypes) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(", ");
                        }
                        sb.append(getTypeString(type));
                    }
                    sb.append(')');
                }
            }
        }

        return sb.toString();
    }

    private static String getTypeString(Type type) {
        String s = type.getClassName();
        if (s.startsWith("java.lang.")) {           //$NON-NLS-1$
            s = s.substring("java.lang.".length()); //$NON-NLS-1$
        }

        return s;
    }

    /**
     * Computes the internal class name of the given fully qualified class name.
     * For example, it converts foo.bar.Foo.Bar into foo/bar/Foo$Bar
     *
     * @param fqcn the fully qualified class name
     * @return the internal class name
     */
    @NonNull
    public static String getInternalName(@NonNull String fqcn) {
        if (fqcn.indexOf('.') == -1) {
            return fqcn;
        }

        int index = fqcn.indexOf('<');
        if(index != -1) {
            fqcn = fqcn.substring(0, index);
        }

        // If class name contains $, it's not an ambiguous inner class name.
        if (fqcn.indexOf('$') != -1) {
            return AsmUtils.toInternalName(fqcn);
        }
        // Let's assume that components that start with Caps are class names.
        StringBuilder sb = new StringBuilder(fqcn.length());
        String prev = null;
        for (String part : Splitter.on('.').split(fqcn)) {
            if (prev != null && !prev.isEmpty()) {
                if (Character.isUpperCase(prev.charAt(0))) {
                    sb.append('$');
                } else {
                    sb.append('/');
                }
            }
            sb.append(part);
            prev = part;
        }

        return sb.toString();
    }
}
