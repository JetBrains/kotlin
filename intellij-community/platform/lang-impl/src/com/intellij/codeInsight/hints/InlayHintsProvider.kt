// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.lang.LanguageExtension
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.psi.PsiFile
import com.intellij.util.xmlb.annotations.Property
import javax.swing.JComponent

object InlayHintsProviderExtension : LanguageExtension<InlayHintsProvider<*>>("com.intellij.codeInsight.inlayProvider")

/**
 * Provider of inlay hints for single language. If you need to create hints for multiple languages, please use InlayHintsProviderFactory.
 * Both vertical and horizontal hints collection are supported.
 * @param T settings type of this provider, if no settings required, please, use [NoSettings]
 */
interface InlayHintsProvider<T : Any> {
  /**
   * If this method is called, provider is enabled for this file
   * Warning! Your collector should not use any settings besides [settings]
   */
  fun getCollectorFor(file: PsiFile, editor: Editor, settings: T, sink: InlayHintsSink): InlayHintsCollector<T>?

  /**
   * Settings must be plain java object, fields of this settings will be copied via serialization.
   * Must implement equals method, otherwise settings won't be able to track modification.
   * Returned object will be used to create configurable and collector.
   * It persists automatically.
   */
  fun createSettings(): T

  /**
   * Name of this kind of hints. It will be used in settings and in context menu.
   */
  val name: String

  /**
   * Used for persistance of settings
   */
  val key: SettingsKey<T>

  /**
   * Text, that will be used in the settings as a preview
   */
  val previewText: String?

  /**
   * Creates configurable, that immediately applies changes from UI to [settings]
   */
  fun createConfigurable(settings: T): ImmediateConfigurable

  val isVisibleInSettings: Boolean
    get() = true
}

/**
 * The same as [UnnamedConfigurable], but not waiting for apply() to save settings.
 */
interface ImmediateConfigurable {
  /**
   * Creates component, which listen to its components and immediately updates state of settings object
   * This is required to make preview in settings works instantly
   */
  fun createComponent(listener: ChangeListener): JComponent
}

interface ChangeListener {
  /**
   * This method should be called on any change of corresponding settings
   */
  fun settingsChanged()
}

/**
 * This class should be used if provider should not have settings. If you use e. g. [Unit] you will have annoying warning in logs.
 */
@Property(assertIfNoBindings = false)
class NoSettings {
  override fun equals(other: Any?): Boolean = other is NoSettings

  override fun hashCode(): Int {
    return javaClass.hashCode()
  }
}