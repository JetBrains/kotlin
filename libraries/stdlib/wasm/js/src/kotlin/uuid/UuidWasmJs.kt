/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

@Suppress("ClassName")
private external object crypto {
    fun randomUUID(): String
}

@ExperimentalUuidApi
internal actual fun secureRandomUuid(): Uuid {
    val uuidString = crypto.randomUUID()
    return Uuid.parse(uuidString)
}
