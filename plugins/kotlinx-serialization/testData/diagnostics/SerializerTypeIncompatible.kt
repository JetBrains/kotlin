// WITH_STDLIB
// SKIP_TXT

// FILE: annotation.kt
package kotlinx.serialization

import kotlin.annotation.*

/*
  Until the annotation is added to the serialization runtime,
  we have to create an annotation with that name in the project itself
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KeepGeneratedSerializer

// FILE: main.kt
import kotlinx.serialization.*

class Bar
@Serializer(forClass = Bar::class)
object BarSerializer: KSerializer<Bar>

class Baz
@Serializer(forClass = Baz::class)
object BazSerializer: KSerializer<Baz>

@Serializer(forClass = FooKept::class)
object FooKeptSerializer

@Serializer(forClass = Foo2Kept::class)
object Foo2KeptSerializer

@Serializer(forClass = Foo25Kept::class)
object Foo25KeptSerializer

@Serializer(forClass = Foo5Kept::class)
object Foo5KeptSerializer

@Serializer(forClass = Foo6Kept::class)
object Foo6KeptSerializer

@Serializer(forClass = Foo7Kept::class)
object Foo7KeptSerializer

@Serializer(forClass = Foo8Kept::class)
class Foo8KeptSerializer(val serializer: KSerializer<*>)

@Serializer(forClass = Foo9Kept::class)
class Foo9KeptSerializer(val serializer: KSerializer<*>)

@Serializer(forClass = Baz::class)
object NullableBazSerializer: KSerializer<Baz?>

<!SERIALIZER_TYPE_INCOMPATIBLE!>@Serializable(with = BazSerializer::class)<!>
class Biz(val i: Int)

@Serializable
class Foo(@Serializable(with = BazSerializer::class) val i: <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar<!>)

@Serializable(FooKeptSerializer::class)
@KeepGeneratedSerializer
class FooKept(@Serializable(with = BazSerializer::class) val i: <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar<!>)

@Serializable
class Foo2(val li: List<@Serializable(with = BazSerializer::class) <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar<!>>)

@Serializable(Foo2KeptSerializer::class)
@KeepGeneratedSerializer
class Foo2Kept(val li: List<@Serializable(with = BazSerializer::class) <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar<!>>)

@Serializable
class Foo25(val i: @Serializable(with = BazSerializer::class) <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar<!>)

@Serializable(Foo25KeptSerializer::class)
@KeepGeneratedSerializer
class Foo25Kept(val i: @Serializable(with = BazSerializer::class) <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar<!>)

@Serializable
class Foo3(@Serializable(with = BazSerializer::class) val i: Baz)

@Serializable
class Foo4(val li: List<@Serializable(with = BazSerializer::class) Baz>)

@Serializable
class Foo5(@Serializable(with = BazSerializer::class) val i: <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar?<!>)

@Serializable(Foo5KeptSerializer::class)
@KeepGeneratedSerializer
class Foo5Kept(@Serializable(with = BazSerializer::class) val i: <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar?<!>)

@Serializable
class Foo6(@Serializable(with = NullableBazSerializer::class) val i: <!SERIALIZER_NULLABILITY_INCOMPATIBLE, SERIALIZER_TYPE_INCOMPATIBLE!>Bar<!>)

@Serializable(Foo6KeptSerializer::class)
@KeepGeneratedSerializer
class Foo6Kept(@Serializable(with = NullableBazSerializer::class) val i: <!SERIALIZER_NULLABILITY_INCOMPATIBLE, SERIALIZER_TYPE_INCOMPATIBLE!>Bar<!>)

@Serializable
class Foo7(@Serializable(with = NullableBazSerializer::class) val i: <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar?<!>)

@Serializable(Foo7KeptSerializer::class)
@KeepGeneratedSerializer
class Foo7Kept(@Serializable(with = NullableBazSerializer::class) val i: <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar?<!>)

// It is OK to report, because subclasses can't generally be deserialized (as `deserialize()` signature returns base class)
@Serializable
@Suppress("FINAL_UPPER_BOUND")
class Foo8<Br: Bar>(@Serializable(BarSerializer::class) val b: <!SERIALIZER_TYPE_INCOMPATIBLE("Br; BarSerializer; Bar")!>Br<!>)

@Serializable(Foo8KeptSerializer::class)
@KeepGeneratedSerializer
@Suppress("FINAL_UPPER_BOUND")
class Foo8Kept<Br: Bar>(@Serializable(BarSerializer::class) val b: <!SERIALIZER_TYPE_INCOMPATIBLE("Br; BarSerializer; Bar")!>Br<!>)

@Serializable
class Foo9<T>(@Serializable(BarSerializer::class) val b: <!SERIALIZER_TYPE_INCOMPATIBLE("T; BarSerializer; Bar")!>T<!>)

@Serializable(Foo9KeptSerializer::class)
@KeepGeneratedSerializer
class Foo9Kept<T>(@Serializable(BarSerializer::class) val b: <!SERIALIZER_TYPE_INCOMPATIBLE("T; BarSerializer; Bar")!>T<!>)
