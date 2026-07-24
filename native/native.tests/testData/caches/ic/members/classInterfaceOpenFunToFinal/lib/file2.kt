package test

fun viaInterface(i: I): String = i.foo()
fun viaParent(parent: Parent): String = parent.foo()
fun viaChild(child: Child): String = child.foo()
