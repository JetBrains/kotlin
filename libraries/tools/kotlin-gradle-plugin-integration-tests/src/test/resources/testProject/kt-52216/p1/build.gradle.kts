plugins {
    kotlin("multiplatform")
}

group = "me.user"
version = "1.0"

kotlin {
    val targets = listOf(
        jvm(),
        js(),
        linuxX64()
    )

    sourceSets {
        val commonMain by getting

        for (target in targets) {
            getByName(target.leafSourceSetName) {
                dependsOn(commonMain)
                dependencies {
                    implementation("kt52216:lib:1.0")
                }
            }
        }
    }
}

val org.jetbrains.kotlin.gradle.plugin.KotlinTarget.leafSourceSetName: String
    get() = "${name}Main"
