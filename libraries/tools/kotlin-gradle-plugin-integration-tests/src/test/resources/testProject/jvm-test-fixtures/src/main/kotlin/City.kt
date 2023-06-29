/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.example

class City(val name: String, val country: String) {
    internal fun isNetherlands(): Boolean = country == "The Netherlands"
}