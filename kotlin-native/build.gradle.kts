/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.jetbrains.kotlin.NativeFullCrossDistKt
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.CopySamples
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.cpp.CompilationDatabasePlugin
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.cpp.GitClangFormatPlugin
import org.jetbrains.kotlin.CompareDistributionSignatures
import org.jetbrains.kotlin.nativeDistribution.InvalidateStaleCaches
import org.jetbrains.kotlin.nativeDistribution.LLVMDistributionKind
import org.jetbrains.kotlin.nativeDistribution.LLVMDistributionSourceKt
import org.jetbrains.kotlin.nativeDistribution.NativeDistributionKt
import org.jetbrains.kotlin.nativeDistribution.NativeProtoDistributionKt
import org.jetbrains.kotlin.nativeDistribution.PrepareDistributionFingerprint
import org.jetbrains.kotlin.nativeDistribution.PrepareKonanProperties
import org.jetbrains.kotlin.xcode.XcodeOverridePlugin
import org.jetbrains.kotlin.UtilsKt
import plugins.KotlinBuildPublishingPluginKt
import java.security.MessageDigest
import BuildPropertiesKt.getKotlinBuildProperties

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

defaultTasks("clean", "dist")

apply<XcodeOverridePlugin>()

if (PlatformInfo.isMac()) {
    PlatformInfo.checkXcodeVersion(project)
}

apply(plugin = "kotlin.native.build-tools-conventions")
apply(plugin = "platform-manager")
apply(plugin = "java")

allprojects {
    repositories {
        mavenCentral()
        maven {
            url = uri(project.property("bootstrapKotlinRepo") as String)
        }
    }
}

val platformManager = project.extensions.getByName("platformManager") as org.jetbrains.kotlin.platformManager.PlatformManager
val hostName = PlatformInfo.hostName
val targetList = EnabledTargetsKt.enabledTargets(platformManager).map { it.visibleName }
val cacheableTargetNames = platformManager.hostPlatform.cacheableTargets
val nativeDistribution = NativeDistributionKt.getNativeDistribution(project)

configurations {
    create("commonSources")

    create("runtimeBitcode") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, CppUsage.LLVM_BITCODE))
        }
    }

    create("objcExportApi") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, CppUsage.API))
        }
    }

    create("embeddableJar") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }

    create("nativeLibs") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, CppUsage.LIBRARY_RUNTIME))
        }
    }
}

apply<CompilationDatabasePlugin>()

dependencies {
    "commonSources"(project(path = ":kotlin-stdlib", configuration = "metadataSourcesElements"))
    "commonSources"(project(path = ":kotlin-test", configuration = "metadataSourcesElements"))
    "compilationDatabase"(project(":kotlin-native:Interop:Runtime"))
    "compilationDatabase"(project(":kotlin-native:libclangext"))
    "compilationDatabase"(project(":kotlin-native:libllvmext"))
    "compilationDatabase"(project(":kotlin-native:llvmDebugInfoC"))
    "compilationDatabase"(project(":kotlin-native:runtime"))
    "compilationDatabase"(project(":kotlin-native:tools:minidump-analyzer"))
    "runtimeBitcode"(project(":kotlin-native:runtime"))
    "objcExportApi"(project(":kotlin-native:runtime"))
    "embeddableJar"(project(path = ":kotlin-native:prepare:kotlin-native-compiler-embeddable", configuration = "runtimeElements"))
    "nativeLibs"(project(":kotlin-native:common:env"))
    "nativeLibs"(project(":kotlin-native:common:files"))
    "nativeLibs"(project(":kotlin-native:llvmInterop"))
    "nativeLibs"(project(":kotlin-native:libclangInterop"))
    "nativeLibs"(project(":kotlin-native:Interop:Runtime"))

    // declared to be included in verification-metadata.xml
    val bootstrapKotlinVersion = project.property("bootstrapKotlinVersion") as String
    "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$bootstrapKotlinVersion:macos-aarch64@tar.gz")
    "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$bootstrapKotlinVersion:macos-x86_64@tar.gz")
    "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$bootstrapKotlinVersion:linux-x86_64@tar.gz")
    "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$bootstrapKotlinVersion:windows-x86_64@zip")
}

apply<GitClangFormatPlugin>()
apply(plugin = "maven-publish")
apply<BasePlugin>()

