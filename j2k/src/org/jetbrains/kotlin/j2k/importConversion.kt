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

package org.jetbrains.kotlin.j2k

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.j2k.ast.Import
import org.jetbrains.kotlin.j2k.ast.ImportList
import org.jetbrains.kotlin.j2k.ast.assignPrototype
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformCompilerServices

fun Converter.convertImportList(importList: PsiImportList): ImportList {
    val imports = importList.allImportStatements
            .flatMap { convertImport(it) }
            .distinctBy { it.name } // duplicated imports may appear
    return ImportList(imports).assignPrototype(importList)
}

fun Converter.convertImport(anImport: PsiImportStatementBase, dumpConversion: Boolean = false): List<Import> {
    val reference = anImport.importReference ?: return emptyList()
    val fqName = FqName(reference.qualifiedName!!)
    val onDemand = anImport.isOnDemand
    val convertedImports = if (dumpConversion) {
        listOf(Import(renderImportName(fqName, onDemand)))
    }
    else {
        convertImport(fqName, reference, onDemand, anImport is PsiImportStaticStatement)
                .map(::Import)
    }
    return convertedImports.map { it.assignPrototype(anImport) }
}

private fun Converter.convertImport(fqName: FqName, ref: PsiJavaCodeReferenceElement, isOnDemand: Boolean, isImportStatic: Boolean): List<String> {
    if (!isOnDemand) {
        if (annotationConverter.isImportNotRequired(fqName)) return emptyList()


        val mapped = JavaToKotlinClassMap.mapJavaToKotlin(fqName)
        mapped?.let {
            // If imported class has a kotlin analog, drop the import if it is not nested
            if (!it.isNestedClass) return emptyList()
            return convertNonStaticImport(it.asSingleFqName(), false, null)
        }
    }

    //TODO: how to detect compiled Kotlin here?
    val target = ref.resolve()
    return if (isImportStatic) {
        if (isOnDemand) {
            convertStaticImportOnDemand(fqName, target)
        }
        else {
            convertStaticExplicitImport(fqName, target)
        }
    }
    else {
        convertNonStaticImport(fqName, isOnDemand, target)
    }
}

private fun Converter.convertStaticImportOnDemand(fqName: FqName, target: PsiElement?): List<String> {
    when (target) {
        is KtLightClassForFacade -> return listOf(target.fqName.parent().render() + ".*")

        is KtLightClass -> {
            val kotlinOrigin = target.kotlinOrigin
            val importFromObject = when (kotlinOrigin) {
                is KtObjectDeclaration -> kotlinOrigin
                is KtClass -> kotlinOrigin.getCompanionObjects().singleOrNull()
                else -> null
            }
            if (importFromObject != null) {
                val objectFqName = importFromObject.fqName
                if (objectFqName != null) {
                    // cannot import on demand from object, generate imports for all members
                    return importFromObject.declarations
                            .mapNotNull {
                                val descriptor = services.resolverForConverter.resolveToDescriptor(it) ?: return@mapNotNull null
                                if (descriptor.hasJvmStaticAnnotation() || descriptor is PropertyDescriptor && descriptor.isConst)
                                    descriptor.name
                                else
                                    null
                            }
                            .distinct()
                            .map { objectFqName.child(it).render() }
                }
            }
        }
    }
    return listOf(renderImportName(fqName, isOnDemand = true))
}

private fun convertStaticExplicitImport(fqName: FqName, target: PsiElement?): List<String> {
    if (target is KtLightDeclaration<*, *>) {
        val kotlinOrigin = target.kotlinOrigin

        val nameToImport = if (target is KtLightMethod && kotlinOrigin is KtProperty)
            kotlinOrigin.nameAsName
        else
            fqName.shortName()

        if (nameToImport != null) {
            val originParent = kotlinOrigin?.parent
            when (originParent) {
                is KtFile -> { // import of function or property accessor from file facade
                    return listOf(originParent.packageFqName.child(nameToImport).render())
                }

                is KtClassBody -> {
                    val parentClass = originParent.parent as KtClassOrObject
                    if (parentClass is KtObjectDeclaration && parentClass.isCompanion()) {
                        return listOfNotNull(parentClass.getFqName()?.child(nameToImport)?.render())
                    }
                }
            }
        }
    }
    return listOf(renderImportName(fqName, isOnDemand = false))
}

private fun convertNonStaticImport(fqName: FqName, isOnDemand: Boolean, target: PsiElement?): List<String> {
    when (target) {
        is KtLightClassForFacade -> return listOf(target.fqName.parent().render() + ".*")

        is KtLightClass -> {
            if (!isOnDemand) {
                if (isFacadeClassFromLibrary(target)) return emptyList()

                if (isImportedByDefault(target)) return emptyList()
            }
        }
    }
    return listOf(renderImportName(fqName, isOnDemand))
}

private fun renderImportName(fqName: FqName, isOnDemand: Boolean)
        = if (isOnDemand) fqName.render() + ".*" else fqName.render()

private val DEFAULT_IMPORTS_SET: Set<FqName> = JvmPlatformCompilerServices.getDefaultImports(
    // TODO: use the correct LanguageVersionSettings instance here
    LanguageVersionSettingsImpl.DEFAULT,
    includeLowPriorityImports = true
).filter { it.isAllUnder }.map { it.fqName }.toSet()

private fun isImportedByDefault(c: KtLightClass) = c.qualifiedName?.let { FqName(it).parent() } in DEFAULT_IMPORTS_SET
