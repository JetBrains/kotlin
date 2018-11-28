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
import org.jetbrains.kotlin.js.translate.expression.translateAndAliasParameters
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlinx.serialization.compiler.backend.common.bodyPropertiesDescriptorsMap
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializerOrContext
import org.jetbrains.kotlinx.serialization.compiler.backend.common.primaryPropertiesDescriptorsMap
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.contextSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.referenceArraySerializerId
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
                property.descriptor.annotations,
                property.descriptor.findPsi()
            ) else null
    return serializerInstance(serializer, property.module, property.type, property.genericIndex)
        ?.let { expr -> if (property.type.isMarkedNullable) JsNew(nullableSerClass, listOf(expr)) else expr }
}

internal fun SerializerJsTranslator.serializerInstance(
    serializerClass: ClassDescriptor?,
    module: ModuleDescriptor,
    kType: KotlinType,
    genericIndex: Int? = null
): JsExpression? {
    val nullableSerClass =
        context.translateQualifiedReference(module.getClassFromInternalSerializationPackage(SpecialBuiltins.nullableSerializer))
    if (serializerClass == null) {
        if (genericIndex == null) return null
        return JsNameRef(context.scope().declareName("${SerialEntityNames.typeArgPrefix}$genericIndex"), JsThisRef())
    }
    if (serializerClass.kind == ClassKind.OBJECT) {
        return context.serializerObjectGetter(serializerClass)
    } else {
        var args = if (serializerClass.classId == enumSerializerId || serializerClass.classId == contextSerializerId)
            listOf(createGetKClassExpression(kType.toClassDescriptor!!))
        else kType.arguments.map {
            val argSer = findTypeSerializerOrContext(module, it.type, sourceElement = serializerClass.findPsi())
            val expr = serializerInstance(argSer, module, it.type, it.type.genericIndex) ?: return null
            if (it.type.isMarkedNullable) JsNew(nullableSerClass, listOf(expr)) else expr
        }
        if (serializerClass.classId == referenceArraySerializerId)
            args = listOf(createGetKClassExpression(kType.arguments[0].type.toClassDescriptor!!)) + args
        val serializable = getSerializableClassDescriptorBySerializer(serializerClass)
        val ref = if (serializable?.declaredTypeParameters?.isNotEmpty() == true) {
            val desc = requireNotNull(
                KSerializerDescriptorResolver.findSerializerConstructorForTypeArgumentsSerializers(serializerClass)
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

internal fun SerializerJsTranslator.createGetKClassExpression(classDescriptor: ClassDescriptor): JsExpression =
    JsInvocation(
        context.namer().kotlin("getKClass"),
        context.translateQualifiedReference(classDescriptor)
    )

fun TranslationContext.buildInitializersRemapping(forClass: KtPureClassOrObject): Map<PropertyDescriptor, KtExpression?> = forClass.run {
    (bodyPropertiesDescriptorsMap(bindingContext()).mapValues { it.value.delegateExpressionOrInitializer } +
            primaryPropertiesDescriptorsMap(bindingContext()).mapValues { it.value.defaultValue })
}