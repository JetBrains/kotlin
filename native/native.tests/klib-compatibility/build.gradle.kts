import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(testFixtures(project(":native:native.tests")))
}

sourceSets {
    "main" { none() }
    "test" { none() }
}

val customCompilerVersion = findProperty("kotlin.internal.native.test.compat.customCompilerVersion") as String
val hostSpecificArtifact = "${HostManager.platformName()}@${if (HostManager.hostIsMingw) "zip" else "tar.gz"}"

val customCompilerDist: Configuration by configurations.creating {
    dependencies {
        // declared to be included in verification-metadata.xml
        implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:$customCompilerVersion:macos-aarch64@tar.gz")
        implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:$customCompilerVersion:macos-x86_64@tar.gz")
        implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:$customCompilerVersion:linux-x86_64@tar.gz")
        implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:$customCompilerVersion:windows-x86_64@zip")
    }
    isTransitive = false
}

dependencies {
    customCompilerDist("org.jetbrains.kotlin:kotlin-native-prebuilt:$customCompilerVersion:$hostSpecificArtifact")
}

val downloadCustomCompilerDist: TaskProvider<Sync> by tasks.registering(Sync::class) {
    val unarchive = { archive: File -> if (HostManager.hostIsMingw) zipTree(archive) else tarTree(archive) }
    from(unarchive(customCompilerDist.singleFile))
    into(layout.buildDirectory.dir("customCompiler$customCompilerVersion"))
}
