package com.jetbrains.kotlin.structuralsearch.sanity

import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.structuralsearch.Matcher
import com.intellij.structuralsearch.plugin.ui.SearchConfiguration
import com.intellij.structuralsearch.plugin.util.CollectingMatchResultSink
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File
import kotlin.random.Random

class KotlinSSSanityTest : BasePlatformTestCase() {
    private val myConfiguration = SearchConfiguration().apply {
        name = "SSR"
        matchOptions.fileType = KotlinFileType.INSTANCE
    }
    private var random = Random(System.currentTimeMillis())

    override fun setUp() {
        super.setUp()
        random = Random(project.hashCode())
    }

    override fun getProjectDescriptor(): KotlinLightSanityProjectDescriptor = KotlinLightSanityProjectDescriptor()

    private fun doTest(source: String): Boolean {
        myFixture.configureByText(KotlinFileType.INSTANCE, source)

        val tree = KtPsiFactory(project).createFile(source).children
        val subtree = SanityTestElementPicker.pickFrom(tree)
        if (subtree == null) {
            println("No element picked.")
            return true
        }

        println(
            "Search pattern [${subtree::class.toString().split('.').last()}]:\n\t${
                subtree.text.trimMargin().replace("\n", "\n\t")
            }"
        )

        val matchOptions = myConfiguration.matchOptions.apply {
            fillSearchCriteria(subtree.text)
            fileType = KotlinFileType.INSTANCE
            scope = GlobalSearchScopes.openFilesScope(project)
        }
        val matcher = Matcher(project, matchOptions)
        val sink = CollectingMatchResultSink()
        matcher.findMatches(sink)

        return sink.matches.size > 0
    }

    /** Picks a random .kt file from this project and returns its content and PSI tree. */
    private fun randomLocalKotlinSource(): File {
        val kotlinFiles = File(".").walkTopDown().toList().filter { it.extension == "kt" && "Predefined" !in it.name }
        assert(kotlinFiles.any()) { "No .kt source found." }
        return kotlinFiles.random()
    }

    fun testLocalSSS() {
        val source = randomLocalKotlinSource()
        TestCase.assertNotNull("Couldn't find Kotlin source code", source)
        println("- ${source.absolutePath}")
        assert(doTest(source.readText())) { "No match found." }
        println("Matched\n")
    }

}