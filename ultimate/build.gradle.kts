description = "Kotlin IDEA Ultimate plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val ideaProjectResources = provider { project(":idea").mainSourceSet.output.resourcesDir }
val intellijUltimateEnabled : Boolean by rootProject.extra

val springClasspath by configurations.creating
val ideaPlugin by configurations.creating

dependencies {
    if (intellijUltimateEnabled) {
        testRuntime(intellijUltimateDep())
    }

    compileOnly(project(":kotlin-reflect-api"))
    compile(kotlinStdlib("jdk8"))
    compile(project(":core:descriptors")) { isTransitive = false }
    compile(project(":compiler:psi")) { isTransitive = false }
    compile(project(":core:descriptors.jvm")) { isTransitive = false }
    compile(project(":core:util.runtime")) { isTransitive = false }
    compile(project(":compiler:light-classes")) { isTransitive = false }
    compile(project(":core:compiler.common")) { isTransitive = false }
    compile(project(":compiler:cli-common")) { isTransitive = false }
    compile(project(":compiler:frontend")) { isTransitive = false }
    compile(project(":compiler:frontend.common")) { isTransitive = false }
    compile(project(":compiler:frontend.java")) { isTransitive = false }
    compile(project(":compiler:util")) { isTransitive = false }
    compile(project(":js:js.frontend")) { isTransitive = false }
    compile(project(":idea")) { isTransitive = false }
    compile(project(":idea:idea-jvm")) { isTransitive = false }
    compile(project(":idea:idea-core")) { isTransitive = false }
    compile(project(":idea:ide-common")) { isTransitive = false }
    compile(project(":idea:idea-gradle")) { isTransitive = false }
    compile(project(":idea:idea-native")) { isTransitive = false }
    compile(project(":idea:kotlin-gradle-tooling")) { isTransitive = false }
    compile(project(":compiler:util")) { isTransitive = false }
    compile(project(":idea:idea-jps-common")) { isTransitive = false }

    if (intellijUltimateEnabled) {
        compileOnly(nodeJSPlugin())
        compileOnly(intellijUltimateDep()) {
            includeJars("trove4j", "platform-api", "platform-impl", "idea", "util", "jdom", "extensions")
        }

        Platform[192].orHigher {
            compileOnly(intellijUltimateDep()) { includeJars("platform-util-ui", "platform-core-ui") }
            compileOnly(intellijUltimatePluginDep("java")) { includeJars("java-api", "java-impl") }
            compileOnly(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
            compileOnly(project(":kotlin-ultimate:ide:common-noncidr-native")) { isTransitive = false }
            compileOnly(project(":kotlin-ultimate:ide:ultimate-native")) { isTransitive = false }
        }

        Platform[193].orLower {
            compileOnly(intellijUltimateDep()) { includeJars("openapi") }
        }

        Platform[193].orHigher {
            compileOnly(intellijUltimatePluginDep("gradle-java"))
            compileOnly(intellijUltimateDep()) {
                includeJars("platform-ide-util-io")
            }
        }

        compileOnly(intellijUltimatePluginDep("CSS"))
        compileOnly(intellijUltimatePluginDep("DatabaseTools"))
        compileOnly(intellijUltimatePluginDep("JavaEE"))
        compileOnly(intellijUltimatePluginDep("jsp"))
        compileOnly(intellijUltimatePluginDep("PersistenceSupport"))
        compileOnly(intellijUltimatePluginDep("Spring"))
        compileOnly(intellijUltimatePluginDep("properties"))
        compileOnly(intellijUltimatePluginDep("java-i18n"))
        compileOnly(intellijUltimatePluginDep("gradle"))
        compileOnly(intellijUltimatePluginDep("Groovy"))
        compileOnly(intellijUltimatePluginDep("junit"))
        compileOnly(intellijUltimatePluginDep("uml"))
        compileOnly(intellijUltimatePluginDep("JavaScriptLanguage"))
        compileOnly(intellijUltimatePluginDep("JavaScriptDebugger"))
    }

    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":plugins:lint")) { isTransitive = false }
    testCompile(project(":idea:idea-jvm")) { isTransitive = false }
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea")) { isTransitive = false }
    testCompile(projectTests(":generators:test-generator"))
    testCompile(commonDep("junit:junit"))

    testCompile(project(":idea:idea-native")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle-native")) { isTransitive = false }

    if (intellijUltimateEnabled) {
        Platform[201].orHigher {
            testCompileOnly(intellijUltimateDep()) {
                includeJars(
                    "testFramework",
                    "testFramework-java",
                    "testFramework.core",
                    rootProject = rootProject
                )
            }
        }

        testCompileOnly(intellijUltimateDep()) {
            includeJars(
                "platform-api",
                "platform-impl",
                "gson",
                "trove4j",
                "openapi",
                "idea",
                "util",
                "jdom",
                rootProject = rootProject
            )
        }
    }
    testCompile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    testRuntime(project(":native:frontend.native"))
    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":kotlin-script-runtime"))
    testRuntime(project(":kotlin-scripting-intellij"))
    testRuntime(project(":plugins:android-extensions-ide")) { isTransitive = false }
    testRuntime(project(":plugins:android-extensions-compiler")) { isTransitive = false }
    testRuntime(project(":plugins:annotation-based-compiler-plugins-ide-support")) { isTransitive = false }
    testRuntime(project(":idea:jvm-debugger:jvm-debugger-util")) { isTransitive = false }
    testRuntime(project(":idea:jvm-debugger:jvm-debugger-core")) { isTransitive = false }
    testRuntime(project(":idea:jvm-debugger:jvm-debugger-evaluation")) { isTransitive = false }
    testRuntime(project(":idea:jvm-debugger:jvm-debugger-sequence")) { isTransitive = false }
    testRuntime(project(":idea:idea-android")) { isTransitive = false }
    testRuntime(project(":idea:idea-maven")) { isTransitive = false }
    testRuntime(project(":idea:idea-jps-common")) { isTransitive = false }
    testRuntime(project(":idea:formatter")) { isTransitive = false }
    testRuntime(project(":sam-with-receiver-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-sam-with-receiver-compiler-plugin")) { isTransitive = false }
    testRuntime(project(":noarg-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-noarg-compiler-plugin")) { isTransitive = false }
    testRuntime(project(":allopen-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-allopen-compiler-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-scripting-idea")) { isTransitive = false }
    testRuntime(project(":kotlin-scripting-compiler-impl")) { isTransitive = false }
    testRuntime(project(":kotlinx-serialization-compiler-plugin")) { isTransitive = false }
    testRuntime(project(":kotlinx-serialization-ide-plugin")) { isTransitive = false }
    testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }
    testRuntime(project(":plugins:uast-kotlin"))
    testRuntime(project(":plugins:uast-kotlin-idea"))
    testRuntime(intellijPluginDep("smali"))

    if (intellijUltimateEnabled) {
        Platform[192].orHigher {
            testRuntime(intellijUltimatePluginDep("java"))
        }
        Platform[193].orHigher {
            testRuntime(intellijUltimatePluginDep("gradle-java"))
        }
        testCompile(nodeJSPlugin())
        testCompile(intellijUltimatePluginDep("CSS"))
        testCompile(intellijUltimatePluginDep("DatabaseTools"))
        testCompile(intellijUltimatePluginDep("JavaEE"))
        testCompile(intellijUltimatePluginDep("jsp"))
        testCompile(intellijUltimatePluginDep("PersistenceSupport"))
        testCompile(intellijUltimatePluginDep("Spring"))
        testCompile(intellijUltimatePluginDep("uml"))
        testCompile(intellijUltimatePluginDep("JavaScriptLanguage"))
        testCompile(intellijUltimatePluginDep("JavaScriptDebugger"))
        testCompile(intellijUltimatePluginDep("properties"))
        testCompile(intellijUltimatePluginDep("java-i18n"))
        testCompile(intellijUltimatePluginDep("gradle"))
        testCompile(intellijUltimatePluginDep("Groovy"))
        testCompile(intellijUltimatePluginDep("junit"))
        testRuntime(intellijUltimatePluginDep("coverage"))
        testRuntime(intellijUltimatePluginDep("maven"))
        if (Platform[201].orHigher()) {
            testRuntime(intellijUltimatePluginDep("repository-search"))
        }
        testRuntime(intellijUltimatePluginDep("android"))
        testRuntime(intellijUltimatePluginDep("testng"))
        testRuntime(intellijUltimatePluginDep("IntelliLang"))
        testRuntime(intellijUltimatePluginDep("copyright"))
        testRuntime(intellijUltimatePluginDep("java-decompiler"))
    }

    testRuntime(files("${System.getProperty("java.home")}/../lib/tools.jar"))

    springClasspath(commonDep("org.springframework", "spring-core"))
    springClasspath(commonDep("org.springframework", "spring-beans"))
    springClasspath(commonDep("org.springframework", "spring-context"))
    springClasspath(commonDep("org.springframework", "spring-tx"))
    springClasspath(commonDep("org.springframework", "spring-web"))

    ideaPlugin(project(":prepare:idea-plugin", configuration = "runtimeJar"))
}

