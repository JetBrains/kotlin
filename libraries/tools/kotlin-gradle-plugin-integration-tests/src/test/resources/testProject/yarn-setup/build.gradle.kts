import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

yarn

tasks {
    val yarnFolderRemove by registering {
        doLast {
            yarn.installationDir.deleteRecursively()
        }
    }

    val yarnFolderCheck by registering {
        dependsOn(getByName("kotlinYarnSetup"))

        doLast {
            if (!yarn.installationDir.exists()) {
                throw GradleException()
            }
        }
    }
}

kotlin.sourceSets {
    getByName("main") {
        dependencies {
            implementation(kotlin("stdlib-js"))
        }
    }
}

kotlin.target {
    nodejs()
}