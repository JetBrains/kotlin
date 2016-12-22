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

package org.jetbrains.kotlin.js.translate.expression

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.*
import org.jetbrains.kotlin.builtins.isBuiltinExtensionFunctionalType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.descriptorUtils.isCoroutineLambda
import org.jetbrains.kotlin.js.inline.util.getInnerFunction
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.context.getNameForCapturedDescriptor
import org.jetbrains.kotlin.js.translate.context.hasCapturedExceptContaining
import org.jetbrains.kotlin.js.translate.context.isCaptured
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getFunctionDescriptor
import org.jetbrains.kotlin.js.translate.utils.FunctionBodyTranslator.translateFunctionBody
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.simpleReturnFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.KotlinType

class LiteralFunctionTranslator(context: TranslationContext) : AbstractTranslator(context) {
    companion object {
        private val SUSPEND_NAME = Name.identifier("SUSPENDED")
        private val COROUTINE_INTRINSICS_FQ_NAME = FqName("kotlin.coroutines.CoroutineIntrinsics")
    }

    fun translate(
            declaration: KtDeclarationWithBody,
            continuationType: KotlinType? = null
    ): JsExpression {
        val invokingContext = context()
        val descriptor = getFunctionDescriptor(invokingContext.bindingContext(), declaration)

        val lambda = invokingContext.getFunctionObject(descriptor)

        val aliases = mutableMapOf<DeclarationDescriptor, JsExpression>()
        if (descriptor.isCoroutineLambda) {
            aliases.put(descriptor, JsLiteral.THIS)
        }

        val functionContext = invokingContext
                .newFunctionBodyWithUsageTracker(lambda, descriptor)
                .innerContextWithDescriptorsAliased(aliases)
        FunctionTranslator.addParameters(lambda.parameters, descriptor, functionContext)

        descriptor.valueParameters.forEach {
            if (it is ValueParameterDescriptorImpl.WithDestructuringDeclaration) {
                lambda.body.statements.add(it.translate(functionContext))
            }
        }

        val functionBody = translateFunctionBody(descriptor, declaration, functionContext)
        lambda.body.statements.addAll(functionBody.statements)
        lambda.functionDescriptor = descriptor

        val tracker = functionContext.usageTracker()!!

        val isRecursive = tracker.isCaptured(descriptor)

        lambda.name = if (isRecursive) {
            tracker.getNameForCapturedDescriptor(descriptor)
        }
        else {
            invokingContext.getInnerNameForDescriptor(descriptor)
        }

        if (tracker.hasCapturedExceptContaining()) {
            val lambdaCreator = simpleReturnFunction(invokingContext.scope(), lambda)
            lambdaCreator.name = invokingContext.getInnerNameForDescriptor(descriptor)
            lambdaCreator.isLocal = true
            if (!isRecursive || descriptor.isCoroutineLambda) {
                lambda.name = null
            }
            lambdaCreator.name.staticRef = lambdaCreator
            lambdaCreator.fillCoroutineMetadata(invokingContext, continuationType)
            return lambdaCreator.withCapturedParameters(descriptor, descriptor.wrapContextForCoroutineIfNecessary(functionContext),
                                                        invokingContext)
        }

        lambda.isLocal = true

        invokingContext.addDeclarationStatement(lambda.makeStmt())
        lambda.fillCoroutineMetadata(invokingContext, continuationType)
        lambda.name.staticRef = lambda
        return getReferenceToLambda(invokingContext, descriptor, lambda.name)
    }

