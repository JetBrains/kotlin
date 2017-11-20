/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.uast.kotlin.internal

import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.state.IncompatibleClassTracker
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.uast.kotlin.KotlinUastBindingContextProviderService

class IdeaKotlinUastBindingContextProviderService : KotlinUastBindingContextProviderService {
    override fun getBindingContext(element: KtElement) = element.analyze(BodyResolveMode.PARTIAL)

    override fun getTypeMapper(element: KtElement): KotlinTypeMapper? {
        return KotlinTypeMapper(getBindingContext(element), ClassBuilderMode.LIGHT_CLASSES,
                                IncompatibleClassTracker.DoNothing, JvmAbi.DEFAULT_MODULE_NAME, false, false)
    }
}
