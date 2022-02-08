/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.analyzer

expect fun readFile(fileName: String): String
expect fun Double.format(decimalNumber: Int = 4): String
expect fun writeToFile(fileName: String, text: String)
expect fun assert(value: Boolean, lazyMessage: () -> Any)
expect fun sendGetRequest(url: String, user: String? = null, password: String? = null,
                          followLocation: Boolean = false) : String