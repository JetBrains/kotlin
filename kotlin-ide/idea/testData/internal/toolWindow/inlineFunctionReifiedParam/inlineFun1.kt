package inlineFun1

inline fun <reified T> myFun1(f: () -> T): T = f()