package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.defects.*

class DefectReport {
    val defects: MutableList<Defect> = ArrayList()

    fun report(type: DefectType, location: Location, vararg attributes: Pair<DefectAttribute, String>) {
        defects.add(Defect(location, DefectInfo(type, attributes.toMap())))
    }

    fun isEmpty() = defects.isEmpty()
}



