plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":idea:idea-frontend-fir"))
    compile(project(":idea:formatter"))
    compile(intellijDep())
    compile(intellijCoreDep())

// <temp>
    compile(project(":idea:idea-core"))
    compile(project(":idea"))
// </temp>
    testCompile(projectTests(":idea:performanceTests"))


    testCompile(toolsJar())
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-fir"))
    compile(project(":idea:idea-fir"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea:idea-test-framework"))
    testCompile(projectTests(":idea:idea-frontend-fir"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))

    testCompileOnly(intellijDep())
    testRuntime(intellijDep())

    Platform[192].orHigher {
        compile(intellijPluginDep("java"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

if (rootProject.findProperty("idea.fir.plugin") == "true") {
    projectTest(parallel = true) {
        dependsOn(":dist")
        workingDir = rootDir
    }
}

testsJar()

projectTest(taskName = "ideaFirPerformanceTest") {
    val currentOs = org.gradle.internal.os.OperatingSystem.current()

    if (!currentOs.isWindows) {
        System.getenv("ASYNC_PROFILER_HOME")?.let { asyncProfilerHome ->
            classpath += files("$asyncProfilerHome/build/async-profiler.jar")
        }
    }

    workingDir = rootDir

    jvmArgs?.removeAll { it.startsWith("-Xmx") }

    maxHeapSize = "3g"
    jvmArgs("-Didea.debug.mode=true")
    jvmArgs("-XX:SoftRefLRUPolicyMSPerMB=50")

    jvmArgs(
        "-XX:+UseCompressedOops",
        "-Didea.ProcessCanceledException=disabled",
        "-XX:+UseConcMarkSweepGC"
    )

    System.getenv("YOURKIT_PROFILER_HOME")?.let {yourKitHome ->
        when {
            currentOs.isLinux -> {
                jvmArgs("-agentpath:$yourKitHome/bin/linux-x86-64/libyjpagent.so")
                classpath += files("$yourKitHome/lib/yjp-controller-api-redist.jar")
            }
            currentOs.isMacOsX -> {
                jvmArgs("-agentpath:$yourKitHome/Contents/Resources/bin/mac/libyjpagent.dylib=delay=5000,_socket_timeout_ms=120000,disablealloc,disable_async_sampling,disablenatives")
                classpath += files("$yourKitHome/Contents/Resources/lib/yjp-controller-api-redist.jar")
            }
        }
    }

    doFirst {
        systemProperty("idea.home.path", intellijRootDir().canonicalPath)
        project.findProperty("cacheRedirectorEnabled")?.let {
            systemProperty("kotlin.test.gradle.import.arguments", "-PcacheRedirectorEnabled=$it")
        }
    }
}