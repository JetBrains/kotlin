package com.jetbrains.kotlin.structuralsearch.sanity

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.structuralsearch.Matcher
import com.intellij.structuralsearch.plugin.ui.SearchConfiguration
import com.intellij.structuralsearch.plugin.util.CollectingMatchResultSink
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.search.fileScope
import org.jetbrains.kotlin.psi.*
import java.util.*
import kotlin.random.Random

class KotlinSSSanityTest : HeavyPlatformTestCase() {
    private val myConfiguration = SearchConfiguration().apply {
        name = "SSR"
        matchOptions.fileType = KotlinFileType.INSTANCE
    }
    private var random = Random(0)
    private val KEEP_RATIO = 0.2

    override fun setUp() {
        super.setUp()
        random = Random(project.hashCode())
        myProject = ProjectUtil.openOrImport("/", null, false)
    }

    /** Tells which Kotlin [PsiElement]-s can be used directly or not (i.e. some child) as a SSR pattern. */
    private val PsiElement.isSearchable: Boolean
        get() = when (this) {
            is PsiWhiteSpace, is KtPackageDirective, is KtImportList -> false
            is KtModifierList, is KtDeclarationModifierList -> false
            else -> true
        }

    private fun shouldStopAt(element: PsiElement) = when (element) {
        is KtAnnotationEntry -> true
        else -> false
    }

    private fun mustContinueAfter(element: PsiElement) = when (element) {
        is KtParameterList, is KtValueArgumentList, is KtSuperTypeList, is KtTypeArgumentList, is KtTypeParameterList -> true
        is KtClassBody, is KtBlockExpression, is KtBlockStringTemplateEntry, is KtBlockCodeFragment -> true
        is KtPrimaryConstructor -> true
        is KtParameter -> true
        else -> false
    }

    /** Returns a random [PsiElement] whose text can be used as a pattern against [tree]. */
    private fun pickRandomSearchableSubElement(tree: Array<PsiElement>): PsiElement {
        var element = tree.filter { it.isSearchable }.random()

        while (element.children.any() && !shouldStopAt(element) && (random.nextFloat() > KEEP_RATIO || mustContinueAfter(element))) {
            val searchableChildren = element.children.filter { it.isSearchable }
            if (searchableChildren.isEmpty()) break

            val newElement = searchableChildren.random()
            if (newElement.children.none() && (!element.isSearchable || mustContinueAfter(newElement))) break
            element = newElement
        }

        return element
    }

    private fun doTest(psiFile: PsiFile): Boolean {
        val subtree = pickRandomSearchableSubElement(psiFile.children)

        val matchOptions = myConfiguration.matchOptions.apply {
            fillSearchCriteria(subtree.text)
            fileType = KotlinFileType.INSTANCE
            scope = psiFile.fileScope()
        }
        val matcher = Matcher(project, matchOptions)
        val sink = CollectingMatchResultSink()
        matcher.findMatches(sink)

        println(
            "Search pattern (${subtree::class.toString().split('.').last()}):\n\t${
                subtree.text.trimMargin().replace("\n", "\n\t")
            }"
        )

        if (sink.matches.size == 0) {
            UsefulTestCase.LOG.warn("\tNo results in: ${psiFile.name}\n")
            return false
        }

        return true
    }

    /** Picks a random .kt file from this project and returns its content and PSI tree. */
    private fun randomLocalKotlinSource(): PsiFile? {
        val projectScope = GlobalSearchScope.projectScope(project)
        val allFiles: List<VirtualFile> =
            ArrayList(FilenameIndex.getAllFilesByExt(project, "kt", projectScope)).filter { "test" !in it.path }
        check(allFiles.isNotEmpty()) { "No Kotlin files in the project" }
        val psiFile = PsiManager.getInstance(project).findFile(allFiles.random()) ?: return null
        return psiFile
    }

    fun testLocalSSS() {
        var successCount = 0
        repeat(5) {
            val psiFile = randomLocalKotlinSource()
            TestCase.assertNotNull("Couldn't find Kotlin source code", psiFile)
            if (doTest(psiFile!!)) successCount += 1
        }
        TestCase.assertEquals(5, successCount)
    }

}