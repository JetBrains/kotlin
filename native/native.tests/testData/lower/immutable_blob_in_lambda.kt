// OUTPUT_DATA_FILE: immutable_blob_in_lambda.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    run {
        val golden = immutableBlobOf(123)
        println(golden[0])
    }
    return "OK"
}