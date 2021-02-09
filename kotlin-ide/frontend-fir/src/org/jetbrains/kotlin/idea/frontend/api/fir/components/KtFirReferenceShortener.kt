/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtReferenceShortener
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenCommand
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.addImportToFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector

internal class KtFirReferenceShortener(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
    override val firResolveState: FirModuleResolveState,
) : KtReferenceShortener(), KtFirAnalysisSessionComponent {
    override fun collectShortenings(file: KtFile, selection: TextRange): ShortenCommand {
        resolveFileToBodyResolve(file)
        val firFile = file.getOrBuildFirOfType<FirFile>(firResolveState)

        val collector = ElementsToShortenCollector()
        firFile.acceptChildren(collector)

        return ShortenCommandImpl(
            file,
            collector.namesToImport.distinct(),
            collector.typesToShorten.distinct().map { it.createSmartPointer() },
            collector.qualifiersToShorten.distinct().map { it.createSmartPointer() }
        )
    }

    private data class AvailableClassifier(val classId: ClassId, val isFromStarOrPackageImport: Boolean)

    private fun findFirstClassifierInScopesByName(positionScopes: List<FirScope>, targetClassName: Name): AvailableClassifier? {
        for (scope in positionScopes) {
            val classifierSymbol = scope.findFirstClassifierByName(targetClassName) ?: continue
            val classifierLookupTag = classifierSymbol.toLookupTag() as? ConeClassLikeLookupTag ?: continue

            return AvailableClassifier(
                classifierLookupTag.classId,
                isFromStarOrPackageImport = scope is FirAbstractStarImportingScope || scope is FirPackageMemberScope
            )
        }

        return null
    }

    private fun findFunctionsInScopes(scopes: List<FirScope>, name: Name): List<FirNamedFunctionSymbol> {
        return scopes.flatMap { it.getFunctions(name) }
    }

    private fun findSinglePropertyInScopesByName(scopes: List<FirScope>, name: Name): FirVariableSymbol<*>? {
        return scopes.asSequence().mapNotNull { it.getSinglePropertyByName(name) }.singleOrNull()
    }

    private fun resolveFileToBodyResolve(file: KtFile) {
        for (declaration in file.declarations) {
            declaration.getOrBuildFir(firResolveState) // temporary hack, resolves declaration to BODY_RESOLVE stage
        }
    }

    private fun FirScope.findFirstClassifierByName(name: Name): FirClassifierSymbol<*>? {
        var element: FirClassifierSymbol<*>? = null

        processClassifiersByName(name) {
            if (element == null) {
                element = it
            }
        }

        return element
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun FirScope.getSingleFunctionByName(name: Name): FirNamedFunctionSymbol? =
        buildList { processFunctionsByName(name, this::add) }.singleOrNull()

    @OptIn(ExperimentalStdlibApi::class)
    private fun FirScope.getSinglePropertyByName(name: Name): FirVariableSymbol<*>? =
        buildList { processPropertiesByName(name, this::add) }.singleOrNull()

    @OptIn(ExperimentalStdlibApi::class)
    private fun findScopesAtPosition(position: KtElement, newImports: List<FqName>): List<FirScope>? {
        val towerDataContext = firResolveState.getTowerDataContextForElement(position) ?: return null

        val result = buildList<FirScope> {
            addAll(towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope })
            addIfNotNull(createFakeImportingScope(newImports))
            addAll(towerDataContext.localScopes)
        }

        return result.asReversed()
    }

    private fun createFakeImportingScope(newImports: List<FqName>): FirScope? {
        val resolvedNewImports = newImports.mapNotNull { createFakeResolvedImport(it) }
        if (resolvedNewImports.isEmpty()) return null

        return FirExplicitSimpleImportingScope(resolvedNewImports, firResolveState.rootModuleSession, ScopeSession())
    }

    private fun createFakeResolvedImport(fqNameToImport: FqName): FirResolvedImport? {
        val packageOrClass = resolveToPackageOrClass(firResolveState.rootModuleSession.firSymbolProvider, fqNameToImport) ?: return null

        val delegateImport = buildImport {
            importedFqName = fqNameToImport
            isAllUnder = false
        }

        return buildResolvedImport {
            delegate = delegateImport
            packageFqName = packageOrClass.packageFqName
        }
    }

    private inner class ElementsToShortenCollector : FirVisitorVoid() {
        val namesToImport: MutableList<FqName> = mutableListOf()
        val typesToShorten: MutableList<KtUserType> = mutableListOf()
        val qualifiersToShorten: MutableList<KtDotQualifiedExpression> = mutableListOf()

        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
            processTypeRef(resolvedTypeRef)

            resolvedTypeRef.acceptChildren(this)
            resolvedTypeRef.delegatedTypeRef?.accept(this)
        }

        override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
            super.visitResolvedQualifier(resolvedQualifier)

            processTypeQualifier(resolvedQualifier)
        }

        override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
            super.visitResolvedNamedReference(resolvedNamedReference)

            processPropertyReference(resolvedNamedReference)
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall) {
            super.visitFunctionCall(functionCall)

            processFunctionCall(functionCall)
        }

        private fun processTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
            val wholeTypeReference = resolvedTypeRef.psi as? KtTypeReference ?: return

            val wholeClassifierId = resolvedTypeRef.type.lowerBoundIfFlexible().classId ?: return
            val wholeTypeElement = wholeTypeReference.typeElement.unwrapNullable() as? KtUserType ?: return

            if (wholeTypeElement.qualifier == null) return

            collectTypeIfNeedsToBeShortened(wholeClassifierId, wholeTypeElement)
        }

        private fun collectTypeIfNeedsToBeShortened(wholeClassifierId: ClassId, wholeTypeElement: KtUserType) {
            val allClassIds = wholeClassifierId.outerClassesWithSelf
            val allTypeElements = wholeTypeElement.qualifiersWithSelf

            val positionScopes = findScopesAtPosition(wholeTypeElement, namesToImport) ?: return

            for ((classId, typeElement) in allClassIds.zip(allTypeElements)) {
                // if qualifier is null, then this type have no package and thus cannot be shortened
                if (typeElement.qualifier == null) return

                val firstFoundClass = findFirstClassifierInScopesByName(positionScopes, classId.shortClassName)?.classId

                if (firstFoundClass == classId) {
                    addTypeToShorten(typeElement)
                    return
                }
            }

            // none class matched
            val (mostTopLevelClassId, mostTopLevelTypeElement) = allClassIds.zip(allTypeElements).last()
            val availableClassifier = findFirstClassifierInScopesByName(positionScopes, mostTopLevelClassId.shortClassName)

            check(availableClassifier?.classId != mostTopLevelClassId) {
                "If this condition were true, we would have exited from the loop on the last iteration. ClassId = $mostTopLevelClassId"
            }

            if (availableClassifier == null || availableClassifier.isFromStarOrPackageImport) {
                addTypeToImportAndShorten(mostTopLevelClassId.asSingleFqName(), mostTopLevelTypeElement)
            } else {
                addFakePackagePrefixToShortenIfPresent(mostTopLevelTypeElement)
            }
        }

        private fun addFakePackagePrefixToShortenIfPresent(typeElement: KtUserType) {
            val deepestTypeWithQualifier = typeElement.qualifiersWithSelf.last().parent as? KtUserType
                ?: error("Type element should have at least one qualifier, instead it was ${typeElement.text}")

            if (deepestTypeWithQualifier.hasFakeRootPrefix()) {
                addTypeToShorten(deepestTypeWithQualifier)
            }
        }

        private fun addTypeToShorten(typeElement: KtUserType) {
            typesToShorten.add(typeElement)
        }

        private fun addTypeToImportAndShorten(classFqName: FqName, mostTopLevelTypeElement: KtUserType) {
            namesToImport.add(classFqName)
            typesToShorten.add(mostTopLevelTypeElement)
        }

        private fun processTypeQualifier(resolvedQualifier: FirResolvedQualifier) {
            val wholeClassQualifier = resolvedQualifier.classId ?: return
            val wholeQualifierElement = when (val qualifierPsi = resolvedQualifier.psi) {
                is KtDotQualifiedExpression -> qualifierPsi
                is KtNameReferenceExpression -> qualifierPsi.getDotQualifiedExpressionForSelector() ?: return
                else -> return
            }

            collectQualifierIfNeedsToBeShortened(wholeClassQualifier, wholeQualifierElement)
        }

        private fun collectQualifierIfNeedsToBeShortened(wholeClassQualifier: ClassId, wholeQualifierElement: KtDotQualifiedExpression) {
            val positionScopes = findScopesAtPosition(wholeQualifierElement, namesToImport) ?: return

            val allClassIds = wholeClassQualifier.outerClassesWithSelf
            val allQualifiers = wholeQualifierElement.qualifiersWithSelf

            for ((classId, qualifier) in allClassIds.zip(allQualifiers)) {
                val firstFoundClass = findFirstClassifierInScopesByName(positionScopes, classId.shortClassName)?.classId

                if (firstFoundClass == classId) {
                    addElementToShorten(qualifier)
                    return
                }
            }

            val (mostTopLevelClassId, mostTopLevelQualifier) = allClassIds.zip(allQualifiers).last()
            val availableClassifier = findFirstClassifierInScopesByName(positionScopes, mostTopLevelClassId.shortClassName)

            check(availableClassifier?.classId != mostTopLevelClassId) {
                "If this condition were true, we would have exited from the loop on the last iteration. ClassId = $mostTopLevelClassId"
            }

            if (availableClassifier == null || availableClassifier.isFromStarOrPackageImport) {
                addElementToImportAndShorten(mostTopLevelClassId.asSingleFqName(), mostTopLevelQualifier)
            } else {
                addFakePackagePrefixToShortenIfPresent(mostTopLevelQualifier)
            }
        }

        private fun processPropertyReference(resolvedNamedReference: FirResolvedNamedReference) {
            val referenceExpression = resolvedNamedReference.psi as? KtNameReferenceExpression
            val qualifiedProperty = referenceExpression?.getDotQualifiedExpressionForSelector() ?: return

            val propertyId = (resolvedNamedReference.resolvedSymbol as? FirCallableSymbol<*>)?.callableId ?: return

            val scopes = findScopesAtPosition(qualifiedProperty, namesToImport) ?: return
            val singleAvailableProperty = findSinglePropertyInScopesByName(scopes, propertyId.callableName)

            if (singleAvailableProperty?.callableId == propertyId) {
                addElementToShorten(qualifiedProperty)
            }
        }

        private fun processFunctionCall(functionCall: FirFunctionCall) {
            if (!canBePossibleToDropReceiver(functionCall)) return

            val callExpression = functionCall.psi as? KtCallExpression ?: return
            val qualifiedCallExpression = callExpression.getDotQualifiedExpressionForSelector() ?: return

            val calleeReference = functionCall.calleeReference
            val callableId = findUnambiguousReferencedCallableId(calleeReference) ?: return

            val scopes = findScopesAtPosition(callExpression, namesToImport) ?: return
            val availableCallables = findFunctionsInScopes(scopes, callableId.callableName)

            when {
                availableCallables.isEmpty() -> {
                    val additionalImport = callableId.asImportableFqName() ?: return
                    addElementToImportAndShorten(additionalImport, qualifiedCallExpression)
                }
                availableCallables.all { it.callableId == callableId } -> {
                    addElementToShorten(qualifiedCallExpression)
                }
                else -> {
                    addFakePackagePrefixToShortenIfPresent(qualifiedCallExpression)
                }
            }
        }

        private fun canBePossibleToDropReceiver(functionCall: FirFunctionCall): Boolean {
            // we can remove receiver only if it is a qualifier
            val explicitReceiver = functionCall.explicitReceiver as? FirResolvedQualifier ?: return false

            // if there is no extension receiver necessary, then it can be removed
            if (functionCall.extensionReceiver is FirNoReceiverExpression) return true

            val receiverType = explicitReceiver.typeRef.toRegularClass(firResolveState.rootModuleSession) ?: return true
            return receiverType.classKind != ClassKind.OBJECT
        }

        private fun findUnambiguousReferencedCallableId(namedReference: FirNamedReference): CallableId? {
            val unambiguousSymbol = when (namedReference) {
                is FirResolvedNamedReference -> namedReference.resolvedSymbol
                is FirErrorNamedReference -> {
                    val candidateSymbol = namedReference.candidateSymbol
                    if (candidateSymbol !is FirErrorFunctionSymbol) {
                        candidateSymbol
                    } else {
                        getSingleUnambiguousCandidate(namedReference)
                    }
                }
                else -> null
            }

            return (unambiguousSymbol as? FirCallableSymbol<*>)?.callableId
        }

        /**
         * If [namedReference] is ambiguous and all candidates point to the callables with same callableId,
         * returns the first candidate; otherwise returns null.
         */
        private fun getSingleUnambiguousCandidate(namedReference: FirErrorNamedReference): FirCallableSymbol<*>? {
            val coneAmbiguityError = namedReference.diagnostic as? ConeAmbiguityError ?: return null

            val candidates = coneAmbiguityError.candidates.map { it as FirCallableSymbol<*> }
            require(candidates.isNotEmpty()) { "Cannot have zero candidates" }

            val distinctCandidates = candidates.distinctBy { it.callableId }
            return distinctCandidates.singleOrNull()
                ?: error("Expected all candidates to have same callableId, but got: ${distinctCandidates.map { it.callableId }}")
        }

        private fun addFakePackagePrefixToShortenIfPresent(wholeQualifiedExpression: KtDotQualifiedExpression) {
            val deepestQualifier = wholeQualifiedExpression.qualifiersWithSelf.last()
            if (deepestQualifier.hasFakeRootPrefix()) {
                addElementToShorten(deepestQualifier)
            }
        }

        private fun addElementToShorten(element: KtDotQualifiedExpression) {
            qualifiersToShorten.add(element)
        }

        private fun addElementToImportAndShorten(nameToImport: FqName, element: KtDotQualifiedExpression) {
            namesToImport.add(nameToImport)
            qualifiersToShorten.add(element)
        }

        private val ClassId.outerClassesWithSelf: Sequence<ClassId>
            get() = generateSequence(this) { it.outerClassId }

        private val KtUserType.qualifiersWithSelf: Sequence<KtUserType>
            get() = generateSequence(this) { it.qualifier }

        private val KtDotQualifiedExpression.qualifiersWithSelf: Sequence<KtDotQualifiedExpression>
            get() = generateSequence(this) { it.receiverExpression as? KtDotQualifiedExpression }
    }
}

