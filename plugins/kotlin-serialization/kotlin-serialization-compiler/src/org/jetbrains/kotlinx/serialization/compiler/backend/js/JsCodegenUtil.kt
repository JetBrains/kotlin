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

package org.jetbrains.kotlinx.serialization.compiler.backend.js

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.expression.ExpressionVisitor
import org.jetbrains.kotlin.js.translate.expression.translateAndAliasParameters
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound
import org.jetbrains.kotlinx.serialization.compiler.backend.common.*
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

internal class JsBlockBuilder {
    val block: JsBlock = JsBlock()
    operator fun JsStatement.unaryPlus() {
        block.statements.add(this)
    }

    val body: List<JsStatement>
        get() = block.statements
}

internal fun JsBlockBuilder.jsWhile(condition: JsExpression, body: JsBlockBuilder.() -> Unit, label: JsLabel? = null) {
    val b = JsBlockBuilder()
    b.body()
    val w = JsWhile(condition, b.block)
    if (label == null) {
        +w
    } else {
        label.statement = w
        +label
    }
}

internal class JsCasesBuilder() {
    val caseList: MutableList<JsSwitchMember> = mutableListOf()
    operator fun JsSwitchMember.unaryPlus() {
        caseList.add(this)
    }
}

internal fun JsCasesBuilder.case(condition: JsExpression, body: JsBlockBuilder.() -> Unit) {
    val a = JsCase()
    a.caseExpression = condition
    val b = JsBlockBuilder()
    b.body()
    a.statements += b.body
    +a
}

internal fun JsCasesBuilder.default(body: JsBlockBuilder.() -> Unit) {
    val a = JsDefault()
    val b = JsBlockBuilder()
    b.body()
    a.statements += b.body
    +a
}

internal fun JsBlockBuilder.jsSwitch(condition: JsExpression, cases: JsCasesBuilder.() -> Unit) {
    val b = JsCasesBuilder()
    b.cases()
    val sw = JsSwitch(condition, b.caseList)
    +sw
}

internal fun TranslationContext.buildFunction(descriptor: FunctionDescriptor, bodyGen: JsBlockBuilder.(JsFunction, TranslationContext) -> Unit): JsFunction {
    val functionObject = this.getFunctionObject(descriptor)
    val innerCtx = this.newDeclaration(descriptor).translateAndAliasParameters(descriptor, functionObject.parameters)
    val b = JsBlockBuilder()
    b.bodyGen(functionObject, innerCtx)
    functionObject.body.statements += b.body
    return functionObject
}

internal fun propNotSeenTest(seenVar: JsNameRef, index: Int): JsBinaryOperation = JsAstUtils.equality(
        JsBinaryOperation(
                JsBinaryOperator.BIT_AND,
                seenVar,
                JsIntLiteral(1 shl (index % 32))
        ),
        JsIntLiteral(0)
)

internal fun TranslationContext.serializerObjectGetter(serializer: ClassDescriptor): JsExpression {
    return ReferenceTranslator.translateAsValueReference(serializer, this)
}

internal fun TranslationContext.translateQualifiedReference(clazz: ClassDescriptor): JsExpression {
    return ReferenceTranslator.translateAsTypeReference(clazz, this)
}

// Does not use sti and therefore does not perform encoder calls optimization
internal fun SerializerJsTranslator.serializerTower(property: SerializableProperty): JsExpression? {
    val nullableSerClass =
        context.translateQualifiedReference(property.module.getClassFromInternalSerializationPackage(SpecialBuiltins.nullableSerializer))
    val serializer =
        property.serializableWith?.toClassDescriptor
            ?: if (!property.type.isTypeParameter()) findTypeSerializerOrContext(
                property.module,
                property.type,
                property.descriptor.findPsi()
            ) else null
    return serializerInstance(context, serializer, property.module, property.type, property.genericIndex)
        ?.let { expr -> if (property.type.isMarkedNullable) JsNew(nullableSerClass, listOf(expr)) else expr }
}

