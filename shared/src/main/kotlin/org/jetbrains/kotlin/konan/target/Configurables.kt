/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed -> in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.properties.*

interface Configurables : TargetableExternalStorage {

    val target: KonanTarget

    val llvmHome get() = hostString("llvmHome")
    val llvmVersion get() = hostString("llvmVersion")
    val libffiDir get() = hostString("libffiDir")

    // TODO: Delegate to a map?
    val llvmLtoNooptFlags get() = targetList("llvmLtoNooptFlags")
    val llvmLtoOptFlags get() = targetList("llvmLtoOptFlags")
    val llvmLtoFlags get() = targetList("llvmLtoFlags")
    val llvmLtoDynamicFlags get() = targetList("llvmLtoDynamicFlags")
    val entrySelector get() = targetList("entrySelector")
    val linkerOptimizationFlags get() = targetList("linkerOptimizationFlags")
    val linkerKonanFlags get() = targetList("linkerKonanFlags")
    val linkerNoDebugFlags get() = targetList("linkerNoDebugFlags")
    val linkerDynamicFlags get() = targetList("linkerDynamicFlags")
    val llvmDebugOptFlags get() = targetList("llvmDebugOptFlags")
    val targetSysRoot get() = targetString("targetSysRoot")

    // Notice: these ones are host-target.
    val targetToolchain get() = hostTargetString("targetToolchain")

    val absoluteTargetSysRoot get() = absolute(targetSysRoot)
    val absoluteTargetToolchain get() = absolute(targetToolchain)
    val absoluteLlvmHome get() = absolute(llvmHome)
}

interface NonAppleConfigurables : Configurables {
    val targetArg get() = targetString("quadruple")
}

interface AppleConfigurables : Configurables {
    val arch get() = targetString("arch")!!
    val osVersionMin get() = targetString("osVersionMin")!!
    val osVersionMinFlagLd get() = targetString("osVersionMinFlagLd")!!
}

interface MingwConfigurables : NonAppleConfigurables

interface LinuxBasedConfigurables : NonAppleConfigurables {
    val gccToolchain get() = hostString("gccToolchain")
    val absoluteGccToolchain get() = absolute(gccToolchain)

    val libGcc get() = targetString("libGcc")!!
    val dynamicLinker get() = targetString("dynamicLinker")!!
    val pluginOptimizationFlags get() = targetList("pluginOptimizationFlags")
    val abiSpecificLibraries get() = targetList("abiSpecificLibraries")
}

interface LinuxConfigurables : LinuxBasedConfigurables
interface LinuxMIPSConfigurables : LinuxBasedConfigurables
interface RaspberryPiConfigurables : LinuxBasedConfigurables
interface AndroidConfigurables : NonAppleConfigurables

interface WasmConfigurables : NonAppleConfigurables {

    val llcFlags get()      = targetList("llcFlags")
    val llcNooptFlags get() = targetList("llcNooptFlags")
    val llcOptFlags get()   = targetList("llcOptFlags")
    val llcDebugFlags get() = targetList("llcDebugFlags")

    val optFlags get()      = targetList("optFlags")
    val optNooptFlags get() = targetList("optNooptFlags")
    val optOptFlags get()   = targetList("optOptFlags")
    val optDebugFlags get() = targetList("optDebugFlags")

    val lldFlags get()      = targetList("lld")
}

interface ZephyrConfigurables : NonAppleConfigurables {
    val boardSpecificClangFlags get() = targetList("boardSpecificClangFlags")
}

