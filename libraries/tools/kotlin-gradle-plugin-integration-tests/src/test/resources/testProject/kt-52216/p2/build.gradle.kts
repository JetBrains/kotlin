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
        val commonMain by getting {
            dependencies {
                implementation("kt52216:lib:1.0")
            }
        }

        for (target in targets) {
            val leafSourceSet = getByName(target.leafSourceSetName)
            create(target.intermediateSourceSetName) {
                leafSourceSet.dependsOn(this)
                dependsOn(commonMain)
            }
        }
    }
}

val org.jetbrains.kotlin.gradle.plugin.KotlinTarget.leafSourceSetName: String
    get() = "${name}Main"

val org.jetbrains.kotlin.gradle.plugin.KotlinTarget.intermediateSourceSetName: String
    get() = "${name}Intermediate"
