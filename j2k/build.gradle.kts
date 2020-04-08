import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())

    compile(kotlinStdlib())
    compile(project(":idea:idea-core"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:util"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testCompile(project(":idea"))
    testCompile(projectTests(":idea:idea-test-framework"))
    testCompile(project(":compiler:light-classes"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))

    testCompileOnly(intellijDep())

    Platform[192].orHigher {
        testCompileOnly(intellijPluginDep("java"))
        testRuntimeOnly(intellijPluginDep("java"))
    }

    testCompile(project(":idea:idea-native")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle-native")) { isTransitive = false }

    testRuntimeOnly(toolsJar())
    testRuntime(project(":native:frontend.native"))
    testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }
    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":idea:idea-android"))
    testRuntime(project(":plugins:android-extensions-ide"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":kotlin-scripting-idea"))
    testRuntime(project(":kotlinx-serialization-ide-plugin"))
    testRuntime(intellijPluginDep("properties"))
    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("coverage"))
    Ide.IJ {
        testRuntime(intellijPluginDep("maven"))

        if (Ide.IJ201.orHigher()) {
            testRuntime(intellijPluginDep("repository-search"))
        }
    }
    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijPluginDep("smali"))
    testRuntime(intellijPluginDep("junit"))
    testRuntime(intellijPluginDep("testng"))
    testRuntime(intellijPluginDep("IntelliLang"))
    testRuntime(intellijPluginDep("testng"))
    testRuntime(intellijPluginDep("copyright"))
    testRuntime(intellijPluginDep("properties"))
    testRuntime(intellijPluginDep("java-i18n"))
    testRuntime(intellijPluginDep("java-decompiler"))
    testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }

    if (Ide.AS36.orHigher()) {
        testRuntime(intellijPluginDep("android-layoutlib"))
    }
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("newSrc")
    }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}

testsJar()

val testForWebDemo by task<Test> {
    include("**/*JavaToKotlinConverterForWebDemoTestGenerated*")
    classpath = testSourceSet.runtimeClasspath
    workingDir = rootDir
}

val test: Test by tasks
test.apply {
    exclude("**/*JavaToKotlinConverterForWebDemoTestGenerated*")
    //dependsOn(testForWebDemo)
}

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
