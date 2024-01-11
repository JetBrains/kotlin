/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

class FirebaseCloudExecutor(
    private val configurables: AppleConfigurables,
    private val description: String = "Test execution sample with K/N executor"
) : Executor {
    private val hostExecutor = HostExecutor()

    private val target by configurables::target

    init {
        require(HostManager.host.family.isAppleFamily) {
            "$this executor isn't available for $configurables"
        }
        require(target == KonanTarget.IOS_ARM64) {
            "$this executor is available only for iOS arm64 (device-only)"
        }
    }

    private val testProjectResourcePath = "/Users/Pavel.Punegov/ws/kotlin/native/executors/build/resources/main/xcode-project"
    private val testProjectName = "test-ios-launch"
    private val entitlementsFile = "test-ios-launch.app.xcent"

    private fun resourceToURI(resourcePath: String) =
        this::class.java.getResource(resourcePath)?.toURI() ?: error("Unable to load resource: $resourcePath")

    private val fileSystem by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        FileSystems.newFileSystem(resourceToURI(testProjectResourcePath), emptyMap<String, Any>())
    }

    @OptIn(ExperimentalPathApi::class)
    override fun execute(request: ExecuteRequest): ExecuteResponse {
        // Copy Xcode project
        val workDir = request.workingDirectory?.toPath() ?: Paths.get(".")
        val projectDir = Files.createTempDirectory(workDir, "xctest-project-runner")
        val xcodeProjectPath = projectDir.resolve("xcode-project")
//
//        fileSystem.let {
//            val sourcePath = it.getPath(testProjectResourcePath)
//            sourcePath.visitFileTree {
//                onPreVisitDirectory { directory, _ ->
//                    val destinationDir = xcodeProjectPath.resolve(directory)
//                    if (!Files.exists(destinationDir)) {
//                        Files.createDirectory(destinationDir)
//                    }
//                    FileVisitResult.CONTINUE
//                }
//                onVisitFile { file: Path, _: BasicFileAttributes ->
//                    val destination = xcodeProjectPath.resolve(file)
//                    Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING)
//                    FileVisitResult.CONTINUE
//                }
//            }
//        }
        File(testProjectResourcePath).copyRecursively(xcodeProjectPath.toFile())

        val derivedData = xcodeProjectPath.resolve(testProjectName).resolve("DerivedData").createDirectory()

        // Build the project
        buildXcodeProject(xcodeProjectPath.resolve(testProjectName).toFile(), derivedData)

        // Copy the test bundle into the project
        val appPath = derivedData.resolve("Build/Products/Debug-iphoneos/test-ios-launch.app")
        val bundlePath = appPath.resolve("PlugIns/test-ios-launchTests.xctest")
        File(request.executableAbsolutePath).copyRecursively(bundlePath.toFile(), overwrite = true)
        setXCTestArguments(hostExecutor, bundlePath.toFile(), request.args)

        // Codesign (ad-hoc) both application and the xctest bundle
        codesign(
            "--force", "--sign", "-",
            "--timestamp=none",
            "--generate-entitlement-der",
            bundlePath.toString()
        )

        // TODO: get rid of this: isn't used at all
        val testLibPath = appPath.resolve("PlugIns/test-ios-launchTests-testLib.xctest")
        codesign(
            "--force", "--sign", "-",
            "--timestamp=none",
            "--generate-entitlement-der",
            testLibPath.toString()
        )

        codesign(
            "--force", "--sign", "-",
            "--timestamp=none",
            "--entitlements", xcodeProjectPath.resolve(entitlementsFile).absolutePathString(),
            "--generate-entitlement-der",
            appPath.toString()
        )

        // Make a zip with the built application and xctestrun file
        val testsZip = projectDir.resolve("KNTests.zip")
        testsZip.createZip(derivedData.resolve("Build/Products"))

        // Execute tests in the Firebase
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val firebaseRequest = ExecuteRequest(
            executableAbsolutePath = "gcloud",
            workingDirectory = projectDir.toFile(),
            args = mutableListOf(
                "firebase", "test", "ios", "run",
                "--test=$testsZip",
                "--device=model=iphone13pro",
                "--client-details=matrixLabel=$description"
            ),
            stdout = stdout,
            stderr = stderr
        )
        hostExecutor.execute(firebaseRequest)

        val firebaseStderrString = stderr.toString("UTF-8").trim()
        println(firebaseStderrString)

        // Get the URL to the results located on Google Cloud Storage
        val resultsBucketURL = Regex(
            "Raw results will be stored in your GCS bucket at \\[https://console.developers.google.com/storage/browser/(.*)]"
        ).find(firebaseStderrString.trim())
            ?.groupValues
            ?.get(1)
            ?.removeSuffixIfPresent("/")
            ?: error("Unable to match URL against pattern, the input was: \"$firebaseStderrString\"")

        // Fetch results from the storage
        val response = hostExecutor.execute(
            ExecuteRequest(
                executableAbsolutePath = "gsutil",
                workingDirectory = projectDir.toFile(),
                args = mutableListOf(
                    "-m", "cp", "-r",
                    "gs://${resultsBucketURL}/iphone*", "."
                )
            )
        ).assertSuccess()
        val executionLog = projectDir.listDirectoryEntries("iphone*")
            .first()
            .resolve("xcodebuild_output.log")
            .readText()

        // Fill in the request's stdout with the execution result
        request.stdout.writer()
            .append(executionLog)
            .close()

        return response
    }

    private fun buildXcodeProject(project: File, derivedData: Path) {
        hostExecutor.execute(
            ExecuteRequest(
                executableAbsolutePath = "/usr/bin/xcrun",
                args = mutableListOf(
                    "xcodebuild",
                    "-project", "test-ios-launch.xcodeproj",
                    "-scheme", "test-ios-launch",
                    "-derivedDataPath", derivedData.absolutePathString(),
                    "-sdk", "iphoneos",
                    "build-for-testing",
                    "CODE_SIGN_IDENTITY=",
                    "CODE_SIGNING_REQUIRED=NO",
                    "CODE_SIGN_ENTITLEMENTS=",
                    "CODE_SIGNING_ALLOWED=NO"
                ),
                workingDirectory = project
            )
        ).assertSuccess()
    }

    private fun codesign(vararg args: String) {
        hostExecutor.execute(
            ExecuteRequest(
                executableAbsolutePath = "/usr/bin/codesign",
                args = args.toMutableList()
            )
        ).assertSuccess()
    }

    private fun Path.createZip(sourceFolder: Path) {
        ZipOutputStream(Files.newOutputStream(this)).use { stream ->
            Files.walk(sourceFolder)
                .filter { p -> !Files.isDirectory(p) }
                .forEach { p ->
                    val entry = ZipEntry(sourceFolder.relativize(p).toString())
                    stream.putNextEntry(entry)
                    Files.copy(p, stream)
                    stream.closeEntry()
                }
        }
    }
}
