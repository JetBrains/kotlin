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
import org.jetbrains.kotlin.nj2k.tree.JKTreeRoot

object ConversionsRunner {

    private fun createConversions(context: NewJ2kConverterContext) = listOf(
        NonCodeElementsConversion(),
        JavaModifiersConversion(context),
        JavaAnnotationsConversion(context),
        AnnotationClassConversion(context),
        AnnotationConversion(context),
        ModalityConversion(context),
        FunctionAsAnonymousObjectToLambdaConversion(),
        ReturnStatementInLambdaExpressionConversion(),
        BoxedTypeOperationsConversion(context),
        AssignmentAsExpressionToAlsoConversion(context),
        AssignmentStatementValCreationConversion(context),
        AssignmentStatementOperatorConversion(),
        AssignmentStatementSimplifyValConversion(),
        AssignmentStatementSimplifyAlsoConversion(),
        AssignmentStatementSplitAlsoConversion(),
        PolyadicExpressionConversion(),
        OperatorExpressionConversion(context),
        AddParenthesisForLineBreaksInBinaryExpression(),
        ThrowStatementConversion(),
        ArrayInitializerConversion(context),
        TryStatementConversion(context),
        EnumFieldAccessConversion(context),
        StaticMemberAccessConversion(context),
        SynchronizedStatementConversion(context),
        JetbrainsNullableAnnotationsConverter(context),
        DefaultArgumentsConversion(context),
        ConstructorConversion(context),
        StaticInitDeclarationConversion(),
        ImplicitInitializerConversion(context),
        ParameterModificationInMethodCallsConversion(context),
        BlockToRunConversion(context),
        PrimaryConstructorDetectConversion(context),
        InsertDefaultPrimaryConstructorConversion(context),
        FieldToPropertyConversion(),
        JavaStandardMethodsConversion(context),
        JavaMethodToKotlinFunctionConversion(context),
        MainFunctionConversion(context),
        AssertStatementConversion(context),
        SwitchStatementConversion(context),
        LiteralConversion(),
        ForConversion(context),
        LabeledStatementConversion(),
        ArrayOperationsConversion(context),
        EqualsOperatorConversion(context),
        TypeMappingConversion(context),
        InternalDeclarationConversion(context),

        InnerClassConversion(),
        StaticsToCompanionExtractConversion(),
        InterfaceWithFieldConversion(),
        ClassToObjectPromotionConversion(context),
        RemoveWrongExtraModifiersForSingleFunctionsConversion(),
        MethodReferenceToLambdaConversion(context),
        BuiltinMembersConversion(context),
        ImplicitCastsConversion(context),
        LiteralConversion(),
        CollectImportsConversion(context),
        FilterImportsConversion(),
        MoveInitBlocksToTheEndConversion(),
        AddElementsInfoConversion(context)
    )

    fun doApply(trees: List<JKTreeRoot>, context: NewJ2kConverterContext) {
        val conversions = createConversions(context)
        for (conversion in conversions) {
            conversion.runConversion(trees, context)
        }
    }
}