tasks.register("dist_compiler") {
    dependsOn("distCompiler")
}
tasks.register("dist_runtime") {
    dependsOn("distRuntime")
}
tasks.register("cross_dist") {
    dependsOn("crossDist")
}
tasks.register("list_dist") {
    dependsOn("listDist")
}

tasks.named("build") {
    dependsOn("dist", "distPlatformLibs")
}

val distNativeSources by tasks.registering(Zip::class) {
    dependsOn(configurations.named("commonSources"))
    duplicatesStrategy = DuplicatesStrategy.FAIL
    destinationDirectory = nativeDistribution.map { it.sources }
    archiveFileName = nativeDistribution.map { it.stdlibSources.asFile.name }

    isIncludeEmptyDirs = false
    include("**/*.kt")

    from({
        configurations.getByName("commonSources")
            .files
            .map { zipTree(it) }
            .reduce { t1, t2 -> t1 + t2 }
    })
    into("nativeWasmMain") {
        from(project(":kotlin-stdlib").file("native-wasm/src/"))
    }
    into("nativeWasmWasiMain") {
        from(project(":kotlin-stdlib").file("native-wasm/wasi/"))
    }
    into("nativeMain") {
        from(project(":kotlin-native:runtime").file("src/main/kotlin"))
        from(project(":kotlin-native:Interop:Runtime").file("src/main/kotlin"))
        from(project(":kotlin-native:Interop:Runtime").file("src/native/kotlin"))
    }
}

tasks.register("distSources") {
    dependsOn(distNativeSources)
}

tasks.register("distCompiler") {
    // Workaround: make distCompiler no-op if we are using custom dist:
    // the dist is already in place and has the compiler, so we don't have to
    // build and copy the compiler to dist.
    // Moreover, if we do copy it, it might overwrite the compiler files already loaded
    // by this Gradle process (including the jar loaded to the custom classloader),
    // causing hard-to-debug errors.
    if (!UtilsKt.isDefaultNativeHome(project)) {
        enabled = false
    } else {
        dependsOn("distNativeLibs")
        dependsOn("distCompilerJars")
        dependsOn("distTools")
        dependsOn("distBin")
        dependsOn("distSwiftExport")
        dependsOn("distKonanPlatforms")
        dependsOn("distKonanProperties")
        dependsOn("distCompilerFingerprint")
    }
}

val distDef by tasks.registering(Sync::class) {
    into(nativeDistribution.map { it.platformLibsDefinitions })
    dependsOn(":kotlin-native:platformLibs:updateDefFileDependencies")

    platformManager.targetValues.forEach { target ->
        from(project(":kotlin-native:platformLibs").file("src/platform/${target.family.name.lowercase()}")) {
            into(target.visibleName)
            include("**/*.def")
        }
    }
}

val listDist by tasks.registering(Exec::class) {
    commandLine("find", nativeDistribution.get().root)
}

tasks.register("distRuntime") {
    dependsOn("${hostName}CrossDistRuntime")
}

tasks.register("distStdlibCache") {
    if (hostName in cacheableTargetNames) {
        dependsOn("${hostName}StdlibCache")
    }
    // Make sure any tasks depending on this one see cleaned-up distribution.
    mustRunAfter("distInvalidateStaleCaches")
}

val distStdlib by tasks.registering(Sync::class) {
    from(
        project(":kotlin-native:runtime")
            .tasks
            .named("nativeStdlib")
            .map { it.outputs.files }
    )
    into(nativeDistribution.map { it.stdlib })
}

val distNativeLibs by tasks.registering(Sync::class) {
    from(configurations.named("nativeLibs"))
    into(nativeDistribution.map { it.nativeLibs })
}

val distCompilerJars by tasks.registering(Sync::class) {
    from(configurations.named("embeddableJar")) {
        rename {
            "kotlin-native-compiler-embeddable.jar"
        }
        filePermissions {
            unix("0644")
        }
    }

    into(nativeDistribution.map { it.compilerJars })
}

val distCompilerFingerprint by tasks.registering(PrepareDistributionFingerprint::class) {
    input.from(tasks.named("distNativeLibs"))
    input.from(tasks.named("distCompilerJars"))
    input.from(tasks.named("distKonanProperties"))

    output = nativeDistribution.map { it.compilerFingerprint }

    finalizedBy("distInvalidateStaleCaches") // if the fingerprint has updated, some caches may need to be invalidated
}

