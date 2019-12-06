import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import com.jetbrains.cidr.xcode.CoreXcodeApplicationEnvironment
import com.jetbrains.cidr.xcode.CoreXcodeProjectEnvironment
import com.jetbrains.cidr.xcode.XcodeBase
import com.jetbrains.cidr.xcode.XcodeInstallation
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.AppleProductType
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import com.jetbrains.cidr.xcode.frameworks.buildSystem.BuildSettingNames
import com.jetbrains.cidr.xcode.model.PBXBuildPhase
import com.jetbrains.cidr.xcode.model.PBXCopyFilesBuildPhase
import com.jetbrains.cidr.xcode.model.PBXDictionary
import com.jetbrains.cidr.xcode.model.PBXProjectFileManipulator
import com.jetbrains.cidr.xcode.plist.Plist
import com.jetbrains.cidr.xcode.plist.XMLPlistDriver
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.nativeplatform.internal.Names
import org.gradle.process.internal.ExecActionFactory
import java.io.*
import javax.inject.Inject
import javax.swing.SwingUtilities

private class AppleSourceSetFactory(private val project: Project) : NamedDomainObjectFactory<AppleSourceSet> {
    override fun create(name: String): AppleSourceSet = DefaultAppleSourceSet(name, project.objects).apply {
        apple.srcDir(File(project.file("src/$name"), "apple"))
    }
}

@Suppress("ABSTRACT_MEMBER_NOT_IMPLEMENTED")
private class DefaultAppleSourceSet(@Suppress("ACCIDENTAL_OVERRIDE") override val name: String,
                                    objects: ObjectFactory) : Named, AppleSourceSet {
    override val apple: SourceDirectorySet = objects.sourceDirectorySet("$name Apple source", name)
}

private open class AppleBuildTask @Inject constructor(
        private val target: AppleTarget,
        private val execActionFactory: ExecActionFactory
) : DefaultTask() {
    init {
        group = BasePlugin.BUILD_GROUP
    }

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
                project.appleImpl.intellijProject,
                vBaseDir,
                vProjectFile,
                null,
                null
        )

        val configName = "Debug"
        val platform = AppleSdkManager.getInstance().findPlatformByType(ApplePlatform.Type.IOS)
                ?: throw RuntimeException("Could not find SDK.")
        val mainGroup = pbxProjectFile.projectObject.mainGroup!!

        val sourceDirectorySet = target.sourceSet.apple
        val symRoot = sourceDirectorySet.outputDir

        with(pbxProjectFile.manipulator) {
            addConfiguration(configName, emptyMap(), null)

            val sourcesGroupDir = sourceDirectorySet.srcDirs.firstOrNull { it.isDirectory && it.exists() }
                    ?: baseDir.resolve("Sources")
            val sourcesGroup = addGroup("SOURCE_ROOT", "Sources", sourcesGroupDir.path)
            val frameworksGroupDir = frameworkDirs.firstOrNull() ?: baseDir.resolve("Frameworks")
            val frameworksGroup = addGroup(null, "Frameworks", frameworksGroupDir.path)

            val targetSettings = mutableMapOf(
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
                    BuildSettingNames.CLANG_ENABLE_OBJC_ARC to "YES",
                    BuildSettingNames.ASSETCATALOG_COMPILER_APPICON_NAME to "AppIcon",
                    // Debug
                    BuildSettingNames.ONLY_ACTIVE_ARCH to "YES"
            )
            target.bridgingHeader?.let { targetSettings[BuildSettingNames.SWIFT_OBJC_BRIDGING_HEADER] = sourcesGroupDir.resolve(it).toRelativeString(baseDir) }

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
            for (file in sourceDirectorySet.srcDirs.flatMap {
                it.listFiles()?.apply { sort() }?.asList() ?: emptyList()
            }) { // don't flatten
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

        SwingUtilities.invokeAndWait {
            pbxProjectFile.save()
        }

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
    override fun create(name: String): AppleTarget = objects.newInstance(DefaultAppleTarget::class.java, project, name)
}

@Suppress("ABSTRACT_MEMBER_NOT_IMPLEMENTED")
private open class DefaultAppleTarget @Inject constructor(project: Project,
                                                          @Suppress("ACCIDENTAL_OVERRIDE") final override val name: String,
                                                          configurations: ConfigurationContainer) : Named, AppleTarget {
    override val configuration: Configuration by configurations.register(Names.of(name).withSuffix("implementation"))
    override val sourceSet: AppleSourceSet by project.apple.sourceSets.register("${name}Main") {
        apple.outputDir = project.buildDir.resolve("bin/$name")
    }
    override val testSourceSet: AppleSourceSet by project.apple.sourceSets.register("${name}Test") {
        apple.outputDir = project.buildDir.resolve("bin/$name")
    }
    override val buildTask: AppleBuildTask by project.tasks.register("build${name.capitalize()}Main", AppleBuildTask::class.java, this).also {
        it.configure {
            dependsOn(configuration.incoming.files)
        }

        project.tasks.named(BasePlugin.ASSEMBLE_TASK_NAME) {
            dependsOn(it)
        }
    }
    override var launchStoryboard: String? = null
    override var mainStoryboard: String? = null
    override var bridgingHeader: String? = null
}

private open class AppleProjectExtensionImpl(project: Project, val intellijProject: com.intellij.openapi.project.Project) : AppleProjectExtension {
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
        val xcodePath = project.properties["xcode.base"]?.toString() ?: detectXcodeInstallation()
        println("Using Xcode: $xcodePath")

        val applicationEnvironment = CoreXcodeApplicationEnvironment(this, false)
        XcodeBase.getInstance().setInstallation(XcodeInstallation(File(xcodePath)))
        val intellijProject = CoreXcodeProjectEnvironment(this, applicationEnvironment).project

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