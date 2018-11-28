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
import com.android.resources.ResourceType;
import com.android.tools.klint.client.api.JavaParser.ResolvedClass;
import com.android.tools.klint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.klint.client.api.LintDriver;
import com.android.tools.klint.client.api.UElementVisitor;
import com.google.common.annotations.Beta;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;

import com.intellij.psi.PsiTypeParameter;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.visitor.UastVisitor;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import lombok.ast.AstVisitor;
import lombok.ast.ClassDeclaration;
import lombok.ast.ConstructorInvocation;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;

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
    /**
     * Specialized interface for detectors that scan Java source file parse trees
     * @deprecated Use {@link JavaPsiScanner} instead
     */
    @Deprecated @SuppressWarnings("unused") // Still here for third-party rules
    public interface JavaScanner  {
        /**
         * Create a parse tree visitor to process the parse tree. All
         * {@link JavaScanner} detectors must provide a visitor, unless they
         * either return true from {@link #appliesToResourceRefs()} or return
         * non null from {@link #getApplicableMethodNames()}.
         * <p>
         * If you return specific AST node types from
         * {@link #getApplicableNodeTypes()}, then the visitor will <b>only</b>
         * be called for the specific requested node types. This is more
         * efficient, since it allows many detectors that apply to only a small
         * part of the AST (such as method call nodes) to share iteration of the
         * majority of the parse tree.
         * <p>
         * If you return null from {@link #getApplicableNodeTypes()}, then your
         * visitor will be called from the top and all node types visited.
         * <p>
         * Note that a new visitor is created for each separate compilation
         * unit, so you can store per file state in the visitor.
         *
         * @param context the {@link Context} for the file being analyzed
         * @return a visitor, or null.
         */
        @Nullable
        AstVisitor createJavaVisitor(@NonNull JavaContext context);

        /**
         * Return the types of AST nodes that the visitor returned from
         * {@link #createJavaVisitor(JavaContext)} should visit. See the
         * documentation for {@link #createJavaVisitor(JavaContext)} for details
         * on how the shared visitor is used.
         * <p>
         * If you return null from this method, then the visitor will process
         * the full tree instead.
         * <p>
         * Note that for the shared visitor, the return codes from the visit
         * methods are ignored: returning true will <b>not</b> prune iteration
         * of the subtree, since there may be other node types interested in the
         * children. If you need to ensure that your visitor only processes a
         * part of the tree, use a full visitor instead. See the
         * OverdrawDetector implementation for an example of this.
         *
         * @return the list of applicable node types (AST node classes), or null
         */
        @Nullable
        List<Class<? extends Node>> getApplicableNodeTypes();

        /**
         * Return the list of method names this detector is interested in, or
         * null. If this method returns non-null, then any AST nodes that match
         * a method call in the list will be passed to the
         * {@link #visitMethod(JavaContext, AstVisitor, MethodInvocation)}
         * method for processing. The visitor created by
         * {@link #createJavaVisitor(JavaContext)} is also passed to that
         * method, although it can be null.
         * <p>
         * This makes it easy to write detectors that focus on some fixed calls.
         * For example, the StringFormatDetector uses this mechanism to look for
         * "format" calls, and when found it looks around (using the AST's
         * {@link Node#getParent()} method) to see if it's called on
         * a String class instance, and if so do its normal processing. Note
         * that since it doesn't need to do any other AST processing, that
         * detector does not actually supply a visitor.
         *
         * @return a set of applicable method names, or null.
         */
        @Nullable
        List<String> getApplicableMethodNames();

        /**
         * Method invoked for any method calls found that matches any names
         * returned by {@link #getApplicableMethodNames()}. This also passes
         * back the visitor that was created by
         * {@link #createJavaVisitor(JavaContext)}, but a visitor is not
         * required. It is intended for detectors that need to do additional AST
         * processing, but also want the convenience of not having to look for
         * method names on their own.
         *
         * @param context the context of the lint request
         * @param visitor the visitor created from
         *            {@link #createJavaVisitor(JavaContext)}, or null
         * @param node the {@link MethodInvocation} node for the invoked method
         */
        void visitMethod(
                @NonNull JavaContext context,
                @Nullable AstVisitor visitor,
                @NonNull MethodInvocation node);

        /**
         * Return the list of constructor types this detector is interested in, or
         * null. If this method returns non-null, then any AST nodes that match
         * a constructor call in the list will be passed to the
         * {@link #visitConstructor(JavaContext, AstVisitor, ConstructorInvocation, ResolvedMethod)}
         * method for processing. The visitor created by
         * {@link #createJavaVisitor(JavaContext)} is also passed to that
         * method, although it can be null.
         * <p>
         * This makes it easy to write detectors that focus on some fixed constructors.
         *
         * @return a set of applicable fully qualified types, or null.
         */
        @Nullable
        List<String> getApplicableConstructorTypes();

        /**
         * Method invoked for any constructor calls found that matches any names
         * returned by {@link #getApplicableConstructorTypes()}. This also passes
         * back the visitor that was created by
         * {@link #createJavaVisitor(JavaContext)}, but a visitor is not
         * required. It is intended for detectors that need to do additional AST
         * processing, but also want the convenience of not having to look for
         * method names on their own.
         *
         * @param context the context of the lint request
         * @param visitor the visitor created from
         *            {@link #createJavaVisitor(JavaContext)}, or null
         * @param node the {@link ConstructorInvocation} node for the invoked method
         * @param constructor the resolved constructor method with type information
         */
        void visitConstructor(
                @NonNull JavaContext context,
                @Nullable AstVisitor visitor,
                @NonNull ConstructorInvocation node,
                @NonNull ResolvedMethod constructor);

        /**
         * Returns whether this detector cares about Android resource references
         * (such as {@code R.layout.main} or {@code R.string.app_name}). If it
         * does, then the visitor will look for these patterns, and if found, it
         * will invoke {@link #visitResourceReference} passing the resource type
         * and resource name. It also passes the visitor, if any, that was
         * created by {@link #createJavaVisitor(JavaContext)}, such that a
         * detector can do more than just look for resources.
         *
         * @return true if this detector wants to be notified of R resource
         *         identifiers found in the code.
         */
        boolean appliesToResourceRefs();

        /**
         * Called for any resource references (such as {@code R.layout.main}
         * found in Java code, provided this detector returned {@code true} from
         * {@link #appliesToResourceRefs()}.
         *
         * @param context the lint scanning context
         * @param visitor the visitor created from
         *            {@link #createJavaVisitor(JavaContext)}, or null
         * @param node the variable reference for the resource
         * @param type the resource type, such as "layout" or "string"
         * @param name the resource name, such as "main" from
         *            {@code R.layout.main}
         * @param isFramework whether the resource is a framework resource
         *            (android.R) or a local project resource (R)
         */
        void visitResourceReference(
                @NonNull JavaContext context,
                @Nullable AstVisitor visitor,
                @NonNull Node node,
                @NonNull String type,
                @NonNull String name,
                boolean isFramework);

        /**
         * Returns a list of fully qualified names for super classes that this
         * detector cares about. If not null, this detector will *only* be called
         * if the current class is a subclass of one of the specified superclasses.
         *
         * @return a list of fully qualified names
         */
        @Nullable
        List<String> applicableSuperClasses();

        /**
         * Called for each class that extends one of the super classes specified with
         * {@link #applicableSuperClasses()}
         *
         * @param context the lint scanning context
         * @param declaration the class declaration node, or null for anonymous classes
         * @param node the class declaration node or the anonymous class construction node
         * @param resolvedClass the resolved class
         */
        // TODO: Change signature to pass in the NormalTypeBody instead of the plain Node?
        void checkClass(@NonNull JavaContext context, @Nullable ClassDeclaration declaration,
                @NonNull Node node, @NonNull ResolvedClass resolvedClass);
    }

    /**
     Interface to be implemented by lint detectors that want to analyze
     Java source files.
     <p>
     The Lint Java API sits on top of IntelliJ IDEA's "PSI" API, an API
     which exposes the underlying abstract syntax tree as well as providing
     core services like resolving symbols.
     <p>
     This new API replaces the older Lombok AST API that was used for Java
     source checks. Migrating a check from the Lombok APIs to the new PSI
     based APIs is relatively straightforward.
     <p>
     First, replace "implements JavaScanner" with "implements
     JavaPsiScanner" in your detector signature, and then locate all the
     JavaScanner methods your detector was overriding and replace them with
     the new corresponding signatures.
     <p>
     For example, replace
     <pre>
     {@code List<Class<? extends Node>> getApplicableNodeTypes();}
     </pre>
     with
     <pre>
     {@code List<Class<? extends PsiElement>> getApplicablePsiTypes();}
     </pre>
     and replace
     <pre>
     void visitMethod(
     &#064;NonNull JavaContext context,
     &#064;Nullable AstVisitor visitor,
     &#064;NonNull MethodInvocation node);
     </pre>
     with
     <pre>
     void visitMethod(
     &#064;NonNull JavaContext context,
     &#064;Nullable JavaElementVisitor visitor,
     &#064;NonNull PsiMethodCallExpression call,
     &#064;NonNull PsiMethod method);
     </pre>
     and so on.
     <p>
     Finally, replace the various Lombok iteration code with PSI based
     code. Both Lombok and PSI used class names that closely resemble the
     Java language specification, so guessing the corresponding names is
     straightforward; here are some examples:
     <pre>
     ClassDeclaration ⇒ PsiClass
     MethodDeclaration ⇒ PsiMethod
     MethodInvocation ⇒ PsiMethodCallExpression
     ConstructorInvocation ⇒ PsiNewExpression
     If ⇒ PsiIfStatement
     For ⇒ PsiForStatement
     Continue ⇒ PsiContinueStatement
     StringLiteral ⇒ PsiLiteral
     IntegralLiteral ⇒ PsiLiteral
     ... etc
     </pre>
     Lombok AST had no support for symbol and type resolution, so lint
     added its own separate API to support (the "ResolvedNode"
     hierarchy). This is no longer needed since PSI supports it directly
     (for example, on a PsiMethodCallExpression you just call
     "resolveMethod" to get the PsiMethod the method calls, and on a
     PsiExpression you just call getType() to get the corresponding
     <p>
     The old ResolvedNode interface was written for lint so it made certain
     kinds of common checks very easy. To help make porting lint rules from
     the old API easier, and to make writing future lint checks easier
     too), there is a new helper class, "JavaEvaluator" (which you can
     obtain from JavaContext). This lets you for example quickly check
     whether a given method is a member of a subclass of a given class, or
     whether a method has a certain set of parameters, etc. It also makes
     it easy to check whether a given method is private, abstract or
     static, and so on. (And most of its parameters are nullable which
     makes it simpler to use; you don't have to null check resolve results
     before calling into it.)
     <p>
     Some further porting tips:
     <ul>
     <li> Make sure you don't call toString() on nodes to get their
     contents. In Lombok, toString returned the underlying source
     text. In PSI, call getText() instead, since toString() is meant for
     debugging and includes node types etc.

     <li> ResolvedClass#getName() used to return *qualified* name. In PSI,
     PsiClass#getName() returns just the simple name, so call
     #getQualifiedName() instead if that's what your code needs! Node
     also that PsiClassType#getClassName() returns the simple name; if
     you want the fully qualified name, call PsiType#getCanonicalText().

     <li> Lombok didn't distinguish between a local variable declaration, a
     parameter and a field declaration. These are all different in PSI,
     so when writing visitors, make sure you replace a single
     visitVariableDeclaration with visitField, visitLocalVariable and
     visitParameter methods as applicable.

     <li> Note that when lint runs in the IDE, there may be extra PSI nodes in
     the hierarchy representing whitespace as well as parentheses. Watch
     out for this when calling getParent, getPrevSibling or
     getNextSibling - don't just go one level up and check instanceof
     {@code <something>}; instead, use LintUtils.skipParentheses (or the
     corresponding methods to skip whitespace left and right.)  Note that
     when you write lint unit tests, the infrastructure will run your
     tests twice, one with a normal AST and once where it has inserted
     whitespace and parentheses everywhere, and it asserts that you come
     up with the same analysis results. (This caught 16 failing tests
     across 7 different detectors.)

     <li> Annotation handling is a bit different. In ResolvedAnnotations I had
     (for convenience) inlined things like annotations on the class; you
     now have to resolve the annotation name reference to the
     corresponding annotation class and look there.
     </ul>

     Some additional conversion examples: replace
     <pre>
     &#064;Override
     public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
     &#064;NonNull MethodInvocation node) {
         ResolvedNode resolved = context.resolve(node);
         if (resolved instanceof ResolvedMethod) {
             ResolvedMethod method = (ResolvedMethod) resolved;
             if (method.getContainingClass().matches("android.os.Parcel")) {
                 ...
     </pre>
     with
     <pre>
     &#064;Override
     public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
             &#064;NonNull PsiCall node) {
         if (method != null &amp;&amp; method.getContainingClass() != null &amp;&amp;
             "android.os.Parcel".equals(method.getContainingClass().getQualifiedName())) {
             ....
     </pre>

     Similarly:
     <pre>
     if (method.getArgumentCount() != 2
             || !method.getArgumentType(0).matchesName(TYPE_OBJECT)
             || !method.getArgumentType(1).matchesName(TYPE_STRING)) {
         return;
     }
     </pre>
     can now be written as
     <pre>
     JavaEvaluator resolver = context.getEvaluator();
     if (!resolver.methodMatches(method, WEB_VIEW, true, TYPE_OBJECT, TYPE_STRING)) {
         return;
     }
     </pre>
     Finally, note that many deprecated methods in lint itself point to the replacement
     methods, see for example {@link JavaContext#findSurroundingMethod(Node)}.
     */
    public interface JavaPsiScanner  {
        /**
         * Create a parse tree visitor to process the parse tree. All
         * {@link JavaScanner} detectors must provide a visitor, unless they
         * either return true from {@link #appliesToResourceRefs()} or return
         * non null from {@link #getApplicableMethodNames()}.
         * <p>
         * If you return specific AST node types from
         * {@link #getApplicablePsiTypes()}, then the visitor will <b>only</b>
         * be called for the specific requested node types. This is more
         * efficient, since it allows many detectors that apply to only a small
         * part of the AST (such as method call nodes) to share iteration of the
         * majority of the parse tree.
         * <p>
         * If you return null from {@link #getApplicablePsiTypes()}, then your
         * visitor will be called from the top and all node types visited.
         * <p>
         * Note that a new visitor is created for each separate compilation
         * unit, so you can store per file state in the visitor.
         * <p>
         * <b>
         * NOTE: Your visitor should <b>NOT</b> extend JavaRecursiveElementVisitor.
         * Your visitor should only visit the current node type; the infrastructure
         * will do the recursion. (Lint's unit test infrastructure will check and
         * enforce this restriction.)
         * </b>
         *
         * @param context the {@link Context} for the file being analyzed
         * @return a visitor, or null.
         */
        @Nullable
        JavaElementVisitor createPsiVisitor(@NonNull JavaContext context);

        /**
         * Return the types of AST nodes that the visitor returned from
         * {@link #createJavaVisitor(JavaContext)} should visit. See the
         * documentation for {@link #createJavaVisitor(JavaContext)} for details
         * on how the shared visitor is used.
         * <p>
         * If you return null from this method, then the visitor will process
         * the full tree instead.
         * <p>
         * Note that for the shared visitor, the return codes from the visit
         * methods are ignored: returning true will <b>not</b> prune iteration
         * of the subtree, since there may be other node types interested in the
         * children. If you need to ensure that your visitor only processes a
         * part of the tree, use a full visitor instead. See the
         * OverdrawDetector implementation for an example of this.
         *
         * @return the list of applicable node types (AST node classes), or null
         */
        @Nullable
        List<Class<? extends PsiElement>> getApplicablePsiTypes();

        /**
         * Return the list of method names this detector is interested in, or
         * null. If this method returns non-null, then any AST nodes that match
         * a method call in the list will be passed to the
         * {@link #visitMethod(JavaContext, JavaElementVisitor, PsiMethodCallExpression, PsiMethod)}
         * method for processing. The visitor created by
         * {@link #createPsiVisitor(JavaContext)} is also passed to that
         * method, although it can be null.
         * <p>
         * This makes it easy to write detectors that focus on some fixed calls.
         * For example, the StringFormatDetector uses this mechanism to look for
         * "format" calls, and when found it looks around (using the AST's
         * {@link PsiElement#getParent()} method) to see if it's called on
         * a String class instance, and if so do its normal processing. Note
         * that since it doesn't need to do any other AST processing, that
         * detector does not actually supply a visitor.
         *
         * @return a set of applicable method names, or null.
         */
        @Nullable
        List<String> getApplicableMethodNames();

        /**
         * Method invoked for any method calls found that matches any names
         * returned by {@link #getApplicableMethodNames()}. This also passes
         * back the visitor that was created by
         * {@link #createJavaVisitor(JavaContext)}, but a visitor is not
         * required. It is intended for detectors that need to do additional AST
         * processing, but also want the convenience of not having to look for
         * method names on their own.
         *
         * @param context the context of the lint request
         * @param visitor the visitor created from
         *            {@link #createPsiVisitor(JavaContext)}, or null
         * @param call the {@link PsiMethodCallExpression} node for the invoked method
         * @param method the {@link PsiMethod} being called
         */
        void visitMethod(
                @NonNull JavaContext context,
                @Nullable JavaElementVisitor visitor,
                @NonNull PsiMethodCallExpression call,
                @NonNull PsiMethod method);

        /**
         * Return the list of constructor types this detector is interested in, or
         * null. If this method returns non-null, then any AST nodes that match
         * a constructor call in the list will be passed to the
         * {@link #visitConstructor(JavaContext, JavaElementVisitor, PsiNewExpression, PsiMethod)}
         * method for processing. The visitor created by
         * {@link #createJavaVisitor(JavaContext)} is also passed to that
         * method, although it can be null.
         * <p>
         * This makes it easy to write detectors that focus on some fixed constructors.
         *
         * @return a set of applicable fully qualified types, or null.
         */
        @Nullable
        List<String> getApplicableConstructorTypes();

        /**
         * Method invoked for any constructor calls found that matches any names
         * returned by {@link #getApplicableConstructorTypes()}. This also passes
         * back the visitor that was created by
         * {@link #createPsiVisitor(JavaContext)}, but a visitor is not
         * required. It is intended for detectors that need to do additional AST
         * processing, but also want the convenience of not having to look for
         * method names on their own.
         *
         * @param context the context of the lint request
         * @param visitor the visitor created from
         *            {@link #createPsiVisitor(JavaContext)}, or null
         * @param node the {@link PsiNewExpression} node for the invoked method
         * @param constructor the called constructor method
         */
        void visitConstructor(
                @NonNull JavaContext context,
                @Nullable JavaElementVisitor visitor,
                @NonNull PsiNewExpression node,
                @NonNull PsiMethod constructor);

        /**
         * Return the list of reference names types this detector is interested in, or null. If this
         * method returns non-null, then any AST elements that match a reference in the list will be
         * passed to the {@link #visitReference(JavaContext, JavaElementVisitor,
         * PsiJavaCodeReferenceElement, PsiElement)} method for processing. The visitor created by
         * {@link #createJavaVisitor(JavaContext)} is also passed to that method, although it can be
         * null. <p> This makes it easy to write detectors that focus on some fixed references.
         *
         * @return a set of applicable reference names, or null.
         */
        @Nullable
        List<String> getApplicableReferenceNames();

        /**
         * Method invoked for any references found that matches any names returned by {@link
         * #getApplicableReferenceNames()}. This also passes back the visitor that was created by
         * {@link #createPsiVisitor(JavaContext)}, but a visitor is not required. It is intended for
         * detectors that need to do additional AST processing, but also want the convenience of not
         * having to look for method names on their own.
         *
         * @param context    the context of the lint request
         * @param visitor    the visitor created from {@link #createPsiVisitor(JavaContext)}, or
         *                   null
         * @param reference  the {@link PsiJavaCodeReferenceElement} element
         * @param referenced the referenced element
         */
        void visitReference(
                @NonNull JavaContext context,
                @Nullable JavaElementVisitor visitor,
                @NonNull PsiJavaCodeReferenceElement reference,
                @NonNull PsiElement referenced);

        /**
         * Returns whether this detector cares about Android resource references
         * (such as {@code R.layout.main} or {@code R.string.app_name}). If it
         * does, then the visitor will look for these patterns, and if found, it
         * will invoke {@link #visitResourceReference} passing the resource type
         * and resource name. It also passes the visitor, if any, that was
         * created by {@link #createJavaVisitor(JavaContext)}, such that a
         * detector can do more than just look for resources.
         *
         * @return true if this detector wants to be notified of R resource
         *         identifiers found in the code.
         */
        boolean appliesToResourceRefs();

        /**
         * Called for any resource references (such as {@code R.layout.main}
         * found in Java code, provided this detector returned {@code true} from
         * {@link #appliesToResourceRefs()}.
         *
         * @param context the lint scanning context
         * @param visitor the visitor created from
         *            {@link #createPsiVisitor(JavaContext)}, or null
         * @param node the variable reference for the resource
         * @param type the resource type, such as "layout" or "string"
         * @param name the resource name, such as "main" from
         *            {@code R.layout.main}
         * @param isFramework whether the resource is a framework resource
         *            (android.R) or a local project resource (R)
         */
        void visitResourceReference(
                @NonNull JavaContext context,
                @Nullable JavaElementVisitor visitor,
                @NonNull PsiElement node,
                @NonNull ResourceType type,
                @NonNull String name,
                boolean isFramework);

        /**
         * Returns a list of fully qualified names for super classes that this
         * detector cares about. If not null, this detector will <b>only</b> be called
         * if the current class is a subclass of one of the specified superclasses.
         *
         * @return a list of fully qualified names
         */
        @Nullable
        List<String> applicableSuperClasses();

        /**
         * Called for each class that extends one of the super classes specified with
         * {@link #applicableSuperClasses()}.
         * <p>
         * Note: This method will not be called for {@link PsiTypeParameter} classes. These
         * aren't really classes in the sense most lint detectors think of them, so these
         * are excluded to avoid having lint checks that don't defensively code for these
         * accidentally report errors on type parameters. If you really need to check these,
         * use {@link #getApplicablePsiTypes} with {@code PsiTypeParameter.class} instead.
         *
         * @param context the lint scanning context
         * @param declaration the class declaration node, or null for anonymous classes
         */
        void checkClass(@NonNull JavaContext context, @NonNull PsiClass declaration);
    }

    public interface UastScanner {
        /**
         * Create a parse tree visitor to process the parse tree. All
         * {@link JavaScanner} detectors must provide a visitor, unless they
         * either return true from {@link #appliesToResourceRefs()} or return
         * non null from {@link #getApplicableMethodNames()}.
         * <p>
         * If you return specific AST node types from
         * {@link #getApplicablePsiTypes()}, then the visitor will <b>only</b>
         * be called for the specific requested node types. This is more
         * efficient, since it allows many detectors that apply to only a small
         * part of the AST (such as method call nodes) to share iteration of the
         * majority of the parse tree.
         * <p>
         * If you return null from {@link #getApplicablePsiTypes()}, then your
         * visitor will be called from the top and all node types visited.
         * <p>
         * Note that a new visitor is created for each separate compilation
         * unit, so you can store per file state in the visitor.
         * <p>
         * <b>
         * NOTE: Your visitor should <b>NOT</b> extend JavaRecursiveElementVisitor.
         * Your visitor should only visit the current node type; the infrastructure
         * will do the recursion. (Lint's unit test infrastructure will check and
         * enforce this restriction.)
         * </b>
         *
         * @param context the {@link Context} for the file being analyzed
         * @return a visitor, or null.
         */
        @Nullable
        UastVisitor createUastVisitor(@NonNull JavaContext context);

        /**
         * Return the types of AST nodes that the visitor returned from
         * {@link #createJavaVisitor(JavaContext)} should visit. See the
         * documentation for {@link #createJavaVisitor(JavaContext)} for details
         * on how the shared visitor is used.
         * <p>
         * If you return null from this method, then the visitor will process
         * the full tree instead.
         * <p>
         * Note that for the shared visitor, the return codes from the visit
         * methods are ignored: returning true will <b>not</b> prune iteration
         * of the subtree, since there may be other node types interested in the
         * children. If you need to ensure that your visitor only processes a
         * part of the tree, use a full visitor instead. See the
         * OverdrawDetector implementation for an example of this.
         *
         * @return the list of applicable node types (AST node classes), or null
         */
        @Nullable
        List<Class<? extends UElement>> getApplicableUastTypes();

        @Nullable
        List<Class<? extends PsiElement>> getApplicablePsiTypes();
        
        /**
         * Return the list of method names this detector is interested in, or
         * null. If this method returns non-null, then any AST nodes that match
         * a method call in the list will be passed to the
         * {@link #visitMethod(JavaContext, JavaElementVisitor, PsiMethodCallExpression, PsiMethod)}
         * method for processing. The visitor created by
         * {@link #createPsiVisitor(JavaContext)} is also passed to that
         * method, although it can be null.
         * <p>
         * This makes it easy to write detectors that focus on some fixed calls.
         * For example, the StringFormatDetector uses this mechanism to look for
         * "format" calls, and when found it looks around (using the AST's
         * {@link PsiElement#getParent()} method) to see if it's called on
         * a String class instance, and if so do its normal processing. Note
         * that since it doesn't need to do any other AST processing, that
         * detector does not actually supply a visitor.
         *
         * @return a set of applicable method names, or null.
         */
        @Nullable
        List<String> getApplicableMethodNames();

        /**
         * Method invoked for any method calls found that matches any names
         * returned by {@link #getApplicableMethodNames()}. This also passes
         * back the visitor that was created by
         * {@link #createJavaVisitor(JavaContext)}, but a visitor is not
         * required. It is intended for detectors that need to do additional AST
         * processing, but also want the convenience of not having to look for
         * method names on their own.
         *
         * @param context the context of the lint request
         * @param visitor the visitor created from
         *            {@link #createPsiVisitor(JavaContext)}, or null
         * @param node the {@link PsiMethodCallExpression} node for the invoked method
         * @param method the {@link PsiMethod} being called
         */
        void visitMethod(
                @NonNull JavaContext context,
                @Nullable UastVisitor visitor,
                @NonNull UCallExpression node,
                @NonNull UMethod method);

        /**
         * Return the list of constructor types this detector is interested in, or
         * null. If this method returns non-null, then any AST nodes that match
         * a constructor call in the list will be passed to the
         * {@link #visitConstructor(JavaContext, JavaElementVisitor, PsiNewExpression, PsiMethod)}
         * method for processing. The visitor created by
         * {@link #createJavaVisitor(JavaContext)} is also passed to that
         * method, although it can be null.
         * <p>
         * This makes it easy to write detectors that focus on some fixed constructors.
         *
         * @return a set of applicable fully qualified types, or null.
         */
        @Nullable
        List<String> getApplicableConstructorTypes();

        /**
         * Method invoked for any constructor calls found that matches any names
         * returned by {@link #getApplicableConstructorTypes()}. This also passes
         * back the visitor that was created by
         * {@link #createPsiVisitor(JavaContext)}, but a visitor is not
         * required. It is intended for detectors that need to do additional AST
         * processing, but also want the convenience of not having to look for
         * method names on their own.
         *
         * @param context the context of the lint request
         * @param visitor the visitor created from
         *            {@link #createPsiVisitor(JavaContext)}, or null
         * @param node the {@link PsiNewExpression} node for the invoked method
         * @param constructor the called constructor method
         */
        void visitConstructor(
                @NonNull JavaContext context,
                @Nullable UastVisitor visitor,
                @NonNull UCallExpression node,
                @NonNull UMethod constructor);

        /**
         * Return the list of reference names types this detector is interested in, or null. If this
         * method returns non-null, then any AST elements that match a reference in the list will be
         * passed to the {@link #visitReference(JavaContext, JavaElementVisitor,
         * PsiJavaCodeReferenceElement, PsiElement)} method for processing. The visitor created by
         * {@link #createJavaVisitor(JavaContext)} is also passed to that method, although it can be
         * null. <p> This makes it easy to write detectors that focus on some fixed references.
         *
         * @return a set of applicable reference names, or null.
         */
        @Nullable
        List<String> getApplicableReferenceNames();

        /**
         * Method invoked for any references found that matches any names returned by {@link
         * #getApplicableReferenceNames()}. This also passes back the visitor that was created by
         * {@link #createPsiVisitor(JavaContext)}, but a visitor is not required. It is intended for
         * detectors that need to do additional AST processing, but also want the convenience of not
         * having to look for method names on their own.
         *
         * @param context    the context of the lint request
         * @param visitor    the visitor created from {@link #createPsiVisitor(JavaContext)}, or
         *                   null
         * @param reference  the {@link PsiJavaCodeReferenceElement} element
         * @param referenced the referenced element
         */
        void visitReference(
                @NonNull JavaContext context,
                @Nullable UastVisitor visitor,
                @NonNull UReferenceExpression reference,
                @NonNull PsiElement referenced);

        /**
         * Returns whether this detector cares about Android resource references
         * (such as {@code R.layout.main} or {@code R.string.app_name}). If it
         * does, then the visitor will look for these patterns, and if found, it
         * will invoke {@link #visitResourceReference} passing the resource type
         * and resource name. It also passes the visitor, if any, that was
         * created by {@link #createJavaVisitor(JavaContext)}, such that a
         * detector can do more than just look for resources.
         *
         * @return true if this detector wants to be notified of R resource
         *         identifiers found in the code.
         */
        boolean appliesToResourceRefs();

        /**
         * Called for any resource references (such as {@code R.layout.main}
         * found in Java code, provided this detector returned {@code true} from
         * {@link #appliesToResourceRefs()}.
         *
         * @param context the lint scanning context
         * @param visitor the visitor created from
         *            {@link #createPsiVisitor(JavaContext)}, or null
         * @param node the variable reference for the resource
         * @param type the resource type, such as "layout" or "string"
         * @param name the resource name, such as "main" from
         *            {@code R.layout.main}
         * @param isFramework whether the resource is a framework resource
         *            (android.R) or a local project resource (R)
         */
        void visitResourceReference(
                @NonNull JavaContext context,
                @Nullable UastVisitor visitor,
                @NonNull UElement node,
                @NonNull ResourceType type,
                @NonNull String name,
                boolean isFramework);

        /**
         * Returns a list of fully qualified names for super classes that this
         * detector cares about. If not null, this detector will <b>only</b> be called
         * if the current class is a subclass of one of the specified superclasses.
         *
         * @return a list of fully qualified names
         */
        @Nullable
        List<String> applicableSuperClasses();

        /**
         * Called for each class that extends one of the super classes specified with
         * {@link #applicableSuperClasses()}.
         * <p>
         * Note: This method will not be called for {@link PsiTypeParameter} classes. These
         * aren't really classes in the sense most lint detectors think of them, so these
         * are excluded to avoid having lint checks that don't defensively code for these
         * accidentally report errors on type parameters. If you really need to check these,
         * use {@link #getApplicablePsiTypes} with {@code PsiTypeParameter.class} instead.
         *
         * @param context the lint scanning context
         * @param declaration the class declaration node, or null for anonymous classes
         */
        void checkClass(@NonNull JavaContext context, @NonNull UClass declaration);
    }

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
         * Return the list of method call names (in VM format, e.g. {@code "<init>"} for
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

    /**
     * Specialized interface for detectors that scan binary resource files
     * (typically bitmaps but also files in res/raw)
     */
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
    @Deprecated // Slated for removal in lint 2.0
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
    @Deprecated // Slated for removal in Lint 2.0
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
    @Deprecated // Slated for removal in Lint 2.0
    public Speed getSpeed(@SuppressWarnings("UnusedParameters") @NonNull Issue issue) {
        // If not overridden, this detector does not distinguish speed by issue type
        return getSpeed();
    }

    // ---- Dummy implementations to make implementing XmlScanner easier: ----

    @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        // This method must be overridden if your detector does
        // not return something from getApplicableElements or
        // getApplicableAttributes
        assert false;
    }

    @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        // This method must be overridden if your detector returns
        // tag names from getApplicableElements
        assert false;
    }

    @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void visitElementAfter(@NonNull XmlContext context, @NonNull Element element) {
    }

    @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
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

    @Deprecated @Nullable @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return null;
    }

    @Deprecated @Nullable@SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return null;
    }

    @Deprecated @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
    }

    @Deprecated @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void visitResourceReference(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull Node node, @NonNull String type, @NonNull String name,
            boolean isFramework) {
    }

    @Deprecated @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void checkClass(@NonNull JavaContext context, @Nullable ClassDeclaration declaration,
            @NonNull Node node, @NonNull ResolvedClass resolvedClass) {
    }

    @Deprecated @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void visitConstructor(
            @NonNull JavaContext context,
            @Nullable AstVisitor visitor,
            @NonNull ConstructorInvocation node,
            @NonNull ResolvedMethod constructor) {
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

    @SuppressWarnings({"UnusedParameters", "unused"})
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

    // ---- Dummy implementation to make implementing UastScanner easier: ----

    public void checkClass(@NonNull JavaContext context, @NonNull UClass declaration) {
    }
    
    public void visitReference(
            @NonNull JavaContext context,
            @Nullable UastVisitor visitor,
            @NonNull UReferenceExpression reference,
            @NonNull PsiElement referenced) {
    }

    public void visitConstructor(
            @NonNull JavaContext context,
            @Nullable UastVisitor visitor,
            @NonNull UCallExpression node,
            @NonNull UMethod constructor) {
    }

    public void visitMethod(
            @NonNull JavaContext context,
            @Nullable UastVisitor visitor,
            @NonNull UCallExpression node,
            @NonNull UMethod method) {
    }

    @Nullable
    public UastVisitor createUastVisitor(@NonNull JavaContext context) {
        return null;
    }
    
    @Nullable
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return null;
    }

    public void visitResourceReference(
            @NonNull JavaContext context,
            @Nullable UastVisitor visitor,
            @NonNull UElement node,
            @NonNull ResourceType type,
            @NonNull String name,
            boolean isFramework) {
    }
    
    // ---- Dummy implementation to make implementing JavaPsiScanner easier: ----

    @Nullable
    public List<String> getApplicableMethodNames() {
        return null;
    }

    @Nullable @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public List<String> getApplicableConstructorTypes() {
        return null;
    }

    @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public boolean appliesToResourceRefs() {
        return false;
    }

    @Nullable @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public List<String> applicableSuperClasses() {
        return null;
    }

    @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void visitMethod(@NonNull JavaContext context, @Nullable JavaElementVisitor visitor,
            @NonNull PsiMethodCallExpression call, @NonNull PsiMethod method) {
    }

    @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void visitConstructor(
            @NonNull JavaContext context,
            @Nullable JavaElementVisitor visitor,
            @NonNull PsiNewExpression node,
            @NonNull PsiMethod constructor) {
    }

    @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void visitResourceReference(@NonNull JavaContext context,
            @Nullable JavaElementVisitor visitor, @NonNull PsiElement node,
            @NonNull ResourceType type, @NonNull String name, boolean isFramework) {
    }

    @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void checkClass(@NonNull JavaContext context, @NonNull PsiClass declaration) {
    }

    @Nullable @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public JavaElementVisitor createPsiVisitor(@NonNull JavaContext context) {
        return null;
    }

    @Nullable @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public List<Class<? extends PsiElement>> getApplicablePsiTypes() {
        return null;
    }

    @Nullable @SuppressWarnings({"unused", "javadoc"})
    public List<String> getApplicableReferenceNames() {
        return null;
    }

    @SuppressWarnings({"UnusedParameters", "unused", "javadoc"})
    public void visitReference(
            @NonNull JavaContext context,
            @Nullable JavaElementVisitor visitor,
            @NonNull PsiJavaCodeReferenceElement reference,
            @NonNull PsiElement referenced) {
    }
}
