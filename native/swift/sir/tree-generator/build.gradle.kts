plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    application
}

configurations.runtimeOnly.get().extendsFrom(configurations.compileOnly.get())

dependencies {
    implementation(project(":generators:tree-generator-common"))
}

application {
    mainClass.set("org.jetbrains.kotlin.sir.tree.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
