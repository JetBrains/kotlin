/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Represents an Xcode project.
 *
 * It allows fetching the template project from the resources, building it for testing, performing code signing, and accessing
 * project artifacts.
 *
 * @property workDir The path to directory the Xcode project should be located in.
 */
internal class XcodeProject(private val workDir: Path) {
    private val hostExecutor = HostExecutor()

    /**
     * Path to the project root
     */
    val path: Path = workDir.resolve(PROJECT_RESOURCE_PATH.removePrefix("/")).resolve(PROJECT_NAME)

    /**
     * Represents the derived data directory for a Xcode project.
     *
     * The `derivedData` stores the path to the derived data directory.
     *
     * The derived data directory is a location within a Xcode project where build products and intermediate files are stored.
     * This directory is used during the build process and can be useful for inspecting build artifacts.
     */
    val derivedData: Path = path.resolve("DerivedData")

    /**
     * Variable that represents the path to the "Products" directory of the derived data for an Xcode project.
     */
    val products: Path = derivedData.resolve("Build/Products")

    /**
     * Represents the file path of the built application.
     *
     * This is a location of built application relative to DerivedData Xcode directory/
     * @see derivedData
     */
    val application: Path = products.resolve("Debug-iphoneos/${PROJECT_NAME}.app")

    /**
     * Represents the path to the test bundle of the application.
     *
     * @see derivedData
     * @see application
     */
    val testBundle: Path = application.resolve("PlugIns/${PROJECT_NAME}Tests.xctest")

    /**
     * Represents the path of the test bundle dSYM file.
     *
     * @see testBundle
     */
    val testBundleDSYM: Path = application.resolve("PlugIns/${PROJECT_NAME}Tests.xctest.dSYM")

    /**
     * Fetches the Xcode project resource and copies its content to project [path].
     *
     * If the specified path already exists, it will be deleted recursively before copying the content.
     */
    @OptIn(ExperimentalPathApi::class)
    fun fetch() {
        synchronized(lock) {
            val url = this::class.java.getResource(PROJECT_RESOURCE_PATH)
                ?: error("The Xcode project resource is missing at $PROJECT_RESOURCE_PATH")

            val projectPath = workDir.toAbsolutePath().also {
                if (it.exists()) {
                    it.deleteRecursively()
                }
                it.createDirectory()
            }

            FileSystems.newFileSystem(url.toURI(), emptyMap<String, Any>()).use {
                val fsRootPath = it.rootDirectories.singleOrNull()?.root ?: error("Root of the file system attached to $url not found")

                fsRootPath.resolve(PROJECT_RESOURCE_PATH).visitFileTree {
                    onPreVisitDirectory { directory, _ ->
                        val destination = projectPath.resolveRelativeToRoot(directory)
                        if (!destination.exists()) {
                            destination.createDirectory()
                        }

                        FileVisitResult.CONTINUE
                    }
                    onVisitFile { file, _ ->
                        val destination = projectPath.resolveRelativeToRoot(file)
                        file.copyTo(destination)

                        FileVisitResult.CONTINUE
                    }
                }
            }
        }
    }

    /**
     * Builds the project for testing.
     *
     * If the project path does not exist, it fetches the project.
     */
    fun build() {
        if (!path.exists()) {
            fetch()
        }

        hostExecutor.execute(
            ExecuteRequest(
                executableAbsolutePath = "/usr/bin/xcrun",
                args = mutableListOf(
                    "xcodebuild",
                    "-project", "$PROJECT_NAME.xcodeproj",
                    "-scheme", PROJECT_NAME,
                    "-derivedDataPath", derivedData.absolutePathString(),
                    "-sdk", "iphoneos",
                    "-destination", "generic/platform=iOS",
                    "build-for-testing"
                ).also { it.addAll(XCODE_CODESIGN_PARAMETERS) },
                workingDirectory = path.toFile()
            )
        ).assertSuccess()
    }

    /**
     * Replaces the test bundle with a new bundle.
     *
     * It also makes an attempt to copy the dSYM info to the project.
     *
     * @param newBundle The path of the new bundle to replace the test bundle with.
     */
    fun replaceTestBundleWith(newBundle: Path) {
        testBundle.toFile().deleteRecursively()
        newBundle.toFile().copyRecursively(testBundle.toFile(), overwrite = true)
        // place dSYM too if exists
        testBundleDSYM.toFile().deleteRecursively()
        val dSYM = newBundle.resolveSibling("${newBundle.name}.dSYM")
        if (dSYM.exists()) {
            dSYM.toFile().copyRecursively(testBundleDSYM.toFile(), overwrite = true)
        }
    }

    /**
     * Resolves a given path relative to the root, which may be different between these two paths.
     */
    private fun Path.resolveRelativeToRoot(path: Path) = this.resolve(path.root.relativize(path).pathString)

    /**
     * Performs project code signing.
     *
     * This code signing is an ad-hoc codesign.
     * For the application, it adds a provided with the project minimally necessary entitlements.
     */
    fun codesign() {
        codesign(
            "--force", "--sign", "-",
            "--timestamp=none",
            "--generate-entitlement-der",
            testBundle.toString()
        )

        // TODO: get rid of this: isn't used at all
        val testLibPath = application.resolve("PlugIns/test-ios-launchTests-testLib.xctest")
        codesign(
            "--force", "--sign", "-",
            "--timestamp=none",
            "--generate-entitlement-der",
            testLibPath.toString()
        )

        codesign(
            "--force", "--sign", "-",
            "--timestamp=none",
            "--entitlements", path.parent.resolve(ENITITLEMENTS).absolutePathString(),
            "--generate-entitlement-der",
            application.toString()
        )
    }

    private fun codesign(vararg args: String) {
        hostExecutor.execute(
            ExecuteRequest("/usr/bin/codesign", args.toMutableList())
        ).assertSuccess()
    }

    companion object {
        private const val PROJECT_NAME = "test-ios-launch"
        private const val PROJECT_RESOURCE_PATH = "/xcode-project"
        private const val ENITITLEMENTS = "$PROJECT_NAME.app.xcent"

        // Those variables make Xcode skip its code signing that we do by ourselves manually and using ad-hoc technique
        private val XCODE_CODESIGN_PARAMETERS = listOf(
            "CODE_SIGN_IDENTITY=",
            "CODE_SIGN_ENTITLEMENTS=",
            "CODE_SIGNING_REQUIRED=NO",
            "CODE_SIGNING_ALLOWED=NO"
        )

        private val lock = Any()
    }
}