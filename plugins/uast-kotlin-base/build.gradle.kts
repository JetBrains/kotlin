plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:light-classes"))

    // BEWARE: UAST should not depend on IJ platform so that it can work in Android Lint CLI mode (where IDE is not available)
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }
    compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }

    testImplementation(commonDep("junit:junit"))
    testCompileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
    testImplementation(projectTests(":compiler:tests-common"))
    testCompileOnly(intellijDep()) { includeJars("uast-tests") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar ()
