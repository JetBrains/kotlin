package testProject.`new-mpp-cocoapods-template`.src.iosMain.kotlin

import cocoapods.AFNetworking.*

fun foo() : Boolean {
    val manager = AFURLSessionManager()
    return manager != null
}