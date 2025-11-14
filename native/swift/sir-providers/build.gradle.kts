import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

description = "SIR Providers - family of classes, that transforms KaSymbol into corresponding SIR nodes"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":native:swift:sir"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":native:analysis-api-based-export-common"))
}

sourceSets {
    "main" { projectDefault() }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
        "org.jetbrains.kotlin.analysis.api.KaContextParameterApi",
    )
}
