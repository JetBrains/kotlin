/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleXcodeTasks
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

/**
 * This is a CLI command you would run once to integrated embedAndSign script in the Xcode project. It shouldn't ever be UTD or cached.
 */
@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class IntegrateEmbedAndSignIntoXcodeProject : DefaultTask() {
    @get:Internal
    abstract val xcodeprojPath: Property<String>

    @get:Internal
    abstract val currentDir: Property<File>

    @get:Internal
    val xcodeprojTemporaries = project.layout.buildDirectory.dir("kotlin/swiftImportEmbedAndSignXcodeprojMutationTemporaries")

    @get:Inject
    protected abstract val execOps: ExecOperations

    @TaskAction
    fun integrate() {
        var projectPath = File(xcodeprojPath.get())
        if (!projectPath.isAbsolute) {
            projectPath = currentDir.get().resolve(projectPath)
        }

        val gradlewPath = System.getenv(GRADLEW_PATH_ENV)?.let { File(it) }
            ?: searchForGradlew(projectPath)
            ?: error("Couldn't find path to Gradle executable. Please specify path using ${GRADLEW_PATH_ENV} environment variable")

        val gradleProjectPath = System.getenv(GRADLE_PROJECT_PATH_ENV)
            ?: error("""
                Please specify path to gradle project in $GRADLE_PROJECT_PATH_ENV environment variable
                For example: export $GRADLE_PROJECT_PATH_ENV=:shared
            """.trimIndent())

        val pbxprojPath = projectPath.resolve("project.pbxproj")
        val project = deserializeXcodeProject(pbxprojPath, execOps)

        val hasEmbedAndSignReference = project.objects.entries.any {
            (it.value as? PbxShellScriptBuildPhase)?.shellScript?.stringValue?.contains("gradle") ?: false
        }
        if (hasEmbedAndSignReference) {
            logger.quiet("Found embedAndSign integration. Nothing to do")
            return
        }

        val nativeTargets = project.objects.values.filterIsInstance<PbxNativeTarget>()
        if (nativeTargets.isEmpty()) {
            error("Couldn't find targets to insert embedAndSign integration")
        }

        val srcrootPath = projectPath.parentFile
        val relativeGradlewPath = gradlewPath.parentFile.relativeTo(srcrootPath)
        nativeTargets.forEach {
            val scriptPhase = generateScriptReference(
                relativeGradlewPath.path,
                gradleProjectPath,
            )
            val scriptPhaseReference = generateRandomPBXObjectReference()
            if (it.buildPhases == null) {
                it.buildPhases = mutableListOf()
            }
            it.buildPhases?.add(0, scriptPhaseReference)
            project.objects[scriptPhaseReference] = scriptPhase
        }

        saveJsonBackIntoPbxproj(
            execOps,
            xcodeprojTemporaries.getFile(),
            project,
            pbxprojPath.path,
        )
    }

    private fun generateScriptReference(
        relativeGradlewRootPath: String,
        gradleProjectPath: String,
    ) = PbxShellScriptBuildPhase(
        name = "Compile Kotlin Framework",
        alwaysOutOfDate = "1",
        runOnlyForDeploymentPostprocessing = "0",
        buildActionMask = "2147483647",
        shellPath = "/bin/sh",
        shellScript = StringOrStringList.StringValue(
            """
            if [ "YES" = "${'$'}OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
              echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
              exit 0
            fi
            cd "${'$'}${SRCROOT_ENV}/${relativeGradlewRootPath}"
            ./gradlew ${gradleProjectPath}:${AppleXcodeTasks.embedAndSignTaskPrefix}${AppleXcodeTasks.embedAndSignTaskPostfix} -i
            """.trimIndent()
        )
    )

    companion object {
        const val TASK_NAME = "integrateEmbedAndSign"
        const val GRADLEW_PATH_ENV = "GRADLEW_PATH"
        const val GRADLE_PROJECT_PATH_ENV = "GRADLE_PROJECT_PATH"
        // This assumes that SRCROOT is the same as PROJECT_FILE_PATH which we read initially
        const val SRCROOT_ENV = "SRCROOT"
    }
}

/**
 * This is a CLI command you would run once to integrate the linkage package in the Xcode project. It shouldn't ever be UTD or cached.
 */
