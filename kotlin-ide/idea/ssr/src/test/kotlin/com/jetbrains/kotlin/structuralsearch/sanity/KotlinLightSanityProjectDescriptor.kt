package com.jetbrains.kotlin.structuralsearch.sanity

import com.intellij.dupLocator.util.NodeFilter
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.PatternContext
import com.jetbrains.kotlin.structuralsearch.KotlinLightProjectDescriptor
import com.jetbrains.kotlin.structuralsearch.visitor.KotlinMatchingVisitor
import org.jetbrains.kotlin.psi.KtElement

class KotlinLightSanityProjectDescriptor : KotlinLightProjectDescriptor() {
    override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
        addLibrary(AccessDeniedException::class.java, OrderRootType.CLASSES, model)
        addLibrary(PsiElement::class.java, OrderRootType.CLASSES, model)
        addLibrary(KtElement::class.java, OrderRootType.CLASSES, model)
        addLibrary(PatternContext::class.java, OrderRootType.CLASSES, model)
        addLibrary(NodeFilter::class.java, OrderRootType.CLASSES, model)
        addLibrary(KotlinMatchingVisitor::class.java, OrderRootType.CLASSES, model)
    }
}