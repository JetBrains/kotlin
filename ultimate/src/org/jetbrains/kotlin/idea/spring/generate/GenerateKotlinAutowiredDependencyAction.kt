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

package org.jetbrains.kotlin.idea.spring.generate

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.spring.SpringBundle
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.editor.BatchTemplateRunner
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtClass

class GenerateKotlinAutowiredDependencyAction : GenerateKotlinSpringBeanDependencyAction(
        SpringBundle.message("action.generate.autowired.dependencies.action.text")
) {
    override fun checkContext(module: Module) = true

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val klass = getTargetClass(editor, file) as? KtClass ?: return
        val lightClass = klass.toLightClass() ?: return
        project
                .executeWriteCommand<List<BatchTemplateRunner>>("") { generateAutowiredDependenciesFor(lightClass) }
                .forEach { it.runTemplates() }
    }
}