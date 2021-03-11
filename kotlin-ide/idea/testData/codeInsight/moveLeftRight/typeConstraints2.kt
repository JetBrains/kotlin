// MOVE: left
interface Foo<E>
interface Bar<K, V>
interface Baz<E, K, N> where B : Bar<E, K>, <caret>E: Any, F: Foo<E>