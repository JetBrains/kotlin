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
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

/**
 * Executes test cases on Firebase Test Lab for iOS arm64 devices.
 *
 * This executor requires the Goggle Cloud command-line utilities to be installed and configured.
 * See also [Firebase guide](https://firebase.google.com/docs/test-lab/ios/command-line)
 *
 * @param configurables The configuration options for the executor.
 * @param description A description of the test execution used to identify run in the cloud console
 */
class FirebaseCloudXCTestExecutor(
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
        val workDir = request.workingDirectory?.toPath() ?: Paths.get(".")
        val projectDir = Files.createTempDirectory(workDir, "xctest-firebase-runner")

        val bundle = XCTestBundle.ProjectWrapped(Path(request.executableAbsolutePath), request.args)

        // Make a zip with the built application and xctestrun file
        val testsZip = projectDir.resolve("KNTests.zip").apply {
            createZip(bundle.prepareToRun(projectDir))
        }

        // Execute tests in the Firebase
        val stderr = ByteArrayOutputStream()
        val firebaseRequest = ExecuteRequest(
            executableAbsolutePath = "gcloud",
            workingDirectory = projectDir.toFile(),
            args = mutableListOf(
                "firebase", "test", "ios", "run",
                "--test=$testsZip",
                "--no-record-video",
                "--device=model=iphone13pro",
                "--client-details=matrixLabel=$description"
            ),
            stderr = stderr
        )
        val firebaseResponse = hostExecutor.execute(firebaseRequest)

        val firebaseStderrString = stderr.toString("UTF-8").trim()
        println(firebaseStderrString)

        // 0 - exit code for "All test executions passed."
        // 10 - exit code for "One or more test cases (tested classes or class methods) within a test execution did not pass."
        // See https://firebase.google.com/docs/test-lab/ios/command-line#script-exit-codes
        // Treat other codes for unsuccessful execution of the Firebase run itself.
        check(firebaseResponse.exitCode == 0 || firebaseResponse.exitCode == 10) {
            "Firebase failed with exit code ${firebaseResponse.exitCode}, see stderr: $firebaseStderrString"
        }

        // Get the URL to the results located on Google Cloud Storage
        // This is a default GCloud bucket and name, non-default buckets require a billing-enabled account,
        // see https://cloud.google.com/sdk/gcloud/reference/firebase/test/ios/run#--results-bucket
        val resultsBucketURL = Regex(
            "Raw results will be stored in your GCS bucket at \\[https://console.developers.google.com/storage/browser/(.*)]"
        ).find(firebaseStderrString.trim())
            ?.groupValues
            ?.get(1)
            ?.removeSuffixIfPresent("/")
            ?: error("Unable to match URL against pattern, the input was: \"$firebaseStderrString\"")

        // Fetch results from the storage
        hostExecutor.execute(
            ExecuteRequest(
                executableAbsolutePath = "gsutil",
                workingDirectory = projectDir.toFile(),
                args = mutableListOf("-m", "cp", "-r", "gs://${resultsBucketURL}/iphone*", ".")
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

        bundle.cleanup()

        return firebaseResponse
    }

    private fun Path.createZip(sourceFolder: Path) {
        ZipOutputStream(Files.newOutputStream(this)).use { stream ->
            Files.walk(sourceFolder)
                .filter { !Files.isDirectory(it) }
                .forEach { path ->
                    val entry = ZipEntry(sourceFolder.relativize(path).toString())
                    stream.putNextEntry(entry)
                    Files.copy(path, stream)
                    stream.closeEntry()
                }
        }
    }
}
