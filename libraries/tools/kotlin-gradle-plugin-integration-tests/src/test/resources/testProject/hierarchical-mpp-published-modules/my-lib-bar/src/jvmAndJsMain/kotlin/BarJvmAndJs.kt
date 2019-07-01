package com.example.bar

import com.example.foo.*
import com.example.thirdparty.thirdPartyFun

actual fun bar(): String = barJvmAndJs()

fun barJvmAndJs(): String {
    thirdPartyFun()
    fooCommon()
    // fooLinuxAndJs() //unresolved
    return fooJvmAndJs()
}