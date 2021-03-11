val x: Int <caret>by Foo()

class Foo

fun Foo.getValue(_this: Any?, p: Any?): Int = 1

// REF: (for Foo in <root>).getValue(Any?, Any?)

