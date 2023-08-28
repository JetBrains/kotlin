package org.jetbrains.dokka.analysis.kotlin.symbols.plugin

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.io.URLUtil
import org.jetbrains.dokka.Platform
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

internal fun Platform.toTargetPlatform() = when (this) {
    Platform.js, Platform.wasm -> JsPlatforms.defaultJsPlatform
    Platform.common -> CommonPlatforms.defaultCommonPlatform
    Platform.native -> NativePlatforms.unspecifiedNativePlatform
    Platform.jvm -> JvmPlatforms.defaultJvmPlatform
}

private fun getJdkHomeFromSystemProperty(): File? {
    val javaHome = File(System.getProperty("java.home"))
    if (!javaHome.exists()) {
        // messageCollector.report(CompilerMessageSeverity.WARNING, "Set existed java.home to use JDK")
        return null
    }
    return javaHome
}

internal fun getLanguageVersionSettings(
    languageVersionString: String?,
    apiVersionString: String?
): LanguageVersionSettingsImpl {
    val languageVersion = LanguageVersion.fromVersionString(languageVersionString) ?: LanguageVersion.LATEST_STABLE
    val apiVersion =
        apiVersionString?.let { ApiVersion.parse(it) } ?: ApiVersion.createByLanguageVersion(languageVersion)
    return LanguageVersionSettingsImpl(
        languageVersion = languageVersion,
        apiVersion = apiVersion, analysisFlags = hashMapOf(
            // special flag for Dokka
            // force to resolve light classes (lazily by default)
            AnalysisFlags.eagerResolveOfLightClasses to true
        )
    )
}

// it should be changed after https://github.com/Kotlin/dokka/issues/3114
internal fun createAnalysisSession(
    classpath: List<File>,
    sourceRoots: Set<File>,
    analysisPlatform: Platform,
    languageVersion: String?,
    apiVersion: String?,
    applicationDisposable: Disposable,
    projectDisposable: Disposable
): Pair<StandaloneAnalysisAPISession, KtSourceModule> {

    var sourceModule: KtSourceModule? = null
    val analysisSession = buildStandaloneAnalysisAPISession(
        applicationDisposable = applicationDisposable,
        projectDisposable = projectDisposable,
        withPsiDeclarationFromBinaryModuleProvider = false
    ) {
        val project = project
        val targetPlatform = analysisPlatform.toTargetPlatform()
        fun KtModuleBuilder.addModuleDependencies(moduleName: String) {
            val libraryRoots = classpath
            addRegularDependency(
                buildKtLibraryModule {
                    contentScope = ProjectScope.getLibrariesScope(project)
                    this.platform = targetPlatform
                    this.project = project
                    binaryRoots = libraryRoots.map { it.toPath() }
                    libraryName = "Library for $moduleName"
                }
            )
            getJdkHomeFromSystemProperty()?.let { jdkHome ->
                val vfm = VirtualFileManager.getInstance()
                val jdkHomePath = jdkHome.toPath()
                val jdkHomeVirtualFile = vfm.findFileByNioPath(jdkHome.toPath())//vfm.findFileByPath(jdkHomePath)
                val binaryRoots = LibraryUtils.findClassesFromJdkHome(jdkHomePath).map {
                    Paths.get(URLUtil.extractPath(it))
                }
                addRegularDependency(
                    buildKtSdkModule {
                        contentScope = GlobalSearchScope.fileScope(project, jdkHomeVirtualFile)
                        this.platform = targetPlatform
                        this.project = project
                        this.binaryRoots = binaryRoots
                        sdkName = "JDK for $moduleName"
                    }
                )
            }
        }
        sourceModule = buildKtSourceModule {
            this.languageVersionSettings = getLanguageVersionSettings(languageVersion, apiVersion)

            //val fs = StandardFileSystems.local()
            //val psiManager = PsiManager.getInstance(project)
            // TODO: We should handle (virtual) file changes announced via LSP with the VFS
            /*val ktFiles = sources
                .flatMap { Files.walk(it).toList() }
                .mapNotNull { fs.findFileByPath(it.toString()) }
                .mapNotNull { psiManager.findFile(it) }
                .map { it as KtFile }*/
            val sourcePaths = sourceRoots.map { it.absolutePath }
            val (ktFilePath, javaFilePath) = getSourceFilePaths(sourcePaths).partition { it.endsWith(KotlinFileType.EXTENSION) }
            val javaFiles: List<PsiFileSystemItem> = getPsiFilesFromPaths(project, javaFilePath)
            val ktFiles: List<KtFile> = getPsiFilesFromPaths(project, getSourceFilePaths(ktFilePath))
            addSourceRoots(ktFiles + javaFiles)
            contentScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)
            platform = targetPlatform
            moduleName = "<module>"
            this.project = project
            addModuleDependencies(moduleName)
        }

        buildKtModuleProvider {
            platform = targetPlatform
            this.project = project
            addModule(sourceModule!!)
        }
    }
    // TODO remove further
    CoreApplicationEnvironment.registerExtensionPoint(
        analysisSession.project.extensionArea,
        KtResolveExtensionProvider.EP_NAME.name,
        KtResolveExtensionProvider::class.java
    )
    return Pair(analysisSession, sourceModule ?: throw IllegalStateException())
}

