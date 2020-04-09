/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import com.intellij.mock.MockProject
import com.intellij.pom.PomModel
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
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
        val treeAspect = TreeAspect.getInstance(project)

        val mockProject = project as MockProject
        createAndRegisterKotlinCodeBlockModificationListener(mockProject, pomModelImpl, treeAspect)
    }

    override fun tearDown() {
        unregisterKotlinCodeBlockModificationListener(project as MockProject)

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
