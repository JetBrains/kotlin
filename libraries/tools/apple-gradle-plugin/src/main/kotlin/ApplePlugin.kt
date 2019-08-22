import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import com.jetbrains.cidr.xcode.XcodeBase
import com.jetbrains.cidr.xcode.XcodeInstallation
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
import org.gradle.api.tasks.TaskAction
import org.gradle.language.nativeplatform.internal.Names
import org.gradle.process.internal.ExecActionFactory
import java.io.*
import javax.inject.Inject

private class AppleSourceSetFactory(private val project: Project) : NamedDomainObjectFactory<AppleSourceSet> {
    override fun create(name: String): AppleSourceSet = AppleSourceSet(name, project.objects).apply {
        apple.srcDir(File(project.file("src/$name"), "apple"))
    }
}

class AppleSourceSet(private val name: String, objects: ObjectFactory) : Named {
    override fun getName(): String = name
    val apple: SourceDirectorySet = objects.sourceDirectorySet("$name Apple source", name)
}

private open class AppleBuildTask @Inject constructor(
        private val target: AppleTarget,
        private val sourceSetProvider: NamedDomainObjectProvider<AppleSourceSet>,
        private val execActionFactory: ExecActionFactory
) : DefaultTask() {
    init {
        group = BasePlugin.BUILD_GROUP
    }

    val sourceSet: AppleSourceSet
        get() = sourceSetProvider.get()

    private fun getQualifiedName(targetName: String): String = listOfNotNull(
            project.group.toString().takeIf { it.isNotBlank() },
            project.name.takeIf { it.isNotBlank() },
            targetName
    ).joinToString(".")

    @TaskAction
    fun build() {
        println("GENERATING XCODEPROJ")

        val baseDir = project.buildDir.resolve("tmp/apple/${target.name}")
        val projectDir = baseDir.resolve("${target.name}.xcodeproj")
        projectDir.mkdirs()

        val projectFile = projectDir.resolve("project.pbxproj")
        projectFile.createNewFile()

        val infoPlistFile = baseDir.resolve("Info-${target.name}.plist")
        XMLPlistDriver().write(Plist().also { plist ->
            plist["CFBundleDevelopmentRegion"] = "$(DEVELOPMENT_LANGUAGE)"
            plist["CFBundleExecutable"] = "$(EXECUTABLE_NAME)"
            plist["CFBundleIdentifier"] = "$(PRODUCT_BUNDLE_IDENTIFIER)"
            plist["CFBundleInfoDictionaryVersion"] = "6.0"
            plist["CFBundleName"] = "$(PRODUCT_NAME)"
            plist["CFBundlePackageType"] = "APPL"
            plist["CFBundleShortVersionString"] = "1.0"
            plist["CFBundleVersion"] = "1"
            target.launchStoryboard?.let { plist["UILaunchStoryboardName"] = it }
            target.mainStoryboard?.let { plist["UIMainStoryboardFile"] = it }
        }, infoPlistFile)

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

        val pbxProjectFile = PBXProjectFileManipulator.createNewProject(
                XcodeWorkspace.project,
                vBaseDir,
                vProjectFile,
                null,
                null
        )

        val configName = "Debug"
        val platform = AppleSdkManager.getInstance().findPlatformByType(ApplePlatform.Type.IOS)
                ?: throw RuntimeException("Could not find SDK.")
        val mainGroup = pbxProjectFile.projectObject.mainGroup!!
        val symRoot = sourceSet.apple.outputDir

        with(pbxProjectFile.manipulator) {
            addConfiguration(configName, emptyMap(), null)

            val sourcesGroupDir = sourceSet.apple.srcDirs.firstOrNull { it.isDirectory && it.exists() }
                    ?: baseDir.resolve("Sources")
            val sourcesGroup = addGroup("SOURCE_ROOT", "Sources", sourcesGroupDir.path)
            val frameworksGroupDir = frameworkDirs.firstOrNull() ?: baseDir.resolve("Frameworks")
            val frameworksGroup = addGroup(null, "Frameworks", frameworksGroupDir.path)

            val targetSettings = mapOf(
                    BuildSettingNames.SDKROOT to ApplePlatform.Type.IOS.platformName,
                    BuildSettingNames.SYMROOT to symRoot.toRelativeString(baseDir),
                    BuildSettingNames.OBJROOT to "build",
                    BuildSettingNames.PRODUCT_BUNDLE_IDENTIFIER to getQualifiedName(target.name),
                    BuildSettingNames.PRODUCT_NAME to target.name,
                    BuildSettingNames.INFOPLIST_FILE to infoPlistFile.toRelativeString(baseDir),
                    BuildSettingNames.FRAMEWORK_SEARCH_PATHS to frameworkSearchPaths,
                    BuildSettingNames.LD_RUNPATH_SEARCH_PATHS to "$(inherited) @executable_path/Frameworks",
                    BuildSettingNames.SWIFT_VERSION to "5.0",
                    BuildSettingNames.ALWAYS_SEARCH_USER_PATHS to "NO",
                    BuildSettingNames.CLANG_ENABLE_MODULES to "YES",
                    BuildSettingNames.ASSETCATALOG_COMPILER_APPICON_NAME to "AppIcon",
                    // Debug
                    BuildSettingNames.ONLY_ACTIVE_ARCH to "YES"
            )
            val pbxTarget = addNativeTarget(target.name, AppleProductType.APPLICATION_TYPE_ID, targetSettings, platform)
            addConfiguration(configName, targetSettings, pbxTarget)

            for (phaseType in listOf(
                    PBXBuildPhase.Type.SOURCES,
                    PBXBuildPhase.Type.FRAMEWORKS,
                    PBXBuildPhase.Type.RESOURCES
            )) {
                addBuildPhase(phaseType, emptyMap(), pbxTarget)
            }

            val embedFrameworksPhase = addBuildPhase(
                    PBXBuildPhase.Type.COPY_FILES, mapOf(
                    "name" to "Embed Frameworks",
                    "dstSubfolderSpec" to PBXCopyFilesBuildPhase.DestinationType.FRAMEWORKS.spec
            ), pbxTarget
            )

            addFile(infoPlistFile.path, emptyArray(), mainGroup, false)

            val targetMemberships = arrayOf(pbxTarget)
            for (file in sourceSet.apple.srcDirs.flatMap { it.listFiles()?.asList() ?: emptyList() }) { // don't flatten
                addFile(file.path, targetMemberships, sourcesGroup, false)
            }

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

        pbxProjectFile.save()

        println("RUNNING XCODEBUILD")

        with(execActionFactory.newExecAction()) {
            environment("DEVELOPER_DIR", XcodeBase.getBasePath())
            commandLine(
                    "xcrun", "xcodebuild",
                    "-project", projectDir.toRelativeString(baseDir),
                    "-scheme", target.name,
                    "-configuration", configName,
                    "-sdk", "iphonesimulator",
                    "-derivedDataPath", "DerivedData" //"-destination", "platform=iOS Simulator,name=iPhone X",
            )
            workingDir = baseDir
            execute().assertNormalExitValue()
        }
    }
}

private open class AppleTargetFactory @Inject constructor(
        private val project: Project,
        private val objects: ObjectFactory
) : NamedDomainObjectFactory<AppleTarget> {
    override fun create(name: String): AppleTarget = objects.newInstance(AppleTarget::class.java, name).also { target ->
        //project.components.add(target)

        val sourceSet = project.apple.sourceSets.register("${name}Main") {
            apple.outputDir = project.buildDir.resolve("bin/$name")
        }

        val buildTask =
                project.tasks.register("build${name.capitalize()}Main", AppleBuildTask::class.java, target, sourceSet)
        buildTask.configure {
            dependsOn(target.configuration.incoming.files)
        }

        project.tasks.named(BasePlugin.ASSEMBLE_TASK_NAME) {
            dependsOn(buildTask)
        }
    }
}

open class AppleTarget @Inject constructor(private val name: String, configurations: ConfigurationContainer) : Named {
    private val names: Names = Names.of(name)
    internal val configuration: Configuration = configurations.create(names.withSuffix("implementation"))

    override fun getName(): String = name
    var launchStoryboard: String? = null
    var mainStoryboard: String? = null
}

open class AppleProjectExtension {
    lateinit var targets: NamedDomainObjectContainer<AppleTarget>
        internal set

    lateinit var sourceSets: NamedDomainObjectContainer<AppleSourceSet>
        internal set

    @JvmOverloads
    fun iosApp(configure: AppleTarget.() -> Unit = { }): AppleTarget = iosApp("iosApp", configure)

    @JvmOverloads
    fun iosApp(name: String, configure: AppleTarget.() -> Unit = { }): AppleTarget =
            targets.maybeCreate(name).apply { configure() }
}

open class ApplePlugin @Inject constructor(private val execActionFactory: ExecActionFactory) : Plugin<Project> {
    override fun apply(project: Project) {
        val xcodePath = project.properties["xcode.base"]?.toString() ?: detectXcodeInstallation()
        println("Using Xcode: $xcodePath")

        XcodeBase.setBaseInstallation(XcodeInstallation(File(xcodePath)))
        CoreXcodeWorkspace.setInstance(XcodeWorkspace.project, XcodeWorkspace)

        val projectExtension = project.extensions.create("apple", AppleProjectExtension::class.java)
        projectExtension.targets = project.container(
                AppleTarget::class.java,
                project.objects.newInstance(AppleTargetFactory::class.java, project)
        )
        projectExtension.sourceSets = project.container(AppleSourceSet::class.java, AppleSourceSetFactory(project))
    }

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

val Project.apple: AppleProjectExtension get() = project.extensions.getByType(AppleProjectExtension::class.java)
fun Project.apple(configure: AppleProjectExtension.() -> Unit = { }) = apple.configure()
