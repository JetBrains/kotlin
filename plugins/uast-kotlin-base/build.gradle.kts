plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:light-classes"))

    // BEWARE: Uast should not depend on IDEA.
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }
    compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
}

sourceSets {
    "main" { projectDefault() }
}
