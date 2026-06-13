plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("project-tests-convention")
}

description = "Standalone Runner for Swift Export"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":native:native.config"))
    implementation(project(":native:swift:sir"))
    implementation(project(":native:swift:sir-providers"))
    implementation(project(":native:swift:sir-light-classes"))
    implementation(project(":native:swift:sir-printer"))

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-standalone"))

    implementation(project(":libraries:tools:analysis-api-based-klib-reader"))
    compileOnly(project(":kotlin-util-klib-metadata"))
}

sourceSets {
    "main" { projectDefault() }
}

optInToK1Deprecation()

publish()

runtimeJar()
sourcesJar()
javadocJar()
