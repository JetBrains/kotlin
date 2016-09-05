/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
