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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.google.common.annotations.Beta;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UFunction;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.jetbrains.uast.visitor.UastVisitor;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.*;

/**
 * A detector is able to find a particular problem (or a set of related problems).
 * Each problem type is uniquely identified as an {@link Issue}.
 * <p>
 * Detectors will be called in a predefined order:
 * <ol>
 *   <li> Manifest file
 *   <li> Resource files, in alphabetical order by resource type
 *        (therefore, "layout" is checked before "values", "values-de" is checked before
 *        "values-en" but after "values", and so on.
 *   <li> Java sources
 *   <li> Java classes
 *   <li> Gradle files
 *   <li> Generic files
 *   <li> Proguard files
 *   <li> Property files
 * </ol>
 * If a detector needs information when processing a file type that comes from a type of
 * file later in the order above, they can request a second phase; see
 * {@link LintDriver#requestRepeat}.
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class Detector {
    /** Specialized interface for detectors that scan Java class files */
    public interface ClassScanner  {
        /**
         * Checks the given class' bytecode for issues.
         *
         * @param context the context of the lint check, pointing to for example
         *            the file
         * @param classNode the root class node
         */
        void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode);

        /**
         * Returns the list of node types (corresponding to the constants in the
         * {@link AbstractInsnNode} class) that this scanner applies to. The
         * {@link #checkInstruction(ClassContext, ClassNode, MethodNode, AbstractInsnNode)}
         * method will be called for each match.
         *
         * @return an array containing all the node types this detector should be
         *         called for, or null if none.
         */
        @Nullable
        int[] getApplicableAsmNodeTypes();

        /**
         * Process a given instruction node, and register lint issues if
         * applicable.
         *
         * @param context the context of the lint check, pointing to for example
         *            the file
         * @param classNode the root class node
         * @param method the method node containing the call
         * @param instruction the actual instruction
         */
        void checkInstruction(@NonNull ClassContext context, @NonNull ClassNode classNode,
                @NonNull MethodNode method, @NonNull AbstractInsnNode instruction);

        /**
         * Return the list of method call names (in VM format, e.g. "<init>" for
         * constructors, etc) for method calls this detector is interested in,
         * or null. T his will be used to dispatch calls to
         * {@link #checkCall(ClassContext, ClassNode, MethodNode, MethodInsnNode)}
         * for only the method calls in owners that the detector is interested
         * in.
         * <p>
         * <b>NOTE</b>: If you return non null from this method, then <b>only</b>
         * {@link #checkCall(ClassContext, ClassNode, MethodNode, MethodInsnNode)}
         * will be called if a suitable method is found;
         * {@link #checkClass(ClassContext, ClassNode)} will not be called under
         * any circumstances.
         * <p>
         * This makes it easy to write detectors that focus on some fixed calls,
         * and allows lint to make a single pass over the bytecode over a class,
         * and efficiently dispatch method calls to any detectors that are
         * interested in it. Without this, each new lint check interested in a
         * single method, would be doing a complete pass through all the
         * bytecode instructions of the class via the
         * {@link #checkClass(ClassContext, ClassNode)} method, which would make
         * each newly added lint check make lint slower. Now a single dispatch
         * map is used instead, and for each encountered call in the single
         * dispatch, it looks up in the map which if any detectors are
         * interested in the given call name, and dispatches to each one in
         * turn.
         *
         * @return a list of applicable method names, or null.
         */
        @Nullable
        List<String> getApplicableCallNames();

        /**
         * Just like {@link Detector#getApplicableCallNames()}, but for the owner
         * field instead. The
         * {@link #checkCall(ClassContext, ClassNode, MethodNode, MethodInsnNode)}
         * method will be called for all {@link MethodInsnNode} instances where the
         * owner field matches any of the members returned in this node.
         * <p>
         * Note that if your detector provides both a name and an owner, the
         * method will be called for any nodes matching either the name <b>or</b>
         * the owner, not only where they match <b>both</b>. Note also that it will
         * be called twice - once for the name match, and (at least once) for the owner
         * match.
         *
         * @return a list of applicable owner names, or null.
         */
        @Nullable
        List<String> getApplicableCallOwners();

        /**
         * Process a given method call node, and register lint issues if
         * applicable. This is similar to the
         * {@link #checkInstruction(ClassContext, ClassNode, MethodNode, AbstractInsnNode)}
         * method, but has the additional advantage that it is only called for known
         * method names or method owners, according to
         * {@link #getApplicableCallNames()} and {@link #getApplicableCallOwners()}.
         *
         * @param context the context of the lint check, pointing to for example
         *            the file
         * @param classNode the root class node
         * @param method the method node containing the call
         * @param call the actual method call node
         */
        void checkCall(@NonNull ClassContext context, @NonNull ClassNode classNode,
                @NonNull MethodNode method, @NonNull MethodInsnNode call);
    }

    /** Specialized interface for detectors that scan binary resource files */
    public interface BinaryResourceScanner {
        /**
         * Called for each resource folder
         *
         * @param context the context for the resource file
         */
        void checkBinaryResource(@NonNull ResourceContext context);

        /**
         * Returns whether this detector applies to the given folder type. This
         * allows the detectors to be pruned from iteration, so for example when we
         * are analyzing a string value file we don't need to look up detectors
         * related to layout.
         *
         * @param folderType the folder type to be visited
         * @return true if this detector can apply to resources in folders of the
         *         given type
         */
        boolean appliesTo(@NonNull ResourceFolderType folderType);
    }

    /** Specialized interface for detectors that scan resource folders (the folder directory
     * itself, not the individual files within it */
    public interface ResourceFolderScanner {
        /**
         * Called for each resource folder
         *
         * @param context    the context for the resource folder
         * @param folderName the resource folder name
         */
        void checkFolder(@NonNull ResourceContext context, @NonNull String folderName);

        /**
         * Returns whether this detector applies to the given folder type. This
         * allows the detectors to be pruned from iteration, so for example when we
         * are analyzing a string value file we don't need to look up detectors
         * related to layout.
         *
         * @param folderType the folder type to be visited
         * @return true if this detector can apply to resources in folders of the
         *         given type
         */
        boolean appliesTo(@NonNull ResourceFolderType folderType);
    }

    /** Specialized interface for detectors that scan XML files */
    public interface XmlScanner {
        /**
         * Visit the given document. The detector is responsible for its own iteration
         * through the document.
         * @param context information about the document being analyzed
         * @param document the document to examine
         */
        void visitDocument(@NonNull XmlContext context, @NonNull Document document);

        /**
         * Visit the given element.
         * @param context information about the document being analyzed
         * @param element the element to examine
         */
        void visitElement(@NonNull XmlContext context, @NonNull Element element);

        /**
         * Visit the given element after its children have been analyzed.
         * @param context information about the document being analyzed
         * @param element the element to examine
         */
        void visitElementAfter(@NonNull XmlContext context, @NonNull Element element);

        /**
         * Visit the given attribute.
         * @param context information about the document being analyzed
         * @param attribute the attribute node to examine
         */
        void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute);

        /**
         * Returns the list of elements that this detector wants to analyze. If non
         * null, this detector will be called (specifically, the
         * {@link #visitElement} method) for each matching element in the document.
         * <p>
         * If this method returns null, and {@link #getApplicableAttributes()} also returns
         * null, then the {@link #visitDocument} method will be called instead.
         *
         * @return a collection of elements, or null, or the special
         *         {@link XmlScanner#ALL} marker to indicate that every single
         *         element should be analyzed.
         */
        @Nullable
        Collection<String> getApplicableElements();

        /**
         * Returns the list of attributes that this detector wants to analyze. If non
         * null, this detector will be called (specifically, the
         * {@link #visitAttribute} method) for each matching attribute in the document.
         * <p>
         * If this method returns null, and {@link #getApplicableElements()} also returns
         * null, then the {@link #visitDocument} method will be called instead.
         *
         * @return a collection of attributes, or null, or the special
         *         {@link XmlScanner#ALL} marker to indicate that every single
         *         attribute should be analyzed.
         */
        @Nullable
        Collection<String> getApplicableAttributes();

        /**
         * Special marker collection returned by {@link #getApplicableElements()} or
         * {@link #getApplicableAttributes()} to indicate that the check should be
         * invoked on all elements or all attributes
         */
        @NonNull
        List<String> ALL = new ArrayList<String>(0); // NOT Collections.EMPTY!
        // We want to distinguish this from just an *empty* list returned by the caller!
    }

    /** Specialized interface for detectors that scan Gradle files */
    public interface GradleScanner {
        void visitBuildScript(@NonNull Context context, Map<String, Object> sharedData);
    }

    /** Specialized interface for detectors that scan other files */
    public interface OtherFileScanner {
        /**
         * Returns the set of files this scanner wants to consider.  If this includes
         * {@link Scope#OTHER} then all source files will be checked. Note that the
         * set of files will not just include files of the indicated type, but all files
         * within the relevant source folder. For example, returning {@link Scope#JAVA_FILE}
         * will not just return {@code .java} files, but also other resource files such as
         * {@code .html} and other files found within the Java source folders.
         * <p>
         * Lint will call the {@link #run(Context)}} method when the file should be checked.
         *
         * @return set of scopes that define the types of source files the
         *    detector wants to consider
         */
        @NonNull
        EnumSet<Scope> getApplicableFiles();
    }

    /**
     * Runs the detector. This method will not be called for certain specialized
     * detectors, such as {@link XmlScanner} and {@link JavaScanner}, where
     * there are specialized analysis methods instead such as
     * {@link XmlScanner#visitElement(XmlContext, Element)}.
     *
     * @param context the context describing the work to be done
     */
    public void run(@NonNull Context context) {
    }

    /**
     * Returns true if this detector applies to the given file
     *
     * @param context the context to check
     * @param file the file in the context to check
     * @return true if this detector applies to the given context and file
     */
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return false;
    }

    /**
     * Analysis is about to begin, perform any setup steps.
     *
     * @param context the context for the check referencing the project, lint
     *            client, etc
     */
    public void beforeCheckProject(@NonNull Context context) {
    }

    /**
     * Analysis has just been finished for the whole project, perform any
     * cleanup or report issues that require project-wide analysis.
     *
     * @param context the context for the check referencing the project, lint
     *            client, etc
     */
    public void afterCheckProject(@NonNull Context context) {
    }

    /**
     * Analysis is about to begin for the given library project, perform any setup steps.
     *
     * @param context the context for the check referencing the project, lint
     *            client, etc
     */
    public void beforeCheckLibraryProject(@NonNull Context context) {
    }

    /**
     * Analysis has just been finished for the given library project, perform any
     * cleanup or report issues that require library-project-wide analysis.
     *
     * @param context the context for the check referencing the project, lint
     *            client, etc
     */
    public void afterCheckLibraryProject(@NonNull Context context) {
    }

    /**
     * Analysis is about to be performed on a specific file, perform any setup
     * steps.
     * <p>
     * Note: When this method is called at the beginning of checking an XML
     * file, the context is guaranteed to be an instance of {@link XmlContext},
     * and similarly for a Java source file, the context will be a
     * {@link JavaContext} and so on.
     *
     * @param context the context for the check referencing the file to be
     *            checked, the project, etc.
     */
    public void beforeCheckFile(@NonNull Context context) {
    }

    /**
     * Analysis has just been finished for a specific file, perform any cleanup
     * or report issues found
     * <p>
     * Note: When this method is called at the end of checking an XML
     * file, the context is guaranteed to be an instance of {@link XmlContext},
     * and similarly for a Java source file, the context will be a
     * {@link JavaContext} and so on.
     *
     * @param context the context for the check referencing the file to be
     *            checked, the project, etc.
     */
    public void afterCheckFile(@NonNull Context context) {
    }

    /**
     * Returns the expected speed of this detector
     *
     * @return the expected speed of this detector
     */
    @NonNull
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    /**
     * Returns the expected speed of this detector.
     * The issue parameter is made available for subclasses which analyze multiple issues
     * and which need to distinguish implementation cost by issue. If the detector does
     * not analyze multiple issues or does not vary in speed by issue type, just override
     * {@link #getSpeed()} instead.
     *
     * @param issue the issue to look up the analysis speed for
     * @return the expected speed of this detector
     */
    @NonNull
    public Speed getSpeed(@SuppressWarnings("UnusedParameters") @NonNull Issue issue) {
        // If not overridden, this detector does not distinguish speed by issue type
        return getSpeed();
    }

    // ---- Dummy implementations to make implementing XmlScanner easier: ----

    @SuppressWarnings("javadoc")
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        // This method must be overridden if your detector does
        // not return something from getApplicableElements or
        // getApplicableAttributes
        assert false;
    }

    @SuppressWarnings("javadoc")
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        // This method must be overridden if your detector returns
        // tag names from getApplicableElements
        assert false;
    }

    @SuppressWarnings("javadoc")
    public void visitElementAfter(@NonNull XmlContext context, @NonNull Element element) {
    }

    @SuppressWarnings("javadoc")
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        // This method must be overridden if your detector returns
        // attribute names from getApplicableAttributes
        assert false;
    }

    @SuppressWarnings("javadoc")
    @Nullable
    public Collection<String> getApplicableElements() {
        return null;
    }

    @Nullable
    @SuppressWarnings("javadoc")
    public Collection<String> getApplicableAttributes() {
        return null;
    }

    // ---- Dummy implementations to make implementing JavaScanner easier: ----

    @Nullable @SuppressWarnings("javadoc")
    public List<String> getApplicableMethodNames() {
        return null;
    }

    @SuppressWarnings("javadoc")
    public boolean appliesToResourceRefs() {
        return false;
    }

    @SuppressWarnings("javadoc")
    public void visitResourceReference(
            UastAndroidContext context,
            UElement element,
            String type,
            String name,
            boolean isFramework
    ) {}

    @Nullable @SuppressWarnings("javadoc")
    public List<String> getApplicableConstructorTypes() {
        return null;
    }

    public List<String> getApplicableSuperClasses() {
        return null;
    }

    public void visitClass(UastAndroidContext context, UClass node) {

    }

    public List<String> getApplicableFunctionNames() {
        return null;
    }

    public void visitConstructor(UastAndroidContext context, UCallExpression functionCall,
                                 UFunction constructor) {

    }

    public UastVisitor createUastVisitor(UastAndroidContext context) {
        return null;
    }

    public void visitCall(UastAndroidContext context, UCallExpression node) {

    }

    // ---- Dummy implementations to make implementing a ClassScanner easier: ----

    @SuppressWarnings("javadoc")
    public void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode) {
    }

    @SuppressWarnings("javadoc")
    @Nullable
    public List<String> getApplicableCallNames() {
        return null;
    }

    @SuppressWarnings("javadoc")
    @Nullable
    public List<String> getApplicableCallOwners() {
        return null;
    }

    @SuppressWarnings("javadoc")
    public void checkCall(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull MethodInsnNode call) {
    }

    @SuppressWarnings("javadoc")
    @Nullable
    public int[] getApplicableAsmNodeTypes() {
        return null;
    }

    @SuppressWarnings("javadoc")
    public void checkInstruction(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull AbstractInsnNode instruction) {
    }

    // ---- Dummy implementations to make implementing an OtherFileScanner easier: ----

    public boolean appliesToFolder(@NonNull Scope scope, @Nullable ResourceFolderType folderType) {
        return false;
    }

    @NonNull
    public EnumSet<Scope> getApplicableFiles() {
        return Scope.OTHER_SCOPE;
    }

    // ---- Dummy implementations to make implementing an GradleScanner easier: ----

    public void visitBuildScript(@NonNull Context context, Map<String, Object> sharedData) {
    }

    // ---- Dummy implementations to make implementing a resource folder scanner easier: ----

    public void checkFolder(@NonNull ResourceContext context, @NonNull String folderName) {
    }

    // ---- Dummy implementations to make implementing a binary resource scanner easier: ----

    public void checkBinaryResource(@NonNull ResourceContext context) {
    }

    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return true;
    }
}
