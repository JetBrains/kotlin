/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ResolverForProject
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

interface ResolutionFacade {
    val project: Project

    fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext
    fun analyze(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode): BindingContext

    fun analyzeWithAllCompilerChecks(elements: Collection<KtElement>): AnalysisResult

    fun resolveToDescriptor(declaration: KtDeclaration, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): DeclarationDescriptor

    val moduleDescriptor: ModuleDescriptor

    // get service for the module this resolution was created for
    fun <T : Any> getFrontendService(serviceClass: Class<T>): T

    fun <T : Any> getIdeService(serviceClass: Class<T>): T

    // get service for the module defined by PsiElement/ModuleDescriptor passed as parameter
    fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T
    fun <T : Any> tryGetFrontendService(element: PsiElement, serviceClass: Class<T>): T?

    fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T

    fun getResolverForProject(): ResolverForProject<out ModuleInfo>
}

inline fun <reified T : Any> ResolutionFacade.frontendService(): T = this.getFrontendService(T::class.java)

inline fun <reified T : Any> ResolutionFacade.ideService(): T = this.getIdeService(T::class.java)