private class ShortenCommandImpl(
    val targetFile: KtFile,
    val importsToAdd: List<FqName>,
    val typesToShorten: List<SmartPsiElementPointer<KtUserType>>,
    val qualifiersToShorten: List<SmartPsiElementPointer<KtDotQualifiedExpression>>,
) : ShortenCommand {

    override fun invokeShortening() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        for (nameToImport in importsToAdd) {
            addImportToFile(targetFile.project, targetFile, nameToImport)
        }

        for (typePointer in typesToShorten) {
            val type = typePointer.element ?: continue
            type.deleteQualifier()
        }

        for (callPointer in qualifiersToShorten) {
            val call = callPointer.element ?: continue
            call.deleteQualifier()
        }
    }
}

private fun KtUserType.hasFakeRootPrefix(): Boolean =
    qualifier?.referencedName == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

private fun KtDotQualifiedExpression.hasFakeRootPrefix(): Boolean =
    (receiverExpression as? KtNameReferenceExpression)?.getReferencedName() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

private fun CallableId.asImportableFqName(): FqName? = if (classId == null) packageName.child(callableName) else null

private fun KtElement.getDotQualifiedExpressionForSelector(): KtDotQualifiedExpression? =
    getQualifiedExpressionForSelector() as? KtDotQualifiedExpression

private tailrec fun KtTypeElement?.unwrapNullable(): KtTypeElement? =
    if (this is KtNullableType) this.innerType.unwrapNullable() else this

private fun KtDotQualifiedExpression.deleteQualifier(): KtExpression? {
    val selectorExpression = selectorExpression ?: return null
    return this.replace(selectorExpression) as KtExpression
}
