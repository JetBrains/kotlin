/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtVisitorVoid
import kotlin.system.measureNanoTime
import kotlin.test.Ignore

@Ignore(value = "[VD] disabled temporary for further investigation: too much noise, have no clue how to handle it")
class WholeProjectLightClassTest : WholeProjectPerformanceTest(), WholeProjectKotlinFileProvider {

    override fun doTest(file: VirtualFile): PerFileTestResult {
        val results = mutableMapOf<String, Long>()
        var totalNs = 0L

        val psiFile = PsiManager.getInstance(project).findFile(file) ?: run {
            return PerFileTestResult(results, totalNs, listOf(AssertionError("PsiFile not found for $file")))
        }

        val errors = mutableListOf<Throwable>()

        fun buildAllLightClasses(name: String, predicate: (KtClassOrObject) -> Boolean) {
            val result = measureNanoTime {
                try {
                    // Build light class by PsiFile
                    psiFile.acceptRecursively(object : KtVisitorVoid() {
                        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                            if (!predicate(classOrObject)) return
                            val lightClass = classOrObject.toLightClass() as? KtLightClassForSourceDeclaration ?: return
                            lightClass.superTypes.contentHashCode()
                            lightClass.fields.contentHashCode()
                            lightClass.methods.contentHashCode()
                            // Just to be sure: access types
                            lightClass.fields.map { it.type }.hashCode()
                            lightClass.methods.map { it.returnType }.hashCode()
                            lightClass.hashCode()
                        }
                    })

                } catch (t: Throwable) {
                    t.printStackTrace()
                    errors += t
                }
            }
            results[name] = result
            totalNs += result
        }

        buildAllLightClasses("LightClasses_Top") {
            it.containingDeclarationForPseudocode == null
        }

        buildAllLightClasses("LightClasses_Members") {
            !it.isLocal && it.containingDeclarationForPseudocode is KtClassOrObject
        }

        return PerFileTestResult(results, totalNs, errors)
    }

    fun testUltraLightPerformance() {
        Registry.get("kotlin.use.ultra.light.classes").setValue(true, testRootDisposable)
        testWholeProjectPerformance()
    }
}