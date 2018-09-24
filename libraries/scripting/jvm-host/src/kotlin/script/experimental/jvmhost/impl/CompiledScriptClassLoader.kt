/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import org.jetbrains.kotlin.codegen.BytesUrlUtils
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.util.*

internal class CompiledScriptClassLoader(parent: ClassLoader, private val entries: Map<String, ByteArray>) : ClassLoader(parent) {

    override fun findClass(name: String): Class<*>? {
        val classPathName = name.replace('.', '/') + ".class"
        val classBytes = entries[classPathName] ?: return null
        return defineClass(name, classBytes, 0, classBytes.size)
    }

    override fun getResourceAsStream(name: String): InputStream? =
        entries[name]?.let(::ByteArrayInputStream) ?: super.getResourceAsStream(name)

    override fun findResources(name: String?): Enumeration<URL>? {
        val fromParent = super.findResources(name)

        val url = entries[name]?.let { BytesUrlUtils.createBytesUrl(it) } ?: return fromParent

        return Collections.enumeration(listOf(url) + fromParent.asSequence())
    }

    override fun findResource(name: String?): URL? =
        entries[name]?.let { BytesUrlUtils.createBytesUrl(it) } ?: super.findResource(name)
}