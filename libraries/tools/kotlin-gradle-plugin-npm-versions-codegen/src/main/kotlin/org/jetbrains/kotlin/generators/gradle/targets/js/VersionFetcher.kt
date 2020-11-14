/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.targets.js

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


class VersionFetcher : AutoCloseable {
    private val client = HttpClient()

    suspend fun fetch(): List<PackageInformation> {
        return coroutineScope {
            npmPackages
                .filter { it.version != null }
                .map { HardcodedPackageInformation(it.name, it.version!!) } +
                    npmPackages
                        .filter { it.version == null }
                        .map { async { fetchPackageInformationAsync(it.name) } }
                        .map { fetched ->
                            val (packageName, value) = fetched.await()
                            val fetchedPackageInformation = Gson().fromJson(value, FetchedPackageInformation::class.java)
                            RealPackageInformation(
                                packageName,
                                fetchedPackageInformation.versions.keys
                            )
                        }
        }
    }

    private suspend fun fetchPackageInformationAsync(packageName: String): Pair<String, String> {
        val packagePath =
            if (packageName.startsWith("@"))
                "@" + encodeURIComponent(packageName)
            else
                encodeURIComponent(packageName)

        return (packageName to client.get<String>("http://registry.npmjs.org/$packagePath"))
    }

    override fun close() {
        client.close()
    }
}

private data class FetchedPackageInformation(
    val versions: Map<String, Any>
)

fun encodeURIComponent(s: String): String {
    return try {
        URLEncoder.encode(s, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
            .replace("%21", "!")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%7E", "~")
    } catch (e: UnsupportedEncodingException) {
        s
    }
}