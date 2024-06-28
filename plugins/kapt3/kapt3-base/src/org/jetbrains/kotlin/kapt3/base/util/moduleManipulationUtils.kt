/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.util

import sun.misc.Unsafe
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Modifier

val REQUIRED_PACKAGES_TO_TEST_CLASSES = mapOf(
    "com.sun.tools.javac.util" to "Context",
    "com.sun.tools.javac.file" to "CacheFSInfo",
    "com.sun.tools.javac.tree" to "TreeTranslator",
    "com.sun.tools.javac.main" to "Option",
    "com.sun.tools.javac.jvm" to "ClassFile",
    "com.sun.tools.javac.parser" to "Tokens\$TokenKind",
    "com.sun.tools.javac.code" to "Source",
    "com.sun.tools.javac.processing" to "PrintingProcessor",
    "com.sun.tools.javac.comp" to "AttrContext",
    "com.sun.tools.javac.api" to "DiagnosticFormatter\$PositionKind"
)

private fun openPackages(packagesToOpen: Collection<String>) {
    @Suppress("UNCHECKED_CAST")
    fun allModules(): Collection<Any>? {
        // Actually it is similar to ModuleLayer.boot().modules()
        try {
            val boot = Class.forName("java.lang.ModuleLayer").getMethod("boot").invoke(null) ?: return null
            return boot.javaClass.getMethod("modules").invoke(boot) as? Collection<Any>
        } catch (_: Exception) {
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun getPackages(module: Any) =
        // similar to module.packages
        module.javaClass.getMethod("getPackages").invoke(module) as Collection<String>
    val modules = allModules() ?: return
    val unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").apply {
        isAccessible = true
    }.get(null) as Unsafe
    val implLookupField = MethodHandles.Lookup::class.java.getDeclaredField("IMPL_LOOKUP")
    val lookup =
        unsafe.getObject(
            unsafe.staticFieldBase(implLookupField),
            unsafe.staticFieldOffset(implLookupField)
        ) as MethodHandles.Lookup
    val modifiers = lookup.findSetter(Method::class.java, "modifiers", Integer.TYPE)

    val exportMethod: Method = Class.forName("java.lang.Module").getDeclaredMethod("implAddOpens", String::class.java)
    modifiers.invokeExact(exportMethod, Modifier.PUBLIC)

    modules.forEach { module ->
        getPackages(module).filter { packagesToOpen.contains(it) }.forEach { name ->
            exportMethod.invoke(module, name)
        }
    }
}

private fun unavailableRequiredPackages() = REQUIRED_PACKAGES_TO_TEST_CLASSES.filter { entry ->
    try {
        val classInstance = Class.forName("${entry.key}.${entry.value}")
        if (classInstance.isEnum) {
            classInstance.getMethod("values").invoke(null)
        } else {
            classInstance.getDeclaredConstructor().newInstance()
        }
        false
    } catch (_: IllegalAccessException) {
        true
    } catch (_: Exception) {
        // in old versions of JDK some classes could be unavailable
        false
    }
}.keys

private var checkDone = false

@Synchronized
fun doOpenInternalPackagesIfRequired() {
    if (checkDone)
        return
    try {
        checkDone = true
        val unavailablePackages = unavailableRequiredPackages()
        if (unavailablePackages.isNotEmpty()) {
            openPackages(unavailablePackages)
            val failedToOpen = unavailableRequiredPackages()
            if (failedToOpen.isNotEmpty()) {
                System.err.println(
                    "WARNING: Some required internal classes are unavailable. Please consider adding the following JVM arguments\n" +
                            "WARNING: ${failedToOpen.joinToString(" ") { "--add-opens jdk.compiler/$it=ALL-UNNAMED" }}"
                )
            }
        }
    } catch (e: Throwable) {
        System.err.println("WARNING: Failed to check for unavailable JDK packages. Reason: ${e.message}")
    }
}
