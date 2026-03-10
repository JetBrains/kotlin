/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.deserializeSwiftPMImportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesMetadataTaskAndConsumableConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.serializeSwiftPMImportMetadata
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class SwiftPMImportMetadataSerializationTests {

    @Test
    fun `smoke test swiftPM metadata serialization`() {
        buildProjectWithMPP {
            val extension = locateOrRegisterSwiftPMDependenciesExtension().apply {
                iosMinimumDeploymentTarget.set("-1.0")
                swiftPackage(
                    repository = SwiftPMDependency.Remote.Repository.Id("foo.bar/package1"),
                    version = exact("1.2.3"),
                    products = listOf(product("explicitProduct", platforms = setOf(SwiftPMDependency.Platform.iOS))),
                    packageName = "packageName",
                )
                swiftPackage(
                    url = "https://foo.bar/package2.git",
                    version = "1.2.3",
                    products = listOf("product"),
                )
                localSwiftPackage(
                    directory = project.layout.projectDirectory.dir("package"),
                    products = listOf("localProduct"),
                )
            }

            val initialMetadata = locateOrRegisterSwiftPMDependenciesMetadataTaskAndConsumableConfiguration(extension)
                .get().swiftPMImportMetadata()
            val serializedMetadata = ByteArrayOutputStream()
            initialMetadata.serializeSwiftPMImportMetadata(serializedMetadata)
            val deserializedMetadata = deserializeSwiftPMImportMetadata(ByteArrayInputStream(serializedMetadata.toByteArray()))

            assertEquals(
                deserializedMetadata.prettyPrinted,
                initialMetadata.prettyPrinted,
                message = "Reserializing SwiftPM metadata should produce identical metadata type"
            )

            assertEquals(
                SwiftPMImportMetadata(
                    "-1.0",
                    null,
                    null,
                    null,
                    true,
                    dependencies = setOf(
                        SwiftPMDependency.Local(
                            absolutePath = layout.projectDirectory.dir("package").asFile,
                            cinteropClangModules = listOf(
                                SwiftPMDependency.CinteropClangModule(
                                    name = "localProduct",
                                    platformConstraints = null,
                                ),
                            ),
                            packageName = "package",
                            products = listOf(
                                SwiftPMDependency.Product(
                                    cinteropClangModules = setOf(
                                    ),
                                    name = "localProduct",
                                    platformConstraints = null,
                                ),
                            ),
                            traits = setOf(
                            ),
                        ),
                        SwiftPMDependency.Remote(
                            cinteropClangModules = listOf(
                                SwiftPMDependency.CinteropClangModule(
                                    name = "explicitProduct",
                                    platformConstraints = setOf(
                                        SwiftPMDependency.Platform.iOS,
                                    ),
                                ),
                            ),
                            packageName = "packageName",
                            products = listOf(
                                SwiftPMDependency.Product(
                                    cinteropClangModules = setOf(
                                        "explicitProduct",
                                    ),
                                    name = "explicitProduct",
                                    platformConstraints = setOf(
                                        SwiftPMDependency.Platform.iOS,
                                    ),
                                ),
                            ),
                            repository = SwiftPMDependency.Remote.Repository.Id(
                                value = "foo.bar/package1",
                            ),
                            traits = setOf(
                            ),
                            version = SwiftPMDependency.Remote.Version.Exact(
                                value = "1.2.3",
                            ),
                        ),
                        SwiftPMDependency.Remote(
                            cinteropClangModules = listOf(
                                SwiftPMDependency.CinteropClangModule(
                                    name = "product",
                                    platformConstraints = null,
                                ),
                            ),
                            packageName = "package2",
                            products = listOf(
                                SwiftPMDependency.Product(
                                    cinteropClangModules = setOf(
                                    ),
                                    name = "product",
                                    platformConstraints = null,
                                ),
                            ),
                            repository = SwiftPMDependency.Remote.Repository.Url(
                                value = "https://foo.bar/package2.git",
                            ),
                            traits = setOf(
                            ),
                            version = SwiftPMDependency.Remote.Version.From(
                                value = "1.2.3",
                            ),
                        ),
                    ),
                ).prettyPrinted,
                deserializedMetadata.prettyPrinted
            )
        }
    }
}
