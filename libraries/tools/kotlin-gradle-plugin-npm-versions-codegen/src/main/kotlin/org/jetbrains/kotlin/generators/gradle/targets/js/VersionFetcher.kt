/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.targets.js

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext


class VersionFetcher(
    private val coroutineContext: CoroutineContext
) : AutoCloseable {
    private val client = HttpClient()

    suspend fun fetch(): List<PackageInformation> {
        return npmPackages
            .map { fetchPackageInformationAsync(it) }
            .map { fetched ->
                val (packageName, value) = fetched.await()
                val fetchedPackageInformation = Gson().fromJson(value, FetchedPackageInformation::class.java)
                PackageInformation(
                    packageName,
                    fetchedPackageInformation.versions.keys
                )
            }
    }

    private suspend fun fetchPackageInformationAsync(packageName: String) =
        withContext(coroutineContext) {
            val packagePath =
                if (packageName.startsWith("@"))
                    "@" + encodeURIComponent(packageName)
                else
                    encodeURIComponent(packageName)

            async {
                packageName to client.get<String>("http://registry.npmjs.org/$packagePath")
            }
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