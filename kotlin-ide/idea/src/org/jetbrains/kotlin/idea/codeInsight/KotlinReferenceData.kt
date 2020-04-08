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

package org.jetbrains.kotlin.idea.codeInsight

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import java.awt.datatransfer.DataFlavor
import java.io.Serializable

data class KotlinReferenceData(
    var startOffset: Int,
    var endOffset: Int,
    val fqName: String,
    val isQualifiable: Boolean,
    val kind: Kind
) : Cloneable, Serializable {

    enum class Kind {
        CLASS,
        PACKAGE,
        FUNCTION,
        PROPERTY;

        companion object {
            fun fromDescriptor(descriptor: DeclarationDescriptor) = when (descriptor.getImportableDescriptor()) {
                is ClassDescriptor -> CLASS
                is PackageViewDescriptor -> PACKAGE
                is FunctionDescriptor -> FUNCTION
                is PropertyDescriptor -> PROPERTY
                else -> null
            }
        }
    }

    public override fun clone(): KotlinReferenceData {
        try {
            return super.clone() as KotlinReferenceData
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException()
        }
    }

    companion object {
        val dataFlavor: DataFlavor? by lazy {
            try {
                val dataClass = KotlinReferenceData::class.java
                DataFlavor(
                    DataFlavor.javaJVMLocalObjectMimeType + ";class=" + dataClass.name,
                    "KotlinReferenceData",
                    dataClass.classLoader
                )
            } catch (e: NoClassDefFoundError) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun isQualifiable(refElement: KtElement, descriptor: DeclarationDescriptor): Boolean {
            refElement.getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference }?.let {
                val receiverExpression = it.receiverExpression

                if (receiverExpression != null) {
                    val lhs = it.analyze(BodyResolveMode.PARTIAL)[BindingContext.DOUBLE_COLON_LHS, receiverExpression]
                    if (lhs is DoubleColonLHS.Expression) return false
                }
                return descriptor.containingDeclaration is ClassifierDescriptor
            }

            return !descriptor.isExtension
        }
    }
}