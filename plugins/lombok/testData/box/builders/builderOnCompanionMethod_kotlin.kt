// ISSUE: KT-87683

import lombok.Builder
import lombok.Singular

// A function declared directly inside a companion object is the Kotlin analogue of a Java static
// factory method (mirrors builderOnMethods.kt's `User.create`).
class User private constructor(val name: String, val age: Int) {
    companion object {
        @Builder
        fun create(name: String, age: Int): User = User(name, age)
    }
}

// A companion function with its own type parameter, returning that type parameter directly rather
// than a parameterized class type (mirrors builderOnGenericMethod.kt's `Method.method`).
class Method {
    companion object {
        @Builder(builderClassName = "MethodBuilder")
        fun <M> method(m: M): M = m
    }
}

// The same shape as `Method` above, but without an explicit `builderClassName`, so the name has to be
// inferred from the return type. That type is a bare type parameter rather than a class, so the builder
// class is named after the type parameter itself — `MBuilder`.
class InferredTypeParameterName {
    companion object {
        @Builder
        fun <M> method(m: M): M = m
    }
}

// A `Unit`-returning companion function: `UnitBuilder` is inferred from the return type, the analogue of
// the `VoidBuilder` Lombok infers for a Java `void` method (see builderOnMethods.kt's `User2.init`).
class ExplicitUnitReturnType {
    companion object {
        var lastId: Int = -1

        @Builder
        fun create(id: Int): Unit {
            lastId = id
        }
    }
}

// The same, but with the `Unit` return type left implicit: a block body without an explicit return type is
// still `Unit`, so `UnitBuilder` is inferred just as above.
class ImplicitUnitReturnType {
    companion object {
        var lastId: Int = -1

        @Builder
        fun create(id: Int) {
            lastId = id
        }
    }
}

// A companion function returning a generic entity class's parameterized type; the function's own
// type parameter is unrelated to (though same-named as) the entity class's (mirrors
// builderOnMethodWithGenericClass.kt's `BuilderOnMethod.create`).
class BuilderOnMethod<T> private constructor(val name: T) {
    companion object {
        @Builder
        fun <T> create(name: T): BuilderOnMethod<T> = BuilderOnMethod(name)
    }
}

class Klass<A, B>(val a: A, val b: B)

// A companion factory whose return type lists its type parameters in the opposite order to their
// declaration, so `build()`'s type arguments for the `create` call cannot be read off the return type
// positionally — they come from the builder class, whose type parameters mirror the function's own.
class SwappedTypeParameters {
    companion object {
        @Builder(builderClassName = "SwappedBuilder")
        fun <T, M> create(name: T, m: M): Klass<M, T> = Klass(m, name)
    }
}

// The extreme of the same: a companion factory whose return type mentions none of its type parameters,
// so the return type carries no type arguments to recover at all.
class UnrelatedReturnType {
    companion object {
        @Builder(builderClassName = "UnrelatedBuilder")
        fun <T, M> create(name: T, m: M): String = "$name-$m"
    }
}

// `@Singular` on a companion function's parameter, the counterpart of `@Singular` on a property of a
// class-level `@Builder`.
class Team private constructor(val members: List<String>) {
    companion object {
        @Builder
        fun create(@Singular members: List<String>): Team = Team(members)
    }
}

// An entity annotated on both itself and its companion factory function: the class-level and the
// companion-level `@Builder`s are collected into a single view, so both builders are generated side by
// side. The names have to be spelled out, since otherwise both would claim `builder()`/`OwnAndCompanionBuilder`
// and Lombok silently skips whichever comes second. `create` doubles the id, so each `build()` shows which
// of the two underlying callables it really invoked.
@Builder(builderClassName = "ClassBuilder", builderMethodName = "classBuilder")
class OwnAndCompanion(val id: Int) {
    companion object {
        @Builder(builderClassName = "FactoryBuilder", builderMethodName = "factoryBuilder")
        fun create(id: Int): OwnAndCompanion = OwnAndCompanion(id * 2)
    }
}

fun box(): String {
    // Check Builder on a companion object factory function
    val userBuilder: User.UserBuilder = User.builder()
    val user: User = userBuilder.name("John").age(42).build()
    assertEquals("John", user.name)
    assertEquals(42, user.age)

    // Check Builder on a generic companion function returning its own type parameter
    val methodBuilder: Method.MethodBuilder<Any> = Method.builder()
    val obj: Any = methodBuilder.m("s").build()
    assertEquals("s", obj)

    // Check the builder class name inferred from a bare type parameter
    val inferredBuilder: InferredTypeParameterName.MBuilder<Any> = InferredTypeParameterName.builder()
    assertEquals("s", inferredBuilder.m("s").build())

    // Check a `Unit`-returning companion function, with the return type spelled out and left implicit
    val unitBuilder: ExplicitUnitReturnType.UnitBuilder = ExplicitUnitReturnType.builder()
    unitBuilder.id(7).build()
    assertEquals(7, ExplicitUnitReturnType.lastId)

    val implicitUnitBuilder: ImplicitUnitReturnType.UnitBuilder = ImplicitUnitReturnType.builder()
    implicitUnitBuilder.id(9).build()
    assertEquals(9, ImplicitUnitReturnType.lastId)

    // Check Builder on a companion function returning a generic entity class's parameterized type
    val genericBuilder = BuilderOnMethod.builder<String>()
    val result = genericBuilder.name("name").build()
    assertEquals("name", result.name)

    // Check a companion factory whose return type reorders its type parameters
    val swappedBuilder: SwappedTypeParameters.SwappedBuilder<String, Int> = SwappedTypeParameters.builder()
    val swapped: Klass<Int, String> = swappedBuilder.name("n").m(1).build()
    assertEquals(1, swapped.a)
    assertEquals("n", swapped.b)

    // Check a companion factory whose return type doesn't mention its type parameters at all
    val unrelatedBuilder: UnrelatedReturnType.UnrelatedBuilder<String, Int> = UnrelatedReturnType.builder()
    assertEquals("n-1", unrelatedBuilder.name("n").m(1).build())

    // Check @Singular on a companion function's parameter
    val team = Team.builder().member("Alex").member("Yulia").build()
    assertEquals(listOf("Alex", "Yulia"), team.members)

    // Check an entity carrying both its own `@Builder` and one on a companion factory function
    val classBuilder: OwnAndCompanion.ClassBuilder = OwnAndCompanion.classBuilder()
    assertEquals(3, classBuilder.id(3).build().id)

    val factoryBuilder: OwnAndCompanion.FactoryBuilder = OwnAndCompanion.factoryBuilder()
    assertEquals(8, factoryBuilder.id(4).build().id)

    return "OK"
}