    fun JsFunction.fillCoroutineMetadata(context: TranslationContext, continuationType: KotlinType?) {
        if (continuationType == null) return

        val suspendPropertyDescriptor = context().currentModule.getPackage(COROUTINE_INTRINSICS_FQ_NAME.parent())
                .memberScope.getContributedClassifier(COROUTINE_INTRINSICS_FQ_NAME.shortName(), NoLookupLocation.FROM_BACKEND)
                .let { (it as ClassDescriptor) }.unsubstitutedMemberScope
                .getContributedVariables(SUSPEND_NAME, NoLookupLocation.FROM_BACKEND).first()

        val coroutineBaseClassRef = ReferenceTranslator.translateAsTypeReference(TranslationUtils.getCoroutineBaseClass(context), context)

        fun getCoroutinePropertyName(id: String) =
                context.getNameForDescriptor(TranslationUtils.getCoroutineProperty(context, id))

        coroutineMetadata = CoroutineMetadata(
                doResumeName = context.getNameForDescriptor(TranslationUtils.getCoroutineDoResumeFunction(context)),
                resumeName = context.getNameForDescriptor(TranslationUtils.getCoroutineResumeFunction(context)),
                suspendObjectRef = ReferenceTranslator.translateAsValueReference(suspendPropertyDescriptor, context()),
                baseClassRef = coroutineBaseClassRef,
                stateName = getCoroutinePropertyName("state"),
                exceptionStateName = getCoroutinePropertyName("exceptionState"),
                finallyPathName = getCoroutinePropertyName("finallyPath"),
                resultName = getCoroutinePropertyName("result"),
                exceptionName = getCoroutinePropertyName("exception"),
                facadeName = getCoroutinePropertyName("facade"),
                hasController = continuationType.isBuiltinExtensionFunctionalType
        )
        coroutineType = continuationType
    }

    fun ValueParameterDescriptorImpl.WithDestructuringDeclaration.translate(context: TranslationContext): JsVars {
        val destructuringDeclaration =
                (DescriptorToSourceUtils.descriptorToDeclaration(this) as? KtParameter)?.destructuringDeclaration
                ?: error("Destructuring declaration for descriptor $this not found")

        val jsParameter = JsParameter(context.getNameForDescriptor(this))
        return DestructuringDeclarationTranslator.translate(destructuringDeclaration, jsParameter.name, null, context)
    }
}

private fun CallableMemberDescriptor.wrapContextForCoroutineIfNecessary(context: TranslationContext): TranslationContext {
    return if (isCoroutineLambda) context.innerContextWithDescriptorsAliased(mapOf(this to JsLiteral.THIS)) else context
}

fun JsFunction.withCapturedParameters(
        descriptor: CallableMemberDescriptor,
        context: TranslationContext,
        invokingContext: TranslationContext
): JsExpression {
    context.addDeclarationStatement(makeStmt())
    val ref = getReferenceToLambda(invokingContext, descriptor, name)
    val invocation = JsInvocation(ref).apply { sideEffects = SideEffectKind.PURE }

    val invocationArguments = invocation.arguments
    val functionParameters = this.parameters

    val tracker = context.usageTracker()!!

    for ((capturedDescriptor, name) in tracker.capturedDescriptorToJsName) {
        if (capturedDescriptor == tracker.containingDescriptor && !capturedDescriptor.isCoroutineLambda) continue

        val capturedRef = invokingContext.getArgumentForClosureConstructor(capturedDescriptor)
        var additionalArgs = listOf(capturedRef)
        var additionalParams = listOf(JsParameter(name))

        if (capturedDescriptor is TypeParameterDescriptor && capturedDescriptor.isReified) {
            // Preserve the usual order
            additionalArgs = listOf(invokingContext.getNameForDescriptor(capturedDescriptor).makeRef()) + additionalArgs
            additionalParams = listOf(JsParameter(context.getNameForDescriptor(capturedDescriptor))) + additionalParams
        }

        if (capturedDescriptor is CallableDescriptor && isLocalInlineDeclaration(capturedDescriptor)) {
            val aliasRef = capturedRef as? JsNameRef
            val localFunAlias = aliasRef?.getStaticRef() as? JsExpression

            if (localFunAlias != null) {
                val (args, params) = moveCapturedLocalInside(this, name, localFunAlias)
                additionalArgs = args
                additionalParams = params
            }
        }

        functionParameters.addAll(additionalParams)
        invocationArguments.addAll(additionalArgs)
    }

    return invocation
}

