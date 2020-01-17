// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl

import com.intellij.facet.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.PluginDescriptor
import java.util.*

class FacetTypeRegistryImpl : FacetTypeRegistry() {
  private val myTypeIds: MutableMap<String, FacetTypeId<*>> = HashMap()
  private val myFacetTypes: MutableMap<FacetTypeId<*>, FacetType<*, *>> = HashMap()
  private var myExtensionsLoaded = false

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

  @Synchronized
  override fun unregisterFacetType(facetType: FacetType<*, *>) {
    val id = facetType.id
    val stringId = facetType.stringId
    LOG.assertTrue(myFacetTypes.remove(id) != null, "Facet type '$stringId' is not registered")
    myFacetTypes.remove(id)
    myTypeIds.remove(stringId)
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
    FacetType.EP_NAME.getPoint(null).addExtensionPointListener(
      object : ExtensionPointListener<FacetType<*, *>?> {
        override fun extensionAdded(extension: FacetType<*, *>, pluginDescriptor: PluginDescriptor) {
          registerFacetType(extension)
        }

        override fun extensionRemoved(extension: FacetType<*, *>,
                                      pluginDescriptor: PluginDescriptor) {
          unregisterFacetType(extension)
        }
      }, true, null)
    myExtensionsLoaded = true
  }

  companion object {
    private val LOG = Logger.getInstance(FacetTypeRegistryImpl::class.java)
    private val FACET_TYPE_COMPARATOR = Comparator { o1: FacetType<*, *>, o2: FacetType<*, *> ->
      o1.presentableName.compareTo(o2.presentableName, ignoreCase = true)
    }
  }
}