plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Integrated Swift Export Environment"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":native:swift:sir"))
    implementation(project(":native:swift:sir-light-classes"))
    implementation(project(":native:swift:sir-printer"))

    implementation(project(":analysis:analysis-api"))
}

sourceSets {
    "main" { projectDefault() }
}


publish()

runtimeJar()
sourcesJar()
javadocJar()
