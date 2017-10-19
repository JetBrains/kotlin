
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kotlin IDEA Ultimate plugin"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    }
}

apply { plugin("kotlin") }

val ideaProjectResources =  project(":idea").the<JavaPluginConvention>().sourceSets["main"].output.resourcesDir

evaluationDependsOn(":prepare:idea-plugin")

dependencies {
    compile(projectDist(":kotlin-reflect"))
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core")) { isTransitive = false }
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

    compile(ideaUltimatePreloadedDeps("*.jar", subdir = "nodejs_plugin/NodeJS/lib"))
    compile(ideaUltimateSdkCoreDeps("annotations", "trove4j", "intellij-core"))
    compile(ideaUltimateSdkDeps("openapi", "idea", "util", "jdom"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "CSS"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "DatabaseTools"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "JavaEE"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "jsp"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "PersistenceSupport"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "Spring"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "properties"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "java-i18n"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "gradle"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "Groovy"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "junit"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "uml"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "JavaScriptLanguage"))
    compile(ideaUltimatePluginDeps("*.jar", plugin = "JavaScriptDebugger"))

    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":plugins:lint")) { isTransitive = false }
    testCompile(project(":idea:idea-jvm")) { isTransitive = false }
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":generators:test-generator"))
    testCompile(commonDep("junit:junit"))
    testCompile(ideaUltimateSdkDeps("gson"))
    testCompile(preloadedDeps("kotlinx-coroutines-core"))

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
    testRuntime(ideaUltimateSdkDeps("*.jar"))
    testRuntime(ideaUltimatePluginDeps("*.jar", plugin = "properties"))
    testRuntime(ideaUltimatePluginDeps("*.jar", plugin = "coverage"))
    testRuntime(ideaUltimatePluginDeps("*.jar", plugin = "maven"))
    testRuntime(ideaUltimatePluginDeps("*.jar", plugin = "android"))
    testRuntime(ideaUltimatePluginDeps("*.jar", plugin = "testng"))
    testRuntime(ideaUltimatePluginDeps("*.jar", plugin = "IntelliLang"))
    testRuntime(ideaUltimatePluginDeps("*.jar", plugin = "testng"))
    testRuntime(ideaUltimatePluginDeps("*.jar", plugin = "copyright"))
    testRuntime(ideaUltimatePluginDeps("*.jar", plugin = "java-decompiler"))
    testRuntime(files("${System.getProperty("java.home")}/../lib/tools.jar"))
}

val preparedResources = File(buildDir, "prepResources")

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        resources.srcDir(preparedResources)
    }
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
}

val generateTests by generator("org.jetbrains.kotlin.tests.GenerateUltimateTestsKt")
