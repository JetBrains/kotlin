/*
 * Copyright 2021-2023 Arrow Meta Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file has been copied from the Arrow Reflection repository available at:
 * https://github.com/arrow-kt/arrow-reflection
 */

package org.jetbrains.kotlin.fir.dataframe

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.KtInMemoryTextSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClassCopy
import org.jetbrains.kotlin.fir.declarations.utils.addDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitTypeBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatcherProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsMappingProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompanionGenerationProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompilerRequiredAnnotationsResolveProcessor
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.renderReadableWithFqNames
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.readSourceFileWithMapping
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.util.PrivateForInline
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

abstract class MetaContext(open val templateCompiler: TemplateCompiler) {

    operator fun List<String>.unaryPlus(): String =
        joinToString()

    operator fun Sequence<String>.unaryPlus(): String =
        joinToString()

    operator fun Name?.unaryPlus(): String =
        this?.asString() ?: ""

    operator fun Visibility?.unaryPlus(): String =
        when (this) {
            Visibilities.Public -> "public"
            Visibilities.Private -> "private"
            Visibilities.PrivateToThis -> "private"
            else -> ""
        }

    operator fun Modality?.unaryPlus(): String =
        when (this) {
            Modality.FINAL -> "final"
            Modality.SEALED -> "sealed"
            Modality.OPEN -> "open"
            Modality.ABSTRACT -> "abstract"
            null -> ""
        }

}

abstract class FirMetaContext(
    open val session: FirSession,
    override val templateCompiler: TemplateCompiler
) : MetaContext(templateCompiler) {

    abstract val scopeDeclarations: List<FirDeclaration>

    fun FirClass.addDeclarations(vararg declarations: FirDeclaration): FirClass {
        val result = if (this is FirRegularClass) {
            buildRegularClassCopy(this) {
                declarations.forEach {
                    when (it) {
                        is FirRegularClass -> {
                            addDeclaration(buildRegularClassCopy(it) {
                                resolvePhase = FirResolvePhase.BODY_RESOLVE
                                symbol = FirRegularClassSymbol(ClassId.fromString(classId.asString() + "." + it.name.asString()))
                            })
                        }
                        else -> {}
                    }
                }
            }
        } else this
        return result
    }

    fun FirClassSymbol<*>.hasCompanion(): Boolean = this.companion() != null

    @OptIn(SymbolInternals::class)
    fun FirClassSymbol<*>.companion(): FirClass? =
        fir.declarations.filterIsInstance<FirClass>()
            .firstOrNull { it.classId.shortClassName == Name.identifier("Companion") }

    @OptIn(SymbolInternals::class)
    fun propertiesOf(firClass: FirClass, f: (FirValueParameter) -> String): String =
        +firClass.primaryConstructorIfAny(session)?.fir?.valueParameters.orEmpty().filter { it.isVal }.map {
            f(it)
        }

    val String.function: FirSimpleFunction
        get() {
            val results = templateCompiler.compileSource(
                this@FirMetaContext as? FirMetaCheckerContext,
                this,
                scopeDeclarations
            )
            val firFiles = results.firResults.flatMap { it.files }
            val currentElement: FirSimpleFunction? = findSelectedFirElement(FirSimpleFunction::class, firFiles)
            return currentElement ?: error("Could not find a ${FirSimpleFunction::class}")
        }

    val String.constructor: FirConstructor
        get() {
            val results = templateCompiler.compileSource(
                this@FirMetaContext as? FirMetaCheckerContext,
                this,
                scopeDeclarations
            )
            val firFiles = results.firResults.flatMap { it.files }
            val currentElement: FirConstructor? = findSelectedFirElement(FirConstructor::class, firFiles)
            return currentElement ?: error("Could not find a ${FirSimpleFunction::class}")
        }

    operator fun FirElement.unaryPlus(): String =
        (this as? FirTypeRef)?.coneType?.renderReadableWithFqNames()?.replace("/", ".")
            ?: source?.text?.toString()
            ?: error("$this has no source psi text element")

    val String.call: FirCall
        get() =
            compile(
                """
            val x = $this
            """
            )

    fun source(@Language("kotlin") source: String): String = source

    inline fun <reified Fir : FirElement> compile(
        @Language("kotlin") source: String,
        extraScopeDeclarations: List<FirDeclaration> = emptyList()
    ): Fir {
        val results = templateCompiler.compileSource(
            this@FirMetaContext as? FirMetaCheckerContext,
            source,
            scopeDeclarations + extraScopeDeclarations
        )
        val firFiles = results.firResults.flatMap { it.files }
        val currentElement: Fir? = findSelectedFirElement(Fir::class, firFiles)
        return currentElement ?: errorNotFound(Fir::class)
    }

    fun errorNotFound(fir: KClass<out FirElement>): Nothing =
        error("Could not find a ${fir}")

    @PublishedApi
    internal fun <Fir : FirElement> findSelectedFirElement(
        firElementClass: KClass<Fir>,
        firFiles: List<FirFile>
    ): Fir? {
        var currentElement: Fir? = null
        firFiles.forEach { firFile ->
            firFile.accept(object : FirVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    if (firElementClass.isInstance(element)) {
                        currentElement = element as Fir
                    } else
                        element.acceptChildren(this)
                }
            })
        }
        return currentElement
    }

    @JvmName("renderFir")
    operator fun Iterable<FirElement>.unaryPlus(): String =
        source()

    fun Iterable<FirElement>.source(separator: String = ", ", unit: Unit = Unit): String =
        joinToString(", ") { +it }
}

