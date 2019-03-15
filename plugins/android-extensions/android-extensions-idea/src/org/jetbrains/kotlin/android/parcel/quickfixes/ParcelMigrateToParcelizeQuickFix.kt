/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.parcel.quickfixes

import com.intellij.openapi.diagnostic.Logger
import kotlinx.android.parcel.Parceler
import org.jetbrains.kotlin.android.parcel.ANDROID_PARCELABLE_CREATOR_CLASS_FQNAME
import org.jetbrains.kotlin.android.parcel.ANDROID_PARCEL_CLASS_FQNAME
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.getOrCreateCompanionObject
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.unwrapBlockOrParenthesis
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.setModifierList
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.TypeUtils

class ParcelMigrateToParcelizeQuickFix(function: KtClass) : AbstractParcelableQuickFix<KtClass>(function) {
    companion object {
        private val PARCELER_FQNAME = FqName(Parceler::class.java.name)
        private val PARCELER_WRITE_FUNCTION_NAME = Name.identifier("write")
        private val PARCELER_CREATE_FUNCTION_NAME = Name.identifier("create")
        private val LOG = Logger.getInstance(ParcelMigrateToParcelizeQuickFix::class.java)

        private fun KtClass.findParcelerCompanionObject(): Pair<KtObjectDeclaration, ClassDescriptor>? {
            for (obj in companionObjects) {
                val objDescriptor = obj.resolveToDescriptorIfAny() ?: continue
                for (superClassifier in objDescriptor.getAllSuperClassifiers()) {
                    val superClass = superClassifier as? ClassDescriptor ?: continue
                    if (superClass.fqNameSafe == PARCELER_FQNAME) return Pair(obj, objDescriptor)
                }
            }

            return null
        }

        private fun KtNamedFunction.doesLookLikeWriteToParcelOverride(): Boolean {
            return name == "writeToParcel"
                   && hasModifier(KtTokens.OVERRIDE_KEYWORD)
                   && receiverTypeReference == null
                   && valueParameters.size == 2
                   && typeParameters.size == 0
                   && valueParameters[0].typeReference?.getFqName() == ANDROID_PARCEL_CLASS_FQNAME.asString()
                   && valueParameters[1].typeReference?.getFqName() == KotlinBuiltIns.FQ_NAMES._int.asString()
        }

        private fun KtNamedFunction.doesLookLikeNewArrayOverride(): Boolean {
            return name == "newArray"
                   && hasModifier(KtTokens.OVERRIDE_KEYWORD)
                   && receiverTypeReference == null
                   && valueParameters.size == 1
                   && typeParameters.size == 0
                   && valueParameters[0].typeReference?.getFqName() == KotlinBuiltIns.FQ_NAMES._int.asString()
        }

        private fun KtNamedFunction.doesLookLikeDescribeContentsOverride(): Boolean {
            return name == "describeContents"
                   && hasModifier(KtTokens.OVERRIDE_KEYWORD)
                   && receiverTypeReference == null
                   && valueParameters.size == 0
                   && typeParameters.size == 0
                   && typeReference?.getFqName() == KotlinBuiltIns.FQ_NAMES._int.asString()
        }

        private fun KtClass.findWriteToParcelOverride() = findFunction { doesLookLikeWriteToParcelOverride() }
        private fun KtClass.findDescribeContentsOverride() = findFunction { doesLookLikeDescribeContentsOverride() }
        private fun KtObjectDeclaration.findNewArrayOverride() = findFunction { doesLookLikeNewArrayOverride() }

        private fun KtClass.findCreatorClass(): KtClassOrObject? {
            for (companion in companionObjects) {
                if (companion.name == "CREATOR") {
                    return companion
                }

                val creatorProperty = companion.declarations.asSequence()
                        .filterIsInstance<KtProperty>()
                        .firstOrNull { it.name == "CREATOR" }
                        ?: continue

                creatorProperty.findAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME) ?: continue

                val initializer = creatorProperty.initializer ?: continue
                when (initializer) {
                    is KtObjectLiteralExpression -> return initializer.objectDeclaration
                    is KtCallExpression -> {
                        val constructedClass = (initializer.resolveToCall()
                            ?.resultingDescriptor as? ConstructorDescriptor)?.constructedClass
                        if (constructedClass != null) {
                            val sourceElement = constructedClass.source as? KotlinSourceElement
                            (sourceElement?.psi as? KtClassOrObject)?.let { return it }
                        }
                    }
                }
            }

            return null
        }

