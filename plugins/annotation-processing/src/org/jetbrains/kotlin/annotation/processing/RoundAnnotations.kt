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

package org.jetbrains.kotlin.annotation.processing

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.java.model.internal.getAnnotationsWithInherited
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

internal class RoundAnnotations() {
    private val mutableAnnotationsMap = mutableMapOf<String, MutableList<PsiModifierListOwner>>()

    val annotationsMap: Map<String, List<PsiModifierListOwner>>
        get() = mutableAnnotationsMap

    fun analyzeFiles(files: Collection<KtFile>) = files.forEach { analyzeFile(it) }
    
    @JvmName("analyzePsiJavaFiles")
    fun analyzeFiles(files: Collection<PsiJavaFile>) = files.forEach { analyzeFile(it) }

    fun analyzeFile(file: KtFile) {
        val lightClass = file.findFacadeClass()

        if (lightClass != null) {
            analyzeDeclaration(lightClass)
        }

        for (declaration in file.declarations) {
            if (declaration !is KtClassOrObject) continue
            val clazz = declaration.toLightClass() ?: continue
            analyzeDeclaration(clazz)
        }
    }

    fun analyzeFile(file: PsiJavaFile) {
        file.classes.forEach { analyzeDeclaration(it) }
    }

    fun analyzeDeclaration(declaration: PsiElement) {
        if (declaration !is PsiModifierListOwner) return

        for (annotation in declaration.getAnnotationsWithInherited()) {
            val fqName = annotation.qualifiedName ?: return
            mutableAnnotationsMap.getOrPut(fqName, { mutableListOf() }).add(declaration)
        }

        if (declaration is PsiClass) {
            declaration.methods.forEach { analyzeDeclaration(it) }
            declaration.fields.forEach { analyzeDeclaration(it) }
            declaration.innerClasses.forEach { analyzeDeclaration(it) }
        }

        if (declaration is PsiMethod) {
            for (parameter in declaration.parameterList.parameters) {
                analyzeDeclaration(parameter)
            }
        }
    }
}