import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(projectTests(":native:native.tests"))
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar {}

val latestReleasedCompiler = findProperty("kotlin.internal.native.test.releasedCompilerVersion") as String
val hostSpecificArtifact = "${HostManager.platformName()}@${if (HostManager.hostIsMingw) "zip" else "tar.gz"}"

val releasedCompiler: Configuration by configurations.creating {
    dependencies {
        // declared to be included in verification-metadata.xml
        implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:$latestReleasedCompiler:macos-aarch64@tar.gz")
        implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:$latestReleasedCompiler:macos-x86_64@tar.gz")
        implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:$latestReleasedCompiler:linux-x86_64@tar.gz")
        implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:$latestReleasedCompiler:windows-x86_64@zip")
    }
    isTransitive = false
}

dependencies {
    releasedCompiler("org.jetbrains.kotlin:kotlin-native-prebuilt:$latestReleasedCompiler:$hostSpecificArtifact")
}

val releasedCompilerDist: TaskProvider<Sync> by tasks.registering(Sync::class) {
    val unarchive = { archive: File -> if (HostManager.hostIsMingw) zipTree(archive) else tarTree(archive) }
    from(unarchive(releasedCompiler.singleFile))
    into(layout.buildDirectory.dir("releaseCompiler$latestReleasedCompiler"))
}

val testTags = findProperty("kotlin.native.tests.tags")?.toString()
nativeTest(
    "test",
    testTags,
    releasedCompilerDist = releasedCompilerDist,
    maxMetaspaceSizeMb = 1024 // to handle two compilers in classloader
)