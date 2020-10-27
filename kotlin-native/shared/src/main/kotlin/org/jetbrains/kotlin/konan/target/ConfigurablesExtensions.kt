package org.jetbrains.kotlin.konan.target

fun ZephyrConfigurables.constructClangArgs(): List<String> = mutableListOf<String>().apply {
        targetCpu?.let {
            add("-mcpu=$it")
        }
        targetAbi?.let {
            add("-mabi=$it")
        }
        addAll(boardSpecificClangFlags)
    }


fun ZephyrConfigurables.constructClangCC1Args(): List<String> = mutableListOf<String>().apply {
    addAll("-cc1 -emit-obj -disable-llvm-optzns -x ir -fdata-sections -ffunction-sections".split(" "))
    targetCpu?.let {
        addAll(listOf("-target-abi", it))
    }
    targetAbi?.let {
        addAll(listOf("-target-cpu", it))
    }
    addAll(boardSpecificClangFlags)
}