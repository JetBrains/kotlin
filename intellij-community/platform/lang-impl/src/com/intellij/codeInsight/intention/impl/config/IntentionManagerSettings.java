// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionBean;
import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.ide.ui.search.SearchableOptionsRegistrar;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionNotApplicableException;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.containers.Interner;
import com.intellij.util.containers.WeakStringInterner;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

@State(name = "IntentionManagerSettings", storages = @Storage("intentionSettings.xml"))
public final class IntentionManagerSettings implements PersistentStateComponent<Element>, Disposable {
  private static final Logger LOG = Logger.getInstance(IntentionManagerSettings.class);
  private static final ExecutorService ourExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("Intentions Loader");

  private static final class MetaDataKey extends Pair<String, String> {
    private static final Interner<String> ourInterner = new WeakStringInterner();
    private MetaDataKey(@NotNull String[] categoryNames, @NotNull final String familyName) {
      super(StringUtil.join(categoryNames, ":"), ourInterner.intern(familyName));
    }
  }

  private final Set<String> myIgnoredActions = Collections.synchronizedSet(new LinkedHashSet<>());

  private final Map<MetaDataKey, IntentionActionMetaData> myMetaData = new LinkedHashMap<>(); // guarded by this
  @NonNls private static final String IGNORE_ACTION_TAG = "ignoreAction";
  @NonNls private static final String NAME_ATT = "name";
  private static final Pattern HTML_PATTERN = Pattern.compile("<[^<>]*>");

  public IntentionManagerSettings() {
    IntentionManager.EP_INTENTION_ACTIONS.getPoint(null).addExtensionPointListener(new ExtensionPointListener<IntentionActionBean>() {
      @Override
      public void extensionAdded(@NotNull IntentionActionBean extension, @NotNull PluginDescriptor pluginDescriptor) {
        registerMetaDataForEP(extension);
      }

      @Override
      public void extensionRemoved(@NotNull IntentionActionBean extension, @NotNull PluginDescriptor pluginDescriptor) {
        String[] categories = extension.getCategories();
        if (categories == null) return;
        String familyName = extension.getInstance().getFamilyName();
        unregisterMetaData(categories, familyName);
      }
    }, true, this);
  }

  @Override
  public void dispose() {
  }

  private void registerMetaDataForEP(IntentionActionBean extension) {
    String[] categories = extension.getCategories();
    if (categories == null) {
      return;
    }

    IntentionActionWrapper instance = new IntentionActionWrapper(extension, categories);
    String descriptionDirectoryName = extension.getDescriptionDirectoryName();
    if (descriptionDirectoryName == null) {
      descriptionDirectoryName = instance.getDescriptionDirectoryName();
    }
    try {
      registerMetaData(new IntentionActionMetaData(instance, extension.getLoaderForClass(), categories, descriptionDirectoryName));
    }
    catch (ExtensionNotApplicableException ignore) {
    }
  }

  @NotNull
  public static IntentionManagerSettings getInstance() {
    return ServiceManager.getService(IntentionManagerSettings.class);
  }

  void registerIntentionMetaData(@NotNull IntentionAction intentionAction,
                                 @NotNull String[] category,
                                 @NotNull String descriptionDirectoryName) {
    registerMetaData(new IntentionActionMetaData(intentionAction, getClassLoader(intentionAction), category, descriptionDirectoryName));
  }

  private static ClassLoader getClassLoader(@NotNull IntentionAction intentionAction) {
    return intentionAction instanceof IntentionActionWrapper
           ? ((IntentionActionWrapper)intentionAction).getImplementationClassLoader()
           : intentionAction.getClass().getClassLoader();
  }

  public boolean isShowLightBulb(@NotNull IntentionAction action) {
    return !myIgnoredActions.contains(action.getFamilyName());
  }

  @Override
  public void loadState(@NotNull Element element) {
    myIgnoredActions.clear();
    for (Element e : element.getChildren(IGNORE_ACTION_TAG)) {
      myIgnoredActions.add(e.getAttributeValue(NAME_ATT));
    }
  }

  @Override
  public Element getState() {
    Element element = new Element("state");
    for (String name : myIgnoredActions) {
      element.addContent(new Element(IGNORE_ACTION_TAG).setAttribute(NAME_ATT, name));
    }
    return element;
  }

  @NotNull
  public synchronized List<IntentionActionMetaData> getMetaData() {
    return new ArrayList<>(myMetaData.values());
  }

  public boolean isEnabled(@NotNull IntentionActionMetaData metaData) {
    return !myIgnoredActions.contains(getFamilyName(metaData));
  }

  private static String getFamilyName(@NotNull IntentionActionMetaData metaData) {
    return StringUtil.join(metaData.myCategory, "/") + "/" + metaData.getFamily();
  }

  private static String getFamilyName(@NotNull IntentionAction action) {
    return action instanceof IntentionActionWrapper ? ((IntentionActionWrapper)action).getFullFamilyName() : action.getFamilyName();
  }

  public void setEnabled(@NotNull IntentionActionMetaData metaData, boolean enabled) {
    if (enabled) {
      myIgnoredActions.remove(getFamilyName(metaData));
    }
    else {
      myIgnoredActions.add(getFamilyName(metaData));
    }
  }

  public boolean isEnabled(@NotNull IntentionAction action) {
    return !myIgnoredActions.contains(getFamilyName(action));
  }

  public void setEnabled(@NotNull IntentionAction action, boolean enabled) {
    if (enabled) {
      myIgnoredActions.remove(getFamilyName(action));
    }
    else {
      myIgnoredActions.add(getFamilyName(action));
    }
  }

  private synchronized void registerMetaData(@NotNull IntentionActionMetaData metaData) {
    MetaDataKey key = new MetaDataKey(metaData.myCategory, metaData.getFamily());
    if (!myMetaData.containsKey(key)){
      processMetaData(metaData);
    }
    myMetaData.put(key, metaData);
  }

  private static void processMetaData(@NotNull IntentionActionMetaData metaData) {
    Application app = ApplicationManager.getApplication();
    if (app.isUnitTestMode() || app.isHeadlessEnvironment()) {
      return;
    }

    ourExecutor.execute(() -> {
      if (app.isDisposed()) {
        return;
      }

      try {
        SearchableOptionsRegistrar registrar = SearchableOptionsRegistrar.getInstance();
        if (registrar == null) {
          return;
        }

        @NonNls String descriptionText = StringUtil.toLowerCase(metaData.getDescription().getText());
        descriptionText = HTML_PATTERN.matcher(descriptionText).replaceAll(" ");
        Set<String> words = registrar.getProcessedWordsWithoutStemming(descriptionText);
        words.addAll(registrar.getProcessedWords(metaData.getFamily()));
        registrar.addOptions(words, metaData.getFamily(), metaData.getFamily(), IntentionSettingsConfigurable.HELP_ID, IntentionSettingsConfigurable.DISPLAY_NAME);
      }
      catch (IOException e) {
        LOG.error(e);
      }
    });
  }

  synchronized void unregisterMetaData(@NotNull IntentionAction intentionAction) {
    for (Map.Entry<MetaDataKey, IntentionActionMetaData> entry : myMetaData.entrySet()) {
      if (entry.getValue().getAction() == intentionAction) {
        myMetaData.remove(entry.getKey());
        break;
      }
    }
  }

  private synchronized void unregisterMetaData(String[] categories, String familyName) {
    myMetaData.remove(new MetaDataKey(categories, familyName));
  }
}
