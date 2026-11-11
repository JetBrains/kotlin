/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.printer.impl

import org.jetbrains.kotlin.kir.*
import org.jetbrains.kotlin.utils.IndentingPrinter
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

internal class KirAsKotlinSourcesPrinter private constructor(
    private val printer: SmartPrinter,
    private val emptyBodyStub: KirFunctionBody,
) : IndentingPrinter by printer {
    companion object {
        fun print(
            module: KirModule,
            emptyBodyStub: KirFunctionBody,
        ): String {
            val printer = KirAsKotlinSourcesPrinter(
                SmartPrinter(StringBuilder()),
                emptyBodyStub = emptyBodyStub,
            )
            val bridges = module.functionBridges()
            val declarationsString = with(printer) {
                printHeader(bridges, module.allDeclarations().any { it is KirClass })
                module.printChildren()
                printBridges(bridges)
                toString().trimIndent()
            }
            return declarationsString
        }

        private val classPackageDependencies = listOf(
            "kotlin.native.internal.NativePtr",
            "kotlinx.cinterop.objcPtr",
            "platform.darwin.NSObject",
        )
    }

    private fun printHeader(bridges: List<KirFunctionBridge>, hasClasses: Boolean) {
        val imports = buildList {
            bridges.flatMapTo(this) { it.kotlinFunctionBridge.packageDependencies }
            // TODO: Replace it with import support as types will be added
            if (hasClasses) addAll(classPackageDependencies)
        }.distinct()
        if (bridges.isEmpty() && imports.isEmpty()) return
        if (bridges.isNotEmpty()) {
            println("@file:kotlin.Suppress(\"DEPRECATION_ERROR\")")
            println()
        }
        imports.forEach { println("import $it") }
        println()
    }

    private fun printBridges(bridges: List<KirFunctionBridge>) = bridges.forEach { bridge ->
        println()
        bridge.kotlinFunctionBridge.lines.forEach { println(it) }
    }

    private fun KirDeclarationContainer.printChildren() = this.declarations.forEach {
        it.print()
    }

    private fun KirDeclaration.print() {
        when (this) {
            is KirClass -> printDeclaration()
            is KirCallable -> printDeclaration()
        }
    }

    private fun KirDeclaration.printVisibility() = print("${this.visibility.value} ")

    private fun KirClass.printDeclaration() {
        printModifiers()
        print("class ")
        print(name)
        // TODO: Replace hardcoded primary constructor with a normal implementation in KirClass as KirType will appear
        println("(private val __obj: NSObject) {")
        withIndent {
            println("internal fun __ptr(): NativePtr = __obj.objcPtr()")
            printChildren()
        }
        println("}")
    }

    private fun KirClass.printModifiers() {
        printVisibility()
        when (modality) {
            KirModality.OPEN -> print("open ")
            KirModality.FINAL -> print("final ")
            KirModality.OVERRIDE -> throw IllegalStateException("OVERRIDE modality is not supported for classes")
            KirModality.SEALED -> print("sealed ")
            KirModality.ABSTRACT -> print("abstract ")
            KirModality.UNSPECIFIED -> {}
        }
    }

    private fun KirCallable.printDeclaration() {
        printVisibility()
        printName()
        printArguments()
        if ((body ?: emptyBodyStub).statements.isNotEmpty()) {
            println(" {")
            withIndent {
                body.print()
            }
            println("}")
        }
    }

    private fun KirCallable.printName() {
        when (this) {
            is KirConstructor -> print("constructor")
            is KirFunction -> print("fun $name")
        }
    }

    private fun KirCallable.printArguments() {
        when (this) {
            is KirConstructor -> print("() : this(${delegationCallArguments.statements.joinToString(", ")})")
            is KirFunction -> print("()")
        }
    }

    private fun KirFunctionBody?.print() = (this ?: emptyBodyStub)
        .statements
        .flatMap { it.split("\n") }
        .forEach {
            println(it)
        }
}
