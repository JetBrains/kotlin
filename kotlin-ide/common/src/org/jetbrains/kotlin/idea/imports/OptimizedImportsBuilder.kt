/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.imports

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.utils.replaceImportingScopes

class OptimizedImportsBuilder(
    private val file: KtFile,
    private val data: InputData,
    private val options: Options
) {
    companion object {
        @get:TestOnly
        @set:TestOnly
        var testLog: StringBuilder? = null
    }

    interface AbstractReference {
        val element: KtElement
        val dependsOnNames: Collection<Name>
        fun resolve(bindingContext: BindingContext): Collection<DeclarationDescriptor>
    }

    class InputData(
        val descriptorsToImport: Set<DeclarationDescriptor>,
        val namesToImport: Map<FqName, Set<Name>>,
        val references: Collection<AbstractReference>,
        val unresolvedNames: Set<Name>,
    )

    class Options(
        val nameCountToUseStarImport: Int,
        val nameCountToUseStarImportForMembers: Int,
        val isInPackagesToUseStarImport: (FqName) -> Boolean
    )

    private val importInsertHelper = ImportInsertHelper.getInstance(file.project)

    private sealed class ImportRule {
        // force presence of this import
        data class Add(val importPath: ImportPath) : ImportRule() {
            override fun toString() = "+$importPath"
        }

        // force absence of this import
        data class DoNotAdd(val importPath: ImportPath) : ImportRule() {
            override fun toString() = "-$importPath"
        }
    }

    private val importRules = HashSet<ImportRule>()

    fun buildOptimizedImports(): List<ImportPath>? {
        val facade = file.getResolutionFacade()
        file.importDirectives
            .asSequence()
            .filter { it.mayReferToSomeUnresolvedName() || it.isExistedUnresolvedName(facade) }
            .mapNotNull { it.importPath }
            .mapNotNullTo(importRules) { ImportRule.Add(it) }

        while (true) {
            ProgressManager.checkCanceled()
            val importRulesBefore = importRules.size
            val result = tryBuildOptimizedImports()
            if (importRules.size == importRulesBefore) return result
            testLog?.append("Trying to build import list again with import rules: ${importRules.joinToString()}\n")
        }
    }

    private fun KtImportDirective.mayReferToSomeUnresolvedName() = isAllUnder && data.unresolvedNames.isNotEmpty()

    private fun KtImportDirective.isExistedUnresolvedName(facade: ResolutionFacade) =
        importedName in data.unresolvedNames && !canResolve(facade)

    private fun getExpressionToAnalyze(element: KtElement): KtExpression? {
        val parent = element.parent
        return when {
            parent is KtQualifiedExpression && element == parent.selectorExpression -> parent
            parent is KtCallExpression && element == parent.calleeExpression -> getExpressionToAnalyze(parent)
            parent is KtOperationExpression && element == parent.operationReference -> parent
            parent is KtUserType -> null //TODO: is it always correct?
            else -> element as? KtExpression //TODO: what if not expression? Example: KtPropertyDelegationMethodsReference
        }
    }

    private fun tryBuildOptimizedImports(): List<ImportPath>? {
        val importsToGenerate = hashSetOf<ImportPath>()
        importRules.filterIsInstance<ImportRule.Add>().mapTo(importsToGenerate) { it.importPath }

        val descriptorsByParentFqName = HashMap<FqName, MutableSet<DeclarationDescriptor>>()
        for (descriptor in data.descriptorsToImport) {
            val fqName = descriptor.importableFqName!!
            for (name in data.namesToImport.getValue(fqName)) {
                val alias = if (name != fqName.shortName()) name else null

                val explicitImportPath = ImportPath(fqName, false, alias)
                if (explicitImportPath in importsToGenerate) continue

                val parentFqName = fqName.parent()
                if (alias == null && canUseStarImport(descriptor, fqName) && ImportPath(parentFqName, true).isAllowedByRules()) {
                    descriptorsByParentFqName.getOrPut(parentFqName) { hashSetOf() }.add(descriptor)
                } else {
                    importsToGenerate.add(explicitImportPath)
                }
            }
        }

        val classNamesToCheck = hashSetOf<FqName>()
        for (parentFqName in descriptorsByParentFqName.keys) {
            ProgressManager.checkCanceled()

            val starImportPath = ImportPath(parentFqName, true)
            if (starImportPath in importsToGenerate) continue

            val descriptors = descriptorsByParentFqName[parentFqName]!!
            val fqNames = descriptors.map { it.importableFqName!! }.toSet()
            val nameCountToUseStar = descriptors.first().nameCountToUseStar()
            val useExplicitImports = fqNames.size < nameCountToUseStar && !options.isInPackagesToUseStarImport(parentFqName)
                    || !starImportPath.isAllowedByRules()

            if (useExplicitImports) {
                fqNames.filter(this::needExplicitImport).mapTo(importsToGenerate) { ImportPath(it, false) }
            } else {
                descriptors.asSequence()
                    .filterIsInstance<ClassDescriptor>()
                    .map { it.importableFqName!! }
                    .filterTo(classNamesToCheck, this::needExplicitImport)

                if (fqNames.all(this::needExplicitImport)) {
                    importsToGenerate.add(starImportPath)
                }
            }
        }

        // now check that there are no conflicts and all classes are really imported
        addExplicitImportsForClassesWhenRequired(classNamesToCheck, descriptorsByParentFqName, importsToGenerate, file)

        val sortedImportsToGenerate = importsToGenerate.sortedWith(importInsertHelper.getImportSortComparator(file))

        // check if no changes to imports required
        val oldImports = file.importDirectives
        if (oldImports.size == sortedImportsToGenerate.size && oldImports.map { it.importPath } == sortedImportsToGenerate) return null

        val originalFileScope = file.getFileResolutionScope()
        val newFileScope = buildScopeByImports(file, sortedImportsToGenerate)

        var references = data.references
        if (testLog != null) {
            // to make log the same for all runs
            references = references.sortedBy { it.toString() }
        }
        for ((names, refs) in references.groupBy { it.dependsOnNames }) {
            if (!areScopeSlicesEqual(originalFileScope, newFileScope, names)) {
                for (ref in refs) {
                    ProgressManager.checkCanceled()

                    val element = ref.element
                    val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
                    val expressionToAnalyze = getExpressionToAnalyze(element) ?: continue
                    val newScope = element.getResolutionScope(
                        bindingContext,
                        file.getResolutionFacade()
                    ).replaceImportingScopes(newFileScope)

                    val newBindingContext = expressionToAnalyze.analyzeAsReplacement(
                        expressionToAnalyze,
                        bindingContext,
                        newScope,
                        trace = BindingTraceContext()
                    )

                    testLog?.append("Additional checking of reference $ref\n")

                    val oldTargets = ref.resolve(bindingContext)
                    val newTargets = ref.resolve(newBindingContext)
                    if (!areTargetsEqual(oldTargets, newTargets)) {
                        testLog?.append("Changed resolve of $ref\n")
                        (oldTargets + newTargets).forEach {
                            lockImportForDescriptor(it, names)
                        }
                    }
                }
            }
        }

        return sortedImportsToGenerate
    }

    private fun lockImportForDescriptor(descriptor: DeclarationDescriptor, existingNames: Collection<Name>) {
        val fqName = descriptor.importableFqName ?: return
        val names = data.namesToImport.getOrElse(fqName) { listOf(descriptor.name) }.intersect(existingNames)

        val starImportPath = ImportPath(fqName.parent(), true)
        val importPaths = file.importDirectives.map { it.importPath }

        for (name in names) {
            val alias = if (name != fqName.shortName()) name else null
            val explicitImportPath = ImportPath(fqName, false, alias)
            when {
                explicitImportPath in importPaths ->
                    importRules.add(ImportRule.Add(explicitImportPath))
                alias == null && starImportPath in importPaths ->
                    importRules.add(ImportRule.Add(starImportPath))
                else -> // there is no import for this descriptor in the original import list, so do not allow to import it by star-import
                    importRules.add(ImportRule.DoNotAdd(starImportPath))
            }
        }
    }

    private fun addExplicitImportsForClassesWhenRequired(
        classNamesToCheck: Collection<FqName>,
        descriptorsByParentFqName: Map<FqName, MutableSet<DeclarationDescriptor>>,
        importsToGenerate: MutableSet<ImportPath>,
        originalFile: KtFile
    ) {
        val scope = buildScopeByImports(originalFile, importsToGenerate.filter { it.isAllUnder })
        for (fqName in classNamesToCheck) {
            if (scope.findClassifier(fqName.shortName(), NoLookupLocation.FROM_IDE)?.importableFqName != fqName) {
                // add explicit import if failed to import with * (or from current package)
                importsToGenerate.add(ImportPath(fqName, false))

                val parentFqName = fqName.parent()

                val siblingsToImport = descriptorsByParentFqName.getValue(parentFqName)
                for (descriptor in siblingsToImport.filter { it.importableFqName == fqName }) {
                    siblingsToImport.remove(descriptor)
                }

                if (siblingsToImport.isEmpty()) { // star import is not really needed
                    importsToGenerate.remove(ImportPath(parentFqName, true))
                }
            }
        }
    }

    private fun buildScopeByImports(originalFile: KtFile, importsToGenerate: Collection<ImportPath>): ImportingScope {
        val fileText = buildString {
            append("package ")
            append(originalFile.packageFqName.toUnsafe().render())
            append("\n")

            for (importPath in importsToGenerate) {
                append("import ")
                append(importPath.pathStr)
                if (importPath.hasAlias()) {
                    append("=")
                    append(importPath.alias!!.render())
                }
                append("\n")
            }
        }

        val fileWithImports = KtPsiFactory(originalFile).createAnalyzableFile("Dummy_" + originalFile.name, fileText, originalFile)
        if (file.isScript()) {
            fileWithImports.originalFile = originalFile
        }

        return fileWithImports.getFileResolutionScope()
    }

    @OptIn(FrontendInternals::class)
    private fun KtFile.getFileResolutionScope() =
        getResolutionFacade().frontendService<FileScopeProvider>().getFileScopes(this).importingScope

    private fun areScopeSlicesEqual(scope1: ImportingScope, scope2: ImportingScope, names: Collection<Name>): Boolean {
        val tower1 = scope1.extractSliceTower(names)
        val tower2 = scope2.extractSliceTower(names)
        val iterator1 = tower1.iterator()
        val iterator2 = tower2.iterator()
        while (true) {
            when {
                !iterator1.hasNext() -> return !iterator2.hasNext()
                !iterator2.hasNext() -> return false
                else -> if (!areTargetsEqual(iterator1.next(), iterator2.next())) return false
            }
        }
    }

    private fun ImportingScope.extractSliceTower(names: Collection<Name>): Sequence<Collection<DeclarationDescriptor>> {
        return parentsWithSelf
            .map { scope ->
                names.flatMap { name ->
                    ProgressManager.checkCanceled()
                    val contributedFunctions = scope.getContributedFunctions(name, NoLookupLocation.FROM_IDE)
                    ProgressManager.checkCanceled()
                    val contributedVariables = scope.getContributedVariables(name, NoLookupLocation.FROM_IDE)
                    ProgressManager.checkCanceled()
                    val contributedClassifier = scope.getContributedClassifier(name, NoLookupLocation.FROM_IDE)
                    contributedFunctions + contributedVariables + listOfNotNull(contributedClassifier)
                }
            }
            .filter { it.isNotEmpty() }
    }

    private fun canUseStarImport(descriptor: DeclarationDescriptor, fqName: FqName): Boolean = when {
        fqName.parent().isRoot -> false
        (descriptor.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.OBJECT -> false
        else -> true
    }

    private fun needExplicitImport(fqName: FqName): Boolean = hasAlias(fqName) || !isImportedByDefault(fqName)

    private fun hasAlias(fqName: FqName) = data.namesToImport[fqName]?.let {
        it.singleOrNull() == null
    } ?: false

    private fun isImportedByDefault(fqName: FqName) = importInsertHelper.isImportedWithDefault(ImportPath(fqName, false), file)

    private fun isImportedByLowPriorityDefault(fqName: FqName) =
        importInsertHelper.isImportedWithLowPriorityDefaultImport(ImportPath(fqName, false), file)

    private fun DeclarationDescriptor.nameCountToUseStar(): Int {
        val isMember = containingDeclaration is ClassDescriptor
        return if (isMember)
            options.nameCountToUseStarImportForMembers
        else
            options.nameCountToUseStarImport
    }

    private fun areTargetsEqual(descriptors1: Collection<DeclarationDescriptor>, descriptors2: Collection<DeclarationDescriptor>): Boolean {
        return descriptors1.size == descriptors2.size &&
                descriptors1.zip(descriptors2).all { (first, second) -> areTargetsEqual(first, second) } //TODO: can have different order?
    }

    private fun areTargetsEqual(first: DeclarationDescriptor, second: DeclarationDescriptor): Boolean {
        if (first == second) return true

        val firstFqName = first.importableFqName
        val secondFqName = second.importableFqName

        return firstFqName == secondFqName ||
                (first.isAliasTo(second) && secondFqName != null && isImportedByLowPriorityDefault(secondFqName)) ||
                (second.isAliasTo(first) && firstFqName != null && isImportedByLowPriorityDefault(firstFqName))
    }

    private fun DeclarationDescriptor.isAliasTo(other: DeclarationDescriptor): Boolean =
        this is TypeAliasDescriptor && classDescriptor == other ||
                this is TypeAliasConstructorDescriptor && underlyingConstructorDescriptor == other

    private fun ImportPath.isAllowedByRules(): Boolean = importRules.none { it is ImportRule.DoNotAdd && it.importPath == this }
}
