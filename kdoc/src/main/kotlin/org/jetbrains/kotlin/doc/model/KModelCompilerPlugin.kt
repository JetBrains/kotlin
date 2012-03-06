package org.jetbrains.kotlin.doc.model

import kotlin.*
import kotlin.util.*

import org.jetbrains.kotlin.doc.KDocConfig
import org.jetbrains.kotlin.doc.templates.*
import org.jetbrains.kotlin.template.TextTemplate

import java.io.File
import java.util.List
import java.util.HashSet
import java.util.Collection

import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingContext.*
import org.jetbrains.jet.compiler.CompilerPlugin
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.resolve.java.JavaNamespaceDescriptor
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.jet.util.slicedmap.WritableSlice
import org.jetbrains.jet.lang.resolve.BindingContextUtils


/** Base class for any compiler plugin which needs to process a KModel */
abstract class KModelCompilerPlugin : CompilerPlugin {

    public open var config: KDocConfig = KDocConfig()

    override fun processFiles(context: BindingContext?, sources: List<JetFile?>?) {
        if (context != null && sources != null) {
            val model = KModel(context, config)
            model.load(sources)

            processModel(model)
        }
    }

    abstract fun processModel(model: KModel): Unit
}