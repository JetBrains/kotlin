// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE,-PROVIDED_RUNTIME_TOO_LOW
// WITH_RUNTIME
// SKIP_TXT

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
class Data(val x: Int, <!INCORRECT_TRANSIENT!>@Transient<!> val y: String)

@Serializable
class Data2(val x: Int, @Transient val y: String) : JavaSerializable