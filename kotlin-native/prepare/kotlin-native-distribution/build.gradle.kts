import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.konan.target.enabledTargets
import org.jetbrains.kotlin.konan.target.withSanitizer
import org.jetbrains.kotlin.nativeDistribution.LLVMDistributionKind
import org.jetbrains.kotlin.nativeDistribution.PrepareDistributionFingerprint
import org.jetbrains.kotlin.nativeDistribution.PrepareKonanProperties
import org.jetbrains.kotlin.nativeDistribution.asProperties
import org.jetbrains.kotlin.nativeDistribution.llvmDistributionSource
import org.jetbrains.kotlin.nativeDistribution.nativeDistribution
import org.jetbrains.kotlin.nativeDistribution.nativeProtoDistribution
import org.jetbrains.kotlin.platformManager
import org.jetbrains.kotlin.utils.capitalized

plugins {
    java
    id("platform-manager")
}

val kotlinVersion: String by rootProject.extra
val kotlinNativeRoot = project(":kotlin-native").isolated.projectDirectory

val distPack by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

val embeddableJar by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

val nativeLibs by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_RUNTIME))
    }
}

val objCExportApi by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(CppUsage.API))
    }
}

val stdlib by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val runtimeBitcode by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(CppUsage.LLVM_BITCODE))
    }
}

val stdlibCache by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-compilation-cache"))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

val platformLibs by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-platform-libs"))
    }
}

val platformLibsCaches by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-platform-libs-caches"))
    }
}

dependencies {
    distPack(project(":kotlin-native:utilities:cli-runner"))
    embeddableJar(project(":kotlin-native:prepare:kotlin-native-compiler-embeddable"))
    nativeLibs(project(":kotlin-native:common:env"))
    nativeLibs(project(":kotlin-native:common:files"))
    nativeLibs(project(":kotlin-native:llvmInterop"))
    nativeLibs(project(":kotlin-native:libclangInterop"))
    nativeLibs(project(":kotlin-native:Interop:Runtime"))
    objCExportApi(project(":kotlin-native:runtime"))
    runtimeBitcode(project(":kotlin-native:runtime"))
    stdlib(project(":kotlin-native:runtime"))
    stdlibCache(project(":kotlin-native:runtime"))
    platformLibs(project(":kotlin-native:platformLibs"))
    platformLibsCaches(project(":kotlin-native:platformLibs"))
}

val shadowJar by tasks.registering(ShadowJar::class) {
    mergeServiceFiles()
    destinationDirectory.set(layout.buildDirectory)
    archiveBaseName.set("kotlin-native")
    archiveVersion.set("")
    archiveClassifier.set("")
    exclude("META-INF/versions/9/module-info.class")
    configurations = listOf(distPack)
}

val distNativeLibs by tasks.registering(Sync::class) {
    from(nativeLibs)
    into(nativeDistribution.map { it.nativeLibs })
}

val distCompilerJars by tasks.registering(Sync::class) {
    from(shadowJar.map { it.archiveFile })
    from(embeddableJar) {
        rename {
            "kotlin-native-compiler-embeddable.jar"
        }
        filePermissions {
            unix("0644")
        }
    }

    into(nativeDistribution.map { it.compilerJars })
}

val distTools by tasks.registering(Sync::class) {
    from(kotlinNativeRoot.file("llvmDebugInfoC/src/scripts/konan_lldb.py"))
    from(kotlinNativeRoot.file("utilities/env_blacklist"))

    into(nativeDistribution.map { it.tools })
}

val distBin by tasks.registering(Sync::class) {
    from(kotlinNativeRoot.file("cmd")) {
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
    from(objCExportApi) {
        into("kotlin_runtime")
        filePermissions {
            unix("0644")
        }
    }
    into(nativeDistribution.map { it.swiftExport })
}

val distKonanProperties by tasks.registering(PrepareKonanProperties::class) {
    input = nativeProtoDistribution.konanProperties
    output = nativeDistribution.map { it.konanProperties }
    compilerVersion = kotlinVersion
    llvmVariants.put(HostManager.host, LLVMDistributionKind.ESSENTIALS)
    llvmProperties.set(llvmDistributionSource.asProperties)
}

val distCompilerComponents = listOf(distNativeLibs, distCompilerJars, distTools, distBin, distSwiftExport, distKonanProperties)

val distCompilerFingerprint by tasks.registering(PrepareDistributionFingerprint::class) {
    input.from(distCompilerComponents)
    output = nativeDistribution.map { it.compilerFingerprint }
}

val distCompilerElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    description = "Native Distribution: compiler component"
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-distribution-component"))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("compiler"))
    }
}

distCompilerElements.outgoing {
    distCompilerComponents.forEach {
        artifact(it)
    }
    artifact(distCompilerFingerprint)
}

val distStdlib by tasks.registering(Sync::class) {
    from(stdlib)
    into(nativeDistribution.map { it.stdlib })
}

val distStdlibElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    description = "Native Distribution: stdlib component"
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-distribution-component"))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("stdlib"))
    }
}

