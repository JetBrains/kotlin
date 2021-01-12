// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl

import com.intellij.facet.*
import com.intellij.facet.impl.invalid.InvalidFacetManager
import com.intellij.facet.impl.invalid.InvalidFacetType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.jetbrains.jps.model.serialization.facet.FacetState
import java.util.*

class FacetTypeRegistryImpl : FacetTypeRegistry() {
  private val myTypeIds: MutableMap<String, FacetTypeId<*>> = HashMap()
  private val myFacetTypes: MutableMap<FacetTypeId<*>, FacetType<*, *>> = HashMap()
  @Volatile private var myExtensionsLoaded = false
  private val myTypeRegistrationLock = Object()

  @Synchronized
  override fun registerFacetType(facetType: FacetType<*, *>) {
    val typeId = facetType.id
    val id = facetType.stringId
    LOG.assertTrue(!id.contains("/"), "Facet type id '$id' contains illegal character '/'")
    LOG.assertTrue(!myFacetTypes.containsKey(typeId), "Facet type '$id' is already registered")
    myFacetTypes[typeId] = facetType
    LOG.assertTrue(!myTypeIds.containsKey(id), "Facet type id '$id' is already registered")
    myTypeIds[id] = typeId
  }

  private fun loadInvalidFacetsOfType(project: Project, facetType: FacetType<*, *>) {
    val modulesWithFacets = InvalidFacetManager.getInstance(project).invalidFacets
      .filter { it.configuration.facetState.facetType == facetType.stringId }
      .mapTo(HashSet()) { it.module }
    for (module in modulesWithFacets) {
      val model = FacetManager.getInstance(module).createModifiableModel()
      val invalidFacets = model.getFacetsByType(InvalidFacetType.TYPE_ID).filter { it.configuration.facetState.facetType == facetType.stringId }
      for (invalidFacet in invalidFacets) {
        val newFacet = FacetManagerImpl.createFacetFromStateRaw(module, facetType, invalidFacet.configuration.facetState,
                                                                invalidFacet.underlyingFacet)
        model.replaceFacet(invalidFacet, newFacet)
        for (subFacet in invalidFacet.configuration.facetState.subFacets) {
          model.addFacet(FacetManagerBase.createInvalidFacet(module, subFacet, newFacet, invalidFacet.configuration.errorMessage, false, false))
        }
      }
      model.commit()
    }
  }

  @Synchronized
  override fun unregisterFacetType(facetType: FacetType<*, *>) {
    try {
      ProjectManager.getInstance().openProjects.forEach {
        convertFacetsToInvalid(it, facetType)
      }
    }
    finally {
      val id = facetType.id
      val stringId = facetType.stringId
      LOG.assertTrue(myFacetTypes.remove(id) != null, "Facet type '$stringId' is not registered")
      myFacetTypes.remove(id)
      myTypeIds.remove(stringId)
    }
  }

  private fun convertFacetsToInvalid(project: Project, facetType: FacetType<*, *>) {
    val modulesWithFacets = ProjectFacetManager.getInstance(project).getFacets(facetType.id).mapTo(HashSet()) { it.module }
    for (module in modulesWithFacets) {
      val model = FacetManager.getInstance(module).createModifiableModel()
      val subFacets = model.allFacets.groupBy { it.underlyingFacet }
      val facets = model.getFacetsByType(facetType.id)
      for (facet in facets) {
        val facetState = saveFacetWithSubFacets(facet, subFacets) ?: continue
        val pluginName = facetType.pluginDescriptor?.name?.let { " '$it'" } ?: ""
        val errorMessage = "Plugin$pluginName which provides support for '${facetType.presentableName}' facets is unloaded"
        val reportError = !ApplicationManager.getApplication().isUnitTestMode
        val invalidFacet = FacetManagerBase.createInvalidFacet(module, facetState, facet.underlyingFacet, errorMessage, true, reportError)
        removeAllSubFacets(model, facet, subFacets)
        model.replaceFacet(facet, invalidFacet)
      }
      model.commit()
    }
  }

  private fun removeAllSubFacets(model: ModifiableFacetModel, facet: Facet<*>, subFacets: Map<Facet<*>, List<Facet<*>>>) {
    val toRemove = subFacets[facet] ?: return
    for (subFacet in toRemove) {
      removeAllSubFacets(model, subFacet, subFacets)
      model.removeFacet(subFacet)
    }
  }

  private fun saveFacetWithSubFacets(facet: Facet<*>?, subFacets: Map<Facet<FacetConfiguration>, List<Facet<*>>>): FacetState? {
    val state = FacetManagerImpl.saveFacetConfiguration(facet) ?: return null
    (subFacets[facet] ?: emptyList()).mapNotNullTo(state.subFacets) { saveFacetWithSubFacets(it, subFacets) }
    return state
  }

  @Synchronized
  override fun getFacetTypeIds(): Array<FacetTypeId<*>> {
    loadExtensions()
    return myFacetTypes.keys.toTypedArray()
  }

  @Synchronized
  override fun getFacetTypes(): Array<FacetType<*, *>> {
    loadExtensions()
    val facetTypes = myFacetTypes.values.toTypedArray()
    Arrays.sort(facetTypes, FACET_TYPE_COMPARATOR)
    return facetTypes
  }

  override fun getSortedFacetTypes(): Array<FacetType<*, *>> {
    val types = facetTypes
    Arrays.sort(types, FACET_TYPE_COMPARATOR)
    return types
  }

  @Synchronized
  override fun findFacetType(id: String): FacetType<*, *>? {
    loadExtensions()
    val typeId = myTypeIds[id] ?: return null
    return myFacetTypes[typeId]
  }

  @Synchronized
  override fun <F : Facet<C>?, C : FacetConfiguration?> findFacetType(typeId: FacetTypeId<F>): FacetType<F, C> {
    loadExtensions()
    @Suppress("UNCHECKED_CAST")
    val type = myFacetTypes[typeId] as FacetType<F, C>?
    LOG.assertTrue(type != null, "Cannot find facet by id '$typeId'")
    return type!!
  }

  private fun loadExtensions() {
    if (myExtensionsLoaded) {
      return
    }

    synchronized(myTypeRegistrationLock) {
      if (myExtensionsLoaded) return

      FacetType.EP_NAME.forEachExtensionSafe {
        registerFacetType(it)
      }
      FacetType.EP_NAME.addExtensionPointListener(
        object : ExtensionPointListener<FacetType<*, *>?> {
          override fun extensionAdded(extension: FacetType<*, *>, pluginDescriptor: PluginDescriptor) {
            registerFacetType(extension)
            runWriteAction {
              ProjectManager.getInstance().openProjects.forEach {
                loadInvalidFacetsOfType(it, extension)
              }
            }
          }

          override fun extensionRemoved(extension: FacetType<*, *>,
                                        pluginDescriptor: PluginDescriptor) {
            unregisterFacetType(extension)
          }
        }, null)
      myExtensionsLoaded = true
    }
  }

  companion object {
    private val LOG = Logger.getInstance(FacetTypeRegistryImpl::class.java)
    private val FACET_TYPE_COMPARATOR = Comparator { o1: FacetType<*, *>, o2: FacetType<*, *> ->
      o1.presentableName.compareTo(o2.presentableName, ignoreCase = true)
    }
  }
}