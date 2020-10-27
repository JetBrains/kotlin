/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import org.jetbrains.kotlin.konan.properties.KonanPropertiesLoader
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.presetName

/**
 * Creates an archive with LLVM distribution without files
 * that are not required for Kotlin/Native.
 *
 * For example, it excludes binaries that are part of LLVM testing infrastructure.
 */
inline fun <reified T: AbstractArchiveTask> createLlvmPackingTask(
        host: KonanTarget,
        whitelist: File?,
        blacklist: File?,
        crossinline additionalConfiguration: T.() -> Unit
) {
    val distribution = Distribution(rootProject.projectDir.absolutePath)
    val reducedLlvmAppendix = distribution.properties
            .getProperty("reducedLlvmAppendix")
    val propertiesLoader = object : KonanPropertiesLoader(
            host = host,
            target = host,
            properties = distribution.properties,
            baseDir = DependencyProcessor.defaultDependenciesRoot.absolutePath
    ) {}

    val hostName = host.presetName.capitalize()
    val downloadTaskName = "downloadLlvmFor$hostName"

    tasks.create(downloadTaskName) {
        doFirst {
            propertiesLoader.downloadDependencies()
        }
    }
    tasks.create<T>("packLlvmFor$hostName") {
        dependsOn(downloadTaskName)
        additionalConfiguration()

        description = "Packs LLVM dependency for $hostName into archive"
        group = "Distribution packing"

        // > When both inclusion and exclusion are used,
        // > only files/directories that match at least one of the include patterns
        // > and don't match any of the exclude patterns are used.
        //
        // See: https://ant.apache.org/manual/dirtasks.html
        whitelist?.let {
            it.readLines()
                    .filter { !it.startsWith("#") && it.isNotBlank() }
                    .forEach(this::include)
        }
        blacklist?.let {
            it.readLines()
                    .filter { !it.startsWith("#") && it.isNotBlank() }
                    .forEach(this::exclude)
        }
        from(propertiesLoader.absoluteLlvmHome)
        destinationDirectory.set(buildDir.resolve("artifacts"))
        archiveBaseName.set(propertiesLoader.llvmHome)
        archiveAppendix.set(reducedLlvmAppendix)
    }
}

createLlvmPackingTask<Tar>(
        KonanTarget.MACOS_X64,
        whitelist = file("macos_llvm_whitelist"),
        blacklist = null
) {
    compression = Compression.GZIP
}

createLlvmPackingTask<Tar>(
        KonanTarget.LINUX_X64,
        whitelist = file("linux_llvm_whitelist"),
        blacklist = file("linux_llvm_blacklist")
) {
    compression = Compression.GZIP
}

createLlvmPackingTask<Zip>(
        KonanTarget.MINGW_X64,
        whitelist = file("mingw_llvm_whitelist"),
        blacklist = file("mingw_llvm_blacklist")
) {}