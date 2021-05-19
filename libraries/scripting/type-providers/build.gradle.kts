
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(kotlinStdlib())
    compileOnly(project(":kotlin-scripting-compiler"))
    compileOnly(project(":kotlin-scripting-common"))
    compileOnly(project(":kotlin-scripting-jvm"))
    compileOnly(project(":kotlin-scripting-jvm-host-unshaded"))

    implementation(kotlin("reflect"))

    testCompile(commonDep("junit"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:cli-common"))
    testCompile(projectTests(":compiler:tests-common"))

    testCompile(project(":compiler:frontend.java"))

    testImplementation(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
}