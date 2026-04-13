/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import java.nio.file.Path

data class MavenToolchainInfo(val version: String, val home: String)

fun Path.writeToolchainsXml(
    entries: List<MavenToolchainInfo>,
) {
    val sb = StringBuilder()
    sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
    sb.appendLine("<toolchains>")
    for ((version, home) in entries) {
        sb.appendLine("  <toolchain>")
        sb.appendLine("    <type>jdk</type>")
        sb.appendLine("    <provides><version>$version</version></provides>")
        sb.appendLine("    <configuration><jdkHome>$home</jdkHome></configuration>")
        sb.appendLine("  </toolchain>")
    }
    sb.appendLine("</toolchains>")
    toFile().writeText(sb.toString())
}
