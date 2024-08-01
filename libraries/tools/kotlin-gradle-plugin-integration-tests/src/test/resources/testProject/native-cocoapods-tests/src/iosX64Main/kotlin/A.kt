@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package testProject.`new-mpp-cocoapods-template`.src.iosX64Main.kotlin

import cocoapods.Base64.*

fun foo() : Boolean {
    val data = MF_Base64Codec.base64StringFromData(MF_Base64Codec.dataFromBase64String("R3JlZXRpbmdzCg=="))
    return data != null
}