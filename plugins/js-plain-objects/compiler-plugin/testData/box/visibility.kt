// ISSUE: KT-89591
// DUMP_KLIB_ABI: DEFAULT
package foo

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface PublicUser {
    val name: String
}

@JsPlainObject
internal external interface InternalUser {
    val name: String
}

@JsPlainObject
private external interface PrivateUser {
    val name: String
}

internal external class InternalParent {
    @JsPlainObject
    interface NestedUser {
        val name: String
    }
}

external class PublicParent {
    @JsPlainObject
    internal interface NestedInternalUser {
        val name: String
    }
}

fun box(): String {
    val publicUser = PublicUser(name = "Public")
    if (publicUser.name != "Public") return "Fail: problem with `PublicUser.name` property"
    if (PublicUser.copy(publicUser, name = "Public2").name != "Public2") return "Fail: problem with `PublicUser.copy`"

    val internalUser = InternalUser(name = "Internal")
    if (internalUser.name != "Internal") return "Fail: problem with `InternalUser.name` property"
    if (InternalUser.copy(internalUser, name = "Internal2").name != "Internal2") return "Fail: problem with `InternalUser.copy`"

    val privateUser = PrivateUser(name = "Private")
    if (privateUser.name != "Private") return "Fail: problem with `PrivateUser.name` property"
    if (PrivateUser.copy(privateUser, name = "Private2").name != "Private2") return "Fail: problem with `PrivateUser.copy`"

    val nestedUser = InternalParent.NestedUser(name = "Nested")
    if (nestedUser.name != "Nested") return "Fail: problem with `InternalParent.NestedUser.name` property"
    if (InternalParent.NestedUser.copy(nestedUser, name = "Nested2").name != "Nested2") return "Fail: problem with `InternalParent.NestedUser.copy`"

    val nestedInternalUser = PublicParent.NestedInternalUser(name = "NestedInternal")
    if (nestedInternalUser.name != "NestedInternal") return "Fail: problem with `PublicParent.NestedInternalUser.name` property"
    if (PublicParent.NestedInternalUser.copy(nestedInternalUser, name = "NestedInternal2").name != "NestedInternal2") return "Fail: problem with `PublicParent.NestedInternalUser.copy`"

    return "OK"
}
