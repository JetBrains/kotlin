/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*
import kotlin.time.Duration.Companion.seconds

class FirebaseCloudExecutor(
    private val configurables: AppleConfigurables,
    private val description: String = "K/N Test execution",
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

    override fun execute(request: ExecuteRequest): ExecuteResponse {
        // Copy Xcode project
        val workDir = request.workingDirectory?.toPath() ?: Paths.get(".")
        val projectDir = Files.createTempDirectory(workDir, "xctest-project-runner")

        val xcodeProject = XcodeProject(projectDir).apply {
            fetch()
            build()
            replaceTestBundleWith(Path(request.executableAbsolutePath))
            testBundle.toFile().writeTestArguments(request.args)
            codesign()
        }
        /*

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
                val firebaseResponse = hostExecutor.execute(firebaseRequest)

                val firebaseStdoutString = stdout.toString("UTF-8").trim()
                val firebaseStderrString = stderr.toString("UTF-8").trim()
                println(firebaseStdoutString)
                println(firebaseStderrString)

                // 0 - exit code for "All test executions passed."
                // 10 - exit code for "One or more test cases (tested classes or class methods) within a test execution did not pass."
                // Treat other codes for unsuccessful execution of the Firebase run itself.
                check(firebaseResponse.exitCode == 0 || firebaseResponse.exitCode == 10) {
                    "Firebase failed with exit code ${firebaseResponse.exitCode}, see stderr: $firebaseStderrString"
                }

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
        */
        return ExecuteResponse(0, 0.seconds)
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
