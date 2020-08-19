/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class JavaCollectionsStaticMethodInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return dotQualifiedExpressionVisitor(fun(expression) {
            val (methodName, firstArg) = getTargetMethodOnMutableList(expression) ?: return
            holder.registerProblem(
                expression,
                KotlinBundle.message("java.collections.static.method.call.should.be.replaced.with.kotlin.stdlib"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                ReplaceWithStdLibFix(methodName, firstArg.text)
            )
        })
    }

    companion object {
        fun getTargetMethodOnImmutableList(expression: KtDotQualifiedExpression) =
            getTargetMethod(expression) { isListOrSubtype() && !isMutableListOrSubtype() }

        private fun getTargetMethodOnMutableList(expression: KtDotQualifiedExpression) =
            getTargetMethod(expression) { isMutableListOrSubtype() }

        private fun getTargetMethod(
            expression: KtDotQualifiedExpression,
            isValidFirstArgument: KotlinType.() -> Boolean
        ): Pair<String, KtValueArgument>? {
            val callExpression = expression.callExpression ?: return null
            val args = callExpression.valueArguments
            val firstArg = args.firstOrNull() ?: return null
            val context = expression.analyze(BodyResolveMode.PARTIAL)
            if (firstArg.getArgumentExpression()?.getType(context)?.isValidFirstArgument() != true) return null

            val descriptor = expression.getResolvedCall(context)?.resultingDescriptor as? JavaMethodDescriptor ?: return null
            val fqName = descriptor.importableFqName?.asString() ?: return null
            if (!canReplaceWithStdLib(expression, fqName, args)) return null

            val methodName = fqName.split(".").last()
            return methodName to firstArg
        }

        private fun canReplaceWithStdLib(expression: KtDotQualifiedExpression, fqName: String, args: List<KtValueArgument>): Boolean {
            if (!fqName.startsWith("java.util.Collections.")) return false
            val size = args.size
            return when (fqName) {
                "java.util.Collections.fill" -> checkApiVersion(ApiVersion.KOTLIN_1_2, expression) && size == 2
                "java.util.Collections.reverse" -> size == 1
                "java.util.Collections.shuffle" -> checkApiVersion(ApiVersion.KOTLIN_1_2, expression) && (size == 1 || size == 2)
                "java.util.Collections.sort" -> {
                    size == 1 || (size == 2 && args.getOrNull(1)?.getArgumentExpression() is KtLambdaExpression)
                }
                else -> false
            }
        }

        private fun checkApiVersion(requiredVersion: ApiVersion, expression: KtDotQualifiedExpression): Boolean {
            val module = ModuleUtilCore.findModuleForPsiElement(expression) ?: return true
            return module.languageVersionSettings.apiVersion >= requiredVersion
        }
    }

}

private fun KotlinType.isMutableList() =
    constructor.declarationDescriptor?.fqNameSafe == StandardNames.FqNames.mutableList

private fun KotlinType.isMutableListOrSubtype(): Boolean {
    return isMutableList() || constructor.supertypes.reversed().any { it.isMutableList() }
}

private fun KotlinType.isList() =
    constructor.declarationDescriptor?.fqNameSafe == StandardNames.FqNames.list

private fun KotlinType.isListOrSubtype(): Boolean {
    return isList() || constructor.supertypes.reversed().any { it.isList() }
}

private class ReplaceWithStdLibFix(private val methodName: String, private val receiver: String) : LocalQuickFix {
    override fun getName() = KotlinBundle.message("replace.with.std.lib.fix.text", receiver, methodName)

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expression = descriptor.psiElement as? KtDotQualifiedExpression ?: return
        val callExpression = expression.callExpression ?: return
        val valueArguments = callExpression.valueArguments
        val firstArg = valueArguments.getOrNull(0)?.getArgumentExpression() ?: return
        val secondArg = valueArguments.getOrNull(1)?.getArgumentExpression()
        val factory = KtPsiFactory(project)
        val newExpression = if (secondArg != null) {
            if (methodName == "sort") {
                factory.createExpressionByPattern("$0.sortWith(Comparator $1)", firstArg, secondArg.text)
            } else {
                factory.createExpressionByPattern("$0.$methodName($1)", firstArg, secondArg)
            }
        } else {
            factory.createExpressionByPattern("$0.$methodName()", firstArg)
        }
        expression.replace(newExpression)
    }
}
