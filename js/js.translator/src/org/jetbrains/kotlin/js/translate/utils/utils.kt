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

package org.jetbrains.kotlin.js.translate.utils

import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.backend.common.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.backend.common.COROUTINE_SUSPENDED_NAME
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.CoroutineMetadata
import org.jetbrains.kotlin.js.backend.ast.metadata.coroutineMetadata
import org.jetbrains.kotlin.js.backend.ast.metadata.exportedPackage
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsicWithReceiverComputed
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.simpleReturnFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOrInheritsParametersWithDefaultValue
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType

fun generateDelegateCall(
        classDescriptor: ClassDescriptor,
        fromDescriptor: FunctionDescriptor,
        toDescriptor: FunctionDescriptor,
        thisObject: JsExpression,
        context: TranslationContext,
        detectDefaultParameters: Boolean,
        source: PsiElement?
): JsStatement {
    fun FunctionDescriptor.getNameForFunctionWithPossibleDefaultParam() =
            if (detectDefaultParameters && hasOrInheritsParametersWithDefaultValue()) {
                context.scope().declareName(context.getNameForDescriptor(this).ident + Namer.DEFAULT_PARAMETER_IMPLEMENTOR_SUFFIX)
            }
            else {
                context.getNameForDescriptor(this)
            }

    val overriddenMemberFunctionName = toDescriptor.getNameForFunctionWithPossibleDefaultParam()
    val overriddenMemberFunctionRef = JsNameRef(overriddenMemberFunctionName, thisObject)

    val parameters = SmartList<JsParameter>()
    val args = SmartList<JsExpression>()

    if (DescriptorUtils.isExtension(fromDescriptor)) {
        val extensionFunctionReceiverName = JsScope.declareTemporaryName(Namer.getReceiverParameterName())
        parameters.add(JsParameter(extensionFunctionReceiverName))
        args.add(JsNameRef(extensionFunctionReceiverName))
    }

    for (param in fromDescriptor.valueParameters) {
        val paramName = param.name.asString()
        val jsParamName = JsScope.declareTemporaryName(paramName)
        parameters.add(JsParameter(jsParamName))
        args.add(JsNameRef(jsParamName))
    }

    val intrinsic = context.intrinsics().getFunctionIntrinsic(toDescriptor)
    val invocation = if (intrinsic.exists() && intrinsic is FunctionIntrinsicWithReceiverComputed) {
        intrinsic.apply(thisObject, args, context)
    }
    else {
        JsInvocation(overriddenMemberFunctionRef, args)
    }

    invocation.source = source

    val functionObject = simpleReturnFunction(context.scope(), invocation)
    functionObject.parameters.addAll(parameters)

    val fromFunctionName = fromDescriptor.getNameForFunctionWithPossibleDefaultParam()

    val prototypeRef = JsAstUtils.prototypeOf(context.getInnerReference(classDescriptor))
    val functionRef = JsNameRef(fromFunctionName, prototypeRef)
    return JsAstUtils.assignment(functionRef, functionObject).makeStmt()
}

fun <T, S> List<T>.splitToRanges(classifier: (T) -> S): List<Pair<List<T>, S>> {
    if (isEmpty()) return emptyList()

    var lastIndex = 0
    var lastClass: S = classifier(this[0])
    val result = mutableListOf<Pair<List<T>, S>>()

    for ((index, e) in asSequence().withIndex().drop(1)) {
        val cls = classifier(e)
        if (cls != lastClass) {
            result += Pair(subList(lastIndex, index), lastClass)
            lastClass = cls
            lastIndex = index
        }
    }

    result += Pair(subList(lastIndex, size), lastClass)
    return result
}

fun getReferenceToJsClass(type: KotlinType, context: TranslationContext): JsExpression {
    val classifierDescriptor = type.constructor.declarationDescriptor

    val referenceToJsClass: JsExpression = when (classifierDescriptor) {
        is ClassDescriptor -> {
            ReferenceTranslator.translateAsTypeReference(classifierDescriptor, context)
        }
        is TypeParameterDescriptor -> {
            assert(classifierDescriptor.isReified)

            context.usageTracker()?.used(classifierDescriptor)

            context.getNameForDescriptor(classifierDescriptor).makeRef()
        }
        else -> {
            throw IllegalStateException("Can't get reference for $type")
        }
    }

    return referenceToJsClass
}

fun TranslationContext.addFunctionToPrototype(
        classDescriptor: ClassDescriptor,
        descriptor: FunctionDescriptor,
        function: JsExpression
): JsStatement {
    val prototypeRef = JsAstUtils.prototypeOf(getInnerReference(classDescriptor))
    val functionRef = JsNameRef(getNameForDescriptor(descriptor), prototypeRef)
    return JsAstUtils.assignment(functionRef, function).makeStmt()
}

fun TranslationContext.addAccessorsToPrototype(
        containingClass: ClassDescriptor,
        propertyDescriptor: PropertyDescriptor,
        literal: JsObjectLiteral
) {
    val prototypeRef = JsAstUtils.prototypeOf(getInnerReference(containingClass))
    val propertyName = getNameForDescriptor(propertyDescriptor)
    val defineProperty = JsAstUtils.defineProperty(prototypeRef, propertyName.ident, literal)
    addDeclarationStatement(defineProperty.makeStmt())
}

fun FunctionDescriptor.requiresStateMachineTransformation(context: TranslationContext): Boolean =
        this is AnonymousFunctionDescriptor ||
        context.bindingContext()[BindingContext.CONTAINS_NON_TAIL_SUSPEND_CALLS, this] == true

fun JsFunction.fillCoroutineMetadata(
        context: TranslationContext,
        descriptor: FunctionDescriptor,
        hasController: Boolean
) {
    if (!descriptor.requiresStateMachineTransformation(context)) return

    val suspendPropertyDescriptor = context.currentModule.getPackage(COROUTINES_INTRINSICS_PACKAGE_FQ_NAME)
            .memberScope
            .getContributedVariables(COROUTINE_SUSPENDED_NAME, NoLookupLocation.FROM_BACKEND).first()

    val coroutineBaseClassRef = ReferenceTranslator.translateAsTypeReference(TranslationUtils.getCoroutineBaseClass(context), context)

    fun getCoroutinePropertyName(id: String) =
            context.getNameForDescriptor(TranslationUtils.getCoroutineProperty(context, id))

    coroutineMetadata = CoroutineMetadata(
            doResumeName = context.getNameForDescriptor(TranslationUtils.getCoroutineDoResumeFunction(context)),
            suspendObjectRef = ReferenceTranslator.translateAsValueReference(suspendPropertyDescriptor, context),
            baseClassRef = coroutineBaseClassRef,
            stateName = getCoroutinePropertyName("state"),
            exceptionStateName = getCoroutinePropertyName("exceptionState"),
            finallyPathName = getCoroutinePropertyName("finallyPath"),
            resultName = getCoroutinePropertyName("result"),
            exceptionName = getCoroutinePropertyName("exception"),
            hasController = hasController,
            hasReceiver = descriptor.dispatchReceiverParameter != null,
            psiElement = descriptor.source.getPsi()
    )
}

fun definePackageAlias(name: String, varName: JsName, tag: String, parentRef: JsExpression): JsStatement {
    val selfRef = JsNameRef(name, parentRef)
    val rhs = JsAstUtils.or(selfRef, JsAstUtils.assignment(selfRef.deepCopy(), JsObjectLiteral(false)))

    return JsAstUtils.newVar(varName, rhs).apply { exportedPackage = tag }
}
