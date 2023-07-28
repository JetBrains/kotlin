@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cocoapods.pod_dependency.*
import cocoapods.subspec_dependency.*

fun bar() {
    println(foo())
}

fun bazz() {
    println(baz())
}