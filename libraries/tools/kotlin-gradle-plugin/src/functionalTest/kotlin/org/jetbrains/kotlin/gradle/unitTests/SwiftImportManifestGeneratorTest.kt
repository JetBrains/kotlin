/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftImportManifestGenerator
import org.junit.Test
import kotlin.test.assertEquals

class SwiftImportManifestGeneratorTest {

    @Test
    fun `test basic manifest with single platform and dependencies`() {
        val manifest = SwiftImportManifestGenerator.generateManifest(
            identifier = "TestPackage",
            productType = ".dynamic",
            platforms = listOf(".iOS(\"15.0\")"),
            repoDependencies = listOf(".package(url: \"https://github.com/example/repo\", from: \"1.0.0\")"),
            targetDependencies = listOf(".product(name: \"ExampleLib\", package: \"repo\")"),
        )

        val expected = """
            |// swift-tools-version: 5.9
            |import PackageDescription
            |let package = Package(
            |  name: "TestPackage",
            |  platforms: [
            |    .iOS("15.0")
            |  ],
            |  products: [
            |    .library(
            |      name: "TestPackage",
            |      type: .dynamic,
            |      targets: ["TestPackage"]
            |    )
            |  ],
            |  dependencies: [
            |    .package(url: "https://github.com/example/repo", from: "1.0.0")
            |  ],
            |  targets: [
            |    .target(
            |      name: "TestPackage",
            |      dependencies: [
            |        .product(name: "ExampleLib", package: "repo")
            |      ]
            |    )
            |  ]
            |)
            |""".trimMargin()

        assertEquals(expected, manifest)
    }

    @Test
    fun `test manifest with multiple platforms`() {
        val manifest = SwiftImportManifestGenerator.generateManifest(
            identifier = "MultiPlatformPackage",
            productType = ".none",
            platforms = listOf(".iOS(\"15.0\")", ".macOS(\"12.0\")", ".watchOS(\"8.0\")"),
            repoDependencies = emptyList(),
            targetDependencies = emptyList(),
        )

        val expected = """
            |// swift-tools-version: 5.9
            |import PackageDescription
            |let package = Package(
            |  name: "MultiPlatformPackage",
            |  platforms: [
            |    .iOS("15.0"),
            |    .macOS("12.0"),
            |    .watchOS("8.0")
            |  ],
            |  products: [
            |    .library(
            |      name: "MultiPlatformPackage",
            |      type: .none,
            |      targets: ["MultiPlatformPackage"]
            |    )
            |  ],
            |  dependencies: [
            |  ],
            |  targets: [
            |    .target(
            |      name: "MultiPlatformPackage",
            |      dependencies: [
            |      ]
            |    )
            |  ]
            |)
            |""".trimMargin()

        assertEquals(expected, manifest)
    }

    @Test
    fun `test manifest with linkerSettings`() {
        val manifest = SwiftImportManifestGenerator.generateManifest(
            identifier = "LinkedPackage",
            productType = ".dynamic",
            platforms = listOf(".iOS(\"15.0\")"),
            repoDependencies = emptyList(),
            targetDependencies = emptyList(),
            linkerHackPath = "/path/to/linker/hack",
        )

        val expected = """
            |// swift-tools-version: 5.9
            |import PackageDescription
            |let package = Package(
            |  name: "LinkedPackage",
            |  platforms: [
            |    .iOS("15.0")
            |  ],
            |  products: [
            |    .library(
            |      name: "LinkedPackage",
            |      type: .dynamic,
            |      targets: ["LinkedPackage"]
            |    )
            |  ],
            |  dependencies: [
            |  ],
            |  targets: [
            |    .target(
            |      name: "LinkedPackage",
            |      dependencies: [
            |      ],
            |      linkerSettings: [.unsafeFlags(["-fuse-ld=/path/to/linker/hack"])]
            |    )
            |  ]
            |)
            |""".trimMargin()

        assertEquals(expected, manifest)
    }

