/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.uast.check

import com.android.tools.klint.detector.api.Issue
import com.android.tools.klint.detector.api.JavaContext
import com.android.tools.klint.detector.api.Location
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.uast.*
import org.jetbrains.uast.UastCallKind.Companion.CONSTRUCTOR_CALL
import org.jetbrains.uast.UastCallKind.Companion.FUNCTION_CALL
import org.jetbrains.uast.java.JavaUastLanguagePlugin
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.uast.visitor.UastExtendableVisitor
import org.jetbrains.uast.visitor.UastVisitor
import java.io.File

interface UastAndroidContext : UastContext {
    val lintContext: JavaContext
    fun report(issue: Issue, element: UElement, location: Location?, message: String)

    fun getLocation(element: UElement?) = element?.getLocation()
}

object UastChecker {
    fun check(project: Project, file: File, context: UastAndroidContext, visitor: UastVisitor) {
        ProgressManager.checkCanceled()

        val vfile = VirtualFileManager.getInstance().findFileByUrl("file://" + file.absolutePath) ?: return

        val plugins = context.languagePlugins
        val extendableVisitor = UastExtendableVisitor(visitor, context, plugins.flatMap { it.visitorExtensions })

        val instance = DumbService.getInstance(project)
        // Do not check anything in dumb mode
        if (instance.isDumb) return

        instance.runReadActionInSmartMode {
            val psiFile = PsiManager.getInstance(project).findFile(vfile)

            if (psiFile != null) {
                when (psiFile) {
                    is PsiJavaFile -> {
                        val ufile = JavaUastLanguagePlugin.converter.convertWithParent(psiFile)
                        ufile?.accept(extendableVisitor)
                    }
                    else -> for (plugin in plugins) {
                        val ufile = plugin.converter.convertWithParent(psiFile)
                        if (ufile != null) {
                            ufile.accept(extendableVisitor)
                            break
                        }
                    }
                }
            }
        }
    }

    fun check(project: Project, file: File, scanner: UastScanner, context: UastAndroidContext) {
        val applicableFunctionNames = scanner.applicableFunctionNames ?: emptyList()
        val applicableSuperClasses = scanner.applicableSuperClasses ?: emptyList()
        val applicableConstructorTypes = scanner.applicableConstructorTypes ?: emptyList()

        val appliesToResourcesRefs = scanner.appliesToResourceRefs()

        val visitor = object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                ProgressManager.checkCanceled()
                if (applicableFunctionNames.isNotEmpty()) {
                    if (node.kind == FUNCTION_CALL && node.functionName in applicableFunctionNames) {
                        scanner.visitCall(context, node)
                    }
                }

                if (applicableConstructorTypes.isNotEmpty()) {
                    if (node.kind == CONSTRUCTOR_CALL) {
                        node.resolve(context)?.let { constructor ->
                            if (constructor.getContainingClass()?.fqName in applicableConstructorTypes) {
                                scanner.visitConstructor(context, node, constructor)
                            }
                        }
                    }
                }

                return false
            }

            override fun visitClass(node: UClass): Boolean {
                ProgressManager.checkCanceled()
                if (applicableSuperClasses.isNotEmpty()) {
                    if (applicableSuperClasses.any { node.isSubclassOf(it) }) {
                        scanner.visitClass(context, node)
                    }
                }

                return false
            }

            override fun visitQualifiedExpression(node: UQualifiedExpression): Boolean {
                ProgressManager.checkCanceled()
                if (appliesToResourcesRefs && node.receiver is UQualifiedExpression) {
                    val parentQualifiedExpr = node.receiver as UQualifiedExpression
                    val resourceName = node.selector
                    val resourceType = parentQualifiedExpr.selector
                    val receiver = parentQualifiedExpr.receiver

                    val receiverIsResourceClass = when (receiver) {
                        is USimpleReferenceExpression -> receiver.identifier == "R"
                        is UQualifiedExpression -> receiver.selectorMatches("R")
                        else -> false
                    }

                    if (resourceName is USimpleReferenceExpression && resourceType is USimpleReferenceExpression
                        && receiverIsResourceClass && receiver is UResolvable) {
                        val resolvedReceiver = receiver.resolve(context)
                        val isFramework = (resolvedReceiver as? UClass)?.matchesFqName("android.R") ?: false

                        scanner.visitResourceReference(context, node, resourceType.identifier, resourceName.identifier, isFramework)
                    }
                }

                return false
            }
        }

        check(project, file, context, visitor)
    }

}