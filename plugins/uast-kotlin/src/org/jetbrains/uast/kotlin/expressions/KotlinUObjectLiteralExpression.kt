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

package org.jetbrains.uast.kotlin

import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.impl.light.LightPsiClassBuilder
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.uast.*

class KotlinUObjectLiteralExpression(
        override val psi: KtObjectLiteralExpression,
        override val uastParent: UElement?
) : KotlinAbstractUExpression(), UObjectLiteralExpression, KotlinUElementWithType {
    override val declaration: UClass by lz {
        val lightClass: KtLightClass? = psi.objectDeclaration.toLightClass()
        if (lightClass != null) {
            getLanguagePlugin().convert<UClass>(lightClass, this)
        }
        else {
            logger.error(
                    "Failed to create light class for object declaration",
                    Attachment(psi.containingFile.virtualFile?.path ?: "<no path>", psi.containingFile.text))

            getLanguagePlugin().convert(LightPsiClassBuilder(psi, "<unnamed>"), this)
        }
    }
    
    override fun getExpressionType() = psi.objectDeclaration.toPsiType()

    private val superClassConstructorCall by lz {
        psi.objectDeclaration.superTypeListEntries.firstOrNull { it is KtSuperTypeCallEntry } as? KtSuperTypeCallEntry
    }
    
    override val classReference: UReferenceExpression? by lz { superClassConstructorCall?.let { ObjectLiteralClassReference(it, this) } }
    
    override val valueArgumentCount: Int
        get() = superClassConstructorCall?.valueArguments?.size ?: 0

    override val valueArguments by lz {
        val psi = superClassConstructorCall ?: return@lz emptyList<UExpression>()
        psi.valueArguments.map { KotlinConverter.convertOrEmpty(it.getArgumentExpression(), this) } 
    }
    
    override val typeArgumentCount: Int
        get() = superClassConstructorCall?.typeArguments?.size ?: 0

    override val typeArguments by lz {
        val psi = superClassConstructorCall ?: return@lz emptyList<PsiType>()
        psi.typeArguments.map { it.typeReference.toPsiType(this, boxed = true) } 
    }

    override fun resolve() = superClassConstructorCall?.resolveCallToDeclaration(this) as? PsiMethod
    
    private class ObjectLiteralClassReference(
            override val psi: KtSuperTypeCallEntry,
            override val uastParent: UElement?
    ) : KotlinAbstractUElement(), USimpleNameReferenceExpression {
        override fun resolve() = (psi.resolveCallToDeclaration(this) as? PsiMethod)?.containingClass

        override val annotations: List<UAnnotation>
            get() = emptyList()

        override val resolvedName: String?
            get() = identifier
        
        override val identifier: String
            get() = psi.name ?: "<error>"
    }

    companion object {
        val logger by lz { Logger.getInstance(KotlinUObjectLiteralExpression::class.java) }
    }
}