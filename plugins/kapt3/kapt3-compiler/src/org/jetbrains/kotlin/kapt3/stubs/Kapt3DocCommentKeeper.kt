/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.stubs

import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class Kapt3DocCommentKeeper(kaptContext: KaptContextForStubGeneration) : AbstractKDocCommentKeeper<KaptContextForStubGeneration>(kaptContext) {
    fun saveKDocComment(tree: JCTree, node: Any) {
        val origin = kaptContext.origins[node] ?: return
        val psiElement = origin.element as? KtDeclaration ?: return
        val descriptor = origin.descriptor
        val docComment = psiElement.docComment ?: return

        if (descriptor is ConstructorDescriptor && psiElement is KtClassOrObject) {
            // We don't want the class comment to be duplicated on <init>()
            return
        }

        if (node is MethodNode
            && psiElement is KtProperty
            && descriptor is PropertyAccessorDescriptor
            && kaptContext.bindingContext[BindingContext.BACKING_FIELD_REQUIRED, descriptor.correspondingProperty] == true
        ) {
            // Do not place documentation on backing field and property accessors
            return
        }

        if (node is FieldNode && psiElement is KtObjectDeclaration && descriptor == null) {
            // Do not write KDoc on object instance field
            return
        }

        saveKDocComment(tree, docComment)
    }
}