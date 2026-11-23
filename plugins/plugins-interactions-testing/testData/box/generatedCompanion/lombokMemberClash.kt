// TARGET_BACKEND: JVM
// ENABLE_LOMBOK
// WITH_STDLIB
// FULL_JDK

package generatedCompanion

import lombok.extern.java.Log
import lombok.AccessLevel
import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import java.util.logging.Logger
import kotlin.test.assertEquals

interface LogApi {
    val log: Logger
        get() = Logger.getLogger("api")
}

// Lombok generates private `log` property in companion, while `LogApi` has public `log` property.
@AddToCompanion("generatedCompanion/LogApi")
@Log
class Owner {
    fun ownLogger(): Logger = log
}

// Lombok generates public `log` property in companion, which accidentally overrides `LogApi.log`
@AddToCompanion("generatedCompanion/LogApi")
@Log(access = AccessLevel.PUBLIC)
class Owner2

fun box(): String {
    // The two `log` declarations are in different classes, so each one must keep its own logger.
    val api: LogApi = Owner.Companion
    assertEquals("generatedCompanion.Owner", Owner().ownLogger().name)
    assertEquals("api", api.log.name)
    val api2: LogApi = Owner2.Companion
    assertEquals("generatedCompanion.Owner2", Owner2.log.name)
    assertEquals("generatedCompanion.Owner2", api2.log.name)
    return "OK"
}
