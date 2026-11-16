// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.Serializable as JavaSerializable
import kotlin.jvm.Transient as JavaTransient

@Serializable
class Data(val x: Int, @Transient val y: String = "a")

@Serializable
class Data2(val x: Int, @Transient val y: String = "a") : JavaSerializable

@Serializable
class Data3(val x: Int, @Transient @JavaTransient val y: String = "a") : JavaSerializable

@Serializable
class Data4(val x: Int, <!INCORRECT_TRANSIENT!>@JavaTransient<!> val y: String)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
