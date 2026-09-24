/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinder
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.scriptingHostConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForModule
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirJavaInterop
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.KmpModuleKind
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.jvm.environment.asJvmClasspathRootId
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.scripting.compiler.plugin.FirScriptingCompilerExtensionRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.SingleScriptCompilationConfigurationProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.withCompilationConfigurationProvider
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import java.io.File
import java.nio.file.Path
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.api.refineConfigurationOnAnnotations
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.util.classpathFromClass

/**
 * The file annotations collector for the legacy PSI-based refinement entry points, which are called outside any FIR session.
 * The annotations are resolved by [collectAndResolveScriptAnnotationsViaFir] in a dedicated "dummy" FIR session, since building FIR for
 * the script requires refined configuration.
 *
 * The session sees the classpath of the regular compilation supplied by the host via [getRegularClasspath], which expected to contain
 * the dependencies of the script and host configurations, and the classpath roots of the accepted annotation classes. The session is
 * created for every call, and only if the script has annotations that could be accepted by its `onAnnotations` refinement handlers.
 */
class PsiScriptAnnotationsCollector(
    private val getRegularClasspath: (KtFile) -> List<File> = { emptyList() },
) {

    fun collectAnnotations(
        scriptFile: KtFile,
        compilationConfiguration: ScriptCompilationConfiguration,
        hostConfiguration: ScriptingHostConfiguration,
    ): ResultWithDiagnostics<ScriptCollectedData> {
        if (!scriptFile.mayHaveAcceptedAnnotations(compilationConfiguration)) return ScriptCollectedData(emptyMap()).asSuccess()
        // the annotation resolution session obtains the script's configuration through the provider in the host configuration
        val baseHostConfiguration =
            hostConfiguration.withCompilationConfigurationProvider(SingleScriptCompilationConfigurationProvider(compilationConfiguration))
        val scriptHostConfiguration =
            compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration].withDefaultsFrom(baseHostConfiguration)
        val classpath = classpathFor(getRegularClasspath(scriptFile), compilationConfiguration, scriptHostConfiguration)
        return collectAndResolveScriptAnnotationsViaFir(
            KtFileScriptSource(scriptFile),
            compilationConfiguration,
            baseHostConfiguration,
            getSessionForAnnotationResolution = { _, _ ->
                createAnnotationResolutionSession(scriptFile.project, classpath, scriptHostConfiguration)
            },
            convertToFir = SourceCode::convertToFirViaPsi,
            tolerateInvalidAnnotations = true,
        )
    }
}

private fun KtFile.mayHaveAcceptedAnnotations(compilationConfiguration: ScriptCompilationConfiguration): Boolean {
    val acceptedShortNames = compilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]
        ?.flatMap { handler -> handler.annotations.map { it.typeName.substringAfterLast('.').substringAfterLast('$') } }
        ?.takeIf { it.isNotEmpty() }
        ?: return false
    val annotationEntries = annotationEntries
    if (annotationEntries.isEmpty()) return false
    if (importDirectives.any { it.aliasName != null }) return true
    return annotationEntries.any { it.shortName?.asString() in acceptedShortNames }
}

private fun classpathFor(
    regularClasspath: List<File>,
    compilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
): List<Path> {
    val dependencies = hostConfiguration[ScriptingHostConfiguration.configurationDependencies].orEmpty() +
            compilationConfiguration[ScriptCompilationConfiguration.dependencies].orEmpty()
    val configurationClasspath = dependencies.flatMap { (it as? JvmDependency)?.classpath ?: emptyList() }
    val annotationsClasspath = loadAcceptedAnnotationClasses(compilationConfiguration, hostConfiguration) { _, _ -> }
        .flatMap { classpathFromClass(it).orEmpty() }
    return (regularClasspath + configurationClasspath + annotationsClasspath)
        .map { it.toPath().normalize().toAbsolutePath() }
        .distinct()
}

