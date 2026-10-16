// ISSUE: KT-89188
// DUMP_KT_IR

import lombok.Data
import lombok.NonNull
import kotlin.test.assertEquals

// Without `staticConstructor`, `@Data` generates no constructor for a Kotlin class: one taking only the
// "required" properties would leave all the other primary constructor ones uninitialized.
@Data
class WithVar(var name: String)

@Data
class WithPlainParameter(x: Int, val y: Int) {
    val sum = x + y
}

@Data
class WithNonNullVar(<!ANNOTATION_IS_NOT_SUPPORTED, ANNOTATION_IS_NOT_SUPPORTED!>@NonNull<!> var name: String, var age: Int)

@Data
class OnlySecondary {
    val x: Int

    constructor(x: Int) {
        this.x = x
    }
}

fun box(): String {
    assertEquals(1, WithVar::class.java.declaredConstructors.size)
    assertEquals("name", WithVar("name").name)

    assertEquals(1, WithPlainParameter::class.java.declaredConstructors.size)
    assertEquals(3, WithPlainParameter(1, 2).sum)

    assertEquals(1, WithNonNullVar::class.java.declaredConstructors.size)
    assertEquals("WithNonNullVar(name=name, age=1)", WithNonNullVar("name", 1).toString())

    assertEquals(1, OnlySecondary::class.java.declaredConstructors.size)
    assertEquals(OnlySecondary(1), OnlySecondary(1))

    return "OK"
}
