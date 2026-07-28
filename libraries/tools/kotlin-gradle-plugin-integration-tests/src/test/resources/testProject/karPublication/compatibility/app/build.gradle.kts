plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()

    sourceSets.linuxMain.dependencies {
        implementation("org.jetbrains.kotlin.kar.test:intermediate:1.0")
        implementation("org.jetbrains.kotlin.kar.test:sample:2.0")
    }
}

tasks.register("assertLegacyPublicationIsNotSelected") {
    doLast {
        val selectedComponents = configurations["linuxX64CompileKlibraries"]
            .incoming.resolutionResult.allComponents
            .map { component -> component.id.displayName }
        check("org.jetbrains.kotlin.kar.test:sample-linuxx64:1.0" !in selectedComponents) {
            "Legacy target publication was selected: $selectedComponents"
        }
    }
}