    @Test
    fun `test manifest with empty dependencies`() {
        val manifest = SwiftImportManifestGenerator.generateManifest(
            identifier = "EmptyDepsPackage",
            productType = ".none",
            platforms = listOf(".iOS(\"16.0\")"),
            repoDependencies = emptyList(),
            targetDependencies = emptyList(),
        )

        val expected = """
            |// swift-tools-version: 5.9
            |import PackageDescription
            |let package = Package(
            |  name: "EmptyDepsPackage",
            |  platforms: [
            |    .iOS("16.0")
            |  ],
            |  products: [
            |    .library(
            |      name: "EmptyDepsPackage",
            |      type: .none,
            |      targets: ["EmptyDepsPackage"]
            |    )
            |  ],
            |  dependencies: [
            |  ],
            |  targets: [
            |    .target(
            |      name: "EmptyDepsPackage",
            |      dependencies: [
            |      ]
            |    )
            |  ]
            |)
            |""".trimMargin()

        assertEquals(expected, manifest)
    }

    @Test
    fun `test no trailing commas before closing brackets`() {
        val manifest = SwiftImportManifestGenerator.generateManifest(
            identifier = "TestPackage",
            productType = ".dynamic",
            platforms = listOf(".iOS(\"15.0\")", ".macOS(\"12.0\")"),
            repoDependencies = listOf(
                ".package(url: \"https://github.com/example/repo1\", from: \"1.0.0\")",
                ".package(url: \"https://github.com/example/repo2\", from: \"2.0.0\")"
            ),
            targetDependencies = listOf(
                ".product(name: \"Lib1\", package: \"repo1\")",
                ".product(name: \"Lib2\", package: \"repo2\")"
            ),
        )

        val expected = """
            |// swift-tools-version: 5.9
            |import PackageDescription
            |let package = Package(
            |  name: "TestPackage",
            |  platforms: [
            |    .iOS("15.0"),
            |    .macOS("12.0")
            |  ],
            |  products: [
            |    .library(
            |      name: "TestPackage",
            |      type: .dynamic,
            |      targets: ["TestPackage"]
            |    )
            |  ],
            |  dependencies: [
            |    .package(url: "https://github.com/example/repo1", from: "1.0.0"),
            |    .package(url: "https://github.com/example/repo2", from: "2.0.0")
            |  ],
            |  targets: [
            |    .target(
            |      name: "TestPackage",
            |      dependencies: [
            |        .product(name: "Lib1", package: "repo1"),
            |        .product(name: "Lib2", package: "repo2")
            |      ]
            |    )
            |  ]
            |)
            |""".trimMargin()

        assertEquals(expected, manifest)
    }

    @Test
    fun `test manifest with multiline dependency declarations`() {
        val multilineDep = """.package(
          url: "https://github.com/example/repo",
          from: "1.0.0"
        )""".trimMargin()

        val multilineTargetDep = """.product(
          name: "ExampleLib",
          package: "repo"
        )""".trimMargin()

        val manifest = SwiftImportManifestGenerator.generateManifest(
            identifier = "TestPackage",
            productType = ".dynamic",
            platforms = listOf(".iOS(\"15.0\")"),
            repoDependencies = listOf(multilineDep),
            targetDependencies = listOf(multilineTargetDep),
        )

        val expected = """
            |// swift-tools-version: 5.9
            |import PackageDescription
            |let package = Package(
            |  name: "TestPackage",
            |  platforms: [
            |    .iOS("15.0")
            |  ],
            |  products: [
            |    .library(
            |      name: "TestPackage",
            |      type: .dynamic,
            |      targets: ["TestPackage"]
            |    )
            |  ],
            |  dependencies: [
            |    .package(
            |              url: "https://github.com/example/repo",
            |              from: "1.0.0"
            |            )
            |  ],
            |  targets: [
            |    .target(
            |      name: "TestPackage",
            |      dependencies: [
            |        .product(
            |                  name: "ExampleLib",
            |                  package: "repo"
            |                )
            |      ]
            |    )
            |  ]
            |)
            |""".trimMargin()

        assertEquals(expected, manifest)
    }
}
