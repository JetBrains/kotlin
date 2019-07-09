package com.example.app

import com.example.bar.barCommon
import com.example.bar.barJvmAndJs
import com.example.foo.fooCommon
import com.example.foo.fooJvmAndJs
import com.example.thirdparty.thirdPartyFun

fun testAppJvmAndJs() {
    fooCommon()
    fooJvmAndJs()
    // fooLinuxAndJs() //unresolved

    barCommon()
    barJvmAndJs()
    // barLinuxAndJs() // unresolved

    thirdPartyFun()

    appJvmAndJs()
}