// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

interface Left {
    fun value(): String = "Left"
}

interface Right {
    fun value(): String = "Right"
}

@AddToCompanion("generatedCompanion/Left", "generatedCompanion/Right")
class Owner {
    companion <!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>object<!>
}
