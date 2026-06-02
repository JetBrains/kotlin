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
    val yarnFolderRemove = register("yarnFolderRemove") {
        val installationDirectory = yarnSpec.installationDirectory
        doLast {
            installationDirectory.get().asFile.deleteRecursively()
        }
    }

    val yarnFolderCheck = register("yarnFolderCheck") {
        dependsOn(getByName("kotlinYarnSetup"))
        val installationDirectory = yarnSpec.installationDirectory

        doLast {
            if (!installationDirectory.get().asFile.exists()) {
                throw GradleException()
            }
        }
    }

    val yarnConcreteVersionFolderChecker = register("yarnConcreteVersionFolderChecker") {
        dependsOn(getByName("kotlinYarnSetup"))
        val installationDirectory = yarnSpec.installationDirectory

        doLast {
            if (!installationDirectory.get().file("yarn-v1.9.3").asFile.exists()) {
                throw GradleException()
            }
        }
    }
}