plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
}

description = "Kotlin/Native utils"

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
    api(project(":kotlin-util-io"))
    api(project(":kotlin-util-klib"))
    api(platform(project(":kotlin-gradle-plugins-bom")))

    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesApi(testFixtures(project(":kotlin-util-klib")))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

configureKotlinCompileTasksGradleCompatibility()

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
        testInputsCheck {
            extraPermissions.addAll(
                // CurrentXcodeTest executes xcrun to query the Xcode version
                """permission java.io.FilePermission "/usr/bin/xcrun", "execute";""",
                // CurrentXcodeTest.bundleVersion invokes PlistBuddy via bash
                """permission java.io.FilePermission "/bin/bash", "execute";""",
                // HostManagerTest.hostManagerWorksInUnknownOs sets os.name to simulate other platforms
                """permission java.util.PropertyPermission "os.name", "write";""",
            )
        }
    }
}

publish()

runtimeJar {
    // JPMS can't handle the jar file name. Fix that by specifying a valid module name in the manifest:
    manifest.attributes["Automatic-Module-Name"] = "kotlin.native_utils"
    // See https://youtrack.jetbrains.com/issue/KT-72063.
}

sourcesJar()
javadocJar()