val distTools by tasks.registering(Sync::class) {
    from(project(":kotlin-native:llvmDebugInfoC").file("src/scripts/konan_lldb.py"))
    from(project(":kotlin-native:utilities").file("env_blacklist"))

    into(nativeDistribution.map { it.tools })
}

val distBin by tasks.registering(Sync::class) {
    from(file("cmd")) {
        filePermissions {
            unix("0755")
        }
        if (!PlatformInfo.isWindows()) {
            exclude("**/*.bat")
        }
    }

    into(nativeDistribution.map { it.bin })
}

val distSwiftExport by tasks.registering(Sync::class) {
    from(configurations.named("objcExportApi")) {
        into("kotlin_runtime")
        filePermissions {
            unix("0644")
        }
    }
    into(nativeDistribution.map { it.swiftExport })
}

val distKonanPlatforms by tasks.registering(Sync::class) {
    from(NativeProtoDistributionKt.getNativeProtoDistribution(project).konanPlatforms)
    into(nativeDistribution.map { it.konanPlatforms })
}

val distKonanProperties by tasks.registering(PrepareKonanProperties::class) {
    input = NativeProtoDistributionKt.getNativeProtoDistribution(project).konanProperties
    output = nativeDistribution.map { it.konanProperties }
    compilerVersion = project.property("kotlinVersion") as String
    llvmVariants.put(HostManager.host, LLVMDistributionKind.ESSENTIALS)
    llvmProperties.set(LLVMDistributionSourceKt.getAsProperties(LLVMDistributionSourceKt.getLlvmDistributionSource(project)))
}

tasks.register("crossDistRuntime") {
    dependsOn(*targetList.map { "${it}CrossDistRuntime" }.toTypedArray())
}

tasks.register("crossDistPlatformLibs") {
    dependsOn(*targetList.map { "${it}PlatformLibs" }.toTypedArray())
}

tasks.register("crossDistStdlibCache") {
    dependsOn(*targetList.filter { it in cacheableTargetNames }.map { "${it}StdlibCache" }.toTypedArray())
    // Make sure any tasks depending on this one see cleaned-up distribution.
    mustRunAfter("distInvalidateStaleCaches")
}

targetList.forEach { target ->
    tasks.register("${target}CrossDistBitcodeCopy", Sync::class) {
        val bitcodeFiles = configurations.getByName("runtimeBitcode").incoming.artifactView {
            attributes {
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer(platformManager.targetByName(target), null))
            }
        }.files

        into(nativeDistribution.map { it.runtime(target) })

        from(bitcodeFiles) {
            include("*.bc")
        }
    }

    tasks.register("${target}CrossDistRuntimeFingerprint", PrepareDistributionFingerprint::class) {
        input.from(tasks.named("${target}CrossDistBitcodeCopy"))
        output = nativeDistribution.map { it.runtimeFingerprint(target) }

        finalizedBy("distInvalidateStaleCaches") // if the fingerprint has updated, some caches may need to be invalidated
    }

    tasks.register("${target}CrossDistRuntime") {
        dependsOn(":kotlin-native:distStdlib")
        dependsOn("${target}CrossDistBitcodeCopy")
        dependsOn("${target}CrossDistRuntimeFingerprint")
    }

    tasks.register("${target}PlatformLibs") {
        dependsOn(":kotlin-native:platformLibs:${target}Install")
        if (target in cacheableTargetNames) {
            dependsOn(":kotlin-native:platformLibs:${target}Cache")
        }
        // Make sure any tasks depending on this one see cleaned-up distribution.
        mustRunAfter("distInvalidateStaleCaches")
    }

    if (target in cacheableTargetNames) {
        tasks.register("${target}StdlibCache", Sync::class) {
            dependsOn("distStdlib")
            dependsOn(":kotlin-native:runtime:${target}StdlibCache")
            // Make sure the cache clean-up has happened, so this task can safely write into the shared cache folder
            mustRunAfter("distInvalidateStaleCaches")

            into(nativeDistribution.map { it.cache("stdlib", target) })

            from(project(":kotlin-native:runtime").layout.buildDirectory.dir("cache/$target/$target-gSTATIC-system/stdlib-cache")) {
                include("**")
            }
        }
    }

    tasks.register("${target}CrossDist") {
        dependsOn("${target}CrossDistRuntime", "distCompiler")
        if (target in cacheableTargetNames) {
            dependsOn("${target}StdlibCache")
        }
        // Make sure any tasks depending on this one see cleaned-up distribution.
        mustRunAfter("distInvalidateStaleCaches")
    }
}