@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class IntegrateLinkagePackageIntoXcodeProject : DefaultTask() {

    @get:Internal
    abstract val xcodeprojPath: Property<String>

    @get:Internal
    abstract val currentDir: Property<File>

    @get:Internal
    val xcodeprojTemporaries = project.layout.buildDirectory.dir("kotlin/swiftImportLinkagePackageXcodeprojMutationTemporaries")

    @get:Inject
    protected abstract val execOps: ExecOperations

    @TaskAction
    fun integrate() {
        var projectPath = File(xcodeprojPath.get())
        if (!projectPath.isAbsolute) {
            projectPath = currentDir.get().resolve(projectPath)
        }

        val pbxprojPath = projectPath.resolve("project.pbxproj")
        val project = deserializeXcodeProject(pbxprojPath, execOps)
        if (linkageProductsReferencedInPBXObjects(project).isNotEmpty()) {
            logger.quiet("Product already referenced, nothing to do")
            return
        }
        val rootProjectId = project.rootObject

        val embedAndSignShellScriptPhaseReference = project.objects.entries.firstOrNull {
            (it.value as? PbxShellScriptBuildPhase)?.shellScript?.stringValue?.contains("gradle") ?: false
        }?.key ?: error("embedAndSign integration wasn't found")
        val embedAndSignTargets = project.objects.values.filterIsInstance<PbxNativeTarget>().filter {
            it.buildPhases?.contains(embedAndSignShellScriptPhaseReference) ?: false
        }
        val rootProject = (project.objects[rootProjectId] ?: error("Couldn't find root project")) as PbxProject

        val productDependencyReference = generateRandomPBXObjectReference()
        val buildFileDependencyReference = generateRandomPBXObjectReference()
        val localPackageReference = generateRandomPBXObjectReference()

        if (rootProject.packageReferences == null) { rootProject.packageReferences = mutableListOf() }
        rootProject.packageReferences?.add(localPackageReference)

        embedAndSignTargets.forEach {
            if (it.packageProductDependencies == null) { it.packageProductDependencies = mutableListOf() }
            it.packageProductDependencies?.add(productDependencyReference)
        }

        val buildPhases = embedAndSignTargets.flatMap { it.buildPhases ?: listOf() }
        val frameworkBuildPhases = buildPhases.mapNotNull {
            project.objects[it] as? PbxFrameworksBuildPhase
        }

        frameworkBuildPhases.forEach {
            if (it.files == null) { it.files = mutableListOf() }
            it.files?.add(buildFileDependencyReference)
        }

        project.objects[buildFileDependencyReference] = PbxBuildFile(productRef = productDependencyReference)
        project.objects[localPackageReference] = XCLocalSwiftPackageReference(relativePath = GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME)
        project.objects[productDependencyReference] = XCSwiftPackageProductDependency(productName = GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME)

        saveJsonBackIntoPbxproj(
            execOps,
            xcodeprojTemporaries.getFile(),
            project,
            pbxprojPath.path,
        )
    }

    companion object {
        const val TASK_NAME = "integrateLinkagePackage"

        const val INPUT_PBXPROJ_JSON_PATH_ENV = "INPUT_PBXPROJ_JSON_PATH"
        const val OUTPUT_PBXPROJ_PATH_ENV = "OUTPUT_PBXPROJ_PATH"
    }
}

