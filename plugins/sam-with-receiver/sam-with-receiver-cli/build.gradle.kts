
description = "Kotlin SamWithReceiver Compiler Plugin"

apply { plugin("kotlin") }

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    runtime(projectRuntimeJar(":kotlin-compiler"))
    runtime(projectDist(":kotlin-stdlib"))
    runtime(projectDist(":kotlin-reflect"))

    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:tests-common"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testRuntime(intellijDep())
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
    workingDir = rootDir
}