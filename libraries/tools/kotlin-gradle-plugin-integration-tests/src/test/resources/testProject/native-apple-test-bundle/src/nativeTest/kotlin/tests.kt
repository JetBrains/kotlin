/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.UIApplication
import kotlin.test.*

@Test
fun ensureUIApplication() {
    // Should pass the test if run without UIApplication
    assertNull(UIApplication.sharedApplication)
}

@Test
fun networkRequest() {
    val response = memScoped {
        val request = NSURLRequest(NSURL(string = "https://cache-redirector.jetbrains.com/"))
        val responseRef = alloc<ObjCObjectVar<NSURLResponse?>>()
        val errorRef = alloc<ObjCObjectVar<NSError?>>()
        NSURLConnection.sendSynchronousRequest(request, responseRef.ptr, errorRef.ptr) ?:
        throw Error(errorRef.value?.toString() ?: "")
        responseRef.value!! as NSHTTPURLResponse
    }
    kotlin.test.assertEquals(200, response.statusCode)
}