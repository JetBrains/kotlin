package inlineFun1

import inlineFun2.*

inline fun myFun1(f: () -> Int): Int = myFun2 { f() }