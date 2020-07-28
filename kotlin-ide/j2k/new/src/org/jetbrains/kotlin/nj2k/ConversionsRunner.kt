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

package org.jetbrains.kotlin.nj2k

import org.jetbrains.kotlin.nj2k.conversions.*
import org.jetbrains.kotlin.nj2k.tree.JKLambdaExpression
import org.jetbrains.kotlin.nj2k.tree.JKParameter
import org.jetbrains.kotlin.nj2k.tree.JKTreeRoot
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object ConversionsRunner {
    private fun createConversions(context: NewJ2kConverterContext) = listOf(
        NonCodeElementsConversion(context),
        JavaModifiersConversion(context),
        JavaAnnotationsConversion(context),
        AnnotationClassConversion(context),
        AnnotationConversion(context),
        ModalityConversion(context),
        FunctionAsAnonymousObjectToLambdaConversion(context),
        ReturnStatementInLambdaExpressionConversion(context),
        BoxedTypeOperationsConversion(context),
        AnyWithStringConcatenationConversion(context),
        AssignmentExpressionUnfoldingConversion(context),
        AddParenthesisForLineBreaksInBinaryExpression(context),
        ThrowStatementConversion(context),
        ArrayInitializerConversion(context),
        TryStatementConversion(context),
        EnumFieldAccessConversion(context),
        SynchronizedStatementConversion(context),
        JetbrainsNullableAnnotationsConverter(context),
        DefaultArgumentsConversion(context),
        ConstructorConversion(context),
        StaticInitDeclarationConversion(context),
        ImplicitInitializerConversion(context),
        ParameterModificationInMethodCallsConversion(context),
        BlockToRunConversion(context),
        PrimaryConstructorDetectConversion(context),
        InsertDefaultPrimaryConstructorConversion(context),
        FieldToPropertyConversion(context),
        JavaStandardMethodsConversion(context),
        JavaMethodToKotlinFunctionConversion(context),
        MainFunctionConversion(context),
        AssertStatementConversion(context),
        SwitchStatementConversion(context),
        LiteralConversion(context),
        ForConversion(context),
        LabeledStatementConversion(context),
        ArrayOperationsConversion(context),
        EqualsOperatorConversion(context),
        TypeMappingConversion(context),
        InternalDeclarationConversion(context),

        InnerClassConversion(context),
        StaticsToCompanionExtractConversion(context),
        InterfaceWithFieldConversion(context),
        ClassToObjectPromotionConversion(context),
        RemoveWrongExtraModifiersForSingleFunctionsConversion(context),
        MethodReferenceToLambdaConversion(context),
        TypeMappingConversion(context) { typeElement ->
            typeElement.parent.safeAs<JKParameter>()?.parent is JKLambdaExpression
        },
        BuiltinMembersConversion(context),
        ImplicitCastsConversion(context),
        LiteralConversion(context),
        StaticMemberAccessConversion(context),
        RemoveRedundantQualifiersForCallsConversion(context),
        FunctionalInterfacesConverter(context),

        FilterImportsConversion(context),
        MoveInitBlocksToTheEndConversion(context),
        AddElementsInfoConversion(context)
    )


    fun doApply(
        trees: List<JKTreeRoot>,
        context: NewJ2kConverterContext,
        updateProgress: (conversionIndex: Int, conversionCount: Int, fileIndex: Int, String) -> Unit
    ) {

        val conversions = createConversions(context)
        for ((conversionIndex, conversion) in conversions.withIndex()) {

            val treeSequence = trees.asSequence().onEachIndexed { index, _ ->
                updateProgress(conversionIndex, conversions.size, index, conversion.description())
            }

            conversion.runConversion(treeSequence, context)
        }
    }

    private fun Conversion.description(): String {
        val conversionName = this::class.simpleName
        val words = conversionName?.let { wordRegex.findAll(conversionName).map { it.value.decapitalize() }.toList() }
        return when {
            conversionName == null -> "Converting..."
            conversionName.endsWith("Conversion") -> "Converting ${words!!.dropLast(1).joinToString(" ")}"
            else -> words!!.joinToString(" ")
        }
    }

    private val wordRegex = "[A-Z][a-z0-9]+".toRegex()
}