class FirMetaContextImpl(
    session: FirSession,
    templateCompiler: TemplateCompiler
) : FirMetaContext(session, templateCompiler) {
    override val scopeDeclarations: List<FirDeclaration>
        get() = emptyList()
}

class FirMetaCheckerContext(
    override val templateCompiler: TemplateCompiler,
    override val session: FirSession,
    val checkerContext: CheckerContext,
    val diagnosticReporter: DiagnosticReporter,
    val additionalContext: FirDeclaration? = null,
) : FirMetaContext(session, templateCompiler) {

    fun FirElement.report(factory: KtDiagnosticFactory1<String>, msg: String) {
        diagnosticReporter.reportOn(
            source,
            factory,
            msg,
            checkerContext,
            AbstractSourceElementPositioningStrategy.DEFAULT
        )
    }

    override val scopeDeclarations: List<FirDeclaration>
        get() = checkerContext.containingDeclarations + listOfNotNull(additionalContext)
}

class FirResult(
  val session: FirSession,
  val scopeSession: ScopeSession,
  val files: List<FirFile>,
  val scopeDeclarations: List<FirDeclaration>
)

class TemplateCompiler(val flag: FlagContainer) {
  private companion object {
      private val lock = Any()
  }

  private var counter = AtomicInteger(0)

  private val chunk: List<Module>
  val templatesFolder = File(File("."), "/build/meta/templates")

  init {
    synchronized(lock) {
      if (!templatesFolder.exists()) try { templatesFolder.createDirectory() } catch (_: Exception) { }
    }

    templatesFolder.deleteOnExit()

    chunk = listOfNotNull(
      ModuleBuilder(
        "meta templates module",
        templatesFolder.absolutePath, "java-production"
      )
    )
  }

  lateinit var session: FirSession

  data class TemplateResult(
    val firResults: List<FirResult>,
//    val irResults: List<Fir2IrResult>
  )

  var compiling: Boolean = false

