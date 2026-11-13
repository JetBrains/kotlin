/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiPackage
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.javaInterop.namedClassSymbol
import org.jetbrains.kotlin.analysis.api.scopes.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.scopes.fileScope
import org.jetbrains.kotlin.analysis.api.scopes.syntheticJavaPropertiesScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.defaultType
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators.StandaloneModeConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.documentation.analyzer.ids.generateDeclarationId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Compares the unhashed declaration IDs of every symbol reachable from the fixture with a golden `.txt` file,
 * in source order, and checks that each ID identifies a single declaration.
 *
 * Runs on the Analysis API test framework in standalone mode, matching the sessions the KDM analyzer itself
 * creates.
 */
abstract class AbstractUniqueDeclarationIdGoldenTest : AbstractAnalysisApiBasedTest() {
    override val configurator: AnalysisApiTestConfigurator =
        object : AnalysisApiTestConfigurator by StandaloneModeConfigurator() {
            override val testPrefixes: List<String> = emptyList()
        }

    override fun doTestByMainModuleAndOptionalMainFile(
        mainFile: KtFile?,
        mainModule: KtTestModule,
        testServices: TestServices,
    ) {
        val contextFile = mainFile ?: mainModule.ktFiles.firstOrNull() ?: error("The fixture must contain a Kotlin file")
        val actual = analyzeForTest(contextFile) {
            dumpUnhashedIds(mainModule.psiFiles)
        }
        assertEqualsToTestOutputFile(actual, ".txt")
    }
}

/**
 * One unhashed declaration ID per line, in the order the declarations appear in each source file, so the golden
 * file reads like the test data it was generated from. Each file first contributes the ID of its package; files
 * sharing a package contribute it once, at the first file that declares it. Symbols with no declaration of their
 * own (default constructors, enum `values`/`valueOf`/`entries`, synthetic Java properties) follow the class body
 * that owns them. The order of the files themselves is not part of the contract.
 *
 * When [errorsByDeclaration] is supplied, every line whose declaration has compiler errors gets them appended
 * after the ID, as `// ERROR_NAME, ...`.
 */
context(session: KaSession)
internal fun dumpUnhashedIds(
    files: Collection<PsiFile>,
    errorsByDeclaration: Map<PsiElement, List<String>> = emptyMap(),
): String {
    // ID -> the real PSI of every visited declaration. Compiler-generated symbols (default constructors,
    // enum `values`/`entries`, synthetic Java properties) and packages have no real PSI; the generated symbols
    // are reached exactly once, through member scopes.
    val ids = LinkedHashMap<String, MutableList<PsiElement?>>()
    fun add(symbol: KaSymbol) {
        try {
            generateDeclarationId(symbol, hashSignature = false)?.let { id ->
                ids.getOrPut(id) { mutableListOf() }.add(symbol.realPsi)
            }
        } catch (e: IllegalArgumentException) {
            error("Failed to generate a declaration ID for $symbol: ${e.message}")
        }
    }

    // Scopes do not promise source order, so it is restored here. A stable sort keeps symbols with no PSI
    // (compiler-generated) after the declarations of the class that owns them, in scope order.
    fun KaSymbol.sourceOrderKey(): Int = realPsi?.textOffset ?: Int.MAX_VALUE

    fun walkDeclaration(symbol: KaDeclarationSymbol) {
        add(symbol)
        if (symbol is KaPropertySymbol) {
            symbol.getter?.let(::add)
            symbol.setter?.let(::add)
        }
        if (symbol is KaNamedClassSymbol) {
            // The primary constructor is declared in the class header, before the body declarations.
            val scope = symbol.combinedDeclaredMemberScope
            for (member in scope.declarations.sortedBy(KaSymbol::sourceOrderKey)) {
                walkDeclaration(member)
            }
            // Symbols with no declaration of their own: synthetic Java properties of this class.
            @OptIn(KaExperimentalApi::class)
            symbol.defaultType.syntheticJavaPropertiesScope
                ?.getCallableSignatures()
                ?.forEach { add(it.symbol) }
        }
    }

    fun addPackage(fqName: FqName) {
        val packageSymbol = if (fqName.isRoot) rootPackageSymbol else findPackage(fqName)
        packageSymbol?.let(::add)
    }

    for (file in files) {
        when (file) {
            is KtFile -> {
                addPackage(file.packageFqName)
                for (symbol in file.symbol.fileScope.declarations.sortedBy(KaSymbol::sourceOrderKey)) {
                    walkDeclaration(symbol)
                }
            }
            is PsiJavaFile -> {
                addPackage(FqName(file.packageName))
                file.classes.forEach { psiClass -> psiClass.namedClassSymbol?.let(::walkDeclaration) }
            }
        }
    }

    checkUniqueDeclarationIds(ids, errorsByDeclaration)
    return ids.entries.joinToString("\n", postfix = "\n") { (id, declarations) ->
        val errors = declarations
            .filterNotNull()
            .flatMap { errorsByDeclaration[it].orEmpty() }
            .distinct()
            .sorted()
        if (errors.isEmpty()) id else "$id  // ${errors.joinToString(", ")}"
    }
}

/**
 * Checks that every generated ID identifies a single declaration. Revisiting the same declaration through
 * PSI and through member scopes is expected, but an ID shared by distinct declarations would make them
 * indistinguishable in the model. Compiler-generated symbols contribute `null` entries, which are skipped.
 *
 * A collision is tolerated when it involves a declaration the compiler already rejects: on code that does
 * not compile, the ambiguity is on the erroneous declaration, not on the ID scheme. Such collisions stay
 * visible in the golden file as repeated lines.
 */
private fun checkUniqueDeclarationIds(
    ids: Map<String, List<PsiElement?>>,
    errorsByDeclaration: Map<PsiElement, List<String>>,
) {
    for ([id, declarations] in ids) {
        if (declarations.all { it is PsiPackage }) continue

        val psis = declarations.filterNotNull()
        if (psis.size > 1 && psis.none { it in errorsByDeclaration }) {
            val locations = psis.joinToString { "${it.containingFile.name}:${it.textOffset}" }
            error("Declaration ID is not unique: \"$id\" is generated by several declarations: $locations")

        }
    }
}
