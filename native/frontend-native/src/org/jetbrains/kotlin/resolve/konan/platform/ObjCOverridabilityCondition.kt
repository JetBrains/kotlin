/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.objcinterop.getExternalObjCMethodInfo
import org.jetbrains.kotlin.ir.objcinterop.isExternalObjCClass
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition

/**
 * Describes method overriding rules for Objective-C methods.
 *
 * This class is applied at [org.jetbrains.kotlin.resolve.OverridingUtil] as configured with
 * `META-INF/services/org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition` resource.
 *
 * The same code, as in this class also exists in [org.jetbrains.kotlin.fir.backend.native.FirNativeOverrideChecker]
 * and in [org.jetbrains.kotlin.ir.objcinterop.IrObjCOverridabilityCondition].
 *
 * When modifying, all three copies should be synchronized.
 *
 */
class ObjCOverridabilityCondition : ExternalOverridabilityCondition {

    override fun getContract() = ExternalOverridabilityCondition.Contract.BOTH

    override fun isOverridable(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        subClassDescriptor: ClassDescriptor?
    ): ExternalOverridabilityCondition.Result {
        if (superDescriptor.name == subDescriptor.name) { // Slow path:
            // KT-57640: There's no necessity to implement platform-dependent overridability check for properties
            if (superDescriptor is FunctionDescriptor && subDescriptor is FunctionDescriptor) {
                superDescriptor.getExternalObjCMethodInfo()?.let { superInfo ->
                    val subInfo = subDescriptor.getExternalObjCMethodInfo()
                    if (subInfo != null) {
                        // Overriding Objective-C method by Objective-C method in interop stubs.
                        // Don't even check method signatures:
                        return if (superInfo.selector == subInfo.selector) {
                            ExternalOverridabilityCondition.Result.OVERRIDABLE
                        } else {
                            ExternalOverridabilityCondition.Result.INCOMPATIBLE
                        }
                    } else {
                        // Overriding Objective-C method by Kotlin method.
                        if (!parameterNamesMatch(superDescriptor, subDescriptor)) {
                            return ExternalOverridabilityCondition.Result.INCOMPATIBLE
                        }
                    }
                }
            }
        }

        return ExternalOverridabilityCondition.Result.UNKNOWN
    }

    private fun parameterNamesMatch(first: FunctionDescriptor, second: FunctionDescriptor): Boolean {
        // The original Objective-C method selector is represented as
        // function name and parameter names (except first).

        if (first.valueParameters.size != second.valueParameters.size) {
            return false
        }

        first.valueParameters.forEachIndexed { index, parameter ->
            if (index > 0 && parameter.name != second.valueParameters[index].name) {
                return false
            }
        }

        return true
    }

}