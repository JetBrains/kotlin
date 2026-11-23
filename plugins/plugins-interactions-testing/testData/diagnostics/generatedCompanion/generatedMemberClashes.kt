// ENABLE_SERIALIZATION
// ENABLE_LOMBOK
// WITH_STDLIB
// FULL_JDK

package generatedCompanion

import kotlinx.serialization.Serializable
import lombok.extern.java.Log
import lombok.AccessLevel
import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo
import java.util.logging.Logger

interface FooApi {
    fun foo(): String = ""
}

@AddToCompanion("generatedCompanion/FooApi")
@CompanionWithFoo
class FooOwner

fun useSandboxMember() = FooOwner.Companion.foo()

interface SerializerApi {
    fun serializer(): String = ""
}

@AddToCompanion("generatedCompanion/SerializerApi")
@Serializable
class SerializableOwner

fun useSerializerMember() = SerializableOwner.Companion.serializer()

interface LogApi {
    val log: Logger
        get() = Logger.getLogger("test")
}

// Even though LogApi has public `log` property, `LogOwner.Companion` has a private version
// which gets prioritized
@AddToCompanion("generatedCompanion/LogApi")
@Log
class LogOwner

fun useLogMember() = LogOwner.Companion.<!INVISIBLE_REFERENCE!>log<!>

@AddToCompanion("generatedCompanion/LogApi")
@Log(access = AccessLevel.PUBLIC)
class LogOwner2

fun useLogMember2() = LogOwner2.Companion.log
