package com.example.app

import com.example.foo.*
import com.example.bar.*
import com.example.thirdparty.thirdPartyFun

fun appJvmAndJs() {
    fooCommon()
    fooJvmAndJs()
    // fooLinuxAndJs() //unresolved

    barCommon()
    barJvmAndJs()
    // barLinuxAndJs() // unresolved

    thirdPartyFun()
}