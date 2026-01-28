plugins {
    kotlin("jvm")
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
    api(project(":core:compiler.common")) { isTransitive = false }
    api(project(":compiler:config")) { isTransitive = false }
    compileOnly(intellijCore())

    compileOnly(project(":core:metadata")) { isTransitive = false }
    embedded(project(":core:metadata")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

configureKotlinCompileTasksGradleCompatibility()
