import com.intellij.openapi.Disposable
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import com.jetbrains.cidr.xcode.*
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.AppleProductType
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import com.jetbrains.cidr.xcode.frameworks.buildSystem.BuildSettingNames
import com.jetbrains.cidr.xcode.model.*
import com.jetbrains.cidr.xcode.plist.Plist
import com.jetbrains.cidr.xcode.plist.XMLPlistDriver
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.getValue
import org.gradle.language.nativeplatform.internal.Names
import org.gradle.process.internal.ExecActionFactory
import org.gradle.process.internal.ExecException
import java.io.*
import javax.inject.Inject
import javax.swing.SwingUtilities

private class AppleSourceSetFactory(private val project: Project) : NamedDomainObjectFactory<AppleSourceSet> {
    override fun create(name: String): AppleSourceSet = DefaultAppleSourceSet(name, project.objects).apply {
        apple.srcDir(File(project.file("src/$name"), "apple"))
    }
}

@Suppress("ABSTRACT_MEMBER_NOT_IMPLEMENTED")
private class DefaultAppleSourceSet(
    @Suppress("ACCIDENTAL_OVERRIDE") override val name: String,
    objects: ObjectFactory
) : Named, AppleSourceSet {
    override val apple: SourceDirectorySet = objects.sourceDirectorySet("$name Apple source", name)
}

private abstract class AppleTask @Inject constructor(
    protected val target: AppleTarget,
    protected val execActionFactory: ExecActionFactory
) : DefaultTask() {
    init {
        @Suppress("LeakingThis")
        onlyIf { SystemInfoRt.isMac }
    }

    protected open val baseDir: File
        get() = System.getProperty("xcodeProjBaseDir")?.let(::File) ?: project.buildDir.resolve("tmp/apple/${target.name}")

    protected val teamIDOrNull: String?
        @Input @Optional get() = project.apple.teamID

    protected val teamID: String
        get() = teamIDOrNull
            ?: throw GradleException("Failed to retrieve Apple Development Team ID.\nPlease specify via 'apple.teamID' property")

    protected fun xcodeBuild(vararg args: String) {
        logger.lifecycle("RUNNING XCODEBUILD:")

        with(execActionFactory.newExecAction()) {
            environment("DEVELOPER_DIR", XcodeBase.getBasePath())
            commandLine(
                "xcrun", "xcodebuild",
                *args
            )
            workingDir = baseDir
            execute().assertNormalExitValue()
        }
    }
}

