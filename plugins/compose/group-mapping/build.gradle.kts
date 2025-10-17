plugins {
    kotlin("jvm")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
    implementation("org.ow2.asm:asm-tree:9.7")
}

base {
    archivesName = "compose-group-mapping"
}

description = "Generator of proguard mappings from Compose groups in bytecode"
publish {
    artifactId = "compose-group-mapping"
    pom {
        name.set("AndroidX Compose Group Mapping Generator")
        developers {
            developer {
                name.set("The Android Open Source Project")
            }
        }
    }
}

runtimeJar()
sourcesJar()
javadocJar()