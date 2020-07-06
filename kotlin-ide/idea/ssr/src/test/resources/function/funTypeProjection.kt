interface A<T>{}

fun fooGen(par: A<Any>) { print(par) }

<warning descr="SSR">fun fooOut(par: A<out Any>) { print(par) }</warning>

fun fooIn(par: A<in String>) { print(par) }

fun fooStar(par: A<*>) { print(par) }