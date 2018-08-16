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

package org.jetbrains.kotlin.idea.spring.diagram

import com.intellij.diagram.DiagramProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.spring.contexts.model.diagram.actions.OpenSpringModelDependenciesAction
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

class OpenKotlinSpringModelDependenciesAction(klass: KtClass) : OpenSpringModelDependenciesAction() {
    private val klassPointer = klass.createSmartPointer()

    override fun findInDataContext(provider: DiagramProvider<*>, context: DataContext): Any? {
        val wrappingContext = DataContext { id ->
            if (CommonDataKeys.PSI_ELEMENT.`is`(id)) klassPointer.element?.toLightClass() else context.getData(id)
        }
        return super.findInDataContext(provider, wrappingContext)
    }
}
