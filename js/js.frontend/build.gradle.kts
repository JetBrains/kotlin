plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:frontend.common"))
    api(project(":core:names"))
    implementation(project(":compiler:frontend"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":core:compiler.common"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

optInToK1Deprecation()