  fun compileSource(
    metaCheckerContext: FirMetaCheckerContext?,
    source: String,
    scopeDeclarations: List<FirDeclaration>,
    produceIr: Boolean = false
  ): TemplateResult {
    flag.shouldIntercept = false
    compiling = true
    try {
      val next = counter.incrementAndGet()
      //val fileName = "meta.template_$next.kt"
      println("parsing source:\n$source")
      println("session: ${session::class}")
      val outputs: ArrayList<FirResult> = arrayListOf()
//      val irOutput: ArrayList<Fir2IrResult> = arrayListOf()
      val messageCollector: MessageCollector = MessageCollector.NONE
      for (module in chunk) {
//        val moduleConfiguration = projectConfiguration//.applyModuleProperties(module, buildFile)
        val context = CompilationContext(
          source,
          messageCollector,
//          moduleConfiguration
        )
        val result = context.compileModule(metaCheckerContext, scopeDeclarations)

        val templateResult = result ?: return TemplateResult(emptyList(), /*emptyList()*/)
        outputs += templateResult

        if (produceIr) {
//          outputs.forEach {
//            irOutput.add(convertToIR(it, moduleConfiguration))
//          }
        }
      }
      return TemplateResult(outputs, /*irOutput*/)
    } finally {
      compiling = false
      flag.shouldIntercept = true
    }
  }

  private fun CompilationContext.compileModule(metaCheckerContext: FirMetaCheckerContext?, scopeDeclarations: List<FirDeclaration>): FirResult? {
    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
    val renderDiagnosticNames = true
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()
    val firResult = runFrontend(source, diagnosticsReporter, scopeDeclarations)
    val diagnosticsContext = metaCheckerContext?.checkerContext
    if (firResult == null) {
      diagnosticsReporter.diagnostics.forEach {
        if (diagnosticsContext != null)
            metaCheckerContext.diagnosticReporter.report(it, diagnosticsContext)
        println("error: [" + it.factory.name + "] " + it.factory.ktRenderer.render(it))
      }
      return null
    }
    return firResult
  }

//  fun convertToIR(firResult: FirResult, moduleConfiguration: CompilerConfiguration): Fir2IrResult {
//    val fir2IrExtensions = JvmFir2IrExtensions(moduleConfiguration, JvmIrDeserializerImpl(), JvmIrMangler)
//    val linkViaSignatures = moduleConfiguration.getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES)
//    val scopeFiles = firResult.scopeDeclarations.filterIsInstance<FirFile>()
//    val files = firResult.files + scopeFiles
//    val validatedFirResult = with(firResult) {
//      org.jetbrains.kotlin.fir.pipeline.FirResult(platformOutput = org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput(
//        session = session, scopeSession = scopeSession, fir = files
//      ), commonOutput = null)
//    }
//    val fir2IrResult = validatedFirResult.convertToIrAndActualize(fir2IrExtensions= fir2IrExtensions, irGeneratorExtensions = emptyList(), linkViaSignatures = linkViaSignatures)
//    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
//    return fir2IrResult
//  }

  private fun runFrontend(
    source: String,
    diagnosticsReporter: BaseDiagnosticsCollector,
    scopeDeclarations: List<FirDeclaration>,
  ): FirResult? {
    val syntaxErrors = false
    val scope = ScopeSession()
    val next = counter.incrementAndGet()
    val fileName = "meta.template_$next.kt"
    val rawFir = session.buildFirViaLightTree(listOf(KtInMemoryTextSourceFile(fileName, null, source))) // ,.buildFirFromKtFiles(ktFiles)
    val (scopeSession, fir) = session.runResolution(rawFir, scope, scopeDeclarations)
    session.runCheckers(scopeSession, fir, diagnosticsReporter)
    return if (syntaxErrors || diagnosticsReporter.hasErrors) null else FirResult(
      session,
      scopeSession,
      fir,
      scopeDeclarations
    )
  }

  fun FirSession.runResolution(
    firFiles: List<FirFile>,
    scopeSession: ScopeSession,
    scopeDeclarations: List<FirDeclaration>
  ): Pair<ScopeSession, List<FirFile>> {
    val resolveProcessor = FirTotalResolveProcessor(this, scopeSession, scopeDeclarations)
    resolveProcessor.process(firFiles)
    return resolveProcessor.scopeSession to firFiles
  }