tasks.register("distPlatformLibs") {
    dependsOn("${hostName}PlatformLibs")
}

tasks.register("dist") {
    dependsOn(
        "distCompiler",
        "distRuntime",
        "distDef",
        "distStdlib",
        "distStdlibCache"
    )
}

tasks.register("crossDist") {
    dependsOn(
        "distCompiler",
        "crossDistRuntime",
        "distDef",
        "distStdlib",
        "crossDistStdlibCache"
    )
}

val distInvalidateStaleCaches by tasks.registering(InvalidateStaleCaches::class) {
    distributionRoot.set(nativeDistribution.map { it.root })
    // If the build graph includes any of the fingerprint updates, this task must run after them.
    // However, it shouldn't force any of the fingerprints to be computed (e.g. when building :kotlin-native:dist
    // this task should run after compiler and host runtime fingerprint updates, but shouldn't force fingerprint from an iOS simulator
    // to be computed)
    mustRunAfter("distCompilerFingerprint")
    targetList.forEach { target ->
        mustRunAfter("${target}CrossDistRuntimeFingerprint")
    }
}

tasks.register("bundle") {
    dependsOn("bundleRegular", "bundlePrebuilt")
}

val sbomBundleRegular = SbomKt.configureSbom(project, "BundleRegular", "Kotlin/Native bundle", emptySet(), null)

val sbomBundleRegularForPublish by tasks.registering(Copy::class) {
    dependsOn(sbomBundleRegular)
    destinationDir = file("$buildDir/spdx/regular")
    from(sbomBundleRegular) {
        rename(".*", "kotlin-native-${HostManager.platformName()}-${project.property("kotlinVersion")}.spdx.json")
    }
}

val bundleRegular by tasks.registering(if (PlatformInfo.isWindows()) Zip::class else Tar::class) {
    dependsOn(sbomBundleRegularForPublish)
    val simpleOsName = HostManager.platformName()
    val kotlinVersion = project.property("kotlinVersion") as String
    archiveBaseName.set("kotlin-native-$simpleOsName")
    archiveVersion.set(kotlinVersion)
    from(nativeDistribution.map { it.root }) {
        include("**")
        exclude("dependencies")
        exclude("klib/testLibrary")
        // Don't include platform libraries into the bundle (generate them at the user side instead).
        exclude("klib/platform")
        // Exclude platform libraries caches too. Keep caches for stdlib.
        exclude("klib/cache/*/org.jetbrains.kotlin.native.platform.*/**")
        into("${archiveBaseName.get()}-${archiveVersion.get()}")
    }
}

val sbomBundlePrebuilt = SbomKt.configureSbom(
    project,
    "BundlePrebuilt",
    "Kotlin/Native bundle (prebuilt platform libs)",
    emptySet(),
    null
)

val sbomBundlePrebuiltForPublish by tasks.registering(Copy::class) {
    dependsOn(sbomBundlePrebuilt)
    destinationDir = file("$buildDir/spdx/prebuilt")
    from(sbomBundlePrebuilt) {
        rename(".*", "kotlin-native-prebuilt-${HostManager.platformName()}-${project.property("kotlinVersion")}.spdx.json")
    }
}

val mergeCrossBundleTask = NativeFullCrossDistKt.setupMergeCrossBundleTask(project)

val bundlePrebuilt by tasks.registering(if (PlatformInfo.isWindows()) Zip::class else Tar::class) {
    dependsOn(sbomBundlePrebuiltForPublish)
    if (mergeCrossBundleTask == null) {
        dependsOn("crossDistPlatformLibs")
    } else {
        dependsOn(mergeCrossBundleTask)
    }

    val simpleOsName = HostManager.platformName()
    val kotlinVersion = project.property("kotlinVersion") as String
    archiveBaseName.set("kotlin-native-prebuilt-$simpleOsName")
    archiveVersion.set(kotlinVersion)
    from(nativeDistribution.map { it.root }) {
        include("**")
        exclude("dependencies")
        exclude("klib/testLibrary")
        into("${archiveBaseName.get()}-${archiveVersion.get()}")
    }
}

