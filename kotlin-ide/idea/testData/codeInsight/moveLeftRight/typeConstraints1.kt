// MOVE: right
interface Foo<E>
interface Bar<K, V>
interface Baz<E, K, N> where B : <caret>Bar<E, K>, E: Any, F: Foo<E>