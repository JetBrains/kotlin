
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kotlin IDEA Ultimate plugin"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

apply { plugin("kotlin") }

val ideaCommunityPlugin by configurations.creating

val ideaProjectClasses =  project(":idea").the<JavaPluginConvention>().sourceSets["main"].output.classesDirs
val ideaProjectResources =  project(":idea").the<JavaPluginConvention>().sourceSets["main"].output.resourcesDir

dependencies {
    val compile by configurations
    val compileOnly by configurations
    val testCompile by configurations
    val testCompileOnly by configurations
    val testRuntime by configurations
    compile(projectDist(":kotlin-reflect"))
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core")) { isTransitive = false }
    compile(project(":core:util.runtime")) { isTransitive = false }
    compile(project(":compiler:light-classes")) { isTransitive = false }
    compile(project(":compiler:frontend")) { isTransitive = false }
    compile(project(":compiler:frontend.java")) { isTransitive = false }
    compile(ideaProjectClasses)
    compile(project(":idea:idea-core")) { isTransitive = false }
    compile(project(":idea:ide-common")) { isTransitive = false }
    compile(project(":idea:idea-gradle")) { isTransitive = false }

    compile(ideaUltimateSdkCoreDeps("annotations", "trove4j", "intellij-core"))
    compile(ideaUltimateSdkDeps("openapi", "idea", "util"))
//    compile(ideaUltimatePluginDeps("gradle-tooling-api", plugin = "gradle"))
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

    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":compiler.tests-common")) { isTransitive = false }
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":plugins:lint")) { isTransitive = false }
    testCompile(project(":idea:idea-jvm")) { isTransitive = false }
    testCompile(project(":generators")) { isTransitive = false }
    testCompile(projectTests(":idea"))
//    testCompileOnly(projectTests(":idea:idea-gradle"))
    testCompile(commonDep("junit:junit"))
    testCompile(ideaUltimateSdkDeps("gson"))
    testCompile(preloadedDeps("kotlinx-coroutines-core"))

    testRuntime(projectDist(":kotlin-compiler"))
    testRuntime(project(":plugins:android-extensions-ide")) { isTransitive = false }
    testRuntime(project(":android-extensions-compiler")) { isTransitive = false }
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

    ideaCommunityPlugin(projectRuntimeJar(":prepare:kotlin-plugin"))
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
    from(ideaProjectResources, { exclude("META-INF/plugin.xml") })
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

val jar = runtimeJar(task<ShadowJar>("shadowJar")) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(preparePluginXml)
    project(":prepare:kotlin-plugin").afterEvaluate {
        ideaCommunityPlugin.files.forEach {
            from(zipTree(it), { exclude("META-INF/plugin.xml") })
        }
    }
    from(preparedResources, { include("META-INF/plugin.xml") })
    from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
    archiveName = "kotlin-plugin.jar"
}

val ideaPluginDir: File by rootProject.extra
val ideaUltimatePluginDir: File by rootProject.extra

task<Copy>("idea-ultimate-plugin") {
    dependsOnTaskIfExistsRec("idea-plugin", rootProject)
    into(ideaUltimatePluginDir)
    from(ideaPluginDir) { exclude("lib/kotlin-plugin.jar") }
    from(jar, { into("lib") })
}

projectTest {
    dependsOn(prepareResources)
    dependsOn(preparePluginXml)
    workingDir = rootDir
}