fun configurePackingLicensesToBundle(task: AbstractArchiveTask, containsPlatformLibraries: Boolean, crossBundleEnabled: Boolean) {
    task.from(projectDir) {
        include("licenses/**")
        val hasXcodeLibraries = PlatformInfo.isMac() || crossBundleEnabled
        if (!containsPlatformLibraries || !hasXcodeLibraries) {
            exclude("**/xcode_license.pdf")
        }
        if (!containsPlatformLibraries) {
            exclude("**/mingw-w64-headers_LICENSE.txt")
        }
        into("${task.archiveBaseName.get()}-${task.archiveVersion.get()}")
    }

    task.from(rootProject.file("license")) {
        into("${task.archiveBaseName.get()}-${task.archiveVersion.get()}/licenses")
    }
}

tasks.named<AbstractArchiveTask>("bundleRegular") {
    configurePackingLicensesToBundle(
        this,
        containsPlatformLibraries = false,
        crossBundleEnabled = mergeCrossBundleTask != null
    )
}
tasks.named<AbstractArchiveTask>("bundlePrebuilt") {
    configurePackingLicensesToBundle(
        this,
        containsPlatformLibraries = true,
        crossBundleEnabled = mergeCrossBundleTask != null
    )
}

listOf(bundleRegular, bundlePrebuilt).forEach {
    it.configure {
        if (mergeCrossBundleTask == null) {
            dependsOn("crossDist")
            dependsOn("crossDistStdlibCache")
            dependsOn("distSources")
            dependsOn("distDef")
        } else {
            dependsOn(mergeCrossBundleTask)
        }

        // Bundle tasks read the entire contents of the Native distribution. Make sure this happens after invalid stale caches are removed
        mustRunAfter("distInvalidateStaleCaches")

        destinationDirectory.set(file("."))

        if (PlatformInfo.isWindows()) {
            (this as Zip).isZip64 = true
        } else {
            archiveExtension.set("tar.gz")
            (this as Tar).compression = Compression.GZIP
        }

        // Calculating SHA-256 checksums for bundle artifacts
        val archiveExtension = if (PlatformInfo.isWindows()) "zip" else "tar.gz"
        val checksumFile = archiveBaseName.zip(archiveVersion) { name, version ->
            file("${layout.buildDirectory.get()}/${name}-${version}.${archiveExtension}.sha256")
        }
        outputs.file(checksumFile).withPropertyName("checksumFile")

        // If `bundleRegular`/`bundlePrebuilt` tasks are executed with CC enabled, you will get a
        // "Couldn't find method 'calculateChecksum' ... " error. This most likely happens due to bug/issue on Gradle side, somewhere around
        // capturing mechanisms in doLast-lambda and CC's serialization/deserialization mechanism.
        //
        // Issue can be workarounded by converting this file to build.gradle.kts, but it's quite an invasive change, and there's no strict
        // need for it. Indeed, normally you can't use `bundleRegular/`bundlePrebuilt` tasks with CC because they depend transitively on
        // `KonanCompileLibraryTask` / `KonanCacheTask` (transitively via `crossDist`), which are inherently not compatible with CC
        //
        // Q: If `bundle*`-tasks depend on CC-incompatible tasks anyways, why we need this explicit call?
        // A: When we're building a cross-dist on CI, we don't depend on 'crossDist' and other tasks, but just reuse the dist built in this
        // build chain, via `mergeCrossBundleTask`. In such case, there won't be any CC-incompatible tasks in execution plan, and by default,
        // Gradle will try using CC, leading to aforementioned "Couldn't find method"-issue. To avoid that, we additionally mark `bundle*`
        // tasks as notCompatibleWithConfigurationCache.
        notCompatibleWithConfigurationCache("Groovy script evaluation issue workaround")

        doLast {
            val bundleFile = archiveFile.get().asFile
            if (bundleFile.exists()) {
                val checksum = calculateChecksum(bundleFile, "SHA-256")
                checksumFile.get().writeText(checksum)
            }
        }
    }
}

fun calculateChecksum(file: File, algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm)
    file.inputStream().use { fis ->
        val byteArray = ByteArray(1024)
        var bytesCount: Int
        while (fis.read(byteArray).also { bytesCount = it } != -1) {
            digest.update(byteArray, 0, bytesCount)
        }
    }
    val bytes = digest.digest()

    return bytes.joinToString("") {
        String.format("%02x", it)
    }
}

