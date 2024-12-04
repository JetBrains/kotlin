// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.arguments

import org.jdom.Element
import org.jdom.Text
import org.jetbrains.kotlin.cli.common.arguments.ArgumentParseErrors
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.InternalArgument
import org.jetbrains.kotlin.cli.common.arguments.LanguageSettingsParser
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import kotlin.reflect.KMutableProperty1

interface CompilerArgumentsDeserializer<T : CommonToolArguments> {
    val compilerArguments: T
    fun deserializeFrom(element: Element)
}

class CompilerArgumentsDeserializerV5<T : CommonToolArguments>(override val compilerArguments: T) : CompilerArgumentsDeserializer<T> {
    override fun deserializeFrom(element: Element) {
        val flagArgumentsByName = readFlagArguments(element)
        val flagArgumentsPropertiesMap = CompilerArgumentsContentProspector.getFlagCompilerArgumentProperties(compilerArguments::class)
            .associateBy { it.name }
        flagArgumentsByName.forEach { (name, value) ->
            val mutableProp = flagArgumentsPropertiesMap[name].safeAs<KMutableProperty1<T, Boolean>>() ?: return@forEach
            mutableProp.set(compilerArguments, value)
        }

        val stringArgumentsByName = readStringArguments(element)
        val stringArgumentPropertiesMap = CompilerArgumentsContentProspector.getStringCompilerArgumentProperties(compilerArguments::class)
            .associateBy { it.name }
        stringArgumentsByName.forEach { (name, arg) ->
            val mutableProp = stringArgumentPropertiesMap[name].safeAs<KMutableProperty1<T, String?>>() ?: return@forEach
            mutableProp.set(compilerArguments, arg)
        }
        val arrayArgumentsByName = readArrayArguments(element)
        val arrayArgumentPropertiesMap = CompilerArgumentsContentProspector.getArrayCompilerArgumentProperties(compilerArguments::class)
            .associateBy { it.name }
        arrayArgumentsByName.forEach { (name, arr) ->
            val mutableProp = arrayArgumentPropertiesMap[name].safeAs<KMutableProperty1<T, Array<String>?>>() ?: return@forEach
            mutableProp.set(compilerArguments, arr)
        }
        val freeArgs = readElementsList(element, FREE_ARGS_ROOT_ELEMENTS_NAME, FREE_ARGS_ELEMENT_NAME)
        CompilerArgumentsContentProspector.freeArgsProperty.also {
            val mutableProp = it.safeAs<KMutableProperty1<T, List<String>>>() ?: return@also
            mutableProp.set(compilerArguments, freeArgs)
        }

        val internalArguments = readElementsList(element, INTERNAL_ARGS_ROOT_ELEMENTS_NAME, INTERNAL_ARGS_ELEMENT_NAME)
            .mapNotNull { parseInternalArgument(it) }
        CompilerArgumentsContentProspector.internalArgumentsProperty.safeAs<KMutableProperty1<T, List<InternalArgument>>>()
            ?.set(compilerArguments, internalArguments)
    }

    companion object {
        private fun readElementConfigurable(element: Element, rootElementName: String, configurable: Element.() -> Unit) {
            element.getChild(rootElementName)?.apply { configurable(this) }
        }

        private fun readStringArguments(element: Element): Map<String, String> = mutableMapOf<String, String>().also {
            readElementConfigurable(element, STRING_ROOT_ELEMENTS_NAME) {
                getChildren(STRING_ELEMENT_NAME).forEach { child ->
                    val name = child.getAttribute(NAME_ATTR_NAME)?.value ?: return@forEach
                    val arg = if (name == "classpath")
                        readElementsList(child, ARGS_ATTR_NAME, ARG_ATTR_NAME).joinToString(File.pathSeparator)
                    else child.getAttribute(ARG_ATTR_NAME)?.value ?: return@forEach
                    it += name to arg
                }
            }
        }

        private fun readFlagArguments(element: Element): Map<String, Boolean> = mutableMapOf<String, Boolean>().also {
            readElementConfigurable(element, FLAG_ROOT_ELEMENTS_NAME) {
                getChildren(FLAG_ELEMENT_NAME).forEach { child ->
                    val name = child.getAttribute(NAME_ATTR_NAME)?.value ?: return@forEach
                    val arg = child.getAttribute(ARG_ATTR_NAME)?.booleanValue ?: return@forEach
                    it += name to arg
                }
            }
        }

        private fun readElementsList(element: Element, rootElementName: String, elementName: String): List<String> =
            mutableListOf<String>().also { list ->
                readElementConfigurable(element, rootElementName) {
                    val items = getChildren(elementName)
                    if (items.isNotEmpty()) {
                        items.mapNotNullTo(list) { (it.content.firstOrNull() as? Text)?.textTrim }
                    } else {
                        list += listOfNotNull((content.firstOrNull() as? Text)?.textTrim)
                    }
                }
            }

        private fun readArrayArguments(element: Element): Map<String, Array<String>> = mutableMapOf<String, Array<String>>().apply {
            readElementConfigurable(element, ARRAY_ROOT_ELEMENTS_NAME) {
                getChildren(ARRAY_ELEMENT_NAME).forEach { child ->
                    val name = child.getAttribute(NAME_ATTR_NAME)?.value ?: return@forEach
                    val array = readElementsList(child, ARGS_ATTR_NAME, ARG_ATTR_NAME).toTypedArray()
                    this@apply += name to array
                }
            }
        }

        private fun parseInternalArgument(argument: String): InternalArgument? {
            val parser = LanguageSettingsParser()
            return parser.parseInternalArgument(argument, ArgumentParseErrors())
        }
    }
}