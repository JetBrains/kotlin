/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.kotlin.analysis.api.KaExperimentalApi::class)

package org.jetbrains.kotlin.documentation.analyzer

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators.StandaloneModeConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.documentation.analyzer.ids.generateDeclarationId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Compares the unhashed declaration IDs of every symbol reachable from the fixture with a golden `.txt` file,
 * in source order, and checks that each ID identifies a single declaration.
 *
 * Runs on the Analysis API test framework in standalone mode, matching the sessions the KDM analyzer itself
 * creates. The `standalone.fir` output prefix is dropped so the JVM fixtures share the default golden files
 * with the other KDM suites.
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
            with(useSiteSession) { dumpUnhashedIds(mainModule.psiFiles) }
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
 */
private fun KaSession.dumpUnhashedIds(files: Collection<PsiFile>): String {
    // ID -> the real PSI of every visited declaration. Compiler-generated symbols (default constructors,
    // enum `values`/`entries`, synthetic Java properties) and packages have no real PSI; the generated symbols
    // are reached exactly once, through member scopes.
    val ids = LinkedHashMap<String, MutableList<PsiElement?>>()
    fun add(symbol: KaSymbol) {
        try {
            generateDeclarationId(symbol, hashSignature = false)?.let { id ->
                ids.getOrPut(id) { mutableListOf() }.add(symbol.realPsi)
            }
        } catch (_: IllegalArgumentException) {
        }
    }

    // Visiting the class scopes also picks up symbols with no declaration in the source; symbols already
    // reached through PSI are kept once by [ids].
    fun addClassScopeSymbols(classSymbol: KaNamedClassSymbol) {
        classSymbol.combinedDeclaredMemberScope.declarations.forEach(::add)
        classSymbol.combinedDeclaredMemberScope.constructors.forEach(::add)
        classSymbol.defaultType.syntheticJavaPropertiesScope
            ?.getCallableSignatures()
            ?.forEach { add(it.symbol) }
    }

    fun walkKtDeclaration(declaration: KtDeclaration) {
        val symbol = declaration.symbol
        add(symbol)
        if (symbol is KaPropertySymbol) {
            symbol.getter?.let(::add)
            symbol.setter?.let(::add)
        }
        if (declaration is KtClassOrObject) {
            // The primary constructor is declared in the class header, before the body declarations.
            declaration.primaryConstructor?.let { add(it.symbol) }
            declaration.declarations.forEach { walkKtDeclaration(it) }
        }
        if (symbol is KaNamedClassSymbol) addClassScopeSymbols(symbol)
    }

    fun walkJavaClass(psiClass: PsiClass) {
        val classSymbol = psiClass.namedClassSymbol ?: return
        add(classSymbol)
        for (member in psiClass.children.filterIsInstance<PsiMember>()) {
            if (member is PsiClass) walkJavaClass(member) else member.callableSymbol?.let(::add)
        }
        addClassScopeSymbols(classSymbol)
    }

    fun addPackage(fqName: FqName) {
        val packageSymbol = if (fqName.isRoot) rootPackageSymbol else findPackage(fqName)
        packageSymbol?.let(::add)
    }

    for (file in files) {
        when (file) {
            is KtFile -> {
                addPackage(file.packageFqName)
                file.declarations.forEach { walkKtDeclaration(it) }
            }
            is PsiJavaFile -> {
                addPackage(FqName(file.packageName))
                file.classes.forEach { walkJavaClass(it) }
            }
        }
    }

    checkUniqueDeclarationIds(ids)
    return ids.keys.joinToString("\n", postfix = "\n")
}

/**
 * Checks that every generated ID identifies a single declaration. Revisiting the same declaration through
 * PSI and through member scopes is expected, but an ID shared by distinct declarations would make them
 * indistinguishable in the model. Compiler-generated symbols contribute `null` entries, which are skipped.
 */
private fun checkUniqueDeclarationIds(ids: Map<String, List<PsiElement?>>) {
    for ([id, declarations] in ids) {
        val psis = declarations.filterNotNull()
        if (psis.size > 1) {
            check(psis.all { it.isEquivalentTo(psis.first()) }) {
                val locations = psis.joinToString { "${it.containingFile.name}:${it.textOffset}" }
                "Declaration ID is not unique: \"$id\" is generated by several declarations: $locations"
            }
        }
    }
}
