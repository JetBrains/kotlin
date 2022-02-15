import org.gradle.tooling.internal.adapter.ProtocolToModelAdapter

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

inline fun <reified T> createProxyInstance(obj: Any): T {
    return ProtocolToModelAdapter().adapt(T::class.java, obj)
}

fun unwrapProxyInstance(obj: Any): Any {
    return ProtocolToModelAdapter().unpack(obj)
}
