// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ex

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInsight.daemon.InspectionProfileConvertor
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.SeveritiesProvider
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar
import com.intellij.codeInspection.InspectionsBundle
import com.intellij.configurationStore.BundledSchemeEP
import com.intellij.configurationStore.SchemeDataHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.SchemeManagerFactory
import com.intellij.openapi.project.processOpenedProjects
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.AtomicNotNullLazyValue
import com.intellij.openapi.util.IconLoader
import com.intellij.profile.codeInspection.*
import com.intellij.serviceContainer.NonInjectable
import org.jdom.Element
import org.jdom.JDOMException
import org.jetbrains.annotations.TestOnly
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.function.Function

@State(name = "InspectionProfileManager", storages = [Storage("editor.xml")], additionalExportFile = InspectionProfileManager.INSPECTION_DIR)
open class ApplicationInspectionProfileManager @TestOnly @NonInjectable constructor(schemeManagerFactory: SchemeManagerFactory) : BaseInspectionProfileManager(ApplicationManager.getApplication().messageBus),
                                                                                                                                  InspectionProfileManager,
                                                                                                                                  PersistentStateComponent<Element> {
  override val schemeManager = schemeManagerFactory.create(InspectionProfileManager.INSPECTION_DIR, object : InspectionProfileProcessor() {
    override fun getSchemeKey(attributeProvider: Function<String, String?>, fileNameWithoutExtension: String) = fileNameWithoutExtension

    override fun createScheme(dataHolder: SchemeDataHolder<InspectionProfileImpl>,
                              name: String,
                              attributeProvider: Function<in String, String?>,
                              isBundled: Boolean): InspectionProfileImpl {
      return InspectionProfileImpl(name, InspectionToolRegistrar.getInstance(), this@ApplicationInspectionProfileManager, dataHolder)
    }

    override fun onSchemeAdded(scheme: InspectionProfileImpl) {
      fireProfileChanged(scheme)
    }
  })

  private val profilesAreInitialized = AtomicNotNullLazyValue.createValue {
    val app = ApplicationManager.getApplication()
    if (!(app.isUnitTestMode || app.isHeadlessEnvironment)) {
      for (ep in BUNDLED_EP_NAME.iterable) {
        schemeManager.loadBundledScheme(ep.path!! + ".xml", ep)
      }
    }
    schemeManager.loadSchemes()

    if (schemeManager.isEmpty) {
      schemeManager.addScheme(InspectionProfileImpl(DEFAULT_PROFILE_NAME, InspectionToolRegistrar.getInstance(), this))
    }
  }

  @Volatile
  private var LOAD_PROFILES = !ApplicationManager.getApplication().isUnitTestMode

  open val converter: InspectionProfileConvertor
    get() = InspectionProfileConvertor(this)

  val rootProfileName: String
    get() = schemeManager.currentSchemeName ?: DEFAULT_PROFILE_NAME

  constructor() : this(SchemeManagerFactory.getInstance())

  companion object {
    private val BUNDLED_EP_NAME = ExtensionPointName<BundledSchemeEP>("com.intellij.bundledInspectionProfile")

    @JvmStatic
    fun getInstanceImpl() = service<InspectionProfileManager>() as ApplicationInspectionProfileManager

    // It should be public to be available from Upsource
    fun registerProvidedSeverities() {
      for (provider in SeveritiesProvider.EP_NAME.iterable) {
        for (t in provider.severitiesHighlightInfoTypes) {
          val highlightSeverity = t.getSeverity(null)
          SeverityRegistrar.registerStandard(t, highlightSeverity)
          val icon = when (t) {
            is HighlightInfoType.Iconable -> {
              object : IconLoader.LazyIcon() {
                override fun compute() = (t as HighlightInfoType.Iconable).icon
              }
            }
            else -> null
          }
          HighlightDisplayLevel.registerSeverity(highlightSeverity, t.attributesKey, icon)
        }
      }
    }
  }

  init {
    registerProvidedSeverities()
  }

  override fun getProfiles(): Collection<InspectionProfileImpl> {
    initProfiles()
    return Collections.unmodifiableList(schemeManager.allSchemes)
  }

  @TestOnly
  fun forceInitProfiles(flag: Boolean) {
    LOAD_PROFILES = flag
    profilesAreInitialized.value
  }

  fun initProfiles() {
    if (LOAD_PROFILES) {
      profilesAreInitialized.value
    }
  }

  @Throws(IOException::class, JDOMException::class)
  fun loadProfile(path: String): InspectionProfileImpl? {
    val file = Paths.get(path)
    if (Files.isRegularFile(file)) {
      try {
        return InspectionProfileLoadUtil.load(file, InspectionToolRegistrar.getInstance(), this)
      }
      catch (e: IOException) {
        throw e
      }
      catch (e: JDOMException) {
        throw e
      }
      catch (ignored: Exception) {
        ApplicationManager.getApplication().invokeLater({
          Messages.showErrorDialog(InspectionsBundle.message("inspection.error.loading.message", 0, file),
            InspectionsBundle.message("inspection.errors.occurred.dialog.title"))
        }, ModalityState.NON_MODAL)
      }
    }
    return getProfile(path, false)
  }

  override fun getState(): Element? {
    val state = Element("state")
    severityRegistrar.writeExternal(state)
    return state
  }

  override fun loadState(state: Element) {
    severityRegistrar.readExternal(state)
  }

  override fun setRootProfile(profileName: String?) {
    schemeManager.currentSchemeName = profileName
  }

  override fun getProfile(name: String, returnRootProfileIfNamedIsAbsent: Boolean): InspectionProfileImpl? {
    val found = schemeManager.findSchemeByName(name)
    if (found != null) {
      return found
    }

    // profile was deleted
    return if (returnRootProfileIfNamedIsAbsent) currentProfile else null
  }

  override fun getCurrentProfile(): InspectionProfileImpl {
    initProfiles()

    val current = schemeManager.activeScheme
    if (current != null) {
      return current
    }

    // use default as base, not random custom profile
    val result = schemeManager.findSchemeByName(DEFAULT_PROFILE_NAME)
    if (result == null) {
      val profile = InspectionProfileImpl(DEFAULT_PROFILE_NAME)
      addProfile(profile)
      return profile
    }
    return result
  }

  override fun fireProfileChanged(profile: InspectionProfileImpl) {
    processOpenedProjects { project ->
      ProjectInspectionProfileManager.getInstance(project).fireProfileChanged(profile)
    }
  }
}
