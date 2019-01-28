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

package org.jetbrains.kotlin.j2k

import org.jetbrains.kotlin.j2k.conversions.*
import org.jetbrains.kotlin.j2k.tree.JKTreeElement

object ConversionsRunner {

    private fun createRootConversion(context: ConversionContext) =
        batchPipe {
            //Java --> Kotlin conversions
            +JavaModifiersConversion(context)
            +JavaAnnotationsConversion(context)
            +AnnotationClassConversion(context)
            +AnnotationConversion(context)
            +ModalityConversion(context)
            +BoxedTypeOperationsConversion(context)
            +AssignmentAsExpressionToAlsoConversion(context)
            +AssignmentStatementValCreationConversion(context)
            +AssignmentStatementOperatorConversion()
            +AssignmentStatementSimplifyValConversion()
            +AssignmentStatementSimplifyAlsoConversion()
            +PolyadicExpressionConversion(context)
            +OperatorExpressionConversion(context)
            +ThrowStatementConversion()
            +EnumClassConversion()
            +ArrayInitializerConversion(context)
            +TryStatementConversion(context)
            +SynchronizedStatementConversion(context)
            +DefaultArgumentsConversion(context)
            +ConstructorConversion(context)
            +ImplicitInitializerConversion(context)
            +ParameterModificationInMethodCallsConversion(context)
            +PrintlnConversion(context)
            +BlockToRunConversion(context)
            +JetbrainsNullableAnnotationsConverter(context)
            +PrimaryConstructorDetectConversion(context)
            +InsertDefaultPrimaryConstructorConversion(context)
            +FieldToPropertyConversion()
            +JavaStandartMethodsConversion(context)
            +JavaMethodToKotlinFunctionConversion(context)
            +MainFunctionConversion(context)
            +AssertStatementConversion(context)
            +SwitchStatementConversion(context)
            +LiteralConversion()
            +ForConversion(context)
            +LabeledStatementConversion()
            +ForInConversion()
            +TypeParametersNullabilityConversion()
            +ArrayOperationsConversion(context)
            +BuiltinMembersConversion(context)
            +EqualsOperatorConversion(context)
            +TypeMappingConversion(context)
            +ImplicitCastsConversion(context)
            +LowerNullabilityInFunctionParametersConversion(context)
            +DefaultNullabilityToNullableConversion()
            +NullableQualifierConversion(context)
            +InternalClassConversion()

            //Kotlin --> Kotlin conversions
            +InnerClassConversion()
            +StaticsToCompanionExtractConversion()
            +InterfaceWithFieldConversion()
            +ClassToObjectPromotionConversion(context)
            +ImportStatementConversion()
            +SortClassMembersConversion()
        }

    fun doApply(trees: List<JKTreeElement>, context: ConversionContext) {
        val conversion = createRootConversion(context)
        conversion.runConversion(trees, context)
    }

}