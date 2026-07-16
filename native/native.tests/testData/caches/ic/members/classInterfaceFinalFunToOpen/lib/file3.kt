package test

fun viaParent(parent: Parent): String = parent.foo()
fun viaChild(child: Child): String = child.foo()
