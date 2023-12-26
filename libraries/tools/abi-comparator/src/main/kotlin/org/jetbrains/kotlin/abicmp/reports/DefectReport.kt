/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.defects.*

class DefectReport {
    val defects: MutableList<Defect> = ArrayList()

    fun report(type: DefectType, location: Location, vararg attributes: Pair<DefectAttribute, String>) {
        defects.add(Defect(location, DefectInfo(type, attributes.toMap())))
    }

    fun isEmpty() = defects.isEmpty()
}



