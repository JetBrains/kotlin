val isEmptyStringList: List<String>.() -> Boolean = List<String>::isEmpty

val isEmptyIntList: List<Int>.() -> Boolean = <warning descr="SSR">List<Int>::isEmpty</warning>