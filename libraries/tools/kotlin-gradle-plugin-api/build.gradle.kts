plugins {
    id("gradle-plugin-dependency-configuration")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

configureDokkaPublication(
    shouldLinkGradleApi = true,
    configurePublishingToKotlinlang = true,
)

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-annotations"))
    commonApi(project(":native:kotlin-native-utils"))
    commonApi(project(":kotlin-tooling-core"))

    commonCompileOnly(project(":kotlin-gradle-compiler-types"))
    commonCompileOnly("com.android.tools.build:gradle-api:4.2.2") {
        // Without it - Gradle dependency resolution fails with unexpected error
        // Caused by: java.lang.IllegalStateException: Unexpected parent dependency id 131. Seen ids: [129, 2, 130, 9, 10, 138, 11, 139, 140, 14, 153, 154, 155, 156, 157, 158, 161, 164, 177, 178, 51, 179, 52, 180, 53, 54, 55, 183, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 195, 68, 200, 201, 202, 203, 206, 211, 212, 215, 222, 223, 224, 231, 232, 105, 233, 106, 107, 108, 109, 110, 111, 112, 113, 114, 242, 115, 243, 116, 244, 117, 118, 119, 120, 121, 122]
        //        at org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.TransientConfigurationResultsBuilder.deserialize(TransientConfigurationResultsBuilder.java:171)
        //        at org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.TransientConfigurationResultsBuilder.lambda$load$5(TransientConfigurationResultsBuilder.java:117)
        // Could be reproduced by running `:kotlin-gradle-plugin-api:gPFFPMP` task
        isTransitive = false
    }

    embedded(project(":kotlin-gradle-compiler-types")) { isTransitive = false }
}

apiValidation {
    nonPublicMarkers += "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
}

tasks {
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}
