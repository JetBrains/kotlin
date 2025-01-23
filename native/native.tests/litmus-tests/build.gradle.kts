import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/plan/litmuskt")
    }
}

// WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
val nativeTargetName = HostManager.host.name

val litmusKt by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
    }
}

val litmusKtVersion = "0.1.2"
val litmusKtArtifacts = listOf(
    "org.jetbrains.litmuskt:litmuskt-core",
    "org.jetbrains.litmuskt:litmuskt-testsuite",
)

dependencies {
    litmusKtArtifacts.forEach { artifact ->
        litmusKt("${artifact}:${litmusKtVersion}")
        // needed for verification metadata
        listOf(
            KonanTarget.LINUX_ARM64,
            KonanTarget.LINUX_X64,
            KonanTarget.MACOS_ARM64,
            KonanTarget.MACOS_X64,
            KonanTarget.MINGW_X64,
        ).forEach { target ->
            // Cannot depend on `${artifact}:${litmusKtVersion}`, because its transitive dependency on
            // `${artifact}-${target}:${litmusKtVersion} forgets `org.gradle.usage` attribute.
            implicitDependencies("${artifact}-${target.name.replace("_", "")}:${litmusKtVersion}") {
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
                    attribute(KotlinNativeTarget.konanTargetAttribute, target.name)
                }
            }
        }
    }

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(projectTests(":native:native.tests"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

nativeTest(
    taskName = "test",
    tag = "litmuskt-native", // Include all tests with the "litmuskt-native" tag.
    requirePlatformLibs = true,
    customTestDependencies = listOf(litmusKt),
    allowParallelExecution = false,
)
