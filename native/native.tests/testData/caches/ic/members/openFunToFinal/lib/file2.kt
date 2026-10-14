package test

fun bar(foo: Foo) = foo.foo()

fun viaChild(child: Child) = child.foo()

fun viaInterfaceChild(child: InterfaceChild) = child.foo()

fun viaInterface(i: I) = i.foo()
