// "Replace invalid positioned arguments for annotation" "true"
// WITH_RUNTIME
// ERROR: Only named arguments are available for Java annotations
// ERROR: Only named arguments are available for Java annotations

@Ann(1, /*abc*/arg1 = "abc", arg2 = arrayOf(Int::class, Array<Int>::class), arg3 = String::class) class A
