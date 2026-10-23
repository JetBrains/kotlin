// ENABLE_LOMBOK
// ENABLE_SERIALIZATION
// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-62194, KT-90321

@file:OptIn(ExperimentalSerializationApi::class)

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable
import lombok.extern.java.Log

@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class MySerializable

@Log
@MySerializable
class <!COMPANION_OBJECT_SERIALIZABLE_CONFLICT!>Sample<!>(val name: String) {
    fun info() = <!UNRESOLVED_REFERENCE!>log<!>.info(name)
}
