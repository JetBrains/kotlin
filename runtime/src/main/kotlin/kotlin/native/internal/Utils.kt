/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.reflect.KClass

@ExportForCppRuntime
internal fun DescribeObjectForDebugging(typeInfo: NativePtr, address: NativePtr): String {
    val kClass = kotlin.native.internal.KClassImpl<Any>(typeInfo)
    return debugDescription(kClass, address.toLong().toInt())
}

internal fun debugDescription(kClass: KClass<*>, identity: Int): String {
    val className = kClass.qualifiedName ?: kClass.simpleName ?: "<object>"
    val unsignedIdentity = identity.toLong() and 0xffffffffL
    val identityStr = unsignedIdentity.toString(16)
    return "$className@$identityStr"
}