private open class AppleGenerateXcodeProjectTask @Inject constructor(
    target: AppleTarget,
    execActionFactory: ExecActionFactory
) : AppleTask(target, execActionFactory) {
    override val baseDir: File
        @OutputDirectory get() = super.baseDir

    val sourceDirectorySet: SourceDirectorySet
        @InputFiles get() = target.sourceSet.apple
    val testSourceDirectorySet: SourceDirectorySet
        @InputFiles get() = target.testSourceSet.apple

    @TaskAction
    fun execute() {
        val teamID = teamIDOrNull
        val projectDir = baseDir.resolve("${target.name}.xcodeproj")

        if (projectDir.exists()) {
            val deleted = FileUtilRt.delete(projectDir)
            if (!deleted) throw ExecException("failed to delete directory $projectDir")
        }
        projectDir.mkdirs()

        val projectFile = projectDir.resolve("project.pbxproj")
        projectFile.createNewFile()

        fun writePlist(name: String, map: Map<String, Any>): File {
            val plist = Plist().also { plist ->
                plist["CFBundleDevelopmentRegion"] = "$(DEVELOPMENT_LANGUAGE)"
                plist["CFBundleExecutable"] = "$(EXECUTABLE_NAME)"
                plist["CFBundleIdentifier"] = "$(PRODUCT_BUNDLE_IDENTIFIER)"
                plist["CFBundleInfoDictionaryVersion"] = "6.0"
                plist["CFBundleName"] = "$(PRODUCT_NAME)"
                plist["CFBundlePackageType"] = "APPL"
                plist["CFBundleShortVersionString"] = "1.0"
                plist["CFBundleVersion"] = "1"
            }
            plist += map

            val file = baseDir.resolve("$name.plist")
            XMLPlistDriver().write(plist, file)
            return file
        }

        val targetPlist = mutableMapOf<String, Any>().also { plist ->
            target.launchStoryboard?.let { plist["UILaunchStoryboardName"] = it }
            val mainStoryboard = target.mainStoryboard
            val sceneDelegateClass = target.sceneDelegateClass

            if (mainStoryboard != null && sceneDelegateClass != null) {
                logger.warn("Both mainStoryboard and sceneDelegateClass are specified, sceneDelegateClass will be ignored")
            }

            when {
                mainStoryboard != null -> {
                    plist["UIMainStoryboardFile"] = mainStoryboard
                }
                sceneDelegateClass != null -> {
                    plist["UIApplicationSceneManifest"] = Plist().also {
                        it["UIApplicationSupportsMultipleScenes"] = false
                        it["UISceneConfigurations"] = Plist().also {
                            it["UIWindowSceneSessionRoleApplication"] = listOf(
                                Plist().also {
                                    it["UISceneConfigurationName"] = "Default Configuration"
                                    it["UISceneDelegateClassName"] = "\$(PRODUCT_MODULE_NAME).$sceneDelegateClass"
                                }
                            )
                        }
                    }
                }
                else -> logger.warn("Please specify either mainStoryboard or sceneDelegateClass")
            }
        }
        val infoPlistFile = writePlist("Info-${target.name}", targetPlist)
        val testInfoPlistFile = writePlist("Info-${target.name}Tests", emptyMap())

        val vBaseDir = StandardFileSystems.local().refreshAndFindFileByPath(baseDir.path)!!
        val vProjectFile =
            object : CoreLocalVirtualFile(StandardFileSystems.local() as CoreLocalFileSystem, projectFile) {
                override fun isWritable() = true
                override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long) =
                    VfsUtilCore.outputStreamAddingBOM(BufferedOutputStream(FileOutputStream(projectFile)), this)
            }

        // TODO Dirty hack -- is there really no better way?
        val frameworks = target.configuration.flatMap { dep ->
            when {
                !dep.isDirectory -> null
                dep.extension != "framework" -> dep.list()?.mapNotNull { name ->
                    File(dep, name).takeIf { it.isDirectory && it.extension == "framework" }
                }
                else -> listOf(dep)
            } ?: emptyList()
        }
        val frameworkDirs = frameworks.mapTo(mutableSetOf()) { it.parentFile }.toList()
        val frameworkSearchPaths = frameworkDirs.map { it.toRelativeString(baseDir) }

        val intellijProject = project.appleImpl.intellijProject!!
        val pbxProjectFile = PBXProjectFileManipulator.createNewProject(
            intellijProject,
            vBaseDir,
            vProjectFile,
            null,
            null
        )

        val platform = AppleSdkManager.getInstance().findPlatformByType(ApplePlatform.Type.IOS)
            ?: throw RuntimeException("Could not find SDK.")
        val mainGroup = pbxProjectFile.projectObject.mainGroup!!

        val symRoot = sourceDirectorySet.outputDir

        with(pbxProjectFile.manipulator) {
            for (configName in arrayOf("Debug", "Release")) {
                addConfiguration(configName, emptyMap(), null)
            }

            val sourcesGroupDir = sourceDirectorySet.srcDirs.firstOrNull { it.isDirectory && it.exists() }
                ?: baseDir.resolve("Sources")
            val sourcesGroup = addGroup("SOURCE_ROOT", "Sources", sourcesGroupDir.path)

            val testSourcesGroupDir = testSourceDirectorySet.srcDirs.firstOrNull { it.isDirectory && it.exists() }
                ?: baseDir.resolve("Tests")
            val testSourcesGroup = addGroup("SOURCE_ROOT", "Tests", testSourcesGroupDir.path)

            val frameworksGroupDir = frameworkDirs.firstOrNull() ?: baseDir.resolve("Frameworks")
            val frameworksGroup = addGroup(null, "Frameworks", frameworksGroupDir.path)

            fun addTarget(name: String, typeId: String, customSettings: Map<String, Any>): PBXTarget {
                val settings = mutableMapOf(
                    BuildSettingNames.SDKROOT to ApplePlatform.Type.IOS.platformName,
                    BuildSettingNames.SYMROOT to symRoot.toRelativeString(baseDir),
                    BuildSettingNames.OBJROOT to "build",
                    BuildSettingNames.PRODUCT_NAME to project.name,
                    BuildSettingNames.PRODUCT_MODULE_NAME to target.name,
                    BuildSettingNames.FRAMEWORK_SEARCH_PATHS to frameworkSearchPaths,
                    BuildSettingNames.LD_RUNPATH_SEARCH_PATHS to "$(inherited) @executable_path/Frameworks @loader_path/Frameworks",
                    BuildSettingNames.SWIFT_VERSION to "5.0",
                    BuildSettingNames.ALWAYS_SEARCH_USER_PATHS to "NO",
                    BuildSettingNames.CLANG_ENABLE_MODULES to "YES",
                    BuildSettingNames.CLANG_ENABLE_OBJC_ARC to "YES"
                )
                settings += customSettings

                val codeSignSettings = teamIDOrNull?.let { mapOf("DEVELOPMENT_TEAM" to teamID) } ?: emptyMap()

                val pbxTarget = addNativeTarget(name, typeId, settings, platform)
                addConfiguration("Debug", settings + mapOf(BuildSettingNames.ONLY_ACTIVE_ARCH to "YES", "ENABLE_TESTABILITY" to "YES"), pbxTarget)
                addConfiguration("Release", settings + codeSignSettings, pbxTarget)

                for (phaseType in listOf(
                    PBXBuildPhase.Type.SOURCES,
                    PBXBuildPhase.Type.FRAMEWORKS,
                    PBXBuildPhase.Type.RESOURCES
                )) {
                    addBuildPhase(phaseType, emptyMap(), pbxTarget)
                }

                return pbxTarget
            }

            val targetSettings = mutableMapOf(
                BuildSettingNames.INFOPLIST_FILE to infoPlistFile.toRelativeString(baseDir),
                BuildSettingNames.PRODUCT_BUNDLE_IDENTIFIER to getQualifiedName(target.name), // should start with team qualifier (e.g. "com.jetbrains")
                BuildSettingNames.ASSETCATALOG_COMPILER_APPICON_NAME to "AppIcon"
            )
            target.bridgingHeader?.let {
                targetSettings[BuildSettingNames.SWIFT_OBJC_BRIDGING_HEADER] = sourcesGroupDir.resolve(it).toRelativeString(baseDir)
            }
            val pbxTarget = addTarget(target.name, AppleProductType.APPLICATION_TYPE_ID, targetSettings)

            val testTargetSettings = mapOf(
                BuildSettingNames.INFOPLIST_FILE to testInfoPlistFile.toRelativeString(baseDir),
                BuildSettingNames.PRODUCT_BUNDLE_IDENTIFIER to getQualifiedName(target.name) + "Tests",
                BuildSettingNames.TEST_HOST to "$(BUILT_PRODUCTS_DIR)/${project.name}.app/${project.name}",
                BuildSettingNames.PRODUCT_NAME to "${project.name}Tests",
                BuildSettingNames.PRODUCT_MODULE_NAME to "${project.name}Tests",
                "BUNDLE_LOADER" to "$(TEST_HOST)"
            )
            val pbxTestTarget = addTarget(target.name + "Tests", AppleProductType.UNIT_TEST_TYPE_ID, testTargetSettings)
            addTargetDependency(pbxTestTarget, pbxTarget)

            for (file in arrayOf(infoPlistFile, testInfoPlistFile)) {
                addFile(file.path, emptyArray(), mainGroup, false)
            }

            fun addTargetFiles(targetMemberships: Array<PBXTarget>, group: PBXGroup, sourceDirectorySet: SourceDirectorySet) {
                sourceDirectorySet.srcDirs.flatMap { dir ->
                    dir.listFiles()
                        ?.filterNot { it.name.startsWith(".") }
                        ?.sorted()
                        ?: emptyList()
                }.forEach { file ->
                    // don't flatten
                    addFile(file.path, targetMemberships, group, false)
                }
            }

            val targetMemberships = arrayOf(pbxTarget)
            addTargetFiles(targetMemberships, sourcesGroup, sourceDirectorySet)
            addTargetFiles(arrayOf(pbxTestTarget), testSourcesGroup, testSourceDirectorySet)

            val embedFrameworksPhase = addBuildPhase(
                PBXBuildPhase.Type.COPY_FILES, mapOf(
                    "name" to "Embed Frameworks",
                    "dstSubfolderSpec" to PBXCopyFilesBuildPhase.DestinationType.FRAMEWORKS.spec
                ), pbxTarget
            )

            for (file in frameworks) {
                val result = addFile(
                    file.path,
                    targetMemberships,
                    frameworksGroup,
                    false,
                    null,
                    PBXBuildPhase.Type.FRAMEWORKS,
                    false
                )
                val buildFile = addToBuildPhase(pbxTarget, embedFrameworksPhase, result.reference)
                buildFile.setAttribute("settings", PBXDictionary(pbxProjectFile).apply {
                    setAttribute("ATTRIBUTES", listOf("CodeSignOnCopy", "RemoveHeadersOnCopy"))
                })
            }
        }

        SwingUtilities.invokeAndWait {
            pbxProjectFile.save()
        }
    }

    private fun getQualifiedName(targetName: String): String = listOfNotNull(
        project.group.toString().takeIf { it.isNotBlank() },
        project.name.takeIf { it.isNotBlank() },
        targetName
    ).joinToString(".")
}

