plugins {
    kotlin("jvm")
}

kotlin {
    explicitApi()
}

configureKotlinCompileTasksGradleCompatibility()

publish()

standardPublicJars()

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }
}
tasks.check {
    dependsOn(tasks.checkLegacyAbi)
}

dependencies {
    // remove stdlib dependency from api artifact in order not to affect the dependencies of the user project
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
}
