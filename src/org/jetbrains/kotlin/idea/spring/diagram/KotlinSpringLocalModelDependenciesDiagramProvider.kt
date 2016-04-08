/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.spring.diagram

import com.intellij.diagram.DiagramPresentationModel
import com.intellij.diagram.presentation.DiagramState
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.component1
import com.intellij.openapi.util.component2
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.spring.SpringManager
import com.intellij.spring.contexts.model.AbstractSimpleLocalModel
import com.intellij.spring.contexts.model.LocalModel
import com.intellij.spring.contexts.model.diagram.SpringLocalModelDependenciesDiagramProvider
import com.intellij.spring.contexts.model.diagram.SpringLocalModelDiagramNodeContentManager
import com.intellij.spring.contexts.model.diagram.SpringLocalModelsDataModel
import com.intellij.spring.contexts.model.diagram.beans.*
import com.intellij.spring.contexts.model.graph.LazyModelDependenciesGraph
import com.intellij.spring.contexts.model.graph.LocalModelDependency
import com.intellij.spring.facet.SpringFacet
import com.intellij.spring.facet.SpringFileSet
import com.intellij.spring.facet.SpringFileSetService
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

// TODO: Drop when SpringLocalModelsDataModel gets support for non-Java/non-XML files
class KotlinSpringLocalModelDependenciesDiagramProvider : SpringLocalModelDependenciesDiagramProvider() {
    inner class KotlinSpringLocalModelsDataModel(
            project: Project,
            private val model: LocalModelGraphElementWrapper<*>,
            private val showModuleFilesetNodes: Boolean
    ) : SpringLocalModelsDataModel(project, this@KotlinSpringLocalModelDependenciesDiagramProvider, model, showModuleFilesetNodes) {
        private val extraNodes = LinkedHashSet<SpringLocalModelDiagramNode>()
        private val extraEdges = LinkedHashSet<SpringLocalModelDependencyEdge>()

        @Suppress("UNCHECKED_CAST")
        private val nodesSequence: Sequence<SpringLocalModelDiagramNode>
            get() = (super.getNodes() as Collection<SpringLocalModelDiagramNode>).asSequence() + extraNodes.asSequence()

        private fun getDiagramNode(model: LocalModelGraphElementWrapper<*>): SpringLocalModelDiagramNode {
            return nodesSequence.firstOrNull { it.identifyingElement == model } ?: SpringLocalModelDiagramNode(model, this.provider)
        }

        private fun showLibraryConfigs() = DiagramState(builder).isCategoryEnabled(SpringLocalModelDiagramNodeContentManager.SHOW_LIBRARY_CONFIGS)

        private fun isLibraryConfig(element: LocalModelGraphElementWrapper<Any>): Boolean {
            if (element !is LocalModelWrapper<*>) return false
            return ProjectRootManager.getInstance(project).fileIndex.isInLibraryClasses(element.element.config.containingFile.virtualFile)
        }

        private fun visitRelated(fromNode: SpringLocalModelDiagramNode, graph: LazyModelDependenciesGraph) {
            val localModel = (fromNode.identifyingElement as? LocalModelWrapper<*>)?.element ?: return
            for ((model, dependency) in graph.getOrCreateOutDependencies(localModel)) {
                createEdgeToLocalModelAndVisitIt(fromNode, graph, LocalModelWrapper.create(model), dependency)
            }
        }

        private fun createEdgeToLocalModelAndVisitIt(
                fromNode: SpringLocalModelDiagramNode?,
                graph: LazyModelDependenciesGraph,
                toNodeLocalModelWrapper: LocalModelWrapper<LocalModel<*>>,
                dependency: LocalModelDependency
        ) {
            val toNode = getDiagramNode(toNodeLocalModelWrapper)
            if (toNode === fromNode) return

            val toNodeExists = toNode in extraNodes
            if (!toNodeExists) {
                extraNodes += toNode
                if (showLibraryConfigs() || !isLibraryConfig(toNode.identifyingElement)) {
                    this.visitRelated(toNode, graph)
                }
            }

            if (fromNode != null) {
                val edge = SpringLocalModelDependencyEdge(fromNode, toNode, dependency)
                if (toNodeExists) {
                    edge.isError = true
                }
                else {
                    fromNode.addChild(toNode)
                }
                extraEdges += edge
            }
        }

        private fun visitFileSet(node: SpringLocalModelDiagramNode?, fileSet: SpringFileSet) {
            val springManager = SpringManager.getInstance(project)
            val module = fileSet.facet.module
            val profiles = fileSet.activeProfiles
            val graph = AbstractSimpleLocalModel.getOrCreateLocalModelDependenciesGraph(module, profiles)

            for (filePointer in fileSet.files) {
                val virtualFile = filePointer.file ?: continue
                val ktFile = virtualFile.toPsiFile(project) as? KtFile ?: continue
                for (psiClass in ktFile.classes) {
                    val model = springManager.getLocalSpringModel(psiClass, module) ?: continue
                    createEdgeToLocalModelAndVisitIt(node, graph, LocalModelWrapper.create(model), LocalModelDependency.create())
                }
            }
        }

        private fun visitFileSet(fileSet: SpringFileSet) {
            visitFileSet(if (showModuleFilesetNodes) getDiagramNode(FilesetLocalModelWrapper.create(fileSet)!!) else null, fileSet)
        }

        private fun visitModule(module: Module) {
            if (module.isDisposed) return
            val springFacet = SpringFacet.getInstance(module) ?: return

            for (fileSet in SpringFileSetService.getInstance().getAllSets(springFacet)) {
                visitFileSet(fileSet)
            }
        }

        override fun updateDataModel() {
            super.updateDataModel()
            when (model) {
                is ModuleLocalModelWrapper -> visitModule(model.element)
                is FilesetLocalModelWrapper -> visitFileSet(model.element)
            }
        }

        override fun getNodes() = nodesSequence.toCollection(LinkedHashSet())
        override fun getEdges() = super.getEdges() + extraEdges
    }

    override fun getID() = "KotlinSpringLocalModels"

    override fun createDataModel(
            project: Project,
            element: LocalModelGraphElementWrapper<*>?,
            file: VirtualFile?,
            presentationModel: DiagramPresentationModel?
    ): SpringLocalModelsDataModel? {
        if (element == null) return null
        return KotlinSpringLocalModelsDataModel(project, element, true)
    }
}