private fun saveJsonBackIntoPbxproj(
    execOps: ExecOperations,
    xcodeprojTemporaries: File,
    project: XcodeProject,
    outputPbxprojPath: String,
) {
    xcodeprojTemporaries.mkdirs()
    val jsonPbxprojPath = xcodeprojTemporaries.resolve("project.pbxproj.json")
    jsonPbxprojPath.outputStream().use {
        project.serializeXcodeProject(it)
    }
    val binName = "mutatePbxproj"
    xcodeprojTemporaries.resolve("Package.swift").writeText(
        """
                // swift-tools-version: 5.9
                import PackageDescription

                let package = Package(
                    name: "${binName}",
                    platforms: [.macOS(.v13)],
                    targets: [
                        .executableTarget(name: "${binName}"),
                    ]
                )
            """.trimIndent()
    )
    xcodeprojTemporaries.resolve("Sources").mkdirs()
    xcodeprojTemporaries.resolve("Sources/main.swift").writeText(
        """
                import Foundation

                let inputEnv = "${IntegrateLinkagePackageIntoXcodeProject.INPUT_PBXPROJ_JSON_PATH_ENV}"
                let outputEnv = "${IntegrateLinkagePackageIntoXcodeProject.OUTPUT_PBXPROJ_PATH_ENV}"
                guard let inputPbxprojJsonPath = ProcessInfo.processInfo.environment[inputEnv] else {
                    fatalError("Specify path to pbxproj json in \(inputEnv) environment variable")
                }
                guard let outputPbxprojPath = ProcessInfo.processInfo.environment[outputEnv] else {
                    fatalError("Specify path to output pbxproj in \(outputEnv) environment variable")
                }

                guard let developerPath = ProcessInfo.processInfo.environment["XCODE_DEVELOPER_PATH"] else {
                    fatalError("XCODE_DEVELOPER_PATH environment variable not set")
                }
                let devToolsCore = URL(fileURLWithPath: developerPath).deletingLastPathComponent().appending(path: "Frameworks/DevToolsCore.framework/DevToolsCore")

                print("Loading DevToolsCore from \(devToolsCore)")

                if (dlopen(devToolsCore.path(), RTLD_NOW) == nil) {
                    fatalError(String(cString: dlerror()))
                }

                guard let inputStream = InputStream(url: URL(filePath: inputPbxprojJsonPath)) else {
                    fatalError("Couldn't create input stream \(inputPbxprojJsonPath)")
                }
                inputStream.open()
                let jsonPbxproj = try JSONSerialization.jsonObject(
                    with: inputStream
                )
                inputStream.close()
                guard let project = jsonPbxproj as? NSDictionary else {
                    fatalError("Couldn't cast \(jsonPbxproj)")
                }

                let data = project.perform(Selector(("plistDescriptionUTF8Data"))).takeRetainedValue()
                guard let nsData = data as? Data else {
                    fatalError("Couldn't cast return type \(data)")
                }

                let handle = try FileHandle(forWritingTo: URL(fileURLWithPath: outputPbxprojPath))
                try handle.seek(toOffset: 0)
                try handle.write(contentsOf: nsData)
                try handle.truncate(atOffset: UInt64(nsData.count))
                try handle.synchronize()
                try handle.close()

            """.trimIndent()
    )

    execOps.exec {
        it.workingDir(xcodeprojTemporaries)
        it.commandLine("swift", "build")
    }

    // For some reason they sanitize or override DYLD_ variables in swift run, so we have to call the binary directly instead
    val output = ByteArrayOutputStream()
    execOps.exec {
        it.workingDir(xcodeprojTemporaries)
        it.commandLine("swift", "build", "--show-bin-path")
        it.standardOutput = output
    }
    val outputsPath = File(output.toString().lineSequence().first()).resolve(binName)

    // Get Xcode developer path dynamically using xcode-select -p
    val xcodeSelectOutput = ByteArrayOutputStream()
    execOps.exec {
        it.commandLine("xcode-select", "-p")
        it.standardOutput = xcodeSelectOutput
    }
    val developerPath = xcodeSelectOutput.toString().trim()
    // developerPath is typically /Applications/Xcode.app/Contents/Developer
    // SharedFrameworks is at /Applications/Xcode.app/Contents/SharedFrameworks (sibling of Developer)
    val sharedFrameworksPath = File(developerPath).parentFile.resolve("SharedFrameworks").path

    execOps.exec {
        it.workingDir(xcodeprojTemporaries)
        it.commandLine(outputsPath.path)
        it.environment("DYLD_FALLBACK_FRAMEWORK_PATH", "$sharedFrameworksPath:/Applications/Xcode.app/Contents/SharedFrameworks")
        it.environment("XCODE_DEVELOPER_PATH", developerPath)
        it.environment(IntegrateLinkagePackageIntoXcodeProject.INPUT_PBXPROJ_JSON_PATH_ENV, jsonPbxprojPath.path)
        it.environment(IntegrateLinkagePackageIntoXcodeProject.OUTPUT_PBXPROJ_PATH_ENV, outputPbxprojPath)
    }
}

private fun generateRandomPBXObjectReference(): String {
    val messageDigest = MessageDigest.getInstance("MD5")
    return messageDigest.digest(
        UUID.randomUUID().toString().toByteArray()
    ).joinToString(separator = "") { byte -> "%02x".format(byte) }.uppercase().subSequence(0, 24).toString()
}

internal fun linkageProductsReferencedInPBXObjects(project: XcodeProject): Set<String> {
    // FIXME: KT-83876 Check if the product is correctly integrated into the build phase
    return project.objects.entries.mapNotNull { (id, pbxObject) ->
        if ((pbxObject as? XCSwiftPackageProductDependency)?.productName == GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME) {
            id
        } else {
            null
        }
    }.toSet()
}
