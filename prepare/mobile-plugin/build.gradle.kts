import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm")
    id("com.github.jk1.tcdeps") version "1.2"
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val proprietaryRepositories: Project.() -> Unit by ultimateTools

val mobilePluginDir: File by rootProject.extra
val mobilePluginZipPath: File by rootProject.extra
val cidrVersion: String by rootProject.extra
val ideaPluginForCidrVersion: String by rootProject.extra(rootProject.extra["versions.ideaPluginForCidr"] as String)
val kotlinNativeBackendVersion: String by rootProject.extra
val kotlinNativeBackendRepo: String by rootProject.extra
val isStandaloneBuild: Boolean by rootProject.extra

repositories {
    maven("https://maven.google.com")
    proprietaryRepositories()
    if (isStandaloneBuild) {
        maven("https://dl.bintray.com/kotlin/kotlin-dev/")
    }
    teamcityServer {
        setUrl("https://buildserver.labs.intellij.net")
    }
}

dependencies {
    runtime(project(":kotlin-ultimate:ide:cidr-gradle-tooling")) { isTransitive = false }
    runtime(project(":kotlin-ultimate:ide:common-cidr-mobile")) { isTransitive = false }
    runtime(project(":kotlin-ultimate:ide:common-cidr-native")) { isTransitive = false }
    runtime(project(":kotlin-ultimate:ide:common-cidr-swift-native")) { isTransitive = false }
    runtime(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    embedded(project(":kotlin-ultimate:ide:mobile-native")) { isTransitive = false }
    if (isStandaloneBuild) {
        runtime("org.jetbrains.kotlin:kotlin-android-extensions-runtime:$ideaPluginForCidrVersion") { isTransitive = false }
    } else {
        runtime(project(":kotlin-android-extensions-runtime")) { isTransitive = false }
        runtime(project(":plugins:android-extensions-compiler")) { isTransitive = false }
    }
    runtime("com.jetbrains.intellij.android:android-kotlin-extensions-common:$cidrVersion") { isTransitive = false }
    runtime("com.android.tools.ddms:ddmlib:26.0.0") {
        exclude("com.google.guava", "guava")
    }
    runtime(project(":kotlin-ultimate:libraries:tools:apple-gradle-plugin-api")) { isTransitive = false }
    runtime(tc("$kotlinNativeBackendRepo:${kotlinNativeBackendVersion}:backend.native.jar"))
}

val pluginJarTask: Task by tasks.named<Jar>("jar") {
    dependsOn(":kotlin-ultimate:ide:mobile-native:assemble")

    configurations.findByName("embedded")?.let { embedded ->
        dependsOn(embedded)
        from(provider { embedded.map(::zipTree) })
    }

    archiveBaseName.set(project.the<BasePluginConvention>().archivesBaseName)
    archiveFileName.set("mobile-plugin.jar")
    manifest.attributes.apply {
        put("Implementation-Vendor", "JetBrains")
        put("Implementation-Title", archiveBaseName.get())
    }
}

val copyRuntimeDeps: Task by tasks.creating(Copy::class) {
    from(configurations.runtime)
    into(File(mobilePluginDir, "lib"))
}

val mobilePlugin: Task by tasks.creating(Copy::class) {
    duplicatesStrategy = DuplicatesStrategy.FAIL

    into(mobilePluginDir)

    into("lib") {
        dependsOn(pluginJarTask)
        from(pluginJarTask)
    }

    dependsOn(
        copyRuntimeDeps
    )
}

val zipMobilePluginTask: Task by tasks.creating(Zip::class) {
    destinationDirectory.set(mobilePluginZipPath.parentFile)
    archiveFileName.set(mobilePluginZipPath.name)

    from(mobilePlugin)
    into("Mobile")

    doLast {
        logger.lifecycle("Plugin artifacts packed to $mobilePluginZipPath")
    }
}