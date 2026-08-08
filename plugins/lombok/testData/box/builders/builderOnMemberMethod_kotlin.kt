// ISSUE: KT-87683

import lombok.Builder
import lombok.Singular

// `@Builder` on a regular member function: `builder()` is an instance method and `build()` invokes the
// function on the very instance it was called on. The Kotlin counterpart of builderOnMethods.kt's
// `User2.init`, for which Lombok generates an inner `VoidBuilder` holding that instance.
class User {
    var name: String = ""
    var age: Int = -1

    @Builder
    fun init(name: String, age: Int) {
        this.name = name
        this.age = age
    }
}

// Custom names, and a return type that isn't `Unit`, so `build()` hands the method's result back.
class Greeter {
    var lastGreeting: String = ""

    @Builder(builderClassName = "GreetingBuilder", builderMethodName = "greetingBuilder", buildMethodName = "greet")
    fun greet(name: String): String {
        lastGreeting = "Hello, $name"
        return lastGreeting
    }
}

// The entity is generic and the method's parameters use its type parameter. This is precisely why the
// builder has to be an inner class: `E` is only in scope through the outer instance.
class Holder<E> {
    var value: E? = null
    var tag: String = ""

    @Builder
    fun init(value: E, tag: String) {
        this.value = value
        this.tag = tag
    }
}

// Both the entity and the method are generic, so the builder carries its own `P` on top of the outer `E`.
class Mixed<E> {
    var last: String = ""

    @Builder(builderClassName = "MixedBuilder", builderMethodName = "mixedBuilder", buildMethodName = "run")
    fun <P> combine(e: E, p: P): String {
        last = "$e/$p"
        return last
    }
}

// `@Singular` on a member method's parameter, the counterpart of `@Singular` on a property.
class Sink {
    var got: List<String> = emptyList()

    @Builder
    fun accept(@Singular items: List<String>) {
        got = items
    }
}

// A static (class-level) builder next to a method builder: the former's factory is a `@JvmStatic` function
// on the generated companion, the latter's an instance method on the entity.
@Builder(builderClassName = "CtorBuilder", builderMethodName = "ctorBuilder")
class Multi(val id: Int) {
    @Builder(builderClassName = "TouchBuilder", builderMethodName = "touchBuilder")
    fun touch(note: String): String = "touched $id with $note"
}

// Two method builders on the same entity, neither of which needs a companion object.
class TwoMethods {
    var touched: String = ""

    @Builder(builderClassName = "FirstBuilder", builderMethodName = "firstBuilder")
    fun first(a: String) {
        touched = "first:$a"
    }

    @Builder(builderClassName = "SecondBuilder", builderMethodName = "secondBuilder")
    fun second(b: String) {
        touched = "second:$b"
    }
}

fun box(): String {
    // Check Builder on a regular member function
    val user = User()
    val userBuilder: User.UnitBuilder = user.builder()
    userBuilder.name("John").age(42).build()
    assertEquals("John", user.name)
    assertEquals(42, user.age)

    // Check custom names and a non-Unit return type propagated through `build()`
    val greeter = Greeter()
    val greetingBuilder: Greeter.GreetingBuilder = greeter.greetingBuilder()
    assertEquals("Hello, Ada", greetingBuilder.name("Ada").greet())
    assertEquals("Hello, Ada", greeter.lastGreeting)

    // Check a method whose parameters use the entity class's own type parameter
    val holder = Holder<Int>()
    val holderBuilder: Holder<Int>.UnitBuilder = holder.builder()
    holderBuilder.value(5).tag("t").build()
    assertEquals(5, holder.value)
    assertEquals("t", holder.tag)

    // Check a generic method inside a generic entity
    val mixed = Mixed<Int>()
    val mixedBuilder: Mixed<Int>.MixedBuilder<String> = mixed.mixedBuilder()
    assertEquals("1/p", mixedBuilder.e(1).p("p").run())
    assertEquals("1/p", mixed.last)

    // Check @Singular on a member method's parameter
    val sink = Sink()
    sink.builder().item("a").item("b").build()
    assertEquals(listOf("a", "b"), sink.got)

    // Check a static builder and a method builder side by side
    val multi: Multi = Multi.ctorBuilder().id(7).build()
    assertEquals(7, multi.id)
    assertEquals("touched 7 with x", multi.touchBuilder().note("x").build())

    // Check two method builders on the same entity
    val twoMethods = TwoMethods()
    twoMethods.firstBuilder().a("x").build()
    assertEquals("first:x", twoMethods.touched)
    twoMethods.secondBuilder().b("y").build()
    assertEquals("second:y", twoMethods.touched)

    return "OK"
}