val preparedResources = File(buildDir, "prepResources")

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        resources.srcDir(preparedResources)
    }
}

val prepareResources by task<Copy> {
    dependsOn(":idea:assemble")
    from(ideaProjectResources, {
        exclude("META-INF/plugin.xml")
    })
    into(preparedResources)
}

val preparePluginXml by task<Copy> {
    val ultimatePluginXmlFile = File(projectDir, "resources/META-INF/ultimate-plugin.xml")
    val ultimatePluginXmlContent: String by lazy {
        val sectRex = Regex("""^\s*</?idea-plugin>\s*$""")
        ultimatePluginXmlFile.readLines()
            .filterNot { it.matches(sectRex) }
            .joinToString("\n")
    }

    dependsOn(":idea:assemble")

    inputs.file(ultimatePluginXmlFile)

    from(ideaProjectResources, { include("META-INF/plugin.xml") })
    into(preparedResources)
    filter {
        it?.replace("<!-- ULTIMATE-PLUGIN-PLACEHOLDER -->", ultimatePluginXmlContent)
    }
}

val jar = runtimeJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(preparePluginXml)
    dependsOn(ideaPlugin)
    
    from(provider { zipTree(ideaPlugin.singleFile) }) { 
        exclude("META-INF/plugin.xml") 
    }

    Platform[192].orHigher {
        from(provider { project(":kotlin-ultimate:ide:common-native").mainSourceSet.output })
        from(provider { project(":kotlin-ultimate:ide:common-noncidr-native").mainSourceSet.output })
        from(provider { project(":kotlin-ultimate:ide:ultimate-native").mainSourceSet.output })
    }

    from(preparedResources, { include("META-INF/plugin.xml") })
    from(mainSourceSet.output)
    archiveName = "kotlin-plugin.jar"
}

