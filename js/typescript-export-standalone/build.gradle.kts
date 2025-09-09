plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

description = "Standalone Runner for TypeScript Export"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-standalone"))
    implementation(project(":core:compiler.common.js"))
    implementation(project(":core:util.runtime"))
    implementation(project(":js:js.ast"))
    implementation(project(":js:typescript-export-model"))
    implementation(project(":js:typescript-printer"))
    implementation(project(":libraries:tools:analysis-api-based-klib-reader"))
    implementation(project(":kotlin-util-klib-metadata"))

    api(project(":core:compiler.common"))
    api(project(":js:js.config"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