private fun createAnnotationResolutionSession(
    project: Project,
    classpath: List<Path>,
    hostConfiguration: ScriptingHostConfiguration,
): FirSession {
    val configuration = CompilerConfiguration.create().apply {
        put(JVMConfigurationKeys.NO_JDK, true)
        scriptingHostConfiguration = hostConfiguration
    }
    val fileManager = VirtualFileManager.getInstance()
    val jarFileSystem = fileManager.getFileSystem(StandardFileSystems.JAR_PROTOCOL)
    val localFileSystem = fileManager.getFileSystem(StandardFileSystems.FILE_PROTOCOL)
    val roots = classpath.mapNotNull { path ->
        val file = path.toFile()
        val id = JvmClasspathRootId.of(path).id
        when {
            file.isDirectory -> localFileSystem?.findFileByPath(id)
            file.isFile -> jarFileSystem?.findFileByPath(id + URLUtil.JAR_SEPARATOR)
            else -> null
        }
    }.map { JavaRoot(it, JavaRoot.RootType.BINARY) }
    val projectEnvironment = AnnotationResolutionProjectEnvironment(
        project,
        listOfNotNull(jarFileSystem, localFileSystem),
        JvmDependenciesIndexImpl(roots),
    ) { scope ->
        JvmPackagePartProvider(configuration.languageVersionSettings, scope).apply { addRoots(roots, configuration) }
    }
    projectEnvironment.registerIndexedClasspathRoots(roots.map { it.file })
    val sessionFactoryContext = FirJvmSessionFactory.Context(
        configuration = configuration,
        projectEnvironment = projectEnvironment,
        librariesClasspath = JvmClasspath.Roots(roots.map { it.file.asJvmClasspathRootId() }),
        javaInterop = projectEnvironment.classNamesOnlyJavaInterop(),
    )
    val moduleDataProvider = ScriptingModuleDataProvider("<script-annotations>", classpath)
    val sharedLibrarySession = FirJvmSessionFactory.createSharedLibrarySession(
        mainModuleName = Name.special("<script-annotations>"),
        extensionRegistrars = emptyList(),
        languageVersionSettings = configuration.languageVersionSettings,
        context = sessionFactoryContext,
    )
    FirJvmSessionFactory.createLibrarySession(
        sharedLibrarySession,
        moduleDataProvider = moduleDataProvider,
        extensionRegistrars = emptyList(),
        languageVersionSettings = configuration.languageVersionSettings,
        context = sessionFactoryContext,
    )
    return FirJvmSessionFactory.createSourceSession(
        moduleData = moduleDataProvider.addNewScriptModuleData(Name.special("<raw-script>"), isDummy = true),
        createIncrementalCompilationSymbolProviders = { null },
        // required for applying the default imports of the script configuration
        extensionRegistrars = listOf(FirScriptingCompilerExtensionRegistrar(configuration)),
        configuration = configuration,
        context = sessionFactoryContext,
        kmpModuleKind = KmpModuleKind.SingleModule,
        init = {},
    )
}

/**
 * Only Kotlin classes are available: Java-declared annotation classes and Java constants in the arguments are not resolved.
 */
private class AnnotationResolutionProjectEnvironment(
    project: Project,
    knownFileSystems: List<VirtualFileSystem>,
    private val index: JvmDependenciesIndex,
    getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider,
) : VfsBasedProjectEnvironment(project, knownFileSystems, getPackagePartProviderFn) {

    override fun getJavaModuleResolver(): JavaModuleResolver = NoJavaModulesResolver

    override fun getKotlinClassFinder(classpath: JvmClasspath): KotlinClassFinder =
        CliVirtualFileFinder(index, psiSearchScope(classpath), enableSearchInCtSym = false, perfManager = null)

    fun classNamesOnlyJavaInterop(): FirJavaInterop = object : FirJavaInterop {
        override fun createBinaryJavaFacade(session: FirSession, moduleData: FirModuleData, classpath: JvmClasspath): FirJavaFacade =
            FirJavaFacadeForModule(session, moduleData, ClassNamesOnlyJavaClassFinder(index))

        override fun createJavaSourcesFacade(session: FirSession, moduleData: FirModuleData): FirJavaFacade =
            FirJavaFacadeForModule(session, moduleData, ClassNamesOnlyJavaClassFinder(index))
    }
}

private class ClassNamesOnlyJavaClassFinder(private val index: JvmDependenciesIndex) : JavaClassFinder {
    override fun findClass(request: JavaClassFinder.Request): JavaClass? = null
    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> = emptyList()
    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        var found = false
        index.traverseDirectoriesInPackage(fqName) { _, _ ->
            found = true
            false
        }
        return if (found) ClassNamesOnlyJavaPackage(fqName) else null
    }

    override fun canComputeKnownClassNamesInPackage(): Boolean = true
    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> {
        val result = HashSet<String>()
        index.traverseClassVirtualFilesInPackage(packageFqName, JavaFileExtensions(JavaFileExtension.CLASS)) { file ->
            result.add(file.nameWithoutExtension)
            true
        }
        return result
    }
}

@OptIn(K1Deprecation::class)
private class ClassNamesOnlyJavaPackage(override val fqName: FqName) : JavaPackage {
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
    override val subPackages: Collection<JavaPackage> get() = emptyList()
    override fun getClasses(nameFilter: (Name) -> Boolean): Collection<JavaClass> = emptyList()
}


fun SourceCode.convertToFirViaPsi(
    session: FirSession,
    @Suppress("unused") diagnosticsReporter: BaseDiagnosticsCollector
): FirFile {
    val ktFile = (this as? KtFileScriptSource)?.ktFile
        ?: error("Expecting a PSI-based script source, got ${this::class.simpleName}")
    val builder = PsiRawFirBuilder(session, session.kotlinScopeProvider)
    return builder.buildFirFile(ktFile).also { firFile ->
        (session.firProvider as FirProviderImpl).recordFile(firFile)
        session.sourcesToPathsMapper.registerFileSource(firFile.source!!, locationId ?: name!!)
    }
}
