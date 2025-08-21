package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.name.Name

object ComposeNames {
    val ComposerParameter = Name.identifier($$"$composer")
    val ChangedParameter = Name.identifier($$"$changed")
    val ForceParameter = Name.identifier($$"$force")
    val DefaultParameter = Name.identifier($$"$default")
    val StabilityFlag = Name.identifier($$"$stable")
    val StabilityFlagProperty = Name.identifier($$"$stableprop")
    val StabilityFlagPropertyGetter = $$"$stableprop_getter"
    val JoinKey = Name.identifier("joinKey")
    val StartRestartGroup = Name.identifier("startRestartGroup")
    val EndRestartGroup = Name.identifier("endRestartGroup")
    val UpdateScope = Name.identifier("updateScope")
    val SourceInformation = "sourceInformation"
    val SourceInformationMarkerStart = "sourceInformationMarkerStart"
    val IsTraceInProgress = "isTraceInProgress"
    val TraceEventStart = "traceEventStart"
    val TraceEventEnd = "traceEventEnd"
    val SourceInformationMarkerEnd = "sourceInformationMarkerEnd"
    val UpdateChangedFlags = "updateChangedFlags"
    val CurrentMarker = Name.identifier("currentMarker")
    val EndToMarker = Name.identifier("endToMarker")
    val RememberComposableLambda = "rememberComposableLambda"
    val RememberComposableLambdaN = "rememberComposableLambdaN"
    val DefaultImpls = Name.identifier("ComposeDefaultImpls")
    val ShouldExecute = Name.identifier("shouldExecute")
}
