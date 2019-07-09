package com.example.bar

import com.example.foo.*

expect fun bar(): String

fun barCommon(): String {
    foo()
    // thirdPartyFun() //unresolved
    return fooCommon()
}