// ENABLE_LOMBOK
// ENABLE_SERIALIZATION
// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-62194, KT-90321

import kotlinx.serialization.Serializable
import lombok.extern.java.Log

@Log
@Serializable
class <!COMPANION_OBJECT_SERIALIZABLE_CONFLICT!>Sample<!>(val name: String) {
    fun info() = <!UNRESOLVED_REFERENCE!>log<!>.info(name)
}
