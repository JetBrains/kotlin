plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:checkers"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(intellijCore())
    compileOnly(project(":kotlin-reflect-api"))

    testApiJUnit5()
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(project(":compiler:fir:checkers"))
    testApi(project(":compiler:fir:checkers:checkers.jvm"))

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(commonDependency("net.java.dev.jna:jna"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:jdom"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(files("/home/nikitak/.m2/repository/org/jetbrains/kotlinx/dataframe-core/0.9.0-dev/dataframe-core-0.9.0-dev.jar"))
    //implementation(files("/home/nikitak/.m2/repository/org/jetbrains/kotlinx/dataframe-core/0.9.0-dev/dataframe-core-0.9.0-dev.jar"))
    implementation("org.jetbrains.kotlinx:dataframe:0.9.0-dev")
    testImplementation("io.kotest:kotest-assertions-core:4.6.3")
    implementation("org.jetbrains.kotlinx.dataframe:bridge-generator:0.9.0-dev")
}

optInToExperimentalCompilerApi()

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.fir.FirImplementationDetail"
    }
}

publish {
    artifactId = "kotlin-dataframe-compiler"
}
//
runtimeJar()
//sourcesJar()
//javadocJar()

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    jvmArgs!!.removeIf { it.contains("-Xmx") }
    maxHeapSize = "3g"
    dependsOn(":plugins:kotlin-dataframe:plugin-annotations:jar")
    useJUnitPlatform()
}

testsJar()
