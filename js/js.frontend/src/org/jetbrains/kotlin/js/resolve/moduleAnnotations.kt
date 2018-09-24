/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve

import org.jetbrains.kotlin.descriptors.ClassOrPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptPackageFragment

fun getAnnotationsOnContainingJsModule(descriptor: DeclarationDescriptor): List<ClassId>? {
    val parent = DescriptorUtils.getParentOfType(descriptor, ClassOrPackageFragmentDescriptor::class.java, false) ?: return emptyList()

    val parentSource = (descriptor as? DeserializedMemberDescriptor)?.containerSource ?: parent.source
    return (parentSource as? KotlinJavascriptPackageFragment.JsContainerSource)?.annotations
}
