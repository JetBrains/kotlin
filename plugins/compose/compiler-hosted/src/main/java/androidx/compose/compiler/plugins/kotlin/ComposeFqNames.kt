/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasEqualFqName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private const val root = "androidx.compose.runtime"
private const val internalRoot = "$root.internal"
private val rootFqName = FqName(root)
private val internalRootFqName = FqName(internalRoot)

object ComposeClassIds {
    private fun classIdFor(cname: String) =
        ClassId(rootFqName, Name.identifier(cname))
    internal fun internalClassIdFor(cname: String) =
        ClassId(internalRootFqName, Name.identifier(cname))

    val Composable = classIdFor("Composable")
    val ComposableInferredTarget = classIdFor("ComposableInferredTarget")
    val ComposableLambda = internalClassIdFor("ComposableLambda")
    val ComposableOpenTarget = classIdFor("ComposableOpenTarget")
    val ComposableTarget = classIdFor("ComposableTarget")
    val ComposeVersion = classIdFor("ComposeVersion")
    val Composer = classIdFor("Composer")
    val DisallowComposableCalls = classIdFor("DisallowComposableCalls")
    val FunctionKeyMetaClass = internalClassIdFor("FunctionKeyMetaClass")
    val FunctionKeyMeta = internalClassIdFor("FunctionKeyMeta")
    val LiveLiteralFileInfo = internalClassIdFor("LiveLiteralFileInfo")
    val LiveLiteralInfo = internalClassIdFor("LiveLiteralInfo")
    val NoLiveLiterals = classIdFor("NoLiveLiterals")
    val ReadOnlyComposable = classIdFor("ReadOnlyComposable")
    val State = classIdFor("State")
    val StabilityInferred = internalClassIdFor("StabilityInferred")
}

object ComposeCallableIds {
    private fun topLevelCallableId(name: String) =
        CallableId(rootFqName, Name.identifier(name))
    internal fun internalTopLevelCallableId(name: String) =
        CallableId(internalRootFqName, Name.identifier(name))

    val cache = topLevelCallableId("cache")
    val composableLambda = internalTopLevelCallableId("composableLambda")
    val composableLambdaInstance =
        internalTopLevelCallableId("composableLambdaInstance")
    val composableLambdaN = internalTopLevelCallableId("composableLambdaN")
    val composableLambdaNInstance =
        internalTopLevelCallableId("composableLambdaNInstance")
    val currentComposer = topLevelCallableId("currentComposer")
    val isLiveLiteralsEnabled =
        internalTopLevelCallableId("isLiveLiteralsEnabled")
    val isTraceInProgress =
        topLevelCallableId(KtxNameConventions.IS_TRACE_IN_PROGRESS)
    val liveLiteral = internalTopLevelCallableId("liveLiteral")
    val remember = topLevelCallableId("remember")
    val sourceInformation =
        topLevelCallableId(KtxNameConventions.SOURCEINFORMATION)
    val sourceInformationMarkerEnd =
        topLevelCallableId(KtxNameConventions.SOURCEINFORMATIONMARKEREND)
    val sourceInformationMarkerStart =
        topLevelCallableId(KtxNameConventions.SOURCEINFORMATIONMARKERSTART)
    val traceEventEnd = topLevelCallableId(KtxNameConventions.TRACE_EVENT_END)
    val traceEventStart = topLevelCallableId(KtxNameConventions.TRACE_EVENT_START)
    val updateChangedFlags = topLevelCallableId(KtxNameConventions.UPDATE_CHANGED_FLAGS)
    val rememberComposableLambda =
        internalTopLevelCallableId(KtxNameConventions.REMEMBER_COMPOSABLE_LAMBDA)
    val rememberComposableLambdaN =
        internalTopLevelCallableId(KtxNameConventions.REMEMBER_COMPOSABLE_LAMBDAN)
}

object ComposeFqNames {
    internal fun fqNameFor(cname: String) = FqName("$root.$cname")
    private fun internalFqNameFor(cname: String) = FqName("$internalRoot.$cname")
    private fun composablesFqNameFor(cname: String) = fqNameFor("ComposablesKt.$cname")

    val InternalPackage = internalRootFqName
    val Composable = ComposeClassIds.Composable.asSingleFqName()
    val ComposableTarget = ComposeClassIds.ComposableTarget.asSingleFqName()
    val ComposableTargetMarker = fqNameFor("ComposableTargetMarker")
    val ComposableTargetMarkerDescription = "description"
    val ComposableTargetApplierArgument = Name.identifier("applier")
    val ComposableOpenTarget = ComposeClassIds.ComposableOpenTarget.asSingleFqName()
    val ComposableOpenTargetIndexArgument = Name.identifier("index")
    val ComposableInferredTarget = ComposeClassIds.ComposableInferredTarget.asSingleFqName()
    val ComposableInferredTargetSchemeArgument = Name.identifier("scheme")
    val CurrentComposerIntrinsic = fqNameFor("<get-currentComposer>")
    val getCurrentComposerFullName = composablesFqNameFor("<get-currentComposer>")
    val DisallowComposableCalls = ComposeClassIds.DisallowComposableCalls.asSingleFqName()
    val ReadOnlyComposable = ComposeClassIds.ReadOnlyComposable.asSingleFqName()
    val ExplicitGroupsComposable = fqNameFor("ExplicitGroupsComposable")
    val NonRestartableComposable = fqNameFor("NonRestartableComposable")
    val NonSkippableComposable = fqNameFor("NonSkippableComposable")
    val DontMemoize = fqNameFor("DontMemoize")
    val composableLambdaType = ComposeClassIds.ComposableLambda.asSingleFqName()
    val composableLambda = ComposeCallableIds.composableLambda.asSingleFqName()
    val rememberComposableLambda = ComposeCallableIds.rememberComposableLambda.asSingleFqName()
    val composableLambdaFullName =
        internalFqNameFor("ComposableLambdaKt.composableLambda")
    val remember = ComposeCallableIds.remember.asSingleFqName()
    val cache = ComposeCallableIds.cache.asSingleFqName()
    val key = fqNameFor("key")
    val StableMarker = fqNameFor("StableMarker")
    val Stable = fqNameFor("Stable")
    val Immutable = fqNameFor("Immutable")
    val Composer = ComposeClassIds.Composer.asSingleFqName()
    val StabilityInferred = ComposeClassIds.StabilityInferred.asSingleFqName()
}

fun IrType.hasComposableAnnotation(): Boolean =
    hasAnnotation(ComposeFqNames.Composable)

fun IrAnnotationContainer.hasComposableAnnotation(): Boolean =
    hasAnnotation(ComposeFqNames.Composable)

@UnsafeDuringIrConstructionAPI
fun IrConstructorCall.isComposableAnnotation() =
    symbol.owner.constructedClass.hasEqualFqName(ComposeFqNames.Composable)
