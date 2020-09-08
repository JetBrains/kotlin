// WITH_RUNTIME
// SKIP_TXT

import kotlinx.serialization.*

class Bar
@Serializer(forClass = Bar::class)
object BarSerializer: KSerializer<Bar>

class Baz
@Serializer(forClass = Baz::class)
object BazSerializer: KSerializer<Baz>
@Serializer(forClass = Baz::class)
object NullableBazSerializer: KSerializer<Baz?>

@Serializable
class Foo(@Serializable(with = BazSerializer::class) val i: <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar<!>)

@Serializable
class Foo2(val li: List<@Serializable(with = BazSerializer::class) <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar<!>>)

@Serializable
class Foo3(@Serializable(with = BazSerializer::class) val i: Baz)

@Serializable
class Foo4(val li: List<@Serializable(with = BazSerializer::class) Baz>)

@Serializable
class Foo5(@Serializable(with = BazSerializer::class) val i: <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar?<!>)

@Serializable
class Foo6(@Serializable(with = NullableBazSerializer::class) val i: <!SERIALIZER_NULLABILITY_INCOMPATIBLE, SERIALIZER_TYPE_INCOMPATIBLE!>Bar<!>)

@Serializable
class Foo7(@Serializable(with = NullableBazSerializer::class) val i: <!SERIALIZER_TYPE_INCOMPATIBLE!>Bar?<!>)