
description = "Kotlin Android Extensions Compiler"

apply { plugin("kotlin") }

val robolectricClasspath by configurations.creating
val androidJar by configurations.creating

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all") }

    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:tests-common"))
    testCompile(project(":kotlin-android-extensions-runtime"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(commonDep("junit:junit"))

    testRuntime(intellijPluginDep("junit")) { includeJars("idea-junit", "resources_en") }

    robolectricClasspath(commonDep("org.robolectric", "robolectric"))
    androidJar(project(":custom-dependencies:android-sdk", configuration = "androidJar"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar {
    from(getSourceSetsFrom(":kotlin-android-extensions-runtime")["main"].output.classesDirs)
}

dist()

ideaPlugin()

testsJar {}

evaluationDependsOn(":kotlin-android-extensions-runtime")

projectTest {
    environment("ANDROID_EXTENSIONS_RUNTIME_CLASSES", getSourceSetsFrom(":kotlin-android-extensions-runtime")["main"].output.classesDirs.asPath)
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = rootDir
    doFirst {
        val androidPluginPath = File(intellijRootDir(), "plugins/android").canonicalPath
        systemProperty("ideaSdk.androidPlugin.path", androidPluginPath)
        systemProperty("robolectric.classpath", robolectricClasspath.asPath)
        systemProperty("android.jar", androidJar.singleFile.canonicalPath)
    }
}