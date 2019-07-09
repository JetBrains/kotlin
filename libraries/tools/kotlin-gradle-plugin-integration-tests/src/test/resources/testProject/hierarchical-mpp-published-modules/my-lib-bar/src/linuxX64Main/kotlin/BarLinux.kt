package com.example.bar

import com.example.foo.*

actual fun bar(): String {
    fooCommon()
    fooLinuxAndJs()
    // fooJvmAndJs() // unresolved
    return barLinuxAndJs()
}