        private fun KtNamedFunction.doesLookLikeCreateFromParcelOverride(): Boolean {
            return name == "createFromParcel"
                   && hasModifier(KtTokens.OVERRIDE_KEYWORD)
                   && receiverTypeReference == null
                   && valueParameters.size == 1
                   && typeParameters.size == 0
                   && valueParameters[0].typeReference?.getFqName() == ANDROID_PARCEL_CLASS_FQNAME.asString()
        }

        private fun findCreateFromParcel(creator: KtClassOrObject) = creator.findFunction { doesLookLikeCreateFromParcelOverride() }

        private fun KtNamedFunction.doesLookLikeWriteImplementation(): Boolean {
            val containingParcelableClassFqName = containingClassOrObject?.containingClass()?.fqName?.asString()

            return name == PARCELER_WRITE_FUNCTION_NAME.asString()
                   && hasModifier(KtTokens.OVERRIDE_KEYWORD)
                   && receiverTypeReference?.getFqName() == containingParcelableClassFqName
                   && valueParameters.size == 2
                   && typeParameters.size == 0
                   && valueParameters[0].typeReference?.getFqName() == ANDROID_PARCEL_CLASS_FQNAME.asString()
                   && valueParameters[1].typeReference?.getFqName() == KotlinBuiltIns.FQ_NAMES._int.asString()
        }

        private fun KtNamedFunction.doesLookLikeCreateImplementation(): Boolean {
            return name == PARCELER_CREATE_FUNCTION_NAME.asString()
                   && hasModifier(KtTokens.OVERRIDE_KEYWORD)
                   && receiverTypeReference == null
                   && valueParameters.size == 1
                   && typeParameters.size == 0
                   && valueParameters[0].typeReference?.getFqName() == ANDROID_PARCEL_CLASS_FQNAME.asString()
        }

        private fun KtObjectDeclaration.findCreateImplementation() = findFunction { doesLookLikeCreateImplementation() }
        private fun KtObjectDeclaration.findWriteImplementation() = findFunction { doesLookLikeWriteImplementation() }

        private fun KtClassOrObject.findFunction(f: KtNamedFunction.() -> Boolean)
                = declarations.asSequence().filterIsInstance<KtNamedFunction>().firstOrNull(f)

