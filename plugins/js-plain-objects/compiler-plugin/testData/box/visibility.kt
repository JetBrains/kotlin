// ISSUE: KT-89591
// DUMP_KLIB_ABI: DEFAULT

// MODULE: lib
// FILE: lib.kt
package lib

import kotlinx.js.JsPlainObject

@PublishedApi
@JsPlainObject
internal external interface PublishedApiLibUser {
    val name: String
}

inline fun createPublishedApiLibUser(name: String): String = PublishedApiLibUser(name = name).name

inline fun copyPublishedApiLibUser(name: String, newName: String): String =
    PublishedApiLibUser.copy(PublishedApiLibUser(name = name), name = newName).name

// MODULE: main(lib)
// FILE: other.kt
package foo

import kotlinx.js.JsPlainObject

@JsPlainObject
internal external interface OtherFileInternalUser {
    val name: String
}

// FILE: main.kt
package foo

import kotlinx.js.JsPlainObject
import lib.createPublishedApiLibUser
import lib.copyPublishedApiLibUser

@JsPlainObject
external interface PublicUser {
    val name: String
}

@JsPlainObject
internal external interface InternalUser {
    val name: String
}

@PublishedApi
@JsPlainObject
internal external interface PublishedApiUser {
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

    val publishedApiUser = PublishedApiUser(name = "PublishedApi")
    if (publishedApiUser.name != "PublishedApi") return "Fail: problem with `PublishedApiUser.name` property"
    if (PublishedApiUser.copy(publishedApiUser, name = "PublishedApi2").name != "PublishedApi2") return "Fail: problem with `PublishedApiUser.copy`"

    if (createPublishedApiLibUser("PublishedApiLib") != "PublishedApiLib") return "Fail: problem with `PublishedApiLibUser.name` property"
    if (copyPublishedApiLibUser("PublishedApiLib", "PublishedApiLib2") != "PublishedApiLib2") return "Fail: problem with `PublishedApiLibUser.copy`"

    val otherFileInternalUser = OtherFileInternalUser(name = "OtherFileInternal")
    if (otherFileInternalUser.name != "OtherFileInternal") return "Fail: problem with `OtherFileInternalUser.name` property"
    if (OtherFileInternalUser.copy(otherFileInternalUser, name = "OtherFileInternal2").name != "OtherFileInternal2") return "Fail: problem with `OtherFileInternalUser.copy`"

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