internal fun AbstractSerialGenerator.serializerInstance(
    context: TranslationContext,
    serializerClass: ClassDescriptor?,
    module: ModuleDescriptor,
    kType: KotlinType,
    genericIndex: Int? = null,
    genericGetter: (Int, KotlinType) -> JsExpression = { it, _ ->
        JsNameRef(
            context.scope().declareName("${SerialEntityNames.typeArgPrefix}$it"),
            JsThisRef()
        )
    }
): JsExpression? {
    val nullableSerClass =
        context.translateQualifiedReference(module.getClassFromInternalSerializationPackage(SpecialBuiltins.nullableSerializer))
    if (serializerClass == null) {
        if (genericIndex == null) return null
        return genericGetter(genericIndex, kType)
    }
    if (serializerClass.kind == ClassKind.OBJECT) {
        return context.serializerObjectGetter(serializerClass)
    } else {
        fun instantiate(serializer: ClassDescriptor?, type: KotlinType): JsExpression? {
            val expr = serializerInstance(context, serializer, module, type, type.genericIndex, genericGetter) ?: return null
            return if (type.isMarkedNullable) JsNew(nullableSerClass, listOf(expr)) else expr
        }
        var args = when {
            serializerClass.classId == contextSerializerId || serializerClass.classId == polymorphicSerializerId -> listOf(
                ExpressionVisitor.getObjectKClass(context, kType.toClassDescriptor!!)
            )
            serializerClass.classId == enumSerializerId -> listOf(
                JsStringLiteral(kType.serialName()),
                // EnumClass.values() invocation
                JsInvocation(
                    context.getInnerNameForDescriptor(
                        DescriptorUtils.getFunctionByName(
                            kType.toClassDescriptor!!.staticScope,
                            DescriptorUtils.ENUM_VALUES
                        )
                    ).makeRef()
                )
            )
            serializerClass.classId == objectSerializerId -> listOf(
                JsStringLiteral(kType.serialName()),
                context.serializerObjectGetter(kType.toClassDescriptor!!)
            )
            serializerClass.classId == sealedSerializerId -> mutableListOf<JsExpression>().apply {
                add(JsStringLiteral(kType.serialName()))
                add(ExpressionVisitor.getObjectKClass(context, kType.toClassDescriptor!!))
                val (subclasses, subSerializers) = allSealedSerializableSubclassesFor(
                    kType.toClassDescriptor!!,
                    module
                )
                add(JsArrayLiteral(subclasses.map {
                    ExpressionVisitor.getObjectKClass(
                        context,
                        it.toClassDescriptor!!
                    )
                }))
                add(JsArrayLiteral(subSerializers.mapIndexed { i, serializer ->
                    val type = subclasses[i]
                    val expr = serializerInstance(context, serializer, module, type, type.genericIndex) { _, genericType ->
                        serializerInstance(
                            context,
                            module.getClassFromSerializationPackage(SpecialBuiltins.polymorphicSerializer),
                            module,
                            (genericType.constructor.declarationDescriptor as TypeParameterDescriptor).representativeUpperBound
                        )!!
                    }!!
                    if (type.isMarkedNullable) JsNew(nullableSerClass, listOf(expr)) else expr
                }))
            }
            else -> kType.arguments.map {
                val argSer = findTypeSerializerOrContext(module, it.type, sourceElement = serializerClass.findPsi())
                instantiate(argSer, it.type) ?: return null
            }
        }
        if (serializerClass.classId == referenceArraySerializerId)
            args = listOf(ExpressionVisitor.getObjectKClass(context, kType.arguments[0].type.toClassDescriptor!!)) + args
        val serializable = getSerializableClassDescriptorBySerializer(serializerClass)
        val ref = if (serializable?.declaredTypeParameters?.isNotEmpty() == true) {
            val desc = requireNotNull(
                findSerializerConstructorForTypeArgumentsSerializers(serializerClass)
            ) { "Generated serializer does not have constructor with required number of arguments" }
            if (!desc.isPrimary)
                JsInvocation(context.getInnerReference(desc), args)
            else
                JsNew(context.getInnerReference(desc), args)
        } else {
            JsNew(context.translateQualifiedReference(serializerClass), args)
        }
        return ref
    }
}

fun TranslationContext.buildInitializersRemapping(
    forClass: KtPureClassOrObject,
    superClass: ClassDescriptor?
): Map<PropertyDescriptor, KtExpression?> {
    val myMap = (forClass.bodyPropertiesDescriptorsMap(bindingContext()).mapValues { it.value.delegateExpressionOrInitializer } +
            forClass.primaryConstructorPropertiesDescriptorsMap(bindingContext()).mapValues { it.value.defaultValue })
    val parentPsi = superClass?.takeIf { it.isInternalSerializable }?.findPsi() as? KtPureClassOrObject ?: return myMap
    val parentMap = buildInitializersRemapping(parentPsi, superClass.getSuperClassNotAny())
    return myMap + parentMap
}