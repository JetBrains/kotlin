/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.*

fun Any.loadPropertyFromResources(propFileName: String, property: String): String {
    val props = Properties()
    val inputStream = javaClass.classLoader!!.getResourceAsStream(propFileName)
        ?: throw FileNotFoundException("property file '$propFileName' not found in the classpath")

    inputStream.use { props.load(it) }
    return props[property] as String
}

@Throws(IOException::class)
fun Project.probeRemoteFileLength(url: String, probingTimeoutMs: Int = 0): Long? {
    val connection = URL(url).openConnection()
    if (connection !is HttpURLConnection) {
        logger.kotlinDebug(::probeRemoteFileLength.name + "($url, $probingTimeoutMs): Failed to obtain content-length. Likely not an HTTP-based URL. URL connection class is ${connection::class.java}.")
        return null
    }

    return try {
        connection.requestMethod = "HEAD"
        connection.connectTimeout = probingTimeoutMs
        connection.readTimeout = probingTimeoutMs
        connection.contentLengthLong.takeIf { it >= 0 }
    } catch (e: SocketTimeoutException) {
        if (probingTimeoutMs == 0)
            throw e
        else {
            logger.kotlinDebug(::probeRemoteFileLength.name + "($url, $probingTimeoutMs): Failed to obtain content-length during the probing timeout.")
            @Suppress("UNCHECKED_CAST")
            null
        }
    } finally {
        connection.disconnect()
    }
}
