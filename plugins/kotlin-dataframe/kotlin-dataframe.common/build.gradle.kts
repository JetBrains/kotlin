description = "Kotlin DataFrame Compiler Plugin (Common)"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":core:compiler.common"))
    api(variantOf(libs.dataframe.compiler.plugin.core) { classifier("all") }) {
        isTransitive = false
    }
    embedded(variantOf(libs.dataframe.compiler.plugin.core) { classifier("all") }) {
        isTransitive = false
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

standardPublicJars()
