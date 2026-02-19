/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText


fun createLocalSwiftPackage(localPackageDir: Path) {
    localPackageDir.resolve("Sources/LocalSwiftPackage").createDirectories()
    localPackageDir.resolve("Package.swift").writeText(
        """
                // swift-tools-version: 5.9
                import PackageDescription

                let package = Package(
                    name: "LocalSwiftPackage",
                    platforms: [.iOS(.v15)],
                    products: [
                        .library(name: "LocalSwiftPackage", targets: ["LocalSwiftPackage"]),
                    ],
                    targets: [
                        .target(name: "LocalSwiftPackage"),
                    ]
                )
            """.trimIndent()
    )

    localPackageDir.resolve("Sources/LocalSwiftPackage/LocalSwiftPackage.swift").writeText(
        """
                import Foundation

                @objc public class LocalHelper: NSObject {
                    @objc public static func greeting() -> String {
                        return "Hello from LocalSwiftPackage"
                    }
                }
            """.trimIndent()
    )
}