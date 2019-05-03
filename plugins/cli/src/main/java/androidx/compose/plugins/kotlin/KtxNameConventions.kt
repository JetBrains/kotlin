package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.name.Name

object KtxNameConventions {
    val COMPOSER = Name.identifier("composer")
    val EMIT = Name.identifier("emit")
    val CALL = Name.identifier("call")
    val JOINKEY = Name.identifier("joinKey")

    val EMIT_KEY_PARAMETER = Name.identifier("key")
    val EMIT_CTOR_PARAMETER = Name.identifier("ctor")
    val EMIT_UPDATER_PARAMETER = Name.identifier("update")
    val EMIT_CHILDREN_PARAMETER = Name.identifier("children")

    val CALL_KEY_PARAMETER = Name.identifier("key")
    val CALL_CTOR_PARAMETER = Name.identifier("ctor")
    val CALL_INVALID_PARAMETER = Name.identifier("invalid")
    val CALL_BLOCK_PARAMETER = Name.identifier("block")
}