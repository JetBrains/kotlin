// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionBean;
import com.intellij.ide.ui.TopHitCache;
import com.intellij.ide.ui.search.SearchableOptionContributor;
import com.intellij.ide.ui.search.SearchableOptionProcessor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionNotApplicableException;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.Interner;
import com.intellij.util.containers.WeakInterner;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@State(name = "IntentionManagerSettings", storages = @Storage("intentionSettings.xml"))
public final class IntentionManagerSettings implements PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance(IntentionManagerSettings.class);

  private static final class MetaDataKey extends Pair<String, String> {
    private static final Interner<String> ourInterner = WeakInterner.createWeakInterner();
    private MetaDataKey(String @NotNull [] categoryNames, @NotNull final String familyName) {
      super(StringUtil.join(categoryNames, ":"), ourInterner.intern(familyName));
    }
  }

  private final Set<String> myIgnoredActions = Collections.synchronizedSet(new LinkedHashSet<>());

  private final Map<MetaDataKey, IntentionActionMetaData> myMetaData = new LinkedHashMap<>(); // guarded by this
  private final Map<IntentionActionBean, MetaDataKey> myExtensionMapping = new HashMap<>(); // guarded by this

  @NonNls private static final String IGNORE_ACTION_TAG = "ignoreAction";
  @NonNls private static final String NAME_ATT = "name";
  private static final Pattern HTML_PATTERN = Pattern.compile("<[^<>]*>");

  public IntentionManagerSettings() {
    IntentionManagerImpl.EP_INTENTION_ACTIONS.getPoint().addExtensionPointListener(new ExtensionPointListener<IntentionActionBean>() {
      @Override
      public void extensionAdded(@NotNull IntentionActionBean extension, @NotNull PluginDescriptor pluginDescriptor) {
        // on each plugin load/unload SearchableOptionsRegistrarImpl drops the cache, so, it will be recomputed later on demand - no need to pass processor here
        registerMetaDataForEp(extension, null);
        TopHitCache topHitCache = ApplicationManager.getApplication().getServiceIfCreated(TopHitCache.class);
        if (topHitCache != null) {
          topHitCache.invalidateCachedOptions(IntentionsOptionsTopHitProvider.class);
        }
      }

      @Override
      public void extensionRemoved(@NotNull IntentionActionBean extension, @NotNull PluginDescriptor pluginDescriptor) {
        String[] categories = extension.getCategories();
        if (categories == null) return;
        unregisterMetaDataForEP(extension);
        TopHitCache topHitCache = ApplicationManager.getApplication().getServiceIfCreated(TopHitCache.class);
        if (topHitCache != null) {
          topHitCache.invalidateCachedOptions(IntentionsOptionsTopHitProvider.class);
        }
      }
    }, false, ApplicationManager.getApplication());
  }

  private void registerMetaDataForEp(@NotNull IntentionActionBean extension, @Nullable SearchableOptionProcessor processor) {
    String[] categories = extension.getCategories();
    if (categories == null) {
      return;
    }

    IntentionActionWrapper instance = new IntentionActionWrapper(extension);
    String descriptionDirectoryName = extension.getDescriptionDirectoryName();
    if (descriptionDirectoryName == null) {
      descriptionDirectoryName = instance.getDescriptionDirectoryName();
    }

    try {
      IntentionActionMetaData metaData = new IntentionActionMetaData(instance, extension.getLoaderForClass(), categories, descriptionDirectoryName);
      MetaDataKey key = new MetaDataKey(metaData.myCategory, metaData.getFamily());
      if (processor != null) {
        processMetaData(metaData, processor);
      }
      //noinspection SynchronizeOnThis
      synchronized (this) {
        myMetaData.put(key, metaData);
        myExtensionMapping.put(extension, key);
      }
    }
    catch (ExtensionNotApplicableException ignore) {
    }
  }

  @NotNull
  public static IntentionManagerSettings getInstance() {
    return ServiceManager.getService(IntentionManagerSettings.class);
  }

  void registerIntentionMetaData(@NotNull IntentionAction intentionAction,
                                 String @NotNull [] category,
                                 @NotNull String descriptionDirectoryName) {
    IntentionActionMetaData metaData = new IntentionActionMetaData(intentionAction, getClassLoader(intentionAction), category, descriptionDirectoryName);
    MetaDataKey key = new MetaDataKey(metaData.myCategory, metaData.getFamily());
    synchronized (this) {
      // not added as searchable option - this method is deprecated and intentionAction extension point must be used instead
      myMetaData.put(key, metaData);
    }
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
    return StringUtil.join(metaData.myCategory, "/") + "/" + metaData.getAction().getFamilyName();
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

  private static void processMetaData(@NotNull IntentionActionMetaData metaData, @NotNull SearchableOptionProcessor processor) {
    try {
      String descriptionText = StringUtil.toLowerCase(metaData.getDescription().getText());
      descriptionText = HTML_PATTERN.matcher(descriptionText).replaceAll(" ");
      String displayName = IntentionSettingsConfigurable.getDisplayNameText();
      String configurableId = IntentionSettingsConfigurable.HELP_ID;
      String family = metaData.getFamily();
      processor.addOptions(descriptionText, family, family, configurableId, displayName, false);
      processor.addOptions(family, family, family, configurableId, displayName, true);
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  synchronized void unregisterMetaData(@NotNull IntentionAction intentionAction) {
    for (Map.Entry<MetaDataKey, IntentionActionMetaData> entry : myMetaData.entrySet()) {
      if (entry.getValue().getAction() == intentionAction) {
        myMetaData.remove(entry.getKey());
        break;
      }
    }
  }

  private synchronized void unregisterMetaDataForEP(IntentionActionBean extension) {
    MetaDataKey key = myExtensionMapping.remove(extension);
    if (key != null) {
      myMetaData.remove(key);
    }
  }

  private static final class IntentionSearchableOptionContributor extends SearchableOptionContributor {
    @Override
    public void processOptions(@NotNull SearchableOptionProcessor processor) {
      IntentionManagerSettings settings = getInstance();
      IntentionManagerImpl.EP_INTENTION_ACTIONS.forEachExtensionSafe(extension -> settings.registerMetaDataForEp(extension, processor));
    }
  }
}