private open class AppleBuildTask @Inject constructor(target: AppleTarget, execActionFactory: ExecActionFactory) :
    AppleTask(target, execActionFactory) {
    init {
        group = BasePlugin.BUILD_GROUP
    }

    val projectDir: File
        @InputDirectory get() = baseDir.resolve("${target.name}.xcodeproj")

    var configName: String = "Debug"
        @Input get

    @TaskAction
    fun build() {
        val sdkArgs = xcodeBuildSdk?.let { arrayOf("-sdk", it) } ?: emptyArray()
        xcodeBuild(
            "-project", projectDir.toRelativeString(baseDir),
            "-scheme", xcodeScheme,
            "-configuration", configName,
            "-derivedDataPath", "DerivedData",
            *sdkArgs,
            xcodeBuildTask, *xcodeBuildArgs
        )
    }

    protected open val xcodeBuildTask: String = "build"
    protected open val xcodeBuildSdk: String? = "iphonesimulator"
    protected open val xcodeBuildArgs: Array<String> = emptyArray()
    protected open val xcodeScheme: String = target.name
}

private open class AppleBuildTestTask @Inject constructor(target: AppleTarget, execActionFactory: ExecActionFactory) :
    AppleBuildTask(target, execActionFactory) {
    override val xcodeBuildTask: String = "build-for-testing"
    override val xcodeScheme: String = target.name + "Tests"
}