private fun getReferenceToLambda(context: TranslationContext, descriptor: CallableMemberDescriptor, name: JsName): JsExpression {
    return if (context.isPublicInlineFunction) {
        val fqn = context.getQualifiedReference(descriptor)
        if (fqn is JsNameRef) {
            fqn.name?.let { it.staticRef = name.staticRef }
        }
        fqn
    }
    else {
        JsAstUtils.pureFqn(name, null)
    }
}

private data class CapturedArgsParams(val arguments: List<JsExpression> = listOf(), val parameters: List<JsParameter> = listOf())

/**
 * Moves captured local inline function inside capturing function.
 *
 * For example:
 *  var inc = _.foo.inc(closure) // local fun that captures closure
 *  capturingFunction(inc)
 *
 * Is transformed to:
 *  capturingFunction(closure) // var inc = _.foo.inc(closure) is moved inside capturingFunction
 */
private fun moveCapturedLocalInside(capturingFunction: JsFunction, capturedName: JsName, localFunAlias: JsExpression): CapturedArgsParams =
    when (localFunAlias) {
        is JsNameRef -> {
            /** Local inline function does not capture anything, so just move alias inside */
            declareAliasInsideFunction(capturingFunction, capturedName, localFunAlias)
            CapturedArgsParams()
        }
        is JsInvocation ->
            moveCapturedLocalInside(capturingFunction, capturedName, localFunAlias)
        else ->
            throw AssertionError("Local function reference has wrong alias $localFunAlias")
    }

/**
 * Processes case when local inline function with capture
 * is captured by capturingFunction.
 *
 * In this case, capturingFunction should
 * capture arguments captured by localFunAlias,
 * and localFunAlias declaration is moved inside.
 *
 * For example:
 *
 * ```
 *  val x = 0
 *  inline fun id() = x
 *  val lambda = {println(id())}
 * ```
 *
 * `lambda` should capture x in this case
 */
private fun moveCapturedLocalInside(capturingFunction: JsFunction, capturedName: JsName, localFunAlias: JsInvocation): CapturedArgsParams {
    val capturedArgs = localFunAlias.arguments

    val scope = capturingFunction.getInnerFunction()?.scope!!
    val freshNames = getTemporaryNamesInScope(scope, capturedArgs)

    val aliasCallArguments = freshNames.map { it.makeRef() }
    val alias = JsInvocation(localFunAlias.qualifier, aliasCallArguments)
    declareAliasInsideFunction(capturingFunction, capturedName, alias)

    val capturedParameters = freshNames.map(::JsParameter)
    return CapturedArgsParams(capturedArgs, capturedParameters)
}

private fun declareAliasInsideFunction(function: JsFunction, name: JsName, alias: JsExpression) {
    name.staticRef = alias
    function.getInnerFunction()?.addDeclaration(name, alias)
}

private fun getTemporaryNamesInScope(scope: JsScope, suggested: List<JsExpression>): List<JsName> {
    val freshNames = arrayListOf<JsName>()

    for (suggestion in suggested) {
        if (suggestion !is JsNameRef) {
            throw AssertionError("Expected suggestion to be JsNameRef")
        }

        val ident = suggestion.ident
        val name = scope.declareTemporaryName(ident)
        freshNames.add(name)
    }

    return freshNames
}

private fun JsFunction.addDeclaration(name: JsName, value: JsExpression?) {
    val declaration = JsAstUtils.newVar(name, value)
    this.body.statements.add(0, declaration)
}

private fun HasName.getStaticRef(): JsNode? {
    return this.name?.staticRef
}

private fun isLocalInlineDeclaration(descriptor: CallableDescriptor): Boolean {
    return descriptor is FunctionDescriptor
           && descriptor.getVisibility() == Visibilities.LOCAL
           && InlineUtil.isInline(descriptor)
}