// ----------- copy-paste from Analysis API ----------------------------------------------------------------------------
/**
 * Collect source file path from the given [root] store them in [result].
 *
 * E.g., for `project/app/src` as a [root], this will walk the file tree and
 * collect all `.kt` and `.java` files under that folder.
 *
 * Note that this util gracefully skips [IOException] during file tree traversal.
 */
internal fun collectSourceFilePaths(
    root: Path,
    result: MutableSet<String>
) {
    // NB: [Files#walk] throws an exception if there is an issue during IO.
    // With [Files#walkFileTree] with a custom visitor, we can take control of exception handling.
    Files.walkFileTree(
        root,
        object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                return if (Files.isReadable(dir))
                    FileVisitResult.CONTINUE
                else
                    FileVisitResult.SKIP_SUBTREE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (!Files.isRegularFile(file) || !Files.isReadable(file))
                    return FileVisitResult.CONTINUE
                val ext = file.toFile().extension
                if (ext == KotlinFileType.EXTENSION || ext == "java"/*JavaFileType.DEFAULT_EXTENSION*/) {
                    result.add(file.toString())
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
                // TODO: report or log [IOException]?
                // NB: this intentionally swallows the exception, hence fail-safe.
                // Skipping subtree doesn't make any sense, since this is not a directory.
                // Skipping sibling may drop valid file paths afterward, so we just continue.
                return FileVisitResult.CONTINUE
            }
        }
    )
}

/**
 * Collect source file path as [String] from the given source roots in [sourceRoot].
 *
 * this util collects all `.kt` and `.java` files under source roots.
 */
internal fun getSourceFilePaths(
    sourceRoot: Collection<String>,
    includeDirectoryRoot: Boolean = false,
): Set<String> {
    val result = mutableSetOf<String>()
    sourceRoot.forEach { srcRoot ->
        val path = Paths.get(srcRoot)
        if (Files.isDirectory(path)) {
            // E.g., project/app/src
            collectSourceFilePaths(path, result)
            if (includeDirectoryRoot) {
                result.add(srcRoot)
            }
        } else {
            // E.g., project/app/src/some/pkg/main.kt
            result.add(srcRoot)
        }
    }

    return result
}

internal inline fun <reified T : PsiFileSystemItem> getPsiFilesFromPaths(
    project: Project,
    paths: Collection<String>,
): List<T> {
    val fs = StandardFileSystems.local()
    val psiManager = PsiManager.getInstance(project)
    val result = mutableListOf<T>()
    for (path in paths) {
        val vFile = fs.findFileByPath(path) ?: continue
        val psiFileSystemItem =
            if (vFile.isDirectory)
                psiManager.findDirectory(vFile) as? T
            else
                psiManager.findFile(vFile) as? T
        psiFileSystemItem?.let { result.add(it) }
    }
    return result
}