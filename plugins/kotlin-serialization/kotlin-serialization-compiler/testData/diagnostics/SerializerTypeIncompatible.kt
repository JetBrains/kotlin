// !DIAGNOSTICS: -OPT_IN_USAGE
// WITH_STDLIB
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

<!SERIALIZER_TYPE_INCOMPATIBLE!>@Serializable(with = BazSerializer::class)<!>
class Biz(val i: Int)

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
