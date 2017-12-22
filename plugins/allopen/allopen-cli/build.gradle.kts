
description = "Kotlin AllOpen Compiler Plugin"

apply { plugin("kotlin") }

dependencies {
    testRuntime(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntime(intellijDep())

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    runtime(projectRuntimeJar(":kotlin-compiler"))
    runtime(projectDist(":kotlin-stdlib"))

    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:tests-common"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}

testsJar {}

dist(targetName = the<BasePluginConvention>().archivesBaseName.removePrefix("kotlin-") + ".jar")

ideaPlugin {
    from(jar)
    rename("^kotlin-", "")
}

projectTest {
    workingDir = rootDir
}
