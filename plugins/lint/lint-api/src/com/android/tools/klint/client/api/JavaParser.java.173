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

package com.android.tools.klint.client.api;

import static com.android.SdkConstants.ATTR_VALUE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.ClassContext;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.DefaultPosition;
import com.android.tools.klint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Position;
import com.google.common.annotations.Beta;
import com.google.common.base.Splitter;
import com.intellij.mock.MockProject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiType;

import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UastContext;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import lombok.ast.Catch;
import lombok.ast.For;
import lombok.ast.Identifier;
import lombok.ast.If;
import lombok.ast.Node;
import lombok.ast.Return;
import lombok.ast.StrictListAccessor;
import lombok.ast.Switch;
import lombok.ast.Throw;
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.While;

/**
 * A wrapper for a Java parser. This allows tools integrating lint to map directly
 * to builtin services, such as already-parsed data structures in Java editors.
 * <p>
 * <b>NOTE: This is not public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
// Currently ships with deprecated API support
@SuppressWarnings({"deprecation", "UnusedParameters"})
@Beta
public abstract class JavaParser {
    public static final String TYPE_OBJECT = "java.lang.Object";
    public static final String TYPE_STRING = "java.lang.String";
    public static final String TYPE_INT = "int";
    public static final String TYPE_LONG = "long";
    public static final String TYPE_CHAR = "char";
    public static final String TYPE_FLOAT = "float";
    public static final String TYPE_DOUBLE = "double";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_SHORT = "short";
    public static final String TYPE_BYTE = "byte";
    public static final String TYPE_NULL = "null";
    public static final String TYPE_INTEGER_WRAPPER = "java.lang.Integer";
    public static final String TYPE_BOOLEAN_WRAPPER = "java.lang.Boolean";
    public static final String TYPE_BYTE_WRAPPER = "java.lang.Byte";
    public static final String TYPE_SHORT_WRAPPER = "java.lang.Short";
    public static final String TYPE_LONG_WRAPPER = "java.lang.Long";
    public static final String TYPE_DOUBLE_WRAPPER = "java.lang.Double";
    public static final String TYPE_FLOAT_WRAPPER = "java.lang.Float";
    public static final String TYPE_CHARACTER_WRAPPER = "java.lang.Character";

    /**
     * Prepare to parse the given contexts. This method will be called before
     * a series of {@link #parseJava(JavaContext)} calls, which allows some
     * parsers to do up front global computation in case they want to more
     * efficiently process multiple files at the same time. This allows a single
     * type-attribution pass for example, which is a lot more efficient than
     * performing global type analysis over and over again for each individual
     * file
     *
     * @param contexts a list of contexts to be parsed
     */
    public abstract void prepareJavaParse(@NonNull List<JavaContext> contexts);

    /**
     * Parse the file pointed to by the given context.
     *
     * @param context the context pointing to the file to be parsed, typically
     *            via {@link Context#getContents()} but the file handle (
     *            {@link Context#file} can also be used to map to an existing
     *            editor buffer in the surrounding tool, etc)
     * @return the compilation unit node for the file
     * @deprecated Use {@link #parseJavaToPsi(JavaContext)} instead
     */
    @Deprecated
    @Nullable
    public Node parseJava(@NonNull JavaContext context) {
        return null;
    }

    /**
     * Parse the file pointed to by the given context.
     *
     * @param context the context pointing to the file to be parsed, typically
     *            via {@link Context#getContents()} but the file handle (
     *            {@link Context#file} can also be used to map to an existing
     *            editor buffer in the surrounding tool, etc)
     * @return the compilation unit node for the file
     */
    @Nullable
    public abstract PsiJavaFile parseJavaToPsi(@NonNull JavaContext context);

    /**
     * Returns an evaluator which can perform various resolution tasks,
     * evaluate inheritance lookup etc.
     *
     * @return an evaluator
     */
    @NonNull
    public abstract JavaEvaluator getEvaluator();
    
    public abstract Project getIdeaProject();
    
    public abstract UastContext getUastContext();

    /**
     * Returns a {@link Location} for the given node
     *
     * @param context information about the file being parsed
     * @param node    the node to create a location for
     * @return a location for the given node
     * @deprecated Use {@link #getNameLocation(JavaContext, PsiElement)} instead
     */
    @Deprecated
    @NonNull
    public Location getLocation(@NonNull JavaContext context, @NonNull Node node) {
        // No longer mandatory to override for children; this is a deprecated API
        return Location.NONE;
    }

    /**
     * Returns a {@link Location} for the given element
     *
     * @param context information about the file being parsed
     * @param element the element to create a location for
     * @return a location for the given node
     */
    @SuppressWarnings("MethodMayBeStatic") // subclasses may want to override/optimize
    @NonNull
    public Location getLocation(@NonNull JavaContext context, @NonNull PsiElement element) {
        TextRange range = element.getTextRange();
        UFile uFile = (UFile) getUastContext().convertElementWithParent(element.getContainingFile(), UFile.class);
        if (uFile == null) {
            return Location.NONE;
        }
        
        PsiFile containingFile = uFile.getPsi();
        File file = context.file;
        if (containingFile != context.getUFile().getPsi()) {
            // Reporting an error in a different file.
            if (context.getDriver().getScope().size() == 1) {
                // Don't bother with this error if it's in a different file during single-file analysis
                return Location.NONE;
            }
            VirtualFile virtualFile = containingFile.getVirtualFile();
            if (virtualFile == null) {
                return Location.NONE;
            }
            file = VfsUtilCore.virtualToIoFile(virtualFile);
        }
        return Location.create(file, context.getContents(), range.getStartOffset(),
                               range.getEndOffset());
    }

    /**
     * Returns a {@link Location} for the given node range (from the starting offset of the first
     * node to the ending offset of the second node).
     *
     * @param context   information about the file being parsed
     * @param from      the AST node to get a starting location from
     * @param fromDelta Offset delta to apply to the starting offset
     * @param to        the AST node to get a ending location from
     * @param toDelta   Offset delta to apply to the ending offset
     * @return a location for the given node
     * @deprecated Use {@link #getRangeLocation(JavaContext, PsiElement, int, PsiElement, int)}
     * instead
     */
    @Deprecated
    @NonNull
    public abstract Location getRangeLocation(
            @NonNull JavaContext context,
            @NonNull Node from,
            int fromDelta,
            @NonNull Node to,
            int toDelta);

    /**
     * Returns a {@link Location} for the given node range (from the starting offset of the first
     * node to the ending offset of the second node).
     *
     * @param context   information about the file being parsed
     * @param from      the AST node to get a starting location from
     * @param fromDelta Offset delta to apply to the starting offset
     * @param to        the AST node to get a ending location from
     * @param toDelta   Offset delta to apply to the ending offset
     * @return a location for the given node
     */
    @SuppressWarnings("MethodMayBeStatic") // subclasses may want to override/optimize
    @NonNull
    public Location getRangeLocation(
            @NonNull JavaContext context,
            @NonNull PsiElement from,
            int fromDelta,
            @NonNull PsiElement to,
            int toDelta) {
        String contents = context.getContents();
        int start = Math.max(0, from.getTextRange().getStartOffset() + fromDelta);
        int end = Math.min(contents == null ? Integer.MAX_VALUE : contents.length(),
                to.getTextRange().getEndOffset() + toDelta);
        return Location.create(context.file, contents, start, end);
    }

    /**
     * Like {@link #getRangeLocation(JavaContext, PsiElement, int, PsiElement, int)}
     * but both offsets are relative to the starting offset of the given node. This is
     * sometimes more convenient than operating relative to the ending offset when you
     * have a fixed range in mind.
     *
     * @param context   information about the file being parsed
     * @param from      the AST node to get a starting location from
     * @param fromDelta Offset delta to apply to the starting offset
     * @param toDelta   Offset delta to apply to the starting offset
     * @return a location for the given node
     */
    @SuppressWarnings("MethodMayBeStatic") // subclasses may want to override/optimize
    @NonNull
    public Location getRangeLocation(
            @NonNull JavaContext context,
            @NonNull PsiElement from,
            int fromDelta,
            int toDelta) {
        return getRangeLocation(context, from, fromDelta, from,
                -(from.getTextRange().getLength() - toDelta));
    }

    /**
     * Returns a {@link Location} for the given node. This attempts to pick a shorter
     * location range than the entire node; for a class or method for example, it picks
     * the name node (if found). For statement constructs such as a {@code switch} statement
     * it will highlight the keyword, etc.
     *
     * @param context information about the file being parsed
     * @param node the node to create a location for
     * @return a location for the given node
     * @deprecated Use {@link #getNameLocation(JavaContext, PsiElement)} instead
     */
    @Deprecated
    @NonNull
    public Location getNameLocation(@NonNull JavaContext context, @NonNull Node node) {
        Node nameNode = JavaContext.findNameNode(node);
        if (nameNode != null) {
            node = nameNode;
        } else {
            if (node instanceof Switch
                    || node instanceof For
                    || node instanceof If
                    || node instanceof While
                    || node instanceof Throw
                    || node instanceof Return) {
                // Lint doesn't want to highlight the entire statement/block associated
                // with this node, it wants to just highlight the keyword.
                Location location = getLocation(context, node);
                Position start = location.getStart();
                if (start != null) {
                    // The Lombok classes happen to have the same length as the target keyword
                    int length = node.getClass().getSimpleName().length();
                    return Location.create(location.getFile(), start,
                            new DefaultPosition(start.getLine(), start.getColumn() + length,
                                    start.getOffset() + length));
                }
            }
        }

        return getLocation(context, node);
    }

    /**
     * Returns a {@link Location} for the given node. This attempts to pick a shorter
     * location range than the entire node; for a class or method for example, it picks
     * the name node (if found). For statement constructs such as a {@code switch} statement
     * it will highlight the keyword, etc.
     *
     * @param context information about the file being parsed
     * @param element the node to create a location for
     * @return a location for the given node
     */
    @NonNull
    public Location getNameLocation(@NonNull JavaContext context, @NonNull PsiElement element) {
        PsiElement nameNode = JavaContext.findNameElement(element);
        if (nameNode != null) {
            element = nameNode;
        }

        return getLocation(context, element);
    }
    /**
     * Creates a light-weight handle to a location for the given node. It can be
     * turned into a full fledged location by
     * {@link com.android.tools.lint.detector.api.Location.Handle#resolve()}.
     *
     * @param context the context providing the node
     * @param node the node (element or attribute) to create a location handle
     *            for
     * @return a location handle
     * @deprecated Use PSI instead (where handles aren't necessary; use PsiElement directly)
     */
    @Deprecated
    @NonNull
    public abstract Location.Handle createLocationHandle(@NonNull JavaContext context,
            @NonNull Node node);

    /**
     * Dispose any data structures held for the given context.
     * @param context information about the file previously parsed
     * @param compilationUnit the compilation unit being disposed
     * @deprecated Use {@link #dispose(JavaContext, PsiJavaFile)} instead
     */
    @Deprecated
    public void dispose(@NonNull JavaContext context, @NonNull Node compilationUnit) {
    }

    /**
     * Dispose any data structures held for the given context.
     * @param context information about the file previously parsed
     * @param compilationUnit the compilation unit being disposed
     */
    public void dispose(@NonNull JavaContext context, @NonNull PsiFile compilationUnit) {
    }

    /**
     * Dispose any data structures held for the given context.
     * @param context information about the file previously parsed
     * @param compilationUnit the compilation unit being disposed
     */
    public void dispose(@NonNull JavaContext context, @NonNull UFile compilationUnit) {
    }

    /**
     * Dispose any remaining data structures held for all contexts.
     * Typically frees up any resources allocated by
     * {@link #prepareJavaParse(List)}
     */
    public void dispose() {
    }

    /**
     * Resolves the given expression node: computes the declaration for the given symbol
     *
     * @param context information about the file being parsed
     * @param node the node to resolve
     * @return a node representing the resolved fully type: class/interface/annotation,
     *          field, method or variable
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @Deprecated
    @Nullable
    public ResolvedNode resolve(@NonNull JavaContext context, @NonNull Node node) {
        return null;
    }

    /**
     * Finds the given type, if possible (which should be reachable from the compilation
     * patch of the given node.
     *
     * @param context information about the file being parsed
     * @param fullyQualifiedName the fully qualified name of the class to look up
     * @return the class, or null if not found
     */
    @Nullable
    public ResolvedClass findClass(
            @NonNull JavaContext context,
            @NonNull String fullyQualifiedName) {
        return null;
    }

    /**
     * Returns the set of exception types handled by the given catch block.
     * <p>
     * This is a workaround for the fact that the Lombok AST API (and implementation)
     * doesn't support multi-catch statements.
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @Deprecated
    public List<TypeDescriptor> getCatchTypes(@NonNull JavaContext context,
            @NonNull Catch catchBlock) {
        TypeReference typeReference = catchBlock.astExceptionDeclaration().astTypeReference();
        return Collections.<TypeDescriptor>singletonList(new DefaultTypeDescriptor(
                typeReference.getTypeName()));
    }

    /**
     * Gets the type of the given node
     *
     * @param context information about the file being parsed
     * @param node the node to look up the type for
     * @return the type of the node, if known
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @Deprecated
    @Nullable
    public TypeDescriptor getType(@NonNull JavaContext context, @NonNull Node node) {
        return null;
    }

    /**
     * Runs the given runnable under a readlock such that it can access the PSI
     *
     * @param runnable the runnable to be run
     */
    public abstract void runReadAction(@NonNull Runnable runnable);

    /**
     * A description of a type, such as a primitive int or the android.app.Activity class
     * @deprecated Use {@link PsiType} instead
     */
    @SuppressWarnings("unused")
    @Deprecated
    public abstract static class TypeDescriptor {
        /**
         * Returns the fully qualified name of the type, such as "int" or "android.app.Activity"
         * */
        @NonNull public abstract String getName();

        /** Returns the simple name of this class */
        @NonNull
        public String getSimpleName() {
            // This doesn't handle inner classes properly, so subclasses with more
            // accurate type information will override to handle it correctly.
            String name = getName();
            int index = name.lastIndexOf('.');
            if (index != -1) {
                return name.substring(index + 1);
            }
            return name;
        }

        /**
         * Returns the full signature of the type, which is normally the same as {@link #getName()}
         * but for arrays can include []'s, for generic methods can include generics parameters
         * etc
         */
        @NonNull public abstract String getSignature();

        /**
         * Computes the internal class name of the given fully qualified class name.
         * For example, it converts foo.bar.Foo.Bar into foo/bar/Foo$Bar.
         * This should only be called for class types, not primitives.
         *
         * @return the internal class name
         */
        @NonNull public String getInternalName() {
            return ClassContext.getInternalName(getName());
        }

        public abstract boolean matchesName(@NonNull String name);

        /**
         * Returns true if the given TypeDescriptor represents an array
         * @return true if this type represents an array
         */
        public abstract boolean isArray();

        /**
         * Returns true if the given TypeDescriptor represents a primitive
         * @return true if this type represents a primitive
         */
        public abstract boolean isPrimitive();

        public abstract boolean matchesSignature(@NonNull String signature);

        @NonNull
        public TypeReference getNode() {
            TypeReference typeReference = new TypeReference();
            StrictListAccessor<TypeReferencePart, TypeReference> parts = typeReference.astParts();
            for (String part : Splitter.on('.').split(getName())) {
                Identifier identifier = Identifier.of(part);
                parts.addToEnd(new TypeReferencePart().astIdentifier(identifier));
            }

            return typeReference;
        }

        /** If the type is not primitive, returns the class of the type if known */
        @Nullable
        public abstract ResolvedClass getTypeClass();

        @Override
        public abstract boolean equals(Object o);

        @Override
        public String toString() {
            return getName();
        }
    }

    /**
     * Convenience implementation of {@link TypeDescriptor}
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @Deprecated
    public static class DefaultTypeDescriptor extends TypeDescriptor {

        private String mName;

        public DefaultTypeDescriptor(String name) {
            mName = name;
        }

        @NonNull
        @Override
        public String getName() {
            return mName;
        }

        @NonNull
        @Override
        public String getSignature() {
            return getName();
        }

        @Override
        public boolean matchesName(@NonNull String name) {
            return mName.equals(name);
        }

        @Override
        public boolean isArray() {
            return mName.endsWith("[]");
        }

        @Override
        public boolean isPrimitive() {
            return mName.indexOf('.') != -1;
        }

        @Override
        public boolean matchesSignature(@NonNull String signature) {
            return matchesName(signature);
        }

        @Override
        public String toString() {
            return getSignature();
        }

        @Override
        @Nullable
        public ResolvedClass getTypeClass() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DefaultTypeDescriptor that = (DefaultTypeDescriptor) o;

            return !(mName != null ? !mName.equals(that.mName) : that.mName != null);

        }

        @Override
        public int hashCode() {
            return mName != null ? mName.hashCode() : 0;
        }
    }

    /**
     * A resolved declaration from an AST Node reference
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @SuppressWarnings("unused")
    @Deprecated
    public abstract static class ResolvedNode {
        @NonNull
        public abstract String getName();

        /** Returns the signature of the resolved node */
        public abstract String getSignature();

        public abstract int getModifiers();

        @Override
        public String toString() {
            return getSignature();
        }

        /** Returns any annotations defined on this node */
        @NonNull
        public abstract Iterable<ResolvedAnnotation> getAnnotations();

        /**
         * Searches for the annotation of the given type on this node
         *
         * @param type the fully qualified name of the annotation to check
         * @return the annotation, or null if not found
         */
        @Nullable
        public ResolvedAnnotation getAnnotation(@NonNull String type) {
            for (ResolvedAnnotation annotation : getAnnotations()) {
                if (annotation.getType().matchesSignature(type)) {
                    return annotation;
                }
            }

            return null;
        }

        /**
         * Returns true if this element is in the given package (or optionally, in one of its sub
         * packages)
         *
         * @param pkg                the package name
         * @param includeSubPackages whether to include subpackages
         * @return true if the element is in the given package
         */
        public boolean isInPackage(@NonNull String pkg, boolean includeSubPackages) {
            return getSignature().startsWith(pkg);
        }

        /**
         * Attempts to find the corresponding AST node, if possible. This won't work if for example
         * the resolved node is from a binary (such as a compiled class in a .jar) or if the
         * underlying parser doesn't support it.
         * <p>
         * Note that looking up the AST node can result in different instances for each lookup.
         *
         * @return an AST node, if possible.
         */
        @Nullable
        public Node findAstNode() {
            return null;
        }
    }

    /**
     * A resolved class declaration (class, interface, enumeration or annotation)
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @SuppressWarnings("unused")
    @Deprecated
    public abstract static class ResolvedClass extends ResolvedNode {
        /** Returns the fully qualified name of this class */
        @Override
        @NonNull
        public abstract String getName();

        /** Returns the simple name of this class */
        @NonNull
        public abstract String getSimpleName();

        /** Returns the package name of this class */
        @NonNull
        public String getPackageName() {
            String name = getName();
            String simpleName = getSimpleName();
            if (name.length() > simpleName.length() + 1) {
                return name.substring(0, name.length() - simpleName.length() - 1);
            }
            return name;
        }

        /** Returns whether this class' fully qualified name matches the given name */
        public abstract boolean matches(@NonNull String name);

        @Nullable
        public abstract ResolvedClass getSuperClass();

        @NonNull
        public abstract Iterable<ResolvedClass> getInterfaces();

        @Nullable
        public abstract ResolvedClass getContainingClass();

        public abstract boolean isInterface();
        public abstract boolean isEnum();

        public TypeDescriptor getType() {
            return new DefaultTypeDescriptor(getName());
        }

        /**
         * Determines whether this class extends the given name. If strict is true,
         * it will not consider C extends C true.
         * <p>
         * The target must be a class; to check whether this class extends an interface,
         * use {@link #isImplementing(String,boolean)} instead. If you're not sure, use
         * {@link #isInheritingFrom(String, boolean)}.
         *
         * @param name the fully qualified class name
         * @param strict if true, do not consider a class to be extending itself
         * @return true if this class extends the given class
         */
        public abstract boolean isSubclassOf(@NonNull String name, boolean strict);

        /**
         * Determines whether this is implementing the given interface.
         * <p>
         * The target must be an interface; to check whether this class extends a class,
         * use {@link #isSubclassOf(String, boolean)} instead. If you're not sure, use
         * {@link #isInheritingFrom(String, boolean)}.
         *
         * @param name the fully qualified interface name
         * @param strict if true, do not consider a class to be extending itself
         * @return true if this class implements the given interface
         */
        public abstract boolean isImplementing(@NonNull String name, boolean strict);

        /**
         * Determines whether this class extends or implements the class of the given name.
         * If strict is true, it will not consider C extends C true.
         * <p>
         * For performance reasons, if you know that the target is a class, consider using
         * {@link #isSubclassOf(String, boolean)} instead, and if the target is an interface,
         * consider using {@link #isImplementing(String,boolean)}.
         *
         * @param name the fully qualified class name
         * @param strict if true, do not consider a class to be inheriting from itself
         * @return true if this class extends or implements the given class
         */
        public abstract boolean isInheritingFrom(@NonNull String name, boolean strict);

        @NonNull
        public abstract Iterable<ResolvedMethod> getConstructors();

        /** Returns the methods defined in this class, and optionally any methods inherited from any superclasses as well */
        @NonNull
        public abstract Iterable<ResolvedMethod> getMethods(boolean includeInherited);

        /** Returns the methods of a given name defined in this class, and optionally any methods inherited from any superclasses as well */
        @NonNull
        public abstract Iterable<ResolvedMethod> getMethods(@NonNull String name, boolean includeInherited);

        /** Returns the fields defined in this class, and optionally any fields declared in any superclasses as well */
        @NonNull
        public abstract Iterable<ResolvedField> getFields(boolean includeInherited);

        /** Returns the named field defined in this class, or optionally inherited from a superclass */
        @Nullable
        public abstract ResolvedField getField(@NonNull String name, boolean includeInherited);

        /** Returns the package containing this class */
        @Nullable
        public abstract ResolvedPackage getPackage();

        @Override
        public boolean isInPackage(@NonNull String pkg, boolean includeSubPackages) {
            String packageName = getPackageName();

            //noinspection SimplifiableIfStatement
            if (pkg.equals(packageName)) {
                return true;
            }

            return includeSubPackages && packageName.length() > pkg.length() &&
                    packageName.charAt(pkg.length()) == '.' &&
                    packageName.startsWith(pkg);
        }
    }

    /**
     * A method or constructor declaration
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @SuppressWarnings("unused")
    @Deprecated
    public abstract static class ResolvedMethod extends ResolvedNode {
        @Override
        @NonNull
        public abstract String getName();

        /** Returns whether this method name matches the given name */
        public abstract boolean matches(@NonNull String name);

        @NonNull
        public abstract ResolvedClass getContainingClass();

        public abstract int getArgumentCount();

        @NonNull
        public abstract TypeDescriptor getArgumentType(int index);

        /** Returns true if the parameter at the given index matches the given type signature */
        public boolean argumentMatchesType(int index, @NonNull String signature) {
            return getArgumentType(index).matchesSignature(signature);
        }

        @Nullable
        public abstract TypeDescriptor getReturnType();

        public boolean isConstructor() {
            return getReturnType() == null;
        }

        /** Returns any annotations defined on the given parameter of this method */
        @NonNull
        public abstract Iterable<ResolvedAnnotation> getParameterAnnotations(int index);

        /**
         * Searches for the annotation of the given type on the method
         *
         * @param type the fully qualified name of the annotation to check
         * @param parameterIndex the index of the parameter to look up
         * @return the annotation, or null if not found
         */
        @Nullable
        public ResolvedAnnotation getParameterAnnotation(@NonNull String type,
                int parameterIndex) {
            for (ResolvedAnnotation annotation : getParameterAnnotations(parameterIndex)) {
                if (annotation.getType().matchesSignature(type)) {
                    return annotation;
                }
            }

            return null;
        }

        /** Returns the super implementation of the given method, if any */
        @Nullable
        public ResolvedMethod getSuperMethod() {
            if ((getModifiers() & Modifier.PRIVATE) != 0) {
                // Private methods aren't overriding anything
                return null;
            }
            ResolvedClass cls = getContainingClass().getSuperClass();
            if (cls != null) {
                String methodName = getName();
                int argCount = getArgumentCount();
                for (ResolvedMethod method : cls.getMethods(methodName, true)) {
                    if (argCount != method.getArgumentCount()) {
                        continue;
                    }
                    boolean sameTypes = true;
                    for (int arg = 0; arg < argCount; arg++) {
                        if (!method.getArgumentType(arg).equals(getArgumentType(arg))) {
                            sameTypes = false;
                            break;
                        }
                    }
                    if (sameTypes) {
                        if ((method.getModifiers() & Modifier.PRIVATE) != 0) {
                            // Normally can't override private methods - unless they're
                            // in the same compilation unit where the compiler will create
                            // an accessor method to trampoline over to it.
                            //
                            // Compare compilation units:
                            if (haveSameCompilationUnit(getContainingClass(),
                                    method.getContainingClass())) {
                                return method;
                            } else {
                                // We can stop the search; this is invalid (you can't have a
                                // private method in the middle of a chain; the compiler would
                                // complain about weaker access)
                                return null;
                            }
                        }
                        return method;
                    }
                }
            }

            return null;
        }

        @Override
        public boolean isInPackage(@NonNull String pkg, boolean includeSubPackages) {
            String packageName = getContainingClass().getPackageName();

            //noinspection SimplifiableIfStatement
            if (pkg.equals(packageName)) {
                return true;
            }

            return includeSubPackages && packageName.length() > pkg.length() &&
                    packageName.charAt(pkg.length()) == '.' &&
                    packageName.startsWith(pkg);
        }
    }

    /**
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @Deprecated
    private static boolean haveSameCompilationUnit(@Nullable ResolvedClass cls1,
            @Nullable ResolvedClass cls2) {
        if (cls1 == null || cls2 == null) {
            return false;
        }
        //noinspection ConstantConditions
        while (cls1.getContainingClass() != null) {
            cls1 = cls1.getContainingClass();
        }
        //noinspection ConstantConditions
        while (cls2.getContainingClass() != null) {
            cls2 = cls2.getContainingClass();
        }
        return cls1.equals(cls2);
    }

    /**
     * A field declaration
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @Deprecated
    public abstract static class ResolvedField extends ResolvedNode {
        @Override
        @NonNull
        public abstract String getName();

        /** Returns whether this field name matches the given name */
        public abstract boolean matches(@NonNull String name);

        @NonNull
        public abstract TypeDescriptor getType();

        @Nullable
        public abstract ResolvedClass getContainingClass();

        @Nullable
        public abstract Object getValue();

        @Nullable
        public String getContainingClassName() {
            ResolvedClass containingClass = getContainingClass();
            return containingClass != null ? containingClass.getName() : null;
        }

        @Override
        public boolean isInPackage(@NonNull String pkg, boolean includeSubPackages) {
            ResolvedClass containingClass = getContainingClass();
            if (containingClass == null) {
                return false;
            }

            String packageName = containingClass.getPackageName();

            //noinspection SimplifiableIfStatement
            if (pkg.equals(packageName)) {
                return true;
            }

            return includeSubPackages && packageName.length() > pkg.length() &&
                   packageName.charAt(pkg.length()) == '.' &&
                   packageName.startsWith(pkg);
        }
    }

    /**
     * An annotation <b>reference</b>. Note that this refers to a usage of an annotation,
     * not a declaraton of an annotation. You can call {@link #getClassType()} to
     * find the declaration for the annotation.
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @Deprecated
    public abstract static class ResolvedAnnotation extends ResolvedNode {
        @Override
        @NonNull
        public abstract String getName();

        /** Returns whether this field name matches the given name */
        public abstract boolean matches(@NonNull String name);

        @NonNull
        public abstract TypeDescriptor getType();

        /** Returns the {@link ResolvedClass} which defines the annotation */
        @Nullable
        public abstract ResolvedClass getClassType();

        public static class Value {
            @NonNull public final String name;
            @Nullable public final Object value;

            public Value(@NonNull String name, @Nullable Object value) {
                this.name = name;
                this.value = value;
            }
        }

        @NonNull
        public abstract List<Value> getValues();

        @Nullable
        public Object getValue(@NonNull String name) {
            for (Value value : getValues()) {
                if (name.equals(value.name)) {
                    return value.value;
                }
            }
            return null;
        }

        @Nullable
        public Object getValue() {
            return getValue(ATTR_VALUE);
        }

        @NonNull
        @Override
        public Iterable<ResolvedAnnotation> getAnnotations() {
            return Collections.emptyList();
        }
    }

    /**
     * A package declaration
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @SuppressWarnings("unused")
    @Deprecated
    public abstract static class ResolvedPackage extends ResolvedNode {
        /** Returns the parent package of this package, if any. */
        @Nullable
        public abstract ResolvedPackage getParentPackage();

        @NonNull
        @Override
        public Iterable<ResolvedAnnotation> getAnnotations() {
            return Collections.emptyList();
        }
    }

    /**
     * A local variable or parameter declaration
     * @deprecated Use {@link JavaPsiScanner} APIs instead
     */
    @Deprecated
    public abstract static class ResolvedVariable extends ResolvedNode {
        @Override
        @NonNull
        public abstract String getName();

        /** Returns whether this variable name matches the given name */
        public abstract boolean matches(@NonNull String name);

        @NonNull
        public abstract TypeDescriptor getType();
    }
}
