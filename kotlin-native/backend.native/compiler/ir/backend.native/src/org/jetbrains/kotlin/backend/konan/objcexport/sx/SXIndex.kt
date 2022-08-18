/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport.sx

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

/**
 * Collection of declarations that affect the result of ObjCExport.
 */
class SXIndex {
    val storage: MutableList<Item> = mutableListOf<Item>()

    class Item(val declaration: DeclarationDescriptor, val moduleBuilder: SXClangModuleBuilder)
}