import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(testFixtures(project(":native:native.tests")))
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar {}

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

projectTests {
    nativeTestTask(
        "test",
        customCompilerDist = downloadCustomCompilerDist,
        maxMetaspaceSizeMb = 1024 // to handle two compilers in classloader
    ) {
        // To workaround KTI-2421, we make these tests run on JDK 11 instead of the project-default JDK 8.
        // Kotlin test infra uses reflection to access JDK internals.
        // With JDK 11, some JVM args are required to silence the warnings caused by that:
        jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
    }
}