  fun FirSession.runCheckers(scopeSession: ScopeSession, firFiles: List<FirFile>, reporter: DiagnosticReporter) {
    val collector = FirDiagnosticsCollector.create(this, scopeSession)
    for (file in firFiles) {
      collector.collectDiagnostics(file, reporter)
    }
  }

  class FirTotalResolveProcessor(
    session: FirSession,
    val scopeSession: ScopeSession,
    scopeDeclarations: List<FirDeclaration>
  ) {

    private val processors: List<FirResolveProcessor> = createAllCompilerResolveProcessors(
      session,
      scopeSession,
      scopeDeclarations
    )

    fun process(files: List<FirFile>) {
      for (processor in processors) {
        processor.beforePhase()
        when (processor) {
          is FirTransformerBasedResolveProcessor -> {
            for (file in files) {
              processor.processFile(file)
            }
          }

          is FirGlobalResolveProcessor -> {
            processor.process(files)
          }
        }
        processor.afterPhase()
      }
    }
  }

  fun FirSession.buildFirViaLightTree(
    files: Collection<KtSourceFile>,
    diagnosticsReporter: DiagnosticReporter? = null,
    reportFilesAndLines: ((Int, Int) -> Unit)? = null
  ): List<FirFile> {
    val firProvider = (firProvider as? FirProviderImpl)
    val sourcesToPathsMapper = sourcesToPathsMapper
    val builder = LightTree2Fir(this, kotlinScopeProvider, diagnosticsReporter)
    val shouldCountLines = (reportFilesAndLines != null)
    var linesCount = 0
    val firFiles = files.map { file ->
      val (code, linesMapping) = file.getContentsAsStream().reader(Charsets.UTF_8).use {
        it.readSourceFileWithMapping()
      }
      if (shouldCountLines) {
        linesCount += linesMapping.lastOffset
      }
      builder.buildFirFile(code, file, linesMapping).also { firFile ->
        firProvider?.recordFile(firFile)
        sourcesToPathsMapper.registerFileSource(firFile.source!!, file.path ?: file.name)
      }
    }
    reportFilesAndLines?.invoke(files.count(), linesCount)
    return firFiles
  }

  fun FirSession.buildFirFromKtFiles(ktFiles: Collection<KtFile>): List<FirFile> {
    val firProvider = (firProvider as? FirProviderImpl)
    val builder = PsiRawFirBuilder(this, kotlinScopeProvider, BodyBuildingMode.NORMAL)
    return ktFiles.map {
      builder.buildFirFile(it).also { firFile ->
        firProvider?.recordFile(firFile)
      }
    }
  }

  private class CompilationContext(
    val source: String,
    val messageCollector: MessageCollector,
//    val moduleConfiguration: CompilerConfiguration
  )
}

fun createAllCompilerResolveProcessors(
  session: FirSession,
  scopeSession: ScopeSession? = null,
  scopeDeclarations: List<FirDeclaration>
): List<FirResolveProcessor> {
  return createAllResolveProcessors(scopeSession) {
    createCompilerProcessorByPhase(session, it, scopeDeclarations)
  }
}

private inline fun <T : FirResolveProcessor> createAllResolveProcessors(
  scopeSession: ScopeSession? = null,
  creator: FirResolvePhase.(ScopeSession) -> T
): List<T> {
  @Suppress("NAME_SHADOWING")
  val scopeSession = scopeSession ?: ScopeSession()
  val phases = FirResolvePhase.values().filter {
    !it.noProcessor
  }
  return phases.map { it.creator(scopeSession) }
}

