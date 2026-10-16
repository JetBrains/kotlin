description = "Kotlin JavaScript Plain Objects Compiler Plugin (K2)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:fir:plugin.api"))
    // Required for access to 'JsStandardClassIds'.
    implementation(project(":core:compiler.common.js"))
    // Required for access to 'FirBasedSymbol<*>.isEffectivelyExternal()'.
    implementation(project(":compiler:fir:checkers:checkers.web.common"))

    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.common"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
