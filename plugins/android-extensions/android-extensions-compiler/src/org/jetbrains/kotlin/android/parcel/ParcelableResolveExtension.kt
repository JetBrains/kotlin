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

package org.jetbrains.kotlin.android.parcel

import com.intellij.openapi.application.ApplicationManager
import kotlinx.android.parcel.Parceler
import kotlinx.android.parcel.Parcelize
import org.jetbrains.kotlin.android.parcel.ParcelableSyntheticComponent.ComponentKind.DESCRIBE_CONTENTS
import org.jetbrains.kotlin.android.parcel.ParcelableSyntheticComponent.ComponentKind.WRITE_TO_PARCEL
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType

open class ParcelableResolveExtension : SyntheticResolveExtension {
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
                componentKind: ParcelableSyntheticComponent.ComponentKind,
                modality: Modality,
                returnType: KotlinType,
                vararg parameters: Pair<String, KotlinType>
        ): SimpleFunctionDescriptor {
            val functionDescriptor = object : ParcelableSyntheticComponent, SimpleFunctionDescriptorImpl(
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
                    null, classDescriptor.thisAsReceiverParameter, emptyList(), emptyList(), valueParameters,
                    returnType, modality, DescriptorVisibilities.PUBLIC)

            return functionDescriptor
        }

        private fun FunctionDescriptor.makeValueParameter(name: String, type: KotlinType, index: Int): ValueParameterDescriptor {
            return ValueParameterDescriptorImpl(
                    this, null, index, Annotations.EMPTY, Name.identifier(name), type, false, false, false, null, this.source)
        }

        private val parcelizeMethodNames: List<Name> =
            listOf(Name.identifier(DESCRIBE_CONTENTS.methodName), Name.identifier(WRITE_TO_PARCEL.methodName))
    }

    @Deprecated(
        "@Parcelize is now available in non-experimental setups as well.",
        replaceWith = ReplaceWith("true"),
        level = DeprecationLevel.ERROR
    )
    protected open fun isExperimental(element: KtElement) = true

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor) = null

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        return if (thisDescriptor.isParcelize)
            parcelizeMethodNames
        else
            emptyList()
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        if (name.asString() == DESCRIBE_CONTENTS.methodName
            && thisDescriptor.isParcelize
            && result.none { it.isDescribeContents() }
            && fromSupertypes.none { it.isDescribeContents() }
        ) {
            result += createMethod(thisDescriptor, DESCRIBE_CONTENTS, Modality.OPEN, thisDescriptor.builtIns.intType)
        } else if (name.asString() == WRITE_TO_PARCEL.methodName
            && thisDescriptor.isParcelize
            && result.none { it.isWriteToParcel() }
        ) {
            val builtIns = thisDescriptor.builtIns
            val parcelClassType = resolveParcelClassType(thisDescriptor.module) ?: ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_PARCEL_TYPE)
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

interface ParcelableSyntheticComponent {
    val componentKind: ComponentKind

    enum class ComponentKind(val methodName: String) {
        WRITE_TO_PARCEL("writeToParcel"),
        DESCRIBE_CONTENTS("describeContents"),
        NEW_ARRAY("newArray"),
        CREATE_FROM_PARCEL("createFromParcel")
    }
}

val PARCELIZE_CLASS_FQNAME: FqName = FqName(Parcelize::class.java.canonicalName)
internal val PARCELER_FQNAME: FqName = FqName(Parceler::class.java.canonicalName)

private val PARCELIZE_PLUGIN_PACKAGE = FqName("kotlinx.parcelize")

val ClassDescriptor.isParcelize: Boolean
    get() {
        val parcelizeAnnotation = this.annotations.findAnnotation(PARCELIZE_CLASS_FQNAME) ?: return false
        if (ApplicationManager.getApplication().isHeadlessEnvironment) {
            // Module check shouldn't affect compilation
            return true
        }

        val module = parcelizeAnnotation.type.constructor.declarationDescriptor?.module ?: return false
        return module.getPackage(PARCELIZE_PLUGIN_PACKAGE).isEmpty()
    }

val KotlinType.isParceler
    get() = constructor.declarationDescriptor?.fqNameSafe == PARCELER_FQNAME
