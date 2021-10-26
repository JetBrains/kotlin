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

package org.jetbrains.kotlin.parcelize

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parcelize.ParcelizeSyntheticComponent.ComponentKind.DESCRIBE_CONTENTS
import org.jetbrains.kotlin.parcelize.ParcelizeSyntheticComponent.ComponentKind.WRITE_TO_PARCEL
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType

open class ParcelizeResolveExtension : SyntheticResolveExtension {
    companion object {
        fun resolveParcelClassType(module: ModuleDescriptor): SimpleType? {
            return module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.os.Parcel")))?.defaultType
        }

        fun resolveParcelableCreatorClassType(module: ModuleDescriptor): SimpleType? {
            val creatorClassId = ClassId(FqName("android.os"), FqName("Parcelable.Creator"), false)
            return module.findClassAcrossModuleDependencies(creatorClassId)?.defaultType
        }

        fun createMethod(
            classDescriptor: ClassDescriptor,
            componentKind: ParcelizeSyntheticComponent.ComponentKind,
            modality: Modality,
            returnType: KotlinType,
            vararg parameters: Pair<String, KotlinType>
        ): SimpleFunctionDescriptor {
            val functionDescriptor = object : ParcelizeSyntheticComponent, SimpleFunctionDescriptorImpl(
                classDescriptor,
                null,
                Annotations.EMPTY,
                Name.identifier(componentKind.methodName),
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                classDescriptor.source
            ) {
                override val componentKind = componentKind
            }

            val valueParameters = parameters.mapIndexed { index, (name, type) -> functionDescriptor.makeValueParameter(name, type, index) }

            functionDescriptor.initialize(
                null, classDescriptor.thisAsReceiverParameter, emptyList(), valueParameters,
                returnType, modality, DescriptorVisibilities.PUBLIC
            )

            return functionDescriptor
        }

        private fun FunctionDescriptor.makeValueParameter(name: String, type: KotlinType, index: Int): ValueParameterDescriptor {
            return ValueParameterDescriptorImpl(
                containingDeclaration = this,
                original = null,
                index = index,
                annotations = Annotations.EMPTY,
                name = Name.identifier(name),
                outType = type,
                declaresDefaultValue = false,
                isCrossinline = false,
                isNoinline = false,
                varargElementType = null,
                source = this.source
            )
        }

        private val parcelizeMethodNames: List<Name> =
            listOf(Name.identifier(DESCRIBE_CONTENTS.methodName), Name.identifier(WRITE_TO_PARCEL.methodName))
    }

    open fun isAvailable(element: PsiElement): Boolean {
        return true
    }

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? = null

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        return if (thisDescriptor.isParcelize) parcelizeMethodNames else emptyList()
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        fun isParcelizePluginEnabled(): Boolean {
            val sourceElement = (thisDescriptor.source as? PsiSourceElement)?.psi ?: return false
            return isAvailable(sourceElement)
        }

        if (name.asString() == DESCRIBE_CONTENTS.methodName
            && thisDescriptor.isParcelize
            && !thisDescriptor.isSealed()
            && isParcelizePluginEnabled()
            && result.none { it.isDescribeContents() }
            && fromSupertypes.none { it.isDescribeContents() }
        ) {
            result += createMethod(thisDescriptor, DESCRIBE_CONTENTS, Modality.OPEN, thisDescriptor.builtIns.intType)
        } else if (name.asString() == WRITE_TO_PARCEL.methodName
            && thisDescriptor.isParcelize
            && !thisDescriptor.isSealed()
            && isParcelizePluginEnabled()
            && result.none { it.isWriteToParcel() }
        ) {
            val builtIns = thisDescriptor.builtIns
            val parcelClassType = resolveParcelClassType(thisDescriptor.module) ?: ErrorUtils.createErrorType("Unresolved 'Parcel' type")
            result += createMethod(
                thisDescriptor, WRITE_TO_PARCEL, Modality.OPEN,
                builtIns.unitType, "parcel" to parcelClassType, "flags" to builtIns.intType
            )
        }
    }

    private fun SimpleFunctionDescriptor.isDescribeContents(): Boolean {
        return this.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                && modality != Modality.ABSTRACT
                && typeParameters.isEmpty()
                && valueParameters.isEmpty()
        // Unfortunately, we can't check the return type as it's unresolved in IDE light classes
    }
}