distStdlibElements.outgoing {
    artifact(distStdlib)
}

val distDef by tasks.registering(Sync::class) {
    dependsOn(":kotlin-native:platformLibs:updateDefFileDependencies")

    enabledTargets(platformManager).forEach { target ->
        from(kotlinNativeRoot.file("platformLibs/src/platform/${target.family.name.lowercase()}")) {
            into(target.name)
            include("**/*.def")
        }
    }

    into(nativeDistribution.map { it.platformLibsDefinitions })
}

val distRuntimeElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    description = "Native Distribution: runtime component"
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-distribution-component"))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("runtime"))
    }
}

val distRuntimeElementsForAllTargets = distRuntimeElements.outgoing.variants.create("all")

// TODO: Export this as configuration
val crossDistRuntime by tasks.registering

// TODO: Export this as configuration
val distRuntime by tasks.registering

// TODO: Export this as configuration
val crossDist by tasks.registering

// TODO: Export this as configuration
val dist by tasks.registering

// TODO: Export this as configuration
val crossDistPlatformLibs by tasks.registering

// TODO: Export this as configuration
val distPlatformLibs by tasks.registering

enabledTargets(platformManager).forEach { target ->
    val crossDistBitcodeCopyForTarget = tasks.register<Sync>("crossDistBitcodeCopy${target.name.capitalized}") {
        from(runtimeBitcode.incoming.artifactView {
            attributes {
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target.withSanitizer())
            }
        }.files)
        into(nativeDistribution.map { it.runtime(target.name) })
    }

    val crossDistRuntimeFingerprintForTarget = tasks.register<PrepareDistributionFingerprint>("crossDistRuntimeFingerprint${target.name.capitalized}") {
        input.from(crossDistBitcodeCopyForTarget)
        output = nativeDistribution.map { it.runtimeFingerprint(target.name) }
    }

    // TODO: Export this as configuration
    val crossDistRuntimeForTarget = tasks.register("crossDistRuntime${target.name.capitalized}") {
        dependsOn(distStdlib)
        dependsOn(crossDistBitcodeCopyForTarget)
        dependsOn(crossDistRuntimeFingerprintForTarget)
    }
    crossDistRuntime.configure {
        dependsOn(crossDistRuntimeForTarget)
    }
    if (HostManager.host == target) {
        distRuntime.configure {
            dependsOn(crossDistRuntimeForTarget)
        }
    }

    val distRuntimeElementsForTarget = distRuntimeElements.outgoing.variants.create(target.name) {
        attributes {
            attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target.withSanitizer())
        }
    }
    listOf(distRuntimeElementsForTarget, distRuntimeElementsForAllTargets).forEach {
        it.artifact(crossDistBitcodeCopyForTarget)
        it.artifact(crossDistRuntimeFingerprintForTarget)
    }

    val stdlibCacheForTarget = if (target.name in platformManager.hostPlatform.cacheableTargets) {
        tasks.register<Sync>("stdlibCache${target.name.capitalized}") {
            from(stdlibCache.incoming.artifactView {
                attributes {
                    attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target.withSanitizer())
                }
            }.files)
            into(nativeDistribution.map { it.stdlibCache(target.name) })
        }
    } else null

    // TODO: Export this as configuration
    val crossDistForTarget = tasks.register("crossDist${target.name.capitalized}") {
        dependsOn(distCompilerComponents)
        dependsOn(distCompilerFingerprint)
        dependsOn(crossDistRuntimeForTarget)
        dependsOn(distDef)
        dependsOn(distStdlib)
        stdlibCacheForTarget?.let { dependsOn(it) }
    }
    crossDist.configure {
        dependsOn(crossDistForTarget)
    }
    if (HostManager.host == target) {
        dist.configure {
            dependsOn(crossDistForTarget)
        }
    }

    val installPlatformLibs = tasks.register<Sync>("installPlatformLibs${target.name.capitalized}") {
        from(platformLibs.incoming.artifactView {
            attributes {
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target.withSanitizer())
            }
        }.files)
        into(nativeDistribution.map { it.platformLibs(target.name) })
    }

    val platformLibsCacheForTarget = if (target.name in platformManager.hostPlatform.cacheableTargets) {
        tasks.register<Sync>("platformLibsCache${target.name.capitalized}") {
            from(platformLibsCaches.incoming.artifactView {
                attributes {
                    attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target.withSanitizer())
                }
            }.files)
            into(nativeDistribution.map { it.cache("", target.name).asFile.parentFile })
        }
    } else null

    // TODO: Export this as configuration
    val distPlatformLibsForTarget = tasks.register("distPlatformLibs${target.name.capitalized}") {
        dependsOn(installPlatformLibs)
        platformLibsCacheForTarget?.let { dependsOn(it) }
    }
    crossDistPlatformLibs.configure {
        dependsOn(distPlatformLibsForTarget)
    }
    if (HostManager.host == target) {
        distPlatformLibs.configure {
            dependsOn(distPlatformLibsForTarget)
        }
    }
}