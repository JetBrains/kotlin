package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.name.Name

object KtxNameConventions {
    val COMPOSER = Name.identifier("composer")
    val COMPOSER_PARAMETER = Name.identifier("\$composer")
    val CHANGED_PARAMETER = Name.identifier("\$changed")
    val STABILITY_FLAG = Name.identifier("\$stable")
    val STABILITY_PROP_FLAG = Name.identifier("\$stableprop")
    val DEFAULT_PARAMETER = Name.identifier("\$default")
    val JOINKEY = Name.identifier("joinKey")
    val STARTRESTARTGROUP = Name.identifier("startRestartGroup")
    val ENDRESTARTGROUP = Name.identifier("endRestartGroup")
    val UPDATE_SCOPE = Name.identifier("updateScope")
    val SOURCEINFORMATION = "sourceInformation"
    val SOURCEINFORMATIONMARKERSTART = "sourceInformationMarkerStart"
    val SOURCEINFORMATIONMARKEREND = "sourceInformationMarkerEnd"
}