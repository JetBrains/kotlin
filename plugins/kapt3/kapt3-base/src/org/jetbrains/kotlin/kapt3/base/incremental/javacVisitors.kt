/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import com.sun.source.tree.*
import com.sun.source.util.SimpleTreeVisitor
import com.sun.source.util.TaskEvent
import com.sun.source.util.TaskListener
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import java.io.File
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

class MentionedTypesTaskListener(
    private val cache: JavaClassCache,
    private val elementUtils: Elements
) : TaskListener {

    var time = 0L
    override fun started(e: TaskEvent) {
        // do nothing, we just process on finish
    }

    override fun finished(e: TaskEvent) {
        if (e.kind != TaskEvent.Kind.ENTER || cache.isAlreadyProcessed(e.sourceFile.toUri())) return

        val l = System.currentTimeMillis()
        val compilationUnit = e.compilationUnit

        val structure = SourceFileStructure(e.sourceFile.toUri())

        val treeVisitor = TypeTreeVisitor(elementUtils, structure)
        compilationUnit.typeDecls.forEach {
            it.accept(treeVisitor, Visibility.ABI)
        }
        cache.addSourceStructure(structure)
        time += System.currentTimeMillis() - l
    }
}

private enum class Visibility {
    ABI, NON_ABI
}

private class TypeTreeVisitor(val elementUtils: Elements, val sourceStructure: SourceFileStructure) :
    SimpleTreeVisitor<Void, Visibility>() {

    /** Handle annotations on this class, including the @Inherited ones as those are not visible using Tree APIs. */
    private fun handleClassAnnotations(classSymbol: Symbol.ClassSymbol) {
        elementUtils.getAllAnnotationMirrors(classSymbol).forEach {
            (it.annotationType.asElement() as? TypeElement)?.let {
                sourceStructure.addMentionedAnnotations(it.qualifiedName.toString())
            }
        }
    }

    override fun visitClass(node: ClassTree, visibility: Visibility): Void? {
        node as JCTree.JCClassDecl
        sourceStructure.addDeclaredType(node.sym.fullname.toString())
        sourceStructure.addMentionedType(node.sym.fullname.toString())

        handleClassAnnotations(node.sym)

        node.modifiers.annotations.forEach { visit(it, visibility) }
        node.typeParameters.forEach { visit(it, visibility) }
        visit(node.extendsClause, visibility)
        node.implementsClause.forEach { visit(it, visibility) }
        node.members.forEach { visit(it, visibility) }

        return null
    }

    override fun visitParameterizedType(node: ParameterizedTypeTree, visibility: Visibility): Void? {
        visit(node.type, visibility)
        node.typeArguments.forEach { visit(it, visibility) }

        return null
    }

    override fun visitMethod(node: MethodTree, visibility: Visibility): Void? {
        val methodVisibility = if (node.modifiers.flags.contains(Modifier.PRIVATE)) Visibility.NON_ABI else Visibility.ABI

        node.modifiers.annotations.forEach { visit(it, methodVisibility) }
        visit(node.returnType, methodVisibility)
        node.parameters.forEach { visit(it, methodVisibility) }
        visit(node.defaultValue, methodVisibility)
        node.throws.forEach { visit(it, methodVisibility) }
        node.typeParameters.forEach { visit(it, methodVisibility) }

        return null
    }

    override fun visitAnnotatedType(node: AnnotatedTypeTree, visibility: Visibility): Void? {
        node.annotations.forEach { visit(it, visibility) }
        visit(node.underlyingType, visibility)

        return null
    }

    override fun visitVariable(node: VariableTree, visibility: Visibility): Void? {
        node as JCTree.JCVariableDecl
        val newVisibility = if (node.sym.getKind() == ElementKind.FIELD) {
            val flags = node.modifiers.getFlags()

            node.sym.constValue?.let { constValue ->
                if (flags.contains(Modifier.FINAL)
                    && flags.contains(Modifier.STATIC)
                    && !flags.contains(Modifier.PRIVATE)
                ) {
                    sourceStructure.addDefinedConstant(node.sym.simpleName.toString(), constValue)
                }
            }
            if (flags.contains(Modifier.PRIVATE)) Visibility.NON_ABI else Visibility.ABI
        } else {
            visibility
        }

        visit(node.getType(), newVisibility)
        node.modifiers.annotations.forEach { visit(it, newVisibility) }

        return null
    }

    override fun visitIdentifier(node: IdentifierTree, visibility: Visibility): Void? {
        node as JCTree.JCIdent
        maybeAddToTracker(node.sym, visibility)
        return null
    }

    override fun visitArrayType(node: ArrayTypeTree, visibility: Visibility): Void? {
        visit(node.type, visibility)
        return null
    }

    override fun visitNewArray(node: NewArrayTree, visibility: Visibility): Void? {
        node.annotations.forEach { visit(it, visibility) }
        node.dimAnnotations.forEach { visit(it, visibility) }
        visit(node.type, visibility)
        node.initializers.forEach { visit(it, visibility) }

        return null
    }

    override fun visitAnnotation(node: AnnotationTree, visibility: Visibility): Void? {
        visit(node.annotationType, visibility)
        node.arguments.forEach { visit(it, visibility) }

        return null
    }

    override fun visitTypeParameter(node: TypeParameterTree, visibility: Visibility): Void? {
        node.annotations.forEach { visit(it, visibility) }
        node.bounds.forEach { visit(it, visibility) }
        return null
    }

    override fun visitMemberSelect(node: MemberSelectTree, visibility: Visibility): Void? {
        // TODO (gavra): explore this for constant usage tracking
        node as JCTree.JCFieldAccess

        if (!maybeAddToTracker(node.sym, visibility)) {
            visit(node.expression, visibility)
        }
        return null
    }

    override fun visitWildcard(node: WildcardTree, visibility: Visibility): Void? {
        visit(node.bound, visibility)
        return null
    }

    private fun maybeAddToTracker(symbol: Symbol, visibility: Visibility): Boolean {
        val kind = symbol.getKind()
        if (!kind.isInterface && !kind.isClass) {
            return false
        }

        val qualifiedName = symbol.qualifiedName.toString()
        if (symbol.getKind() == ElementKind.ANNOTATION_TYPE) {
            sourceStructure.addMentionedAnnotations(qualifiedName)
        }

        when (visibility) {
            Visibility.ABI -> sourceStructure.addMentionedType(qualifiedName)
            Visibility.NON_ABI -> sourceStructure.addPrivateType(qualifiedName)
        }
        return true
    }
}

class GeneratedTypesTaskListener(private val cache: JavaClassCache) : TaskListener {

    override fun started(e: TaskEvent) {
        // do nothing, we just process on finish
    }

    override fun finished(e: TaskEvent) {
        if (e.kind != TaskEvent.Kind.ENTER || cache.isAlreadyProcessed(e.sourceFile.toUri())) return

        val treeVisitor = object : SimpleTreeVisitor<Void, Void>() {
            override fun visitClass(node: ClassTree, p: Void?): Void? {
                node as JCTree.JCClassDecl
                cache.addGeneratedType(node.sym.fullname.toString(), File(e.sourceFile.toUri()))

                node.members.forEach { visit(it, null) }
                return null
            }
        }
        e.compilationUnit.typeDecls.forEach {
            it.accept(treeVisitor, null)
        }
    }
}