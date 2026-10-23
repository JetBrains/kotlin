// ENABLE_LOMBOK
// ENABLE_SERIALIZATION
// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-90321

@file:OptIn(ExperimentalSerializationApi::class)

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable
import lombok.extern.java.Log

@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class MySerializable

@Log
@MySerializable
class Sample(val name: String) {
    fun info() = log.info(name)
    companion object
}
