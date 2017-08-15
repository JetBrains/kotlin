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
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.js.translate.declaration.DefaultPropertyTranslator
import org.jetbrains.kotlin.js.translate.expression.translateAndAliasParameters
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializerCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializer
import org.jetbrains.kotlinx.serialization.compiler.backend.common.getSerialTypeInfo
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.referenceArraySerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.getClassFromInternalSerializationPackage
import org.jetbrains.kotlinx.serialization.compiler.resolve.getClassFromSerializationPackage
import org.jetbrains.kotlinx.serialization.compiler.resolve.getSerializableClassDescriptorBySerializer
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

/**
 *  @author Leonid Startsev
 *          sandwwraith@gmail.com
 */

class SerializerJsTranslator(declaration: KtPureClassOrObject,
                             val translator: DeclarationBodyVisitor,
                             val context: TranslationContext) : SerializerCodegen(declaration, context.bindingContext()) {

    private class JsBlockBuilder {
        private val block: JsBlock = JsBlock()
        operator fun JsStatement.unaryPlus() {
            block.statements.add(this)
        }

        val body: List<JsStatement>
            get() = block.statements
    }

    private fun generateFunction(descriptor: FunctionDescriptor, bodyGen: JsBlockBuilder.(JsFunction, TranslationContext) -> Unit) {
        val functionObject = context.getFunctionObject(descriptor)
        val innerCtx = context.newDeclaration(descriptor).translateAndAliasParameters(descriptor, functionObject.parameters)
        val b = JsBlockBuilder()
        b.bodyGen(functionObject, innerCtx)
        functionObject.body.statements += b.body
        translator.addFunction(descriptor, functionObject, null)
    }

    override fun generateSerialDesc() {
        val desc = serialDescPropertyDescriptor ?: return
        val serialDescImplClass = serializerDescriptor
                .getClassFromInternalSerializationPackage("SerialClassDescImpl")
        val serialDescImplConstructor = serialDescImplClass
                .unsubstitutedPrimaryConstructor!!

        // this.serialDesc = new SerialDescImpl(...)
        val value = JsNew(context.getInnerReference(serialDescImplConstructor), listOf(JsStringLiteral(serialName)))
        val assgmnt = TranslationUtils.assignmentToBackingField(context, desc, value)
        translator.addInitializerStatement(assgmnt.makeStmt())

        // adding elements via serialDesc.addElement(...)
        val addFunc = serialDescImplClass.unsubstitutedMemberScope
                .getDescriptorsFiltered { it == Name.identifier("addElement") }.single()
        val serialClassDescRef = JsNameRef(context.getNameForDescriptor(serialDescPropertyDescriptor), JsThisRef())
        for (prop in orderedProperties) {
            if (prop.transient) continue
            val call = JsInvocation(JsNameRef(context.getNameForDescriptor(addFunc), serialClassDescRef), JsStringLiteral(prop.name))
            translator.addInitializerStatement(call.makeStmt())
            //todo: annotations
        }
    }

    override fun generateSerializableClassProperty(property: PropertyDescriptor) {
        val propDesc = serialDescPropertyDescriptor ?: return
        val propTranslator = DefaultPropertyTranslator(propDesc, context,
                                                       translator.getBackingFieldReference(propDesc))
        val getterDesc = propDesc.getter!!
        val getterExpr = context.getFunctionObject(getterDesc)
                .apply { propTranslator.generateDefaultGetterFunction(getterDesc, this) }
        translator.addProperty(propDesc, getterExpr, null)
    }

    private fun ClassDescriptor.getFuncDesc(funcName: String) =
            unsubstitutedMemberScope.getDescriptorsFiltered { it == Name.identifier(funcName) }

    override fun generateSave(function: FunctionDescriptor) = generateFunction(function) { jsFun, ctx ->
        val kOutputClass = serializerDescriptor.getClassFromSerializationPackage("KOutput")
        val wBeginFunc = ctx.getNameForDescriptor(
                kOutputClass.getFuncDesc("writeBegin").single { (it as FunctionDescriptor).valueParameters.size == 2 })
        val serialClassDescRef = JsNameRef(context.getNameForDescriptor(serialDescPropertyDescriptor!!), JsThisRef())

        // output.writeBegin(desc, [])
        val call = JsInvocation(JsNameRef(wBeginFunc, JsNameRef(jsFun.parameters[0].name)),
                                serialClassDescRef,
                                JsArrayLiteral())
        val objRef = JsNameRef(jsFun.parameters[1].name)
        // output = output.writeBegin...
        val localOutputName = jsFun.scope.declareFreshName("output")
        val localOutputRef = JsNameRef(localOutputName)
        +JsVars(JsVars.JsVar(localOutputName, call))

        // todo: internal serialization via virtual calls
        val labeledProperties = orderedProperties.filter { !it.transient }
        for (index in labeledProperties.indices) {
            val property = labeledProperties[index]
            if (property.transient) continue
            // output.writeXxxElementValue(classDesc, index, value)
            val sti = getSerialTypeInfo(property)
            val innerSerial = if (sti.serializer == null) null else serializerInstance(sti.serializer, property.module, property.type)
            if (innerSerial == null) {
                val writeFunc =
                        kOutputClass.getFuncDesc("write${sti.elementMethodPrefix}ElementValue").single()
                                .let { ctx.getNameForDescriptor(it) }
                +JsInvocation(JsNameRef(writeFunc, localOutputRef),
                              serialClassDescRef,
                              JsIntLiteral(index),
                              JsNameRef(ctx.getNameForDescriptor(property.descriptor), objRef)).makeStmt()
            }
            else {
                val writeFunc =
                        kOutputClass.getFuncDesc("write${sti.elementMethodPrefix}SerializableElementValue").single()
                                .let { ctx.getNameForDescriptor(it) }
                +JsInvocation(JsNameRef(writeFunc, localOutputRef),
                              serialClassDescRef,
                              JsIntLiteral(index),
                              innerSerial,
                              JsNameRef(ctx.getNameForDescriptor(property.descriptor), objRef)).makeStmt()
            }
        }

        // output.writeEnd(serialClassDesc)
        val wEndFunc = kOutputClass.getFuncDesc("writeEnd").single()
                .let { ctx.getNameForDescriptor(it) }
        +JsInvocation(JsNameRef(wEndFunc, localOutputRef), serialClassDescRef).makeStmt()
    }

    private fun getQualifiedClassReferenceName(classDescriptor: ClassDescriptor): JsExpression {
        return context.getQualifiedReference(classDescriptor)
    }

    private fun serializerInstance(serializerClass: ClassDescriptor, module: ModuleDescriptor, kType: KotlinType): JsExpression? {
        val nullableSerClass = getQualifiedClassReferenceName(
                serializerClass.getClassFromInternalSerializationPackage("NullableSerializer"))
        if (serializerClass.kind == ClassKind.OBJECT) {
            return getQualifiedClassReferenceName(serializerClass)
        }
        else {
            val args = when {
                serializerClass.classId == enumSerializerId -> listOf(createGetKClassExpression(kType.toClassDescriptor!!))
                serializerClass.classId == referenceArraySerializerId -> TODO("arrays are not supported yet")
                else -> kType.arguments.map {
                    val argSer = findTypeSerializer(module, it.type) ?: return null
                    val expr = serializerInstance(argSer, module, it.type) ?: return null
                    if (it.type.isMarkedNullable) JsNew(nullableSerClass, listOf(expr)) else expr
                }
            }
            return JsNew(getQualifiedClassReferenceName(serializerClass), args)
        }
    }

    private fun createGetKClassExpression(classDescriptor: ClassDescriptor): JsExpression =
            JsInvocation(context.namer().kotlin("getKClass"),
                         getQualifiedClassReferenceName(classDescriptor))


    override fun generateLoad(function: FunctionDescriptor) {
        // todo
    }

    companion object {
        fun translate(declaration: KtPureClassOrObject, descriptor: ClassDescriptor, translator: DeclarationBodyVisitor, context: TranslationContext) {
            if (getSerializableClassDescriptorBySerializer(descriptor) != null)
                SerializerJsTranslator(declaration, translator, context).generate()
        }
    }
}