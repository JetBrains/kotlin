@file:OptIn(ExperimentalForeignApi::class)

import kotlinx.cinterop.ExperimentalForeignApi

fun linuxMain() {
    MyExpectClass().myExpectClassProperty
    MyExpectClass().myNativeProperty
}