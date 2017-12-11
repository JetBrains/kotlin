
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kotlin IDEA Ultimate plugin"

apply {
    plugin("kotlin")
}

val ideaProjectResources =  project(":idea").the<JavaPluginConvention>().sourceSets["main"].output.resourcesDir

evaluationDependsOn(":prepare:idea-plugin")

val springClasspath by configurations.creating

dependencies {
    compileOnly(project(":kotlin-reflect-api"))
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core:descriptors")) { isTransitive = false }
    compile(project(":core:descriptors.jvm")) { isTransitive = false }
    compile(project(":core:util.runtime")) { isTransitive = false }
    compile(project(":compiler:light-classes")) { isTransitive = false }
    compile(project(":compiler:frontend")) { isTransitive = false }
    compile(project(":compiler:frontend.java")) { isTransitive = false }
    compile(project(":js:js.frontend")) { isTransitive = false }
    compile(projectClasses(":idea"))
    compile(project(":idea:idea-jvm")) { isTransitive = false }
    compile(project(":idea:idea-core")) { isTransitive = false }
    compile(project(":idea:ide-common")) { isTransitive = false }
    compile(project(":idea:idea-gradle")) { isTransitive = false }
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijUltimateDep()) { includeJars("annotations", "trove4j", "openapi", "idea", "util", "jdom") }
    compileOnly(intellijPluginDep("CSS"))
    compileOnly(intellijPluginDep("DatabaseTools"))
    compileOnly(intellijPluginDep("JavaEE"))
    compileOnly(intellijPluginDep("jsp"))
    compileOnly(intellijPluginDep("PersistenceSupport"))
    compileOnly(intellijPluginDep("Spring"))
    compileOnly(intellijPluginDep("properties"))
    compileOnly(intellijPluginDep("java-i18n"))
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijPluginDep("Groovy"))
    compileOnly(intellijPluginDep("junit"))
    compileOnly(intellijPluginDep("uml"))
    compileOnly(intellijPluginDep("JavaScriptLanguage"))
    compileOnly(intellijPluginDep("JavaScriptDebugger"))
    compileOnly(intellijPluginDep("NodeJS"))

    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":plugins:lint")) { isTransitive = false }
    testCompile(project(":idea:idea-jvm")) { isTransitive = false }
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea")) { isTransitive = false }
    testCompile(projectTests(":generators:test-generator"))
    testCompile(commonDep("junit:junit"))
    testCompile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(projectDist(":kotlin-script-runtime"))
    testRuntime(projectRuntimeJar(":kotlin-compiler"))
    testRuntime(project(":plugins:android-extensions-ide")) { isTransitive = false }
    testRuntime(project(":plugins:android-extensions-compiler")) { isTransitive = false }
    testRuntime(project(":plugins:annotation-based-compiler-plugins-ide-support")) { isTransitive = false }
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
    testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }
    testRuntime(project(":plugins:uast-kotlin"))
    testRuntime(project(":plugins:uast-kotlin-idea"))
    testRuntime(files("${System.getProperty("java.home")}/../lib/tools.jar"))
    testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }

    springClasspath(commonDep("org.springframework", "spring-core"))
    springClasspath(commonDep("org.springframework", "spring-beans"))
    springClasspath(commonDep("org.springframework", "spring-context"))
    springClasspath(commonDep("org.springframework", "spring-tx"))
    springClasspath(commonDep("org.springframework", "spring-web"))

    testCompileOnly(intellijDep()) { includeJars("gson-2.5", "annotations", "trove4j", "openapi", "idea", "util", "jdom") }
    testRuntime(intellijUltimateDep())
    testCompile(intellijPluginDep("CSS"))
    testCompile(intellijPluginDep("DatabaseTools"))
    testCompile(intellijPluginDep("JavaEE"))
    testCompile(intellijPluginDep("jsp"))
    testCompile(intellijPluginDep("PersistenceSupport"))
    testCompile(intellijPluginDep("Spring"))
    testCompile(intellijPluginDep("properties"))
    testCompile(intellijPluginDep("java-i18n"))
    testCompile(intellijPluginDep("gradle"))
    testCompile(intellijPluginDep("Groovy"))
    testCompile(intellijPluginDep("junit"))
    testCompile(intellijPluginDep("uml"))
    testCompile(intellijPluginDep("JavaScriptLanguage"))
    testCompile(intellijPluginDep("JavaScriptDebugger"))
    testCompile(intellijPluginDep("NodeJS"))
    testRuntime(intellijPluginDep("coverage"))
    testRuntime(intellijPluginDep("maven"))
    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijPluginDep("testng"))
    testRuntime(intellijPluginDep("IntelliLang"))
    testRuntime(intellijPluginDep("copyright"))
    testRuntime(intellijPluginDep("java-decompiler"))
}

val preparedResources = File(buildDir, "prepResources")

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val ultimatePluginXmlContent: String by lazy {
    val sectRex = Regex("""^\s*</?idea-plugin>\s*$""")
    File(projectDir, "resources/META-INF/ultimate-plugin.xml")
            .readLines()
            .filterNot { it.matches(sectRex) }
            .joinToString("\n")
}

val prepareResources by task<Copy> {
    dependsOn(":idea:assemble")
    from(ideaProjectResources, {
        exclude("META-INF/plugin.xml")
    })
    into(preparedResources)
}

val preparePluginXml by task<Copy> {
    dependsOn(":idea:assemble")
    from(ideaProjectResources, { include("META-INF/plugin.xml") })
    into(preparedResources)
    filter {
        it?.replace("<!-- ULTIMATE-PLUGIN-PLACEHOLDER -->", ultimatePluginXmlContent)
    }
}

val communityPluginProject = ":prepare:idea-plugin"

val jar = runtimeJar(task<ShadowJar>("shadowJar")) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(preparePluginXml)
    dependsOn("$communityPluginProject:shadowJar")
    val communityPluginJar = project(communityPluginProject).configurations["runtimeJar"].artifacts.files.singleFile
    from(zipTree(communityPluginJar), { exclude("META-INF/plugin.xml") })
    from(preparedResources, { include("META-INF/plugin.xml") })
    from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
    archiveName = "kotlin-plugin.jar"
}

val ideaPluginDir: File by rootProject.extra
val ideaUltimatePluginDir: File by rootProject.extra

task<Copy>("ideaUltimatePlugin") {
    dependsOn("$communityPluginProject:ideaPlugin")
    dependsOnTaskIfExistsRec("ideaPlugin", rootProject)
    into(ideaUltimatePluginDir)
    from(ideaPluginDir) { exclude("lib/kotlin-plugin.jar") }
    from(jar, { into("lib") })
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
        classpath.add(files(preparedResources))
        systemProperty("spring.classpath", springClasspath.asPath)
    }
}

val generateTests by generator("org.jetbrains.kotlin.tests.GenerateUltimateTestsKt")