internal fun SimpleFunctionDescriptor.isWriteToParcel(): Boolean {
    return typeParameters.isEmpty()
            && valueParameters.size == 2
            // Unfortunately, we can't check the first parameter type as it's unresolved in IDE light classes
            && KotlinBuiltIns.isInt(valueParameters[1].type)
            && returnType?.let { KotlinBuiltIns.isUnit(it) } == true
}

interface ParcelizeSyntheticComponent {
    val componentKind: ComponentKind

    enum class ComponentKind(val methodName: String) {
        WRITE_TO_PARCEL("writeToParcel"),
        DESCRIBE_CONTENTS("describeContents"),
        NEW_ARRAY("newArray"),
        CREATE_FROM_PARCEL("createFromParcel")
    }
}

val PACKAGES_FQ_NAMES = listOf(
    FqName("kotlinx.parcelize"),
    FqName("kotlinx.android.parcel")
)

private fun createClassIds(name: String): List<ClassId> {
    return PACKAGES_FQ_NAMES.map { ClassId(it, Name.identifier(name)) }
}

private fun List<ClassId>.fqNames(): List<FqName> {
    return map { it.asSingleFqName() }
}

val TYPE_PARCELER_CLASS_IDS = createClassIds("TypeParceler")
val TYPE_PARCELER_FQ_NAMES = TYPE_PARCELER_CLASS_IDS.fqNames()

val WRITE_WITH_CLASS_IDS = createClassIds("WriteWith")
val WRITE_WITH_FQ_NAMES = WRITE_WITH_CLASS_IDS.fqNames()

val IGNORED_ON_PARCEL_CLASS_IDS = createClassIds("IgnoredOnParcel")
val IGNORED_ON_PARCEL_FQ_NAMES = IGNORED_ON_PARCEL_CLASS_IDS.fqNames()

val PARCELIZE_CLASS_CLASS_IDS = createClassIds("Parcelize")
val PARCELIZE_CLASS_FQ_NAMES: List<FqName> = PARCELIZE_CLASS_CLASS_IDS.fqNames()

val RAW_VALUE_ANNOTATION_CLASS_IDS = createClassIds("RawValue")
val RAW_VALUE_ANNOTATION_FQ_NAMES = RAW_VALUE_ANNOTATION_CLASS_IDS.fqNames()

internal val PARCELER_CLASS_ID = ClassId(FqName("kotlinx.parcelize"), Name.identifier("Parceler"))
internal val PARCELER_FQNAME = PARCELER_CLASS_ID.asSingleFqName()

internal val OLD_PARCELER_CLASS_ID = ClassId(FqName("kotlinx.android.parcel"), Name.identifier("Parceler"))
internal val OLD_PARCELER_FQNAME = OLD_PARCELER_CLASS_ID.asSingleFqName()

val ClassDescriptor.hasParcelizeAnnotation: Boolean
    get() = PARCELIZE_CLASS_FQ_NAMES.any(annotations::hasAnnotation)

val ClassDescriptor.isParcelize: Boolean
    get() = hasParcelizeAnnotation
            || getSuperClassNotAny()?.takeIf(DescriptorUtils::isSealedClass)?.hasParcelizeAnnotation == true
            || getSuperInterfaces().any { DescriptorUtils.isSealedClass(it) && it.hasParcelizeAnnotation }

val KotlinType.isParceler: Boolean
    get() = constructor.declarationDescriptor?.fqNameSafe == PARCELER_FQNAME

fun Annotated.findAnyAnnotation(fqNames: List<FqName>): AnnotationDescriptor? {
    for (fqName in fqNames) {
        val annotation = annotations.findAnnotation(fqName)
        if (annotation != null) {
            return annotation
        }
    }

    return null
}

fun Annotated.hasAnyAnnotation(fqNames: List<FqName>): Boolean {
    for (fqName in fqNames) {
        if (annotations.hasAnnotation(fqName)) {
            return true
        }
    }

    return false
}
