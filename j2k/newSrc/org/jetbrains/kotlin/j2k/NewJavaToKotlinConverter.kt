/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.j2k.tree.JKClass
import org.jetbrains.kotlin.j2k.tree.JKElement
import org.jetbrains.kotlin.j2k.tree.JKJavaField
import org.jetbrains.kotlin.j2k.tree.JKJavaKtVisitorVoid

class NewJavaToKotlinConverter(
        private val project: Project,
        private val settings: ConverterSettings
) {

    fun filesToKotlin(files: List<PsiJavaFile>, progressIndicator: ProgressIndicator = EmptyProgressIndicator()): String {
        val treeBuilder = JavaToJKTreeBuilder()
        val fileTrees = files.map(treeBuilder::buildTree)

        val result = buildString {

            for (tree in fileTrees.filterNotNull()) {
                appendln()
                tree.accept(object : JKJavaKtVisitorVoid {
                    override fun visitElement(element: JKElement) {
                        element.acceptChildren(this, null)
                    }

                    override fun visitClass(klass: JKClass) {
                        appendln("class")
                        super.visitClass(klass)
                    }

                    override fun visitJavaField(javaField: JKJavaField, data: Nothing?) {
                        appendln("java field ${javaField.name}")
                        super.visitJavaField(javaField, data)
                    }

                }, null)
                appendln()
            }
        }

        return result
    }
}