val tcDist by tasks.registering(if (PlatformInfo.isWindows()) Zip::class else Tar::class) {
    dependsOn("dist")
    dependsOn("distSources")
    val simpleOsName = HostManager.platformName()
    val kotlinVersion = project.property("kotlinVersion") as String
    archiveBaseName.set("kotlin-native-dist-$simpleOsName")
    archiveVersion.set(kotlinVersion)
    from(nativeDistribution) {
        include("**")
        exclude("dependencies")
        into("${archiveBaseName.get()}-${archiveVersion.get()}")
    }

    destinationDirectory.set(file("."))

    if (PlatformInfo.isWindows()) {
        (this as Zip).isZip64 = true
    } else {
        archiveExtension.set("tar.gz")
        (this as Tar).compression = Compression.GZIP
    }
}

tasks.register("samples") {
    dependsOn("samplesZip", "samplesTar")
}

val samplesZip by tasks.registering(Zip::class)
val samplesTar by tasks.registering(Tar::class) {
    archiveExtension.set("tar.gz")
    compression = Compression.GZIP
}

listOf(samplesZip, samplesTar).forEach {
    it.configure {
        val kotlinVersion = project.property("kotlinVersion") as String
        archiveBaseName.set("kotlin-native-samples-$kotlinVersion")
        destinationDirectory.set(projectDir)
        into(archiveBaseName)

        from(file("samples")) {
            // Process properties files separately.
            exclude("**/gradle.properties")
        }

        from(projectDir) {
            include("licenses/**")
        }

        from(file("samples")) {
            include("**/gradle.properties")
            filter { line ->
                if (line.startsWith("org.jetbrains.kotlin.native.home=") ||
                    line.startsWith("# Use custom Kotlin/Native home:")) null else line
            }
            filter(org.apache.tools.ant.filters.FixCrLfFilter::class, "eol" to org.apache.tools.ant.filters.FixCrLfFilter.CrLf.newInstance("lf"))
        }

        // Exclude build artifacts.
        exclude("**/build")
        exclude("**/.gradle")
        exclude("**/.idea")
        exclude("**/*.kt.bc-build/")
    }
}

tasks.register("copy_samples") {
    dependsOn("copySamples")
}
val copySamples by tasks.registering(CopySamples::class) {
    destinationDir = file("build/samples-under-test")
}

configure<org.jetbrains.kotlin.cpp.CompilationDatabaseExtension> {
    allTargets()
}

// TODO: Replace with a more convenient user-facing task that can build for a specific target.
//       like compilationDatabase with optional argument --target.
val compdb by tasks.registering(Copy::class) {
    val compilationDatabaseExt = project.extensions.getByType<org.jetbrains.kotlin.cpp.CompilationDatabaseExtension>()
    from(compilationDatabaseExt.hostTarget.task)
    into(layout.projectDirectory)

    group = org.jetbrains.kotlin.cpp.CompilationDatabasePlugin.TASK_GROUP
    description = "Copy host compilation database to kotlin-native/"
}

targetList.forEach { targetName ->
    CompareDistributionSignatures.registerForPlatform(project, targetName)
}

CompareDistributionSignatures.registerForStdlib(project)

// FIXME: should be a part of Host/TargetManager
fun platformName(target: KonanTarget): String {
    return when (target) {
        KonanTarget.LINUX_X64 -> "linux-x86_64"
        KonanTarget.MACOS_X64 -> "macos-x86_64"
        KonanTarget.MACOS_ARM64 -> "macos-aarch64"
        KonanTarget.MINGW_X64 -> "windows-x86_64"
        else -> throw TargetSupportException("Unknown host target")
    }
}

fun createConfigurations(bundles: List<File>): Map<KonanTarget, File> {
    val hostTargets = platformManager.enabledByHost.keys
    val result = hostTargets.associateWith { target ->
        bundles.find { it.name.contains(platformName(target)) }
    }.filterValues { it != null } as Map<KonanTarget, File>

    val missingBundles = hostTargets - result.keys
    if (missingBundles.isNotEmpty()) {
        println("Some of the archive bundles are missing for targets $missingBundles:")
        println(result)
        throw IllegalArgumentException("Bundle archives are missing for $missingBundles")
    }
    val kotlinVersion = project.property("kotlinVersion") as String
    result.forEach { (_, file) ->
        if (!file.name.contains(kotlinVersion)) {
            throw IllegalArgumentException("Incorrect version specified for the publish: ${file.name}")
        }
    }
    return result
}

