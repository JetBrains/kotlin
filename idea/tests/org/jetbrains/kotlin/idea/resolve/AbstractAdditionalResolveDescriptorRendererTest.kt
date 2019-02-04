/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
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

abstract class AbstractAdditionalResolveDescriptorRendererTest : AbstractDescriptorRendererTest() {
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
