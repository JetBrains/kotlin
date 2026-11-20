// ISSUE: KT-89188
// DUMP_KT_IR

import lombok.Data
import lombok.NoArgsConstructor
import kotlin.test.*

@Data(staticConstructor = "create")
class Point(val x: Int, val y: Int)

// The factory takes every primary constructor parameter, not only the `val` ones Lombok would call required.
@Data(staticConstructor = "create")
class Mixed(x: Int, var y: String, val z: Int) {
    val xCopy = x
}

@Data(staticConstructor = "create")
class Box<T>(val value: T)

@Data(staticConstructor = "create")
class Bounded<T : Comparable<T>>(val value: T)

// The factory calls the primary constructor, never the secondary one that happens to take as many parameters.
@Data(staticConstructor = "create")
class WithSecondary(val x: Int, var y: String) {
    constructor(y: String) : this(0, y)
}

// The factory is not generated when its name is taken, so the primary constructor stays public.
@Data(staticConstructor = "create")
class NameTaken(val x: Int) {
    companion object {
        fun create(x: Int): NameTaken = NameTaken(x + 100)
    }
}

// An explicit constructor annotation replaces the constructor `@Data` would generate, `staticConstructor` included.
@Data(staticConstructor = "create")
@NoArgsConstructor(force = true)
class WithNoArgs(val x: Int)

@Data(staticConstructor = "create")
data class DataClass(val a: Int)

// There is no factory for a vararg parameter, so the primary constructor stays public.
@Data(staticConstructor = "create")
class WithVararg(vararg val items: String)

fun box(): String {
    val point = Point.create(1, 2)
    assertEquals(1, point.x)
    assertEquals(2, point.y)

    val mixed = Mixed.create(1, "y", 3)
    assertEquals(1, mixed.xCopy)
    assertEquals("y", mixed.y)
    assertEquals(3, mixed.z)

    assertEquals("value", Box.create("value").value)
    assertEquals(Box.create(listOf(1)), Box.create(listOf(1)))
    assertEquals(42, Bounded.create(42).value)

    val withSecondary = WithSecondary.create(1, "y")
    assertEquals(1, withSecondary.x)
    assertEquals("y", withSecondary.y)
    assertEquals(0, WithSecondary("z").x)

    assertEquals(101, NameTaken.create(1).x)
    assertEquals(1, NameTaken(1).x)

    assertEquals(0, WithNoArgs().x)
    assertEquals(5, WithNoArgs(5).x)

    assertEquals(DataClass.create(1), DataClass.create(1))
    assertEquals("DataClass(a=1)", DataClass.create(1).toString())

    assertEquals(listOf("a", "b"), WithVararg("a", "b").items.toList())

    @Data(staticConstructor = "create")
    class Local(val x: Int)
    assertEquals(Local(1), Local(1))

    return "OK"
}
