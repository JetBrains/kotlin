package com.example.bar

import com.example.foo.*

fun barLinuxAndJs(): String {
    fooCommon()
    // thirdPartyFun() // unresolved
    // fooJvmAndJs() // unresolved
    return fooLinuxAndJs()
}