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

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.backend.konan.serialization.Base64
import org.jetbrains.kotlin.backend.konan.serialization.parseModuleHeader
import org.jetbrains.kotlin.backend.konan.serialization.parsePackageFragment

class PrettyPrinter(val library: Base64, val packageLoader: (String) -> Base64) {

    private val moduleHeader: KonanLinkData.Library
        get() = parseModuleHeader(library)

    fun packageFragment(fqname: String): KonanLinkData.PackageFragment 
        = parsePackageFragment(packageLoader(fqname))
            
    val packageFragmentNameList: List<String>
        get() = moduleHeader.packageFragmentNameList

    fun printPackageFragment(fqname: String) {
        if (fqname.isNotEmpty()) println("package $fqname" ) 
        println("\tHere goes the \"$fqname\" package body.\n\tIt is not implemented yet.\n")
        // TODO: implement deserialized package protobuf print out.
    }
}

