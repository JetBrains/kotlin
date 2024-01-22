/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package library

fun readDataFromLibraryClass(input: A): String {
    return input.data
}

fun readDataFromLibraryInterface(input: I): String {
    return input.data
}

fun readDataFromLibraryEnum(input: E): String {
    return input.data
}