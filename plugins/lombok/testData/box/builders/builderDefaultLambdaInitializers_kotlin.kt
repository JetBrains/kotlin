// ISSUE: KT-90590
// WITH_STDLIB
import lombok.Builder
import kotlin.test.assertEquals

var counter = 0

fun topLevel(): Int = ++counter

@Builder
class BodyProperties {
    // Not called in place and not touching the instance: created by `build()`, invoked later
    @Builder.Default
    var lambda: () -> Int = { topLevel() + COMPANION_CONST }

    // Called in place, not touching the instance
    @Builder.Default
    var inPlace: Int = COMPANION_CONST.let { it * 10 }

    @Builder.Default
    var mapped: List<Int> = listOf(1, 2).map { it * COMPANION_CONST }

    // A lambda using its own parameter and a nested lambda
    @Builder.Default
    var withParameter: (Int) -> Int = { x -> listOf(x).map { it + 1 }.single() }

    @Builder.Default
    var nested: () -> () -> Int = { { COMPANION_CONST } }

    // Anonymous function and callable reference to a top-level function
    @Builder.Default
    var anonymousFunction: (Int) -> Int = fun(x: Int): Int = x * 2

    @Builder.Default
    var reference: () -> Int = ::topLevel

    companion object {
        const val COMPANION_CONST = 3
    }
}

@Builder
class ConstructorParameters(
    @Builder.Default val a: Int = 1,
    // Captures an earlier parameter: must see its resolved value
    @Builder.Default val f: () -> Int = { a + 1 },
    @Builder.Default val g: Int = run { a * 10 },
    @Builder.Default val h: () -> () -> Int = { { a + 2 } },
)

fun box(): String {
    val defaults = BodyProperties.builder().build()
    // `lambda` is only created by `build()`, not called
    assertEquals(0, counter)
    assertEquals(30, defaults.inPlace)
    assertEquals(4, defaults.lambda())
    assertEquals(1, counter)
    assertEquals(listOf(3, 6), defaults.mapped)
    assertEquals(6, defaults.withParameter(5))
    assertEquals(3, defaults.nested()())
    assertEquals(8, defaults.anonymousFunction(4))
    assertEquals(2, defaults.reference())

    val custom = BodyProperties.builder().lambda { -1 }.inPlace(-2).build()
    assertEquals(-1, custom.lambda())
    assertEquals(-2, custom.inPlace)

    val ctorDefaults = ConstructorParameters.builder().build()
    assertEquals(2, ctorDefaults.f())
    assertEquals(10, ctorDefaults.g)
    assertEquals(3, ctorDefaults.h()())

    val ctorCustom = ConstructorParameters.builder().a(5).build()
    assertEquals(6, ctorCustom.f())
    assertEquals(50, ctorCustom.g)
    assertEquals(7, ctorCustom.h()())

    return "OK"
}
