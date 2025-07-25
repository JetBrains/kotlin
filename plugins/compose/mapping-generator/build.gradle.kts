plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":kotlin-stdlib"))
    implementation("org.ow2.asm:asm-tree:9.7")
}

base {
    archivesName = "compose-mapping-generator"
}

publish {
    artifactId = "compose-mapping-generator"
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