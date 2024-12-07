/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.example.serialization_lib

import kotlinx.serialization.*

@Serializable
enum class Access {
    Read, Write, @SerialName("rw") ReadWrite, Unknown
}

