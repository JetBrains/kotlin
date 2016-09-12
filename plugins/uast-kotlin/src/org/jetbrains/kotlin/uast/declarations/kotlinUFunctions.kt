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

package org.jetbrains.kotlin.uast

import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.state.IncompatibleClassTracker
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.uast.*
import org.jetbrains.uast.kinds.UastVariableInitialierKind
import org.jetbrains.uast.psi.PsiElementBacked

abstract class KotlinAbstractUFunction : KotlinAbstractUElement(), UFunction, PsiElementBacked {
    override abstract val psi: KtFunction

    override val name by lz { psi.name.orAnonymous() }

    override val valueParameterCount: Int
        get() = psi.valueParameters.size

    override val valueParameters by lz { psi.valueParameters.map { KotlinConverter.convert(it, this) } }

    override fun getSuperFunctions(context: UastContext): List<UFunction> {
        if (this.isTopLevel()) return emptyList()
        val bindingContext = psi.analyze(BodyResolveMode.PARTIAL)
        val clazz = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, psi] as? FunctionDescriptor ?: return emptyList()
        return clazz.overriddenDescriptors.map {
            context.convert(it.toSource()) as? UFunction
        }.filterNotNull()
    }

    override val bytecodeDescriptor by lz {
        val bindingContext = psi.analyze(BodyResolveMode.PARTIAL)
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, psi] as? FunctionDescriptor ?: return@lz null

        fun KotlinType?.isAnonymous(): Boolean {
            if (this == null) return true
            return false
        }

        if (descriptor.valueParameters.any { it.type.isAnonymous() } || descriptor.returnType.isAnonymous()) {
            return@lz null
        }

        val typeMapper = KotlinTypeMapper(BindingContext.EMPTY, ClassBuilderMode.LIGHT_CLASSES, NoResolveFileClassesProvider, null,
                                          IncompatibleClassTracker.DoNothing, JvmAbi.DEFAULT_MODULE_NAME, null)
        typeMapper.mapAsmMethod(descriptor).descriptor
    }

    override fun hasModifier(modifier: UastModifier) = psi.hasModifier(modifier)
    override val annotations by lz { psi.getUastAnnotations(this) }

    override val body by lz { KotlinConverter.convertOrNull(psi.bodyExpression, this) }
    override val visibility by lz { psi.getVisibility() }
}

class KotlinConstructorUFunction(
        override val psi: KtConstructor<*>,
        override val parent: UElement
) : KotlinAbstractUFunction(), PsiElementBacked {
    override val name: String
        get() = "<init>"

    override val nameElement by lz {
        val constructorKeyword = psi.getConstructorKeyword()?.let { KotlinDumbUElement(it, this) }
        constructorKeyword ?: this.getContainingFunction()?.nameElement
    }

    override val kind = UastFunctionKind.CONSTRUCTOR

    override val typeParameterCount = 0
    override val typeParameters = emptyList<UTypeReference>()

    override val returnType = null
}

class KotlinUFunction(
        override val psi: KtFunction,
        override val parent: UElement
) : KotlinAbstractUFunction(), PsiElementBacked {
    override val nameElement by lz { psi.nameIdentifier?.let { KotlinDumbUElement(it, this) } }

    override val kind = UastFunctionKind.FUNCTION

    override val returnType by lz {
        val descriptor = psi.resolveToDescriptorIfAny() as? FunctionDescriptor ?: return@lz null
        val type = descriptor.returnType ?: return@lz null
        KotlinConverter.convert(type, psi.project, this)
    }

    override val typeParameterCount: Int
        get() = psi.typeParameters.size

    override val typeParameters by lz { psi.typeParameters.map { KotlinParameterUTypeReference(it, this) } }
}

