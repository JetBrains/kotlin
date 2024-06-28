@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package testProject.`new-mpp-cocoapods-template`.src.iosX64Main.kotlin

import cocoapods.AFNetworking.*

fun foo() : Boolean {
    val manager = AFURLSessionManager()
    return manager != null
}