private open class AppleArchiveTask @Inject constructor(target: AppleTarget, execActionFactory: ExecActionFactory) :
    AppleBuildTask(target, execActionFactory) {
    init {
        configName = "Release"
    }

    val xcarchive: File
        @OutputDirectory get() = target.sourceSet.apple.outputDir
            .resolve("archive")
            .resolve("${target.name}.xcarchive")

    override val xcodeBuildTask: String = "archive"
    override val xcodeBuildSdk: String? = null
    override val xcodeBuildArgs: Array<String>
        get() = arrayOf(
            "-destination", "generic/platform=iOS",
            "-archivePath", xcarchive.toRelativeString(baseDir),
            "-allowProvisioningUpdates"
        )
    override val xcodeScheme: String = target.name
}

private open class AppleExportIPATask @Inject constructor(
    target: AppleTarget,
    execActionFactory: ExecActionFactory
) : AppleTask(target, execActionFactory) {

    // development / app-store / ad-hoc / enterprise
    var method: String = "development"
        @Input get

    @TaskAction
    fun build() {
        val xcarchive = dependsOn.filterIsInstance<AppleArchiveTask>().single().xcarchive

        val exportOptions = Plist().also { plist ->
            plist["method"] = method
            plist["teamID"] = teamID
        }
        val exportOptionsPlist = baseDir.resolve("${target.name}-exportOptions.plist")
        XMLPlistDriver().write(exportOptions, exportOptionsPlist)

        val optionsPlistPath = exportOptionsPlist.toRelativeString(baseDir)
        xcodeBuild(
            "-exportArchive",
            "-archivePath", xcarchive.toRelativeString(baseDir),
            "-exportOptionsPlist", optionsPlistPath,
            "-exportPath", target.sourceSet.apple.outputDir.toRelativeString(baseDir),
            "-allowProvisioningUpdates"
        )
    }
}

private open class AppleTargetFactory @Inject constructor(
    private val project: Project,
    private val objects: ObjectFactory
) : NamedDomainObjectFactory<AppleTarget> {
    override fun create(name: String): AppleTarget = objects.newInstance(DefaultAppleTarget::class.java, project, name)
}

