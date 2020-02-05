import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())

    compile(kotlinStdlib())
    compile(project(":idea:idea-core"))
    compile(project(":j2k"))
    compile(project(":compiler:psi"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }
    compileOnly(intellijDep()) { includeJars("platform-api", "platform-impl", rootProject = rootProject) }

    Platform[191].orLower {
        compileOnly(intellijDep()) { includeJars("java-api", "java-impl") }
    }

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
        testCompileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
        testRuntime(intellijPluginDep("java"))
    }

    testCompile(project(":idea"))
    testCompile(projectTests(":j2k"))
    testCompile(projectTests(":idea"))
    testCompile(project(":nj2k:nj2k-services"))
    testCompile(projectTests(":idea:idea-test-framework"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))

    testCompileOnly(intellijDep())

    testRuntimeOnly(toolsJar())
    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":kotlinx-serialization-ide-plugin"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}


testsJar()

configureFreeCompilerArg(true, "-Xeffect-system")
configureFreeCompilerArg(true, "-Xnew-inference")

fun configureFreeCompilerArg(isEnabled: Boolean, compilerArgument: String) {
    if (isEnabled) {
        allprojects {
            tasks.withType<KotlinCompile<*>> {
                kotlinOptions {
                    freeCompilerArgs += listOf(compilerArgument)
                }
            }
        }
    }
}
