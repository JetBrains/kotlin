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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.java.model.internal.getAnnotationsWithInherited
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

internal class RoundAnnotations(
        val bindingContext: BindingContext,
        val typeMapper: KotlinTypeMapper
) {
    private companion object {
        private val BLACKLISTED_ANNOTATATIONS = listOf(
                "java.lang.Deprecated", "kotlin.Deprecated", // Deprecated annotations
                "java.lang.annotation.", // Java annotations
                "org.jetbrains.annotations.", // Nullable/NotNull, ReadOnly, Mutable
                "kotlin.jvm.", "kotlin.Metadata" // Kotlin annotations from runtime
        )
    }
    
    private val mutableAnnotationsMap = mutableMapOf<String, MutableList<PsiModifierListOwner>>()
    private val mutableAnalyzedClasses = mutableSetOf<String>()
    
    val annotationsMap: Map<String, List<PsiModifierListOwner>>
        get() = mutableAnnotationsMap
    
    val analyzedClasses: Set<String>
        get() = mutableAnalyzedClasses
    
    fun copy() = RoundAnnotations(bindingContext, typeMapper)

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
    
    fun PsiElement.getTopLevelClassParent(): PsiClass? = when (this) {
        is PsiClass -> containingClass?.let { it.getTopLevelClassParent() } ?: this
        else -> PsiTreeUtil.getParentOfType(this, PsiClass::class.java, true)?.getTopLevelClassParent()
    }
    
    private val PsiAnnotation.hasSourceRetention: Boolean
        get() {
            val annotationDeclaration = nameReferenceElement?.resolve() as? PsiClass ?: return false
            val metaAnnotations = annotationDeclaration.modifierList?.annotations ?: return false
            return metaAnnotations.any { anno ->
                val declaration = anno.nameReferenceElement?.resolve() as? PsiClass ?: return@any false
                if (declaration.qualifiedName != "java.lang.annotation.Retention") return@any false
                val value = (anno.findAttributeValue("value") as? PsiReferenceExpression)?.resolve() ?: return@any false
                value is PsiEnumConstant && value.name == "SOURCE"
            }
        }

    fun analyzeDeclaration(declaration: PsiElement): Boolean {
        if (declaration !is PsiModifierListOwner) return false
        
        // Do not analyze classes twice (for incremental compilation data)
        if (declaration is PsiClass && declaration.qualifiedName in analyzedClasses) return false

        for (annotation in declaration.getAnnotationsWithInherited()) {
            val fqName = annotation.qualifiedName ?: continue
            
            if (BLACKLISTED_ANNOTATATIONS.any { fqName.startsWith(it) }) continue
            mutableAnnotationsMap.getOrPut(fqName, { mutableListOf() }).add(declaration)

            // Add only top-level classes
            val topLevelClassQualifiedName = declaration.getTopLevelClassParent()?.qualifiedName
            if (topLevelClassQualifiedName != null) {
                mutableAnalyzedClasses += topLevelClassQualifiedName
            }
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
        
        return true
    }
}