        private fun KtTypeReference.getFqName(): String? = analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, this]
                ?.constructor?.declarationDescriptor?.fqNameSafe?.asString()
    }

    object FactoryForWrite : AbstractFactory({ findElement<KtClass>()?.let { ParcelMigrateToParcelizeQuickFix(it) } })

    object FactoryForCREATOR : AbstractFactory({
        findElement<KtObjectDeclaration>()?.getStrictParentOfType<KtClass>()?.let { ParcelMigrateToParcelizeQuickFix(it) }
    })

    override fun getText() = "Migrate to ''Parceler'' companion object"

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun invoke(ktPsiFactory: KtPsiFactory, parcelableClass: KtClass) {
        val parcelerObject = parcelableClass.findParcelerCompanionObject()?.first ?: parcelableClass.getOrCreateCompanionObject()
        val bindingContext = parcelerObject.analyze(BodyResolveMode.PARTIAL)

        val parcelerTypeArg = parcelableClass.name ?: run {
            LOG.error("Parceler class should not be an anonymous class")
            return
        }

        val parcelerObjectDescriptor = bindingContext[BindingContext.CLASS, parcelerObject] ?: run {
            LOG.error("Unable to resolve parceler object for ${parcelableClass.name ?: "<unnamed Parcelable class>"}")
            return
        }

        if (!parcelerObjectDescriptor.getAllSuperClassifiers().any { it.fqNameSafe == PARCELER_FQNAME }) {
            val entryText = PARCELER_FQNAME.asString() + "<" + parcelerTypeArg + ">"
            parcelerObject.addSuperTypeListEntry(ktPsiFactory.createSuperTypeEntry(entryText)).shortenReferences()
        }

        val oldWriteToParcelFunction = parcelableClass.findWriteToParcelOverride()
        val oldCreateFromParcelFunction = parcelableClass.findCreatorClass()?.let { findCreateFromParcel(it) }

        for (superTypeEntry in parcelerObject.superTypeListEntries) {
            val superClass = bindingContext[BindingContext.TYPE, superTypeEntry.typeReference]?.constructor?.declarationDescriptor ?: continue
            if (superClass.getAllSuperClassifiers().any { it.fqNameSafe == ANDROID_PARCELABLE_CREATOR_CLASS_FQNAME }) {
                parcelerObject.removeSuperTypeListEntry(superTypeEntry)
            }
        }

        if (parcelerObject.name == "CREATOR") {
            parcelerObject.nameIdentifier?.delete()
        }

        if (oldWriteToParcelFunction != null) {
            parcelerObject.findWriteImplementation()?.delete() // Remove old implementation

            val newFunction = oldWriteToParcelFunction.copy() as KtFunction
            oldWriteToParcelFunction.delete()

            newFunction.setName(PARCELER_WRITE_FUNCTION_NAME.asString())
            newFunction.setModifierList(ktPsiFactory.createModifierList(KtTokens.OVERRIDE_KEYWORD))
            newFunction.setReceiverTypeReference(ktPsiFactory.createType(parcelerTypeArg))
            newFunction.valueParameterList?.apply {
                assert(parameters.size == 2)
                val parcelParameterName = parameters[0].name ?: "parcel"
                val flagsParameterName = parameters[1].name ?: "flags"

                repeat(parameters.size) { removeParameter(0) }
                addParameter(ktPsiFactory.createParameter("$parcelParameterName : ${ANDROID_PARCEL_CLASS_FQNAME.asString()}"))
                addParameter(ktPsiFactory.createParameter("$flagsParameterName : Int"))
            }

            parcelerObject.addDeclaration(newFunction).valueParameterList?.shortenReferences()
        } else if (parcelerObject.findWriteImplementation() == null) {
            val writeFunction = "fun $parcelerTypeArg.write(parcel: ${ANDROID_PARCEL_CLASS_FQNAME.asString()}, flags: Int) = TODO()"
            parcelerObject.addDeclaration(ktPsiFactory.createFunction(writeFunction)).valueParameterList?.shortenReferences()
        }

        if (oldCreateFromParcelFunction != null) {
            parcelerObject.findCreateImplementation()?.delete() // Remove old implementation

            val newFunction = oldCreateFromParcelFunction.copy() as KtFunction
            if (oldCreateFromParcelFunction.containingClassOrObject == parcelerObject) {
                oldCreateFromParcelFunction.delete()
            }

            newFunction.setName(PARCELER_CREATE_FUNCTION_NAME.asString())
            newFunction.setModifierList(ktPsiFactory.createModifierList(KtTokens.OVERRIDE_KEYWORD))
            newFunction.setReceiverTypeReference(null)
            newFunction.valueParameterList?.apply {
                assert(parameters.size == 1)
                val parcelParameterName = parameters[0].name ?: "parcel"

                removeParameter(0)
                addParameter(ktPsiFactory.createParameter("$parcelParameterName : ${ANDROID_PARCEL_CLASS_FQNAME.asString()}"))
            }

            parcelerObject.addDeclaration(newFunction).valueParameterList?.shortenReferences()
        } else if (parcelerObject.findCreateImplementation() == null) {
            val createFunction = "override fun create(parcel: ${ANDROID_PARCEL_CLASS_FQNAME.asString()}): $parcelerTypeArg = TODO()"
            parcelerObject.addDeclaration(ktPsiFactory.createFunction(createFunction)).valueParameterList?.shortenReferences()
        }

        // Always use the default newArray() implementation
        parcelerObject.findNewArrayOverride()?.delete()

        parcelableClass.findDescribeContentsOverride()?.let { describeContentsFunction ->
            val returnExpr = describeContentsFunction.bodyExpression?.unwrapBlockOrParenthesis()
            if (returnExpr is KtReturnExpression && returnExpr.getTargetLabel() == null) {
                val returnValue = returnExpr.analyze()[BindingContext.COMPILE_TIME_VALUE, returnExpr.returnedExpression]
                        ?.getValue(TypeUtils.NO_EXPECTED_TYPE)
                if (returnValue == 0) {
                    // There are no additional overrides in the hierarchy
                    if (bindingContext[BindingContext.FUNCTION, describeContentsFunction]?.overriddenDescriptors?.size == 1) {
                        describeContentsFunction.delete()
                    }
                }
            }
        }

        for (property in parcelerObject.declarations.asSequence().filterIsInstance<KtProperty>().filter { it.name == "CREATOR" }) {
            if (property.findAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME) != null) {
                property.delete()
            }
        }
    }
}