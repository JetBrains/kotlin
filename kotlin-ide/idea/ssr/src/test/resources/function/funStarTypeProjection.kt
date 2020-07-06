interface A<T>{}

fun fooGen(par: A<Any>) { print(par) }

fun fooOut(par: A<out Any>) { print(par) }

fun fooIn(par: A<in String>) { print(par) }

<warning descr="SSR">fun fooStar(par: A<*>) { print(par) }</warning>