class KotlinAnonymousInitializerUFunction(
        override val psi: KtAnonymousInitializer,
        override val parent: UElement
) : KotlinAbstractUElement(), UFunction, PsiElementBacked {
    override val kind = KotlinFunctionKinds.INIT_BLOCK

    override val valueParameters: List<UVariable>
        get() = emptyList()

    override val valueParameterCount: Int
        get() = 0

    override val typeParameters: List<UTypeReference>
        get() = emptyList()

    override val typeParameterCount: Int
        get() = 0

    override val returnType: UType?
        get() = null

    override val body by lz { KotlinConverter.convertOrNull(psi.body, this) }

    override val visibility: UastVisibility
        get() = UastVisibility.PRIVATE

    override fun getSuperFunctions(context: UastContext) = emptyList<UFunction>()

    override val nameElement by lz { KotlinDumbUElement(psi.node.findChildByType(KtTokens.INIT_KEYWORD)?.psi ?: psi, this) }

    override val name: String
        get() = "<init>"

    override fun hasModifier(modifier: UastModifier) = false

    override val annotations: List<UAnnotation>
        get() = emptyList()
}

open class KotlinDefaultPrimaryConstructorUFunction(
        override val psi: KtClassOrObject,
        override val parent: UClass
) : KotlinAbstractUElement(), UFunction, PsiElementBacked, NoModifiers, NoAnnotations {
    override val kind: UastFunctionKind
        get() = UastFunctionKind.CONSTRUCTOR

    override val nameElement by lz { KotlinDumbUElement(psi.nameIdentifier, this) }
    override val name: String
        get() = "<init>"

    override val valueParameters: List<UVariable>
        get() = emptyList()
    override val valueParameterCount: Int
        get() = 0

    override val typeParameters: List<UTypeReference>
        get() = emptyList()
    override val typeParameterCount: Int
        get() = 0

    override val returnType: UType?
        get() = null

    override val body: UExpression?
        get() = null

    override val visibility: UastVisibility
        get() = parent.visibility

    override fun getSuperFunctions(context: UastContext) = emptyList<UFunction>()
}

open class KotlinObjectLiteralConstructorUFunction(
        override val psi: KtObjectDeclaration,
        override val parent: UClass
) : KotlinAbstractUElement(), UFunction, PsiElementBacked, NoModifiers, NoAnnotations {
    private val resolvedCall by lz {
        val bindingContext = psi.analyze(BodyResolveMode.PARTIAL)
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, psi] as? ClassDescriptor
        val primaryConstructor = descriptor?.unsubstitutedPrimaryConstructor ?: return@lz null
        bindingContext[BindingContext.CONSTRUCTOR_RESOLVED_DELEGATION_CALL, primaryConstructor]
    }

    override val kind: UastFunctionKind
        get() = UastFunctionKind.CONSTRUCTOR

    override val nameElement by lz { KotlinDumbUElement(psi.nameIdentifier, this) }
    override val name: String
        get() = "<init>"


    override val valueParameters by lz {
        val params = resolvedCall?.valueArguments?.keys ?: return@lz emptyList<UVariable>()
        params.map { param ->
            object : UVariable {
                override val initializer: UExpression?
                    get() = null
                override val initializerKind: UastVariableInitialierKind
                    get() = UastVariableInitialierKind.NO_INITIALIZER
                override val kind: UastVariableKind
                    get() = UastVariableKind.VALUE_PARAMETER
                override val type: UType
                    get() = KotlinConverter.convert(param.type, psi.project, this)
                override val nameElement: UElement?
                    get() = null
                override val parent: UElement
                    get() = this@KotlinObjectLiteralConstructorUFunction
                override val name: String
                    get() = param.name.asString()
                override val visibility: UastVisibility
                    get() = UastVisibility.LOCAL

                override fun hasModifier(modifier: UastModifier) = when(modifier) {
                    UastModifier.VARARG -> param.varargElementType != null
                    else -> false
                }

                override val annotations: List<UAnnotation>
                    get() = emptyList()
            }
        }
    }

    override val valueParameterCount: Int
        get() = resolvedCall?.valueArgumentsByIndex?.size ?: 0

    override val typeParameters: List<UTypeReference>
        get() = emptyList()

    override val typeParameterCount: Int
        get() = 0

    override val returnType: UType?
        get() = null

    override val body: UExpression?
        get() = null

    override val visibility: UastVisibility
        get() = parent.visibility

    override fun getSuperFunctions(context: UastContext) = emptyList<UFunction>()
}