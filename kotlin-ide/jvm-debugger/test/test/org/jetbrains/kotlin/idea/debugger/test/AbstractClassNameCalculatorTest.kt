package org.jetbrains.kotlin.idea.debugger.test

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.core.util.getLineNumber
import org.jetbrains.kotlin.idea.debugger.ClassNameCalculator
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

abstract class AbstractClassNameCalculatorTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(unused: String) {
        val testFile = testDataFile()
        myFixture.configureByFile(testFile)

        project.executeWriteCommand("Add class name information") {
            deleteExistingComments(file)

            val ktFile = file as KtFile
            val allNames = ClassNameCalculator.getClassNames(ktFile)
            checkConsistency(ktFile, allNames)

            val ktPsiFactory = KtPsiFactory(project)

            for ((element, name) in allNames) {
                val comment = ktPsiFactory.createComment("/* $name */")
                element.addBefore(comment, element.firstChild)
            }
        }

        myFixture.checkResultByFile(testFile)
    }

    private fun checkConsistency(file: KtFile, allNames: Map<KtElement, String>) {
        val analysisResult = file.analyzeWithAllCompilerChecks()
        assert(!analysisResult.isError())

        val generationState = GenerationState.Builder(
            project,
            ClassBuilderFactories.TEST,
            analysisResult.moduleDescriptor,
            analysisResult.bindingContext,
            listOf(file),
            CompilerConfiguration.EMPTY
        ).build()

        try {
            generationState.beforeCompile()

            val inconsistencies = mutableListOf<String>()

            for ((element, name) in allNames) {
                val target = when (element) {
                    is KtLambdaExpression -> element.functionLiteral
                    else -> element
                }

                val backendType = CodegenBinding.asmTypeForAnonymousClassOrNull(generationState.bindingContext, target)
                val backendName = backendType?.className
                if (backendName != null && backendName != name) {
                    inconsistencies += "Line ${target.getLineNumber()}: Oracle[$name], Backend[$backendName]"
                }
            }

            if (inconsistencies.isNotEmpty()) {
                inconsistencies.forEach { System.err.print(it) }
                throw AssertionError("Inconsistencies found (see above).")
            }
        } finally {
          generationState.destroy()
        }
    }

    private fun deleteExistingComments(file: PsiFile) {
        val comments = mutableListOf<PsiComment>()

        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitComment(comment: PsiComment) {
                comments += comment
            }
        })

        comments.forEach { it.delete() }
    }
}