fun FirResolvePhase.createCompilerProcessorByPhase(
  session: FirSession,
  scopeSession: ScopeSession,
  scopeDeclarations: List<FirDeclaration>
): FirResolveProcessor {
  return when (this) {
    FirResolvePhase.RAW_FIR -> throw IllegalArgumentException("Raw FIR building phase does not have a transformer")
    FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS -> FirCompilerRequiredAnnotationsResolveProcessor(
      session,
      scopeSession
    )

    FirResolvePhase.COMPANION_GENERATION -> FirCompanionGenerationProcessor(session, scopeSession)
    FirResolvePhase.IMPORTS -> FirImportResolveProcessor(session, scopeSession)
    FirResolvePhase.SUPER_TYPES -> FirSupertypeResolverProcessor(session, scopeSession)
    FirResolvePhase.SEALED_CLASS_INHERITORS -> FirSealedClassInheritorsProcessor(session, scopeSession)
    FirResolvePhase.TYPES -> FirTypeResolveProcessor(session, scopeSession)
    FirResolvePhase.STATUS -> FirStatusResolveProcessor(session, scopeSession)
    FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS -> FirAnnotationArgumentsResolveProcessor(session, scopeSession)
    FirResolvePhase.CONTRACTS -> FirContractResolveProcessor(session, scopeSession)
    FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> FirImplicitTypeBodyResolveProcessor(session, scopeSession)
    FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING -> FirAnnotationArgumentsMappingProcessor(session, scopeSession)
    FirResolvePhase.BODY_RESOLVE -> FirBodyResolveProcessor(session, scopeSession, scopeDeclarations)
    FirResolvePhase.EXPECT_ACTUAL_MATCHING -> FirExpectActualMatcherProcessor(session, scopeSession)
  }
}

class FirBodyResolveProcessor(
  session: FirSession,
  scopeSession: ScopeSession,
  scopeDeclarations: List<FirDeclaration>
) : FirTransformerBasedResolveProcessor(session, scopeSession, FirResolvePhase.BODY_RESOLVE) {

  override val transformer = FirBodyResolveTransformerAdapter(session, scopeSession, scopeDeclarations)
}


class FirBodyResolveTransformerAdapter(
  session: FirSession,
  scopeSession: ScopeSession,
  scopeDeclarations: List<FirDeclaration>
) : FirTransformer<Any?>() {

  @OptIn(PrivateForInline::class)
  private val transformer = FirBodyResolveTransformer(
    session,
    phase = FirResolvePhase.BODY_RESOLVE,
    implicitTypeOnly = false,
    scopeSession = scopeSession,
    outerBodyResolveContext = BodyResolveContext(
      ReturnTypeCalculatorForFullBodyResolve.Default,
      DataFlowAnalyzerContext(session),
      scopeDeclarations.filterIsInstance<FirClassLikeDeclaration>().toSet()
    )
  ).also { bodyResolveTransformer ->
    val ctx = bodyResolveTransformer.context
    scopeDeclarations.forEach { analysisContext ->
      when (analysisContext) {
        is FirRegularClass -> {
          ctx.addReceiver(null, ImplicitDispatchReceiverValue(analysisContext.symbol, session, scopeSession))
          //ctx.addInaccessibleImplicitReceiverValue(analysisContext, SessionHolderImpl(session, scopeSession))
        }

        is FirFile -> {
          val filePackageScope = FirPackageMemberScope(analysisContext.packageFqName, session)
          ctx.addNonLocalTowerDataElement(filePackageScope.asTowerDataElement(false))
          //ctx.addLocalScope(FirLocalScope(session))
        }
//          is FirVariable -> {
//              val localScope = FirLocalScope(session)
//              localScope.storeVariable(analysisContext as FirValueParameter, session)
//              ctx.addLocalScope(localScope)
//          }
        else -> {
          val localScope = FirLocalScope(session)
          ctx.addLocalScope(localScope)
        } //error("unsupported declaration: $analysisContext")
      }
    }
  }

  override fun <E : FirElement> transformElement(element: E, data: Any?): E {
    return element
  }


  override fun transformFile(file: FirFile, data: Any?): FirFile {
    return file.transform(transformer, ResolutionMode.ContextIndependent)
  }
}
