// LANGUAGE_VERSION: 1.2

import kotlin.reflect.KClass

class Foo
class Bar

annotation class Ann(val values: Array<KClass<*>>)

@Ann(values = <caret>arrayOf(Foo::class, Bar::class))
class Test