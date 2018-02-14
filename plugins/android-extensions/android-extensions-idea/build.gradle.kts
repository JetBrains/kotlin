
description = "Kotlin Android Extensions IDEA"

apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    testRuntime(intellijDep())

    compile(project(":compiler:util"))
    compile(project(":compiler:light-classes"))
    compile(project(":idea:idea-core"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-gradle"))
    compile(project(":plugins:android-extensions-compiler"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    compileOnly(intellijPluginDep("android")) { includeJars("android", "android-common", "sdk-common", "sdk-tools") }
    compileOnly(intellijPluginDep("Groovy")) { includeJars("Groovy") }
    compileOnly(intellijDep()) { includeJars("extensions", "openapi", "util", "idea", "android-base-common", rootProject = rootProject) }

    testCompile(project(":compiler:tests-common"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":plugins:kapt3-idea"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-android"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(commonDep("junit:junit"))
    testRuntime(projectDist(":kotlin-reflect"))
    testCompile(intellijPluginDep("android")) { includeJars("android", "android-common", "sdk-common", "sdk-tools") }
    testCompile(intellijPluginDep("Groovy")) { includeJars("Groovy") }
    testCompile(intellijDep()) { includeJars("extensions") }

    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":plugins:android-extensions-jps"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":plugins:lint"))
    testRuntime(intellijPluginDep("junit"))
    testRuntime(intellijPluginDep("IntelliLang"))
    testRuntime(intellijPluginDep("properties"))
    testRuntime(intellijPluginDep("java-i18n"))
    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("java-decompiler"))
    testRuntime(intellijPluginDep("maven"))
    testRuntime(intellijPluginDep("android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    dependsOn(":kotlin-android-extensions-runtime:dist")
    workingDir = rootDir
    useAndroidSdk()
    useAndroidJar()
    doFirst {
        systemProperty("idea.home.path", intellijRootDir().canonicalPath)
    }
}

runtimeJar()

ideaPlugin()