@Suppress("ABSTRACT_MEMBER_NOT_IMPLEMENTED")
private open class DefaultAppleTarget @Inject constructor(
    project: Project,
    @Suppress("ACCIDENTAL_OVERRIDE") final override val name: String,
    configurations: ConfigurationContainer
) : Named, AppleTarget {
    override val configuration: Configuration by configurations.register(Names.of(name).withSuffix("implementation"))
    override val sourceSet: AppleSourceSet by project.apple.sourceSets.register("${name}Main") {
        apple.outputDir = project.buildDir.resolve("bin/$name")
    }
    override val testSourceSet: AppleSourceSet by project.apple.sourceSets.register("${name}Test") {
        apple.outputDir = project.buildDir.resolve("bin/$name")
    }

    val generateXcodeprojTask: AppleGenerateXcodeProjectTask by project.tasks.register("generateXcodeproj", AppleGenerateXcodeProjectTask::class.java, this).also {
        it.configure {
            dependsOn(configuration.incoming.files)
        }
    }
    override val buildTask: AppleBuildTask by project.tasks.register("build${name.capitalize()}Main", AppleBuildTask::class.java, this).also {
        it.configure {
            dependsOn(configuration.incoming.files)
            dependsOn(generateXcodeprojTask)
        }

        project.tasks.named(BasePlugin.ASSEMBLE_TASK_NAME) {
            dependsOn(it)
        }
    }
    override val buildTestTask: AppleBuildTask by project.tasks.register("build${name.capitalize()}Test", AppleBuildTestTask::class.java, this).also {
        it.configure {
            dependsOn(configuration.incoming.files)
            dependsOn(generateXcodeprojTask)
        }
    }
    override val archiveTask: AppleArchiveTask by project.tasks.register("archive", AppleArchiveTask::class.java, this).also {
        it.configure {
            dependsOn(configuration.incoming.files)
            dependsOn(generateXcodeprojTask)
        }
    }
    override val exportIPATask: AppleExportIPATask by project.tasks.register("exportIpa", AppleExportIPATask::class.java, this).also {
        it.configure {
            dependsOn(archiveTask)
        }
    }

    override var launchStoryboard: String? = null
    override var mainStoryboard: String? = null
    override var sceneDelegateClass: String? = null
    override var bridgingHeader: String? = null
}

private open class AppleProjectExtensionImpl(project: Project, val intellijProject: com.intellij.openapi.project.Project?) : AppleProjectExtension {
    override var teamID: String? = null

    override val targets: NamedDomainObjectContainer<AppleTarget> =
        project.container(AppleTarget::class.java, project.objects.newInstance(AppleTargetFactory::class.java, project))

    override val sourceSets: NamedDomainObjectContainer<AppleSourceSet> =
        project.container(AppleSourceSet::class.java, AppleSourceSetFactory(project))

    override fun iosApp(configure: AppleTarget.() -> Unit): AppleTarget = iosApp("iosApp", configure)

    override fun iosApp(name: String, configure: AppleTarget.() -> Unit): AppleTarget =
        targets.maybeCreate(name).apply { configure() }
}

open class ApplePlugin @Inject constructor(private val execActionFactory: ExecActionFactory) : Plugin<Project>, Disposable {
    override fun apply(project: Project) {
        val intellijProject = when {
            SystemInfoRt.isMac -> {
                val xcodePath = project.properties["xcode.base"]?.toString() ?: detectXcodeInstallation()
                println("Using Xcode: $xcodePath")

                val applicationEnvironment = CoreXcodeApplicationEnvironment(this, false)
                XcodeSettingsBase.INSTANCE.setSelectedXcodePath(xcodePath)
                CoreXcodeProjectEnvironment(this, applicationEnvironment).project
            }
            else -> null
        }

        project.extensions.create(AppleProjectExtension::class.java, "apple", AppleProjectExtensionImpl::class.java, project, intellijProject)
    }

    override fun dispose() = Unit

    private fun detectXcodeInstallation(): String {
        val xcodeSelectOut = ByteArrayOutputStream()

        with(execActionFactory.newExecAction()) {
            commandLine("xcode-select", "-print-path")
            standardOutput = xcodeSelectOut
            execute().assertNormalExitValue()
        }

        return ByteArrayInputStream(xcodeSelectOut.toByteArray()).bufferedReader().use { it.readLine() }
                ?: throw RuntimeException("Failed to detect Xcode. Make sure Xcode is installed.\n" +
                        "Consider specifying it manually via the `xcode.base` property.")
    }
}

private val Project.appleImpl: AppleProjectExtensionImpl
    get() = apple as AppleProjectExtensionImpl