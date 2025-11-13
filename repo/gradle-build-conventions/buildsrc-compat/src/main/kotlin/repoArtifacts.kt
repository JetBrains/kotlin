@file:Suppress("unused") // usages in build scripts are not tracked properly
@file:JvmName("RepoArtifacts")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.JAVADOC_ELEMENTS_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPlugin.SOURCES_ELEMENTS_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.internal.component.external.model.TestFixturesSupport
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf
import plugins.KotlinBuildPublishingPlugin
import plugins.mainPublicationName


private const val MAGIC_DO_NOT_CHANGE_TEST_JAR_TASK_NAME = "testJar"

fun Project.testsJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> {
    val testsJarCfg = configurations.getOrCreate("tests-jar").extendsFrom(configurations["testApi"])

    return tasks.register<Jar>(MAGIC_DO_NOT_CHANGE_TEST_JAR_TASK_NAME) {
        dependsOn("testClasses")
        pluginManager.withPlugin("java") {
            from(testSourceSet.output)
        }
        archiveClassifier.set("tests")
        body()
    }.also {
        project.addArtifact(testsJarCfg.name, it)
    }
}

/**
 * This is a dirty hack that allows depending both on tests and test-fixture
 * of the module from some other module. Please don't use it.
 *
 * The proper approach should be implemented in the scope of KTI-2521.
 */
fun Project.testsJarToBeUsedAlongWithFixtures() {
    // Define a test jar task.
    val testsJar by tasks.registering(Jar::class) {
        archiveClassifier.set("tests")
        from(sourceSets["test"].output)
    }

    // Create a consumable, non-resolvable configuration with a unique capability.
    val testsJarConfig by configurations.creating {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        }
        outgoing.capabilities.clear()
        outgoing.capability("org.jetbrains.kotlin:${project.name}-tests-jar:${project.version}")
    }

    // Publish the test jar artifact only to this configuration (not to testImplementation/testRuntime)
    artifacts {
        add(testsJarConfig.name, testsJar)
    }
}

fun Project.setPublishableArtifact(
    jarTask: TaskProvider<out Jar>
) {
    addArtifact("runtimeElements", jarTask)
    addArtifact("apiElements", jarTask)
    addArtifact("archives", jarTask)
}

fun removeJarTaskArtifact(
    jarTask: TaskProvider<out Jar>
): Configuration.() -> Unit = {
    val jarFile = jarTask.get().archiveFile.get().asFile
    artifacts.removeIf { it.file == jarFile }
}

fun Project.noDefaultJar() {
    val jarTask = tasks.named<Jar>("jar") {
        enabled = false
    }

    configurations.named("apiElements", removeJarTaskArtifact(jarTask))
    configurations.named("runtimeElements", removeJarTaskArtifact(jarTask))
    configurations.named("archives", removeJarTaskArtifact(jarTask))
}

fun Project.runtimeJar(body: Jar.() -> Unit = {}): TaskProvider<out Jar> {
    val jarTask = tasks.named<Jar>("jar")
    jarTask.configure {
        addEmbeddedRuntime()
        setupPublicJar(project.extensions.getByType<BasePluginExtension>().archivesName.get())
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        body()
    }

    return jarTask
}

