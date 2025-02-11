import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnSetupTask

plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin.js {
    nodejs()
}

tasks {
    val yarnSpec = project.the<YarnRootEnvSpec>()
    val yarnFolderRemove by registering {
        doLast {
            yarnSpec.installationDirectory.get().asFile.deleteRecursively()
        }
    }

    val yarnFolderCheck by registering {
        dependsOn(getByName("kotlinYarnSetup"))

        doLast {
            if (!yarnSpec.installationDirectory.get().asFile.exists()) {
                throw GradleException()
            }
        }
    }

    val yarnConcreteVersionFolderChecker by registering {
        dependsOn(getByName("kotlinYarnSetup"))

        doLast {
            if (!yarnSpec.installationDirectory.get().file("yarn-v1.9.3").asFile.exists()) {
                throw GradleException()
            }
        }
    }
}