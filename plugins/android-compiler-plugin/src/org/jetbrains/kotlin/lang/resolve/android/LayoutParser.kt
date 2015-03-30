/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.lang.resolve.android

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

public class LayoutParser(val resourceManager: AndroidResourceManager,
                          val psiManager: PsiManager,
                          val getXmlElements: (PsiFile) -> List<LayoutParser.UIXmlElement>) {

    protected val LOG: Logger = Logger.getInstance(javaClass)

    public data class UIXmlElement(val id: String?, val clazz: String, val layout: String?)

    private val WIDGET_CONTAINER: Key<WidgetContainer> = Key.create<WidgetContainer>("WIDGET_CONTAINER")

    private trait UIElement {
        val id: String?
        val parent: WidgetContainer?
        fun updateId(newId: String): UIElement
    }

    private class WidgetContainer(override val id: String?, val layout: String, override val parent: WidgetContainer?) : UIElement {
        override fun updateId(newId: String): UIElement {
            return WidgetContainer(newId, layout, parent)
        }
    }

    private class Widget(override val id: String?, val clazz: String, override val parent: WidgetContainer?) : UIElement {
        override fun updateId(newId: String): UIElement {
            return Widget(newId, clazz, parent)
        }
    }

    private class UISpecialElement(val name: String) {
        public companion object {
            val ELEMENT_MERGE = "merge"
            val ELEMENT_REQUESTFOCUS = "requestFocus"
            val ELEMENT_FRAGMENT = "fragment"
            val REGEX_SPECIAL_ELEMENTS = "$ELEMENT_MERGE|$ELEMENT_REQUESTFOCUS|$ELEMENT_FRAGMENT"
            public fun isSpecialElement(name: String): Boolean {
                return name.matches(REGEX_SPECIAL_ELEMENTS)
            }

            val MERGE = UISpecialElement(ELEMENT_MERGE)
        }
    }

    private fun loadLayout(layoutFile: PsiFile, widgetContainer: WidgetContainer?): Pair<List<UIElement>, List<UISpecialElement>> {
        val xmlElements = getXmlElements(layoutFile)
        val specialElements = xmlElements.filter { UISpecialElement.isSpecialElement(it.clazz) }.map { UISpecialElement(it.clazz) }
        val elements = xmlElements.filter { !UISpecialElement.isSpecialElement(it.clazz) }.map {
            if (it.layout != null) {
                WidgetContainer(it.id, it.layout, widgetContainer)
            }
            else {
                Widget(it.id, it.clazz, widgetContainer)
            }
        }
        return Pair(elements, specialElements)
    }

    private fun updateUIElements(parentId: String?, elements: List<UIElement>): List<UIElement> {
        if (parentId != null) {
            return elements.mapIndexed { index, elem -> if (index == 0) elem.updateId(parentId!!) else elem }
        }
        return elements
    }

    private fun getPsiFiles(widgetContainers: List<WidgetContainer>): List<PsiFile> {
        var psiFiles = arrayListOf<PsiFile>()
        for (container in widgetContainers) {
            val layoutXmlFile = resourceManager.getMainResDirectory()?.findFileByRelativePath("layout/${container.layout}.xml");
            val psiLayoutXmlFile = psiManager.findFile(layoutXmlFile)
            psiLayoutXmlFile.putUserData(WIDGET_CONTAINER, container)
            psiFiles.add(psiLayoutXmlFile)
        }
        return psiFiles
    }

    public fun parse(file: PsiFile): List<AndroidWidget> {

        fun getWidgets(layoutsFiles: List<PsiFile>, accumulator: List<Widget> = arrayListOf<Widget>()): List<Widget> {
            val widgetsContainers = arrayListOf<WidgetContainer>()
            val widgets = arrayListOf<Widget>()

            for (file in layoutsFiles) {
                val widgetsContainer = file.getUserData(WIDGET_CONTAINER)
                val parentId: String? = widgetsContainer?.id
                val (elements, specialElements) = loadLayout(file, widgetsContainer)
                val skipRootElement = specialElements.firstOrNull()?.equals(UISpecialElement.MERGE) ?: false
                val updatedElements = if (!skipRootElement) updateUIElements(parentId, elements) else elements

                widgets.addAll(updatedElements.filterIsInstance<Widget>())
                widgetsContainers.addAll(updatedElements.filterIsInstance<WidgetContainer>())
            }

            if (widgetsContainers.size() == 0) {
                return accumulator + widgets
            } else {
                val psiFiles = getPsiFiles(widgetsContainers)
                return accumulator + getWidgets(psiFiles, widgets)
            }
        }

        val allWidgets = getWidgets(arrayListOf(file))
        return  allWidgets.filter { widget -> widget.id != null }.map { widget -> AndroidWidget(widget.id!!, widget.clazz) }
    }

}