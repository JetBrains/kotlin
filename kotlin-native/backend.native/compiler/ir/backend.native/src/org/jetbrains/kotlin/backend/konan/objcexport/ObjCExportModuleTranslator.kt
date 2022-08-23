/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.sx.ModuleBuilderWithStdlib
import org.jetbrains.kotlin.backend.konan.objcexport.sx.SXClangModuleBuilder
import org.jetbrains.kotlin.backend.konan.objcexport.sx.SXObjCHeader
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile

/**
 * Populates [moduleBuilder] with declarations that are received though [eventQueue].
 */
internal open class ObjCExportModuleTranslator(
        private val moduleBuilder: SXClangModuleBuilder,
        private val objcGenerics: Boolean,
        private val problemCollector: ObjCExportProblemCollector,
        internal val mapper: ObjCExportMapper,
        val namer: ObjCExportNamer,
        val resolver: CrossModuleResolver,
        private val eventQueue: EventQueue,
        private val frameworkName: String,
) : EventProcessor {

    private val generatedClasses = mutableSetOf<ClassDescriptor>()
    private val topLevel = mutableMapOf<SourceFile, MutableList<CallableMemberDescriptor>>()
    private val extensions = mutableMapOf<ClassDescriptor, MutableList<CallableMemberDescriptor>>()

    private val processedEvents = mutableSetOf<Event>()

    private var finalized = false

    open protected val containsStdlib = false

    open override fun begin() {

    }

    override fun process(event: Event) {
        if (event in processedEvents) return
        processedEvents += event
        when (event) {
            is Event.TranslateClass -> {
                (event.declaration).ifHeaderFound {
                    if (generatedClasses.contains(event.declaration)) return
                    addTopLevelDeclaration(getTranslator().translateClass(event.declaration))
                    generatedClasses += event.declaration
                }
            }

            is Event.TranslateInterface -> {
                (event.declaration).ifHeaderFound {
                    if (generatedClasses.contains(event.declaration)) return
                    addTopLevelDeclaration(getTranslator().translateInterface(event.declaration))
                    generatedClasses += event.declaration
                }
            }

            is Event.TranslateUnexposedClass -> {
                (event.classDescriptor).ifHeaderFound {
                    val stub = getTranslator().translateUnexposedClassAsUnavailableStub(event.classDescriptor)
                    addTopLevelDeclaration(stub)
                }
            }

            is Event.TranslateUnexposedInterface -> {
                (event.classDescriptor).ifHeaderFound {
                    val stub = getTranslator().translateUnexposedInterfaceAsUnavailableStub(event.classDescriptor)
                    addTopLevelDeclaration(stub)
                }
            }

            is Event.TranslateTopLevel -> {
                topLevel.getOrPut(event.sourceFile, ::mutableListOf) += event.declaration
            }

            is Event.TranslateExtension -> {
                extensions.getOrPut(event.classDescriptor, ::mutableListOf) += event.extension
            }

            is Event.TranslateClassForwardDeclaration -> {
                (event.classDescriptor).ifHeaderFound {
                    addClassForwardDeclaration(getTranslator().translateClassForwardDeclaration(event.classDescriptor))
                }
            }

            is Event.TranslateInterfaceForwardDeclaration -> {
                event.classDescriptor.ifHeaderFound {
                    addProtocolForwardDeclaration(getTranslator().translateProtocolForwardDeclaration(event.classDescriptor))
                }
            }
        }
    }

    override fun finalize() {
        if (finalized) return
        finalized = true
        topLevel.forEach { (sourceFile, topLevels) ->
            topLevels.first().ifHeaderFound {
                addTopLevelDeclaration(getTranslator().translateFile(sourceFile, topLevels))
            }
        }
        extensions.forEach { (klass, extensions) ->
            klass.ifHeaderFound {
                addTopLevelDeclaration(getTranslator().translateExtensions(klass, extensions))
            }
        }
    }

    internal fun buildInterface(): ObjCExportedInterface {
        return ObjCExportedInterface(
                generatedClasses,
                extensions,
                topLevel,
                namer,
                mapper,
                moduleBuilder.build(),
                frameworkName,
                containsStdlib,
        )
    }

    protected inline fun DeclarationDescriptor.ifHeaderFound(action: SXObjCHeader.() -> Unit) {
        moduleBuilder.findHeaderForDeclaration(this)?.action()
    }

    protected fun SXObjCHeader.getTranslator(): ObjCExportTranslatorImpl {
        return ObjCExportTranslatorImpl(
                mapper,
                namer,
                problemCollector,
                objcGenerics,
                SXHeaderImportReferenceTracker(this, resolver, mapper, eventQueue),
                eventQueue,
        )
    }
}

internal class ObjCExportStdlibTranslator(
        private val moduleBuilder: ModuleBuilderWithStdlib,
        objcGenerics: Boolean,
        problemCollector: ObjCExportProblemCollector,
        mapper: ObjCExportMapper,
        namer: ObjCExportNamer,
        resolver: CrossModuleResolver,
        eventQueue: EventQueue,
        frameworkName: String,
) : ObjCExportModuleTranslator(moduleBuilder, objcGenerics, problemCollector, mapper, namer, resolver, eventQueue, frameworkName) {

    override fun begin() {
        super.begin()
        translateBaseDeclarations()
    }

    override fun finalize() {
        super.finalize()
        buildImports()
    }

    override val containsStdlib: Boolean
        get() = true

    private fun translateBaseDeclarations() {
        val headerForStdlib = moduleBuilder.getStdlibHeader()
        headerForStdlib.getTranslator().generateBaseDeclarations().forEach {
            headerForStdlib.addTopLevelDeclaration(it)
        }
    }

    private fun buildImports() {
        val headerForStdlib = moduleBuilder.getStdlibHeader()
        foundationImports.forEach {
            headerForStdlib.addImport(it)
        }
//            getAdditionalImports().forEach {
//                headerForStdlib.addImport(it)
//            }
    }

    companion object {
        val foundationImports = listOf(
                "Foundation/NSArray.h",
                "Foundation/NSDictionary.h",
                "Foundation/NSError.h",
                "Foundation/NSObject.h",
                "Foundation/NSSet.h",
                "Foundation/NSString.h",
                "Foundation/NSValue.h"
        )
    }
}