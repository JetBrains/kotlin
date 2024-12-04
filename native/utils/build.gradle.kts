plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native utils"

dependencies {
    compileOnly(kotlinStdlib())
    api(project(":kotlin-util-io"))
    api(project(":kotlin-util-klib"))
    api(platform(project(":kotlin-gradle-plugins-bom")))

    testImplementation(libs.junit4)
    testImplementation(kotlinStdlib())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
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
