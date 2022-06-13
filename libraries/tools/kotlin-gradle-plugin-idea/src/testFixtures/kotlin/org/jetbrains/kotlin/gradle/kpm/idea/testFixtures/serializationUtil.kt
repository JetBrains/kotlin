package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import java.io.*

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun Any.serialize(): ByteArray {
    return ByteArrayOutputStream().use { byteArrayOutputStream ->
        ObjectOutputStream(byteArrayOutputStream).writeObject(this)
        byteArrayOutputStream.toByteArray()
    }
}

inline fun <reified T : Serializable> ByteArray.deserialize(): T {
    val inputStream = ByteArrayInputStream(this)
    val objectInputStream = ObjectInputStream(inputStream)
    return objectInputStream.use { it.readObject() } as T
}

fun ByteArray.deserialize(classLoader: ClassLoader): Any {
    val inputStream = ByteArrayInputStream(this)
    val objectInputStream = ClassLoaderObjectInputStream(inputStream, classLoader)
    return objectInputStream.use { it.readObject() }
}

fun ByteArray.deserialize(): Any {
    val inputStream = ByteArrayInputStream(this)
    val objectInputStream = ObjectInputStream(inputStream)
    return objectInputStream.use { it.readObject() }
}

class ClassLoaderObjectInputStream(stream: InputStream?, private val classLoader: ClassLoader) : ObjectInputStream(stream) {
    @Throws(IOException::class, ClassNotFoundException::class)
    override fun resolveClass(desc: ObjectStreamClass): Class<*> {
        return Class.forName(desc.name, false, classLoader)
    }
}
