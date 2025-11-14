plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

description = "Kotlin/Native utils"

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
    api(project(":kotlin-util-io"))
    api(project(":kotlin-util-klib"))
    api(platform(project(":kotlin-gradle-plugins-bom")))

    testImplementation(libs.junit4)
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

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
