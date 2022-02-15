import org.gradle.internal.io.ClassLoaderObjectInputStream
import java.io.*

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

internal fun Any.serialize(): ByteArray {
    return ByteArrayOutputStream().use { byteArrayOutputStream ->
        ObjectOutputStream(byteArrayOutputStream).writeObject(this)
        byteArrayOutputStream.toByteArray()
    }
}

internal inline fun <reified T : Serializable> ByteArray.deserialize(): T {
    val inputStream = ByteArrayInputStream(this)
    val objectInputStream = ObjectInputStream(inputStream)
    return objectInputStream.use { it.readObject() } as T
}

internal fun ByteArray.deserialize(classLoader: ClassLoader): Any {
    val inputStream = ByteArrayInputStream(this)
    val objectInputStream = ClassLoaderObjectInputStream(inputStream, classLoader)
    return objectInputStream.use { it.readObject() }
}
