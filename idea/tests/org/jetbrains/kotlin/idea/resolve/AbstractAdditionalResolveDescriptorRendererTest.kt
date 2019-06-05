/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import com.intellij.mock.MockProject
import com.intellij.pom.PomManager
import com.intellij.pom.PomModel
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.trackers.KotlinCodeBlockModificationListener
import org.jetbrains.kotlin.idea.project.IdeaEnvironment
import org.jetbrains.kotlin.idea.project.ResolveElementCache
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.renderer.AbstractDescriptorRendererTest
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.picocontainer.MutablePicoContainer

abstract class AbstractAdditionalResolveDescriptorRendererTest : AbstractDescriptorRendererTest() {
    override fun setUp() {
        super.setUp()

        val pomModelImpl = PomModelImpl(project)
        val treeAspect = TreeAspect(pomModelImpl)

        (project as MockProject).registerService(PomModel::class.java, pomModelImpl)
        (project.picoContainer as MutablePicoContainer).registerComponentInstance(
            KotlinCodeBlockModificationListener(
                PsiModificationTracker.SERVICE.getInstance(project),
                project,
                treeAspect
            )
        )
    }

    override fun tearDown() {
        (project.picoContainer as MutablePicoContainer).unregisterComponentByInstance(
            KotlinCodeBlockModificationListener.getInstance(project)
        )

        val pomModel = PomManager.getModel(project)
        (project.picoContainer as MutablePicoContainer).unregisterComponentByInstance(pomModel)

        super.tearDown()
    }

    override fun getDescriptor(declaration: KtDeclaration, container: ComponentProvider): DeclarationDescriptor {
        if (declaration is KtAnonymousInitializer || KtPsiUtil.isLocal(declaration)) {
            return container.get<ResolveElementCache>()
                .resolveToElements(listOf(declaration), BodyResolveMode.FULL)
                .get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)!!
        }
        return container.get<ResolveSession>().resolveToDescriptor(declaration)
    }

    override val targetEnvironment: TargetEnvironment
        get() = IdeaEnvironment
}
