/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import org.intellij.lang.annotations.Language
import java.nio.file.Path
import kotlin.io.path.exists

fun Path.checkOrWriteKotlinMavenTestSettingsXml(
    kotlinBuildRepo: Path
) {
    if (!exists()) { writeKotlinMavenTestSettingsXml(kotlinBuildRepo); return }
    require(toFile().readText().contains("cache-redirector.jetbrains.com")) {
        "$this does not contain cache-redirector.jetbrains.com"
    }
}

fun Path.writeKotlinMavenTestSettingsXml(
    kotlinBuildRepo: Path
) {
    require(!exists()) { "File '$this' already exists" }
    val kotlinBuildRepoUrl = kotlinBuildRepo.toCanonicalLocalFileUrlString()

    @Language("XML")
    val xml = $$"""
        <?xml version="1.0" encoding="UTF-8"?>
        <settings xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd"
                  xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <profiles>
                <profile>
                    <id>cache-redirector</id>
                    <pluginRepositories>
                        <pluginRepository>
                            <id>MavenCentral-JBCached</id>
                            <url>https://cache-redirector.jetbrains.com/maven-central</url>
                        </pluginRepository>
                    </pluginRepositories>
                    <repositories>
                        <repository>
                            <id>MavenCentral-JBCached</id>
                            <url>https://cache-redirector.jetbrains.com/maven-central</url>
                        </repository>
                    </repositories>
                </profile>
                <profile>
                    <id>kotlin-local-repo</id>
                    <pluginRepositories>
                        <pluginRepository>
                            <id>Kotlin Build Maven Repo</id>
                            <url>$$kotlinBuildRepoUrl</url>
                        </pluginRepository>
                    </pluginRepositories>
                    <repositories>
                        <repository>
                            <id>Kotlin Build Maven Repo</id>
                            <url>$$kotlinBuildRepoUrl</url>
                        </repository>
                    </repositories>
                </profile>
            </profiles>
            <mirrors>
                <mirror>
                    <id>MavenCentral-JBCached</id>
                    <url>https://cache-redirector.jetbrains.com/maven-central</url>
                    <mirrorOf>central</mirrorOf>
                </mirror>
            </mirrors>
            <activeProfiles>
                <activeProfile>cache-redirector</activeProfile>
                <activeProfile>kotlin-local-repo</activeProfile>
            </activeProfiles>
        </settings>
    """.trimIndent()

    toFile().writeText(xml)
}