fun Project.runtimeJarWithRelocation(body: ShadowJar.() -> Unit = {}): TaskProvider<out Jar> {
    noDefaultJar()

    val shadowJarTask = tasks.register<ShadowJar>("shadowJar") {
        archiveClassifier.set("shadow")
        configurations.add(project.configurations["embedded"])
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        body()
    }

    val runtimeJarTask = tasks.register<Jar>("runtimeJar") {
        dependsOn(shadowJarTask)
        from {
            zipTree(shadowJarTask.get().outputs.files.singleFile)
        }
        setupPublicJar(project.extensions.getByType<BasePluginExtension>().archivesName.get())
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    project.addArtifact("archives", runtimeJarTask, runtimeJarTask)
    project.addArtifact("runtimeElements", runtimeJarTask, runtimeJarTask)
    project.addArtifact("apiElements", runtimeJarTask, runtimeJarTask)

    return runtimeJarTask
}

fun Project.runtimeJar(task: TaskProvider<ShadowJar>, body: ShadowJar.() -> Unit = {}): TaskProvider<out Jar> {

    noDefaultJar()

    task.configure {
        configurations.add(project.configurations["embedded"])
        setupPublicJar(project.extensions.getByType<BasePluginExtension>().archivesName.get())
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        body()
    }

    project.addArtifact("archives", task, task)
    project.addArtifact("runtimeElements", task, task)
    project.addArtifact("apiElements", task, task)

    return task
}

@JvmOverloads
fun Project.sourcesJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> {
    configure<JavaPluginExtension> {
        withSourcesJar()
    }

    val sourcesJar = getOrCreateTask<Jar>("sourcesJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("sources")

        from(project.sources())
        addEmbeddedSources()

        body()
    }

    addArtifact("archives", sourcesJar)
    addArtifact("sources", sourcesJar)

    configurePublishedComponent {
        addVariantsFromConfiguration(configurations[SOURCES_ELEMENTS_CONFIGURATION_NAME]) { }
    }

    return sourcesJar
}

/**
 * Also embeds into final '-sources.jar' file source files from embedded dependencies.
 */
fun Project.sourcesJarWithSourcesFromEmbedded(
    vararg embeddedDepSourcesJarTasks: TaskProvider<out Jar>,
    body: Jar.() -> Unit = {},
): TaskProvider<Jar> {
    val sourcesJarTask = sourcesJar(body)

    sourcesJarTask.configure {
        val archiveOperations = serviceOf<ArchiveOperations>()
        embeddedDepSourcesJarTasks.forEach { embeddedSourceJarTask ->
            dependsOn(embeddedSourceJarTask)
            from(embeddedSourceJarTask.map { archiveOperations.zipTree(it.archiveFile) })
        }
    }

    return sourcesJarTask
}

@JvmOverloads
fun Jar.addEmbeddedSources(configurationName: String = "embedded") {
    project.configurations.findByName(configurationName)?.let { embedded ->
        val allSources by lazy {
            embedded.resolvedConfiguration
                .resolvedArtifacts
                .map { it.id.componentIdentifier }
                .filterIsInstance<ProjectComponentIdentifier>()
                .mapNotNull {
                    project.project(it.projectPath).sources()
                }
        }
        from({ allSources })
    }
}

@JvmOverloads
fun Project.javadocJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> {
    configure<JavaPluginExtension> {
        withJavadocJar()
    }

    val javadocTask = getOrCreateTask<Jar>("javadocJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("javadoc")
        tasks.findByName("javadoc")?.let { it as Javadoc }?.takeIf { it.enabled }?.let {
            dependsOn(it)
            from(it.destinationDir)
        }
        body()
    }

    addArtifact("archives", javadocTask)

    configurePublishedComponent {
        addVariantsFromConfiguration(configurations[JAVADOC_ELEMENTS_CONFIGURATION_NAME]) { }
    }

    return javadocTask
}

/**
 * Also embeds into final '-javadoc.jar' file javadoc files from embedded dependencies.
 */
fun Project.javadocJarWithJavadocFromEmbedded(
    vararg embeddedDepJavadocJarTasks: TaskProvider<out Jar>,
    body: Jar.() -> Unit = {},
): TaskProvider<Jar> {
    val javadocJarTask = javadocJar(body)

    javadocJarTask.configure {
        val archiveOperations = serviceOf<ArchiveOperations>()
        embeddedDepJavadocJarTasks.forEach { embeddedJavadocJarTask ->
            dependsOn(embeddedJavadocJarTask)
            from(embeddedJavadocJarTask.map { archiveOperations.zipTree(it.archiveFile) })
        }
    }

    return javadocJarTask
}


fun Project.standardPublicJars() {
    runtimeJar()
    sourcesJar()
    javadocJar()
}

@JvmOverloads
fun Project.publish(moduleMetadata: Boolean = false, sbom: Boolean = true, configure: MavenPublication.() -> Unit = { }) {
    apply<KotlinBuildPublishingPlugin>()

    if (!moduleMetadata) {
        tasks.withType<GenerateModuleMetadata> {
            enabled = false
        }
    }

    val publication = extensions.findByType<PublishingExtension>()
        ?.publications
        ?.findByName(mainPublicationName) as MavenPublication
    publication.configure()
    if (sbom) {
        configureSbom()
    }
}

fun Project.idePluginDependency(block: () -> Unit) {
    val shouldActivate = rootProject.findProperty("publish.ide.plugin.dependencies")?.toString()?.toBoolean() == true
    if (shouldActivate) {
        block()
    }
}

fun Project.publishJarsForIde(projects: List<String>, libraryDependencies: List<String> = emptyList()) {
    val projectsUsedInIntelliJKotlinPlugin: Array<String> by rootProject.extra

    for (projectName in projects) {
        check(projectName in projectsUsedInIntelliJKotlinPlugin) {
            "`$projectName` is used in IntelliJ Kotlin Plugin, it should be added to `extra[\"projectsUsedInIntelliJKotlinPlugin\"]`"
        }
    }

    idePluginDependency {
        publishProjectJars(projects, libraryDependencies)
    }
    configurations.all {
        // Don't allow `ideaIC` from compiler to leak into Kotlin plugin modules. Compiler and
        // plugin may depend on different versions of IDEA and it will lead to version conflict
        exclude(module = ideModuleName())
    }
    dependencies {
        projects.forEach {
            jpsLikeJarDependency(project(it), JpsDepScope.COMPILE, { isTransitive = false }, exported = true)
        }
        libraryDependencies.forEach {
            jpsLikeJarDependency(it, JpsDepScope.COMPILE, exported = true)
        }
    }
}

/**
 * If you need to pack both tests and test fixtures for some module (i.e. xyz) you need to:
 * - use `testsJarToBeUsedAlongWithFixtures()` instead of `testsJar()` utility in the `build.gradle.kts` of `xyz` project
 * - pass `xyz` both to [projectWithFixturesNames] and [projectWithRenamedTestJarNames]
 */
fun Project.publishTestJarsForIde(
    projectNames: List<String>,
    projectWithFixturesNames: List<String> = emptyList(),
    projectWithRenamedTestJarNames: List<String> = emptyList(),
) {
    idePluginDependency {
        // Compiler test infrastructure should not affect test running in IDE.
        // If required, the components should be registered on the IDE plugin side.
        val excludedPaths = listOf("junit-platform.properties", "META-INF/services/**/*")
        publishTestJar(
            projectNames,
            projectWithFixturesNames,
            projectWithRenamedTestJarNames,
            excludedPaths,
        )
    }
    configurations.all {
        // Don't allow `ideaIC` from compiler to leak into Kotlin plugin modules. Compiler and
        // plugin may depend on different versions of IDEA and it will lead to version conflict
        exclude(module = ideModuleName())
    }
    dependencies {
        fun declareDependency(notation: Any) {
            jpsLikeJarDependency(notation, JpsDepScope.COMPILE, exported = true)
        }

        for (projectName in projectNames) {
            declareDependency(projectTests(projectName))
        }
        for (projectName in projectWithFixturesNames) {
            declareDependency(testFixtures(project(projectName)))
        }
        for (projectName in projectWithRenamedTestJarNames) {
            declareDependency(project(projectName, "testsJarConfig"))
        }
    }
}

fun Project.publishProjectJars(projects: List<String>, libraryDependencies: List<String> = emptyList()) {
    apply<JavaPlugin>()

    val fatJarContents by configurations.creating

    dependencies {
        for (projectName in projects) {
            fatJarContents(project(projectName)) { isTransitive = false }
        }

        for (libraryDependency in libraryDependencies) {
            fatJarContents(libraryDependency) { isTransitive = false }
        }
    }

    publish()

    val jar: Jar by tasks

    jar.apply {
        dependsOn(fatJarContents)
        val archiveOperations = project.serviceOf<ArchiveOperations>()
        from {
            fatJarContents.map(archiveOperations::zipTree)
        }
    }

    sourcesJar {
        for (projectPath in projects) {
            val projectTasks = project(projectPath).tasks
            if (projectTasks.names.any { it == "compileKotlin" }) {
                // this is needed in order to declare explicit dependency on code generation tasks
                dependsOn(projectTasks.named("compileKotlin").map { it.dependsOn })
            }
        }
        from {
            projects.map {
                project(it).mainSourceSet.allSource
            }
        }
    }

    javadocJar()
}

private fun Project.publishTestJar(
    projects: List<String>,
    projectWithFixturesNames: List<String>,
    projectWithRenamedTestJarNames: List<String>,
    excludedPaths: List<String>,
) {
    apply<JavaPlugin>()

    val fatJarContents by configurations.creating

    dependencies {
        for (projectName in projects) {
            fatJarContents(project(projectName, configuration = "tests-jar")) { isTransitive = false }
        }

        for (projectName in projectWithFixturesNames) {
            fatJarContents(testFixtures(project(projectName)) as ModuleDependency) { isTransitive = false }
        }

        for (projectName in projectWithRenamedTestJarNames) {
            fatJarContents(project(projectName, configuration = "testsJarConfig")) { isTransitive = false }
        }
    }

    publish(sbom = false)

    val jar: Jar by tasks

    jar.apply {
        dependsOn(fatJarContents)
        val archiveOperations = project.serviceOf<ArchiveOperations>()
        from {
            fatJarContents.map(archiveOperations::zipTree)
        }

        exclude(excludedPaths)
    }

    sourcesJar {
        fun registerTestSources(projectNames: List<String>) {
            from {
                projectNames.map { project(it).testSourceSet.allSource }
            }
        }

        registerTestSources(projects)
        registerTestSources(projectWithRenamedTestJarNames)

        from {
            projectWithFixturesNames.map { project(it).sourceSets.getByName(TestFixturesSupport.TEST_FIXTURE_SOURCESET_NAME).allSource }
        }
    }

    javadocJar()
}

fun Project.addArtifact(configuration: Configuration, task: Task, artifactRef: Any, body: ConfigurablePublishArtifact.() -> Unit = {}) {
    artifacts.add(configuration.name, artifactRef) {
        builtBy(task)
        body()
    }
}

fun Project.addArtifact(configurationName: String, task: Task, artifactRef: Any, body: ConfigurablePublishArtifact.() -> Unit = {}) =
    addArtifact(configurations.getOrCreate(configurationName), task, artifactRef, body)

fun <T : Task> Project.addArtifact(
    configurationName: String,
    task: TaskProvider<T>,
    artifactRef: Any,
    body: ConfigurablePublishArtifact.() -> Unit = {}
): PublishArtifact {
    configurations.maybeCreate(configurationName)
    return artifacts.add(configurationName, artifactRef) {
        builtBy(task)
        body()
    }
}

fun Project.cleanArtifacts() {
    configurations["archives"].artifacts.let { artifacts ->
        artifacts.forEach {
            artifacts.remove(it)
        }
    }
}

fun Project.configurePublishedComponent(configure: AdhocComponentWithVariants.() -> Unit) =
    (components.findByName(KotlinBuildPublishingPlugin.ADHOC_COMPONENT_NAME) as AdhocComponentWithVariants?)?.apply(configure)