val ideaPluginDir: File by rootProject.extra
val ideaUltimatePluginDir: File by rootProject.extra

task<Copy>("ideaUltimatePlugin") {
    dependsOn(":ideaPlugin")
    into(ideaUltimatePluginDir)
    from(ideaPluginDir) { exclude("lib/kotlin-plugin.jar") }
    from(jar, { into("lib") })
    Platform[192].orHigher {
        from(rootProject.extra["lldbFrontendLinuxDir"] as File) { into("bin/linux") }
        from(rootProject.extra["lldbFrontendMacosDir"] as File) { into("bin/macos") }
        from(rootProject.extra["lldbFrontendWindowsDir"] as File) { into("bin/windows") }
    }
}

task("idea-ultimate-plugin") {
    dependsOn("ideaUltimatePlugin")
    doFirst { logger.warn("'$name' task is deprecated, use '${dependsOn.last()}' instead") }
}

task("ideaUltimatePluginTest") {
    dependsOn("check")
}

projectTest {
    dependsOn(prepareResources)
    dependsOn(preparePluginXml)
    workingDir = rootDir
    doFirst {
        if (intellijUltimateEnabled) {
            systemProperty("idea.home.path", intellijUltimateRootDir().canonicalPath)
        }
        systemProperty("spring.classpath", springClasspath.asPath)
    }
}

val generateTests by generator("org.jetbrains.kotlin.tests.GenerateUltimateTestsKt")
