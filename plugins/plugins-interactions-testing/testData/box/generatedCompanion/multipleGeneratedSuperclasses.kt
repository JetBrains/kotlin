// IGNORE_PHASE_VERIFICATION: the exception leaves declarations unresolved
// EXPECT_EXCEPTION: ANY
// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.AlsoAddToCompanion

open class BaseA
open class BaseB

@AddToCompanion("generatedCompanion/BaseA")
@AlsoAddToCompanion("generatedCompanion/BaseB")
class Owner

fun box(): String {
    val base: BaseA = Owner.Companion
    return "OK"
}