val bundlesLocationFiles = UtilsKt.getNativeBundlesLocation(project)
    .listFiles()
    ?.toList() ?: emptyList()

KotlinBuildPublishingPluginKt.configureDefaultPublishing(
    /* receiver = */ project,
    /* signingRequired = */ KotlinBuildPublishingPluginKt.getSignLibraryPublication(project)
)

tasks.named<Delete>("clean") {
    dependsOn(subprojects.map { it.tasks.matching { task -> task.name == "clean" } })
    delete(nativeDistribution.map { it.root })
    delete(layout.buildDirectory)
    delete(bundleRegular.get().outputs.files)
    delete("${projectDir}/compile_commands.json")
    delete(rootProject.file("test.output").absolutePath) // Clean up after legacy test infrastructure
}

publishing {
    publications {
        val publishBundlesFromLocation = UtilsKt.getNativeBundlesLocation(project) != projectDir
        val kotlinVersion = project.property("kotlinVersion") as String

        create<MavenPublication>("Bundle") {
            groupId = project.group.toString()
            artifactId = project.name
            version = kotlinVersion

            if (publishBundlesFromLocation) {
                val bundleArchives = bundlesLocationFiles
                    .filter {
                        it.name.startsWith("kotlin-native") &&
                        !it.name.contains("prebuilt") &&
                        (it.name.endsWith("zip") || it.name.endsWith("tar.gz"))
                    }
                val bundleConfigs = createConfigurations(bundleArchives)
                bundleConfigs.forEach { (target, file) ->
                    val archiveExtension = if (target.family == Family.MINGW) "zip" else "tar.gz"
                    artifact(file) {
                        classifier = platformName(target)
                        extension = archiveExtension
                    }
                    artifact("${UtilsKt.getNativeBundlesLocation(project)}/kotlin-native-${platformName(target)}-${kotlinVersion}.spdx.json") {
                        classifier = platformName(target)
                        extension = "spdx.json"
                    }
                }
            } else {
                artifact(bundleRegular) {
                    classifier = HostManager.platformName()
                    extension = if (PlatformInfo.isWindows()) "zip" else "tar.gz"
                }
                artifact(sbomBundleRegular) {
                    classifier = HostManager.platformName()
                    extension = "spdx.json"
                }
            }

            KotlinBuildPublishingPluginKt.configureKotlinPomAttributes(
                /* receiver = */ this,
                /* project = */ project,
                /* explicitDescription = */ "Kotlin/Native bundle",
                /* packaging = */ "pom",
                /* explicitName = */ null
            )
        }
        create<MavenPublication>("BundlePrebuilt") {
            groupId = project.group.toString()
            artifactId = project.name + "-prebuilt"
            version = kotlinVersion

            if (publishBundlesFromLocation) {
                val prebuiltBundleArchives = bundlesLocationFiles
                    .filter {
                        it.name.startsWith("kotlin-native-prebuilt") &&
                        (it.name.endsWith("zip") || it.name.endsWith("tar.gz"))
                    }
                val bundlePrebuiltConfigs = createConfigurations(prebuiltBundleArchives)
                bundlePrebuiltConfigs.forEach { (target, file) ->
                    val archiveExtension = if (target.family == Family.MINGW) "zip" else "tar.gz"
                    artifact(file) {
                        classifier = platformName(target)
                        extension = archiveExtension
                    }
                    artifact("${UtilsKt.getNativeBundlesLocation(project)}/kotlin-native-prebuilt-${platformName(target)}-${kotlinVersion}.spdx.json") {
                        classifier = platformName(target)
                        extension = "spdx.json"
                    }
                }
            } else {
                artifact(bundlePrebuilt) {
                    classifier = HostManager.platformName()
                    extension = if (PlatformInfo.isWindows()) "zip" else "tar.gz"
                }
                artifact(sbomBundlePrebuilt) {
                    classifier = HostManager.platformName()
                    extension = "spdx.json"
                }
            }
            KotlinBuildPublishingPluginKt.configureKotlinPomAttributes(
                /* receiver = */ this,
                /* project = */ project,
                /* explicitDescription = */ "Kotlin/Native bundle (prebuilt platform libs)",
                /* packaging = */ "pom",
                /* explicitName = */ null
            )
        }
    }
}
