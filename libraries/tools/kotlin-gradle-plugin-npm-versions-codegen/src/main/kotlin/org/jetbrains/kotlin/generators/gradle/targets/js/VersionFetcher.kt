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
) {
    private val client = HttpClient()

    suspend fun fetch(): List<PackageInformation> {
        return withContext(coroutineContext) {
            npmPackages
                .map { packageName ->
                    val packagePath =
                        if (packageName.startsWith("@"))
                            "@" + encodeURIComponent(packageName)
                        else
                            encodeURIComponent(packageName)

                    val fetch = async<String> {
                        client.get("http://registry.npmjs.org/$packagePath")
                    }

                    val fetchedPackageInformation = Gson().fromJson(fetch.await(), FetchedPackageInformation::class.java)
                    PackageInformation(
                        packageName,
                        fetchedPackageInformation.versions.keys
                    )
                }.also {
                    client.close()
                }
        }
    }
}

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

data class PackageInformation(
    val name: String,
    val versions: Set<String>
)

data class Package(
    val name: String,
    val version: SemVer
)

data class FetchedPackageInformation(
    val versions: Map<String, Any>
)