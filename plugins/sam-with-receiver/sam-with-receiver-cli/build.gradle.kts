
description = "Kotlin SamWithReceiver Compiler Plugin"

apply { plugin("kotlin") }

dependencies {
    testRuntime(intellijDep())

    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    runtime(projectDist(":kotlin-stdlib"))
    runtime(projectDist(":kotlin-reflect"))
    runtime(projectRuntimeJar(":kotlin-compiler"))

    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:tests-common"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}
sourcesJar()
javadocJar()
testsJar {}

publish()

dist {
    rename("kotlin-", "")
}

ideaPlugin {
    from(jar)
    rename("^kotlin-", "")
}

projectTest {
    dependsOn(":prepare:mock-runtime-for-test:dist")
    workingDir = rootDir
}
