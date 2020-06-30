/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.*
import com.intellij.codeInspection.actions.RunInspectionIntention
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.codeInspection.ex.InspectionToolWrapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.configuration.isLanguageVersionUpdate
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents

internal abstract class ObsoleteCodeMigrationInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool, MigrationFix {
    protected abstract val fromVersion: LanguageVersion
    protected abstract val toVersion: LanguageVersion
    protected abstract val problems: List<ObsoleteCodeMigrationProblem>

    final override fun isApplicable(migrationInfo: MigrationInfo): Boolean {
        return migrationInfo.isLanguageVersionUpdate(fromVersion, toVersion)
    }

    final override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): KtVisitorVoid {
        return simpleNameExpressionVisitor(fun(simpleNameExpression) {
            run {
                val versionIsSatisfied = simpleNameExpression.languageVersionSettings.languageVersion >= toVersion
                if (!versionIsSatisfied && !ApplicationManager.getApplication().isUnitTestMode) {
                    return
                }
            }

            for (registeredProblem in problems) {
                if (registeredProblem.report(holder, isOnTheFly, simpleNameExpression)) {
                    return
                }
            }
        })
    }
}

internal interface ObsoleteCodeMigrationProblem {
    fun report(holder: ProblemsHolder, isOnTheFly: Boolean, simpleNameExpression: KtSimpleNameExpression): Boolean
}

internal interface ObsoleteCodeFix {
    fun applyFix(project: Project, descriptor: ProblemDescriptor)
}

// Shortcut quick fix for running inspection in the project scope.
// Should work like RunInspectionAction.runInspection.
internal abstract class ObsoleteCodeInWholeProjectFix : LocalQuickFix {
    protected abstract val inspectionName: String

    final override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val toolWrapper = InspectionProjectProfileManager.getInstance(project).currentProfile.getInspectionTool(inspectionName, project)!!
        runToolInProject(project, toolWrapper)
    }

    final override fun startInWriteAction(): Boolean = false

    private fun runToolInProject(project: Project, toolWrapper: InspectionToolWrapper<*, *>) {
        val managerEx = InspectionManager.getInstance(project) as InspectionManagerEx
        val kotlinSourcesScope: GlobalSearchScope = KotlinSourceFilterScope.projectSources(GlobalSearchScope.allScope(project), project)
        val cleanupScope = AnalysisScope(kotlinSourcesScope, project)

        val cleanupToolProfile = runInInspectionProfileInitMode { RunInspectionIntention.createProfile(toolWrapper, managerEx, null) }
        managerEx.createNewGlobalContext(false)
            .codeCleanup(
                cleanupScope,
                cleanupToolProfile,
                KotlinBundle.message("apply.in.the.project.0", toolWrapper.displayName),
                null,
                false
            )
    }

    // Overcome failure during profile creating because of absent tools in tests
    private inline fun <T> runInInspectionProfileInitMode(runnable: () -> T): T {
        return if (!ApplicationManager.getApplication().isUnitTestMode) {
            runnable()
        } else {
            val old = InspectionProfileImpl.INIT_INSPECTIONS
            try {
                InspectionProfileImpl.INIT_INSPECTIONS = true
                runnable()
            } finally {
                InspectionProfileImpl.INIT_INSPECTIONS = old
            }
        }
    }
}

/**
 * There should be a single fix class with the same family name, this way it can be executed for all found coroutines problems from UI.
 */
internal abstract class ObsoleteCodeFixDelegateQuickFix(private val delegate: ObsoleteCodeFix) : LocalQuickFix {
    final override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        delegate.applyFix(project, descriptor)
    }
}

internal abstract class ObsoleteImportsUsage : ObsoleteCodeMigrationProblem {
    /**
     * Required to report the problem only on one psi element instead of the every single qualifier
     * in the import statement.
     */
    protected abstract val textMarker: String

    protected abstract val packageBindings: Map<String, String>
    protected open val importsToRemove: Set<String> = emptySet()

    protected abstract val wholeProjectFix: LocalQuickFix
    protected abstract fun problemMessage(): String

    protected abstract fun wrapFix(fix: ObsoleteCodeFix): LocalQuickFix

    final override fun report(holder: ProblemsHolder, isOnTheFly: Boolean, simpleNameExpression: KtSimpleNameExpression): Boolean {
        if (simpleNameExpression.text != textMarker) return false

        val parent = simpleNameExpression.parent as? KtExpression ?: return false
        val reportExpression = parent as? KtDotQualifiedExpression ?: simpleNameExpression

        findBinding(simpleNameExpression) ?: return false

        holder.registerProblem(
            reportExpression,
            problemMessage(),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            *fixesWithWholeProject(isOnTheFly, wrapFix(ObsoleteCoroutineImportFix()), wholeProjectFix)
        )

        return true
    }

    private fun findBinding(simpleNameExpression: KtSimpleNameExpression): Binding? {
        if (simpleNameExpression.text != textMarker) return null

        val importDirective = simpleNameExpression.parents
            .takeWhile { it is KtDotQualifiedExpression || it is KtImportDirective }
            .lastOrNull() as? KtImportDirective ?: return null

        val fqNameStr = importDirective.importedFqName?.asString() ?: return null

        val bindEntry = packageBindings.entries.find { (affectedImportPrefix, _) ->
            fqNameStr.startsWith(affectedImportPrefix)
        } ?: return null

        return Binding(
            FqName(bindEntry.value),
            fqNameStr in importsToRemove,
            importDirective
        )
    }

    private inner class ObsoleteCoroutineImportFix : ObsoleteCodeFix {
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val simpleNameExpression = when (element) {
                is KtSimpleNameExpression -> element
                is KtDotQualifiedExpression -> element.selectorExpression as? KtSimpleNameExpression
                else -> null
            } ?: return

            val binding = findBinding(simpleNameExpression) ?: return

            if (binding.shouldRemove) {
                binding.importDirective.delete()
            } else {
                simpleNameExpression.mainReference.bindToFqName(
                    binding.bindTo, shorteningMode = KtSimpleNameReference.ShorteningMode.NO_SHORTENING
                )
            }
        }
    }

    private class Binding(
        val bindTo: FqName,
        val shouldRemove: Boolean,
        val importDirective: KtImportDirective
    )
}

