description = "Kotlin DataFrame Compiler Plugin (Common)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":core:compiler.common"))
    api(libs.dataframe.compiler.plugin.core) {
        isTransitive = false
    }
    embedded(libs.dataframe.compiler.plugin.core) {
        isTransitive = false
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

standardPublicJars()
