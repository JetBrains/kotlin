// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.openapi.extensions.ExtensionPointAndAreaListener;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.util.ClearableLazyValue;
import com.intellij.util.JdomKt;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import gnu.trove.THashMap;
import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.jvm.functions.Function0;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TemplateContext {
  private final Map<String, Boolean> myContextStates = new THashMap<>();

  private static final ClearableLazyValue<Map<String, String>> INTERN_MAP = new ClearableLazyValue<Map<String, String>>() {
    private final AtomicBoolean isListenerAdded = new AtomicBoolean();

    @NotNull
    @Override
    protected Map<String, String> compute() {
      if (isListenerAdded.compareAndSet(false, true)) {
        TemplateManagerImpl.TEMPLATE_CONTEXT_EP.getValue().addExtensionPointListener(new ExtensionPointAndAreaListener<TemplateContextType>() {
          @Override
          public void areaReplaced(@NotNull ExtensionsArea oldArea) {
            drop();
          }

          @Override
          public void extensionAdded(@NotNull TemplateContextType extension, @NotNull PluginDescriptor pluginDescriptor) {
            drop();
          }

          @Override
          public void extensionRemoved(@NotNull TemplateContextType extension, @NotNull PluginDescriptor pluginDescriptor) {
            drop();
          }
        }, false, null);
      }

      return TemplateManagerImpl.getAllContextTypes().stream()
        .map(TemplateContextType::getContextId)
        .distinct()
        .collect(Collectors.toMap(Function.identity(), Function.identity()));
    }
  };

  public TemplateContext createCopy() {
    TemplateContext cloneResult = new TemplateContext();
    cloneResult.myContextStates.putAll(myContextStates);
    return cloneResult;
  }

  @Nullable
  TemplateContextType getDifference(@NotNull TemplateContext defaultContext) {
    return ContainerUtil.find(TemplateManagerImpl.getAllContextTypes(), type -> isEnabled(type) != defaultContext.isEnabled(type));
  }

  public boolean isEnabled(@NotNull TemplateContextType contextType) {
    synchronized (myContextStates) {
      Boolean storedValue = getOwnValue(contextType);
      if (storedValue == null) {
        TemplateContextType baseContextType = contextType.getBaseContextType();
        return baseContextType != null && isEnabled(baseContextType);
      }
      return storedValue.booleanValue();
    }
  }

  @Nullable
  public Boolean getOwnValue(TemplateContextType contextType) {
    synchronized (myContextStates) {
      return myContextStates.get(contextType.getContextId());
    }
  }

  public void setEnabled(TemplateContextType contextType, boolean value) {
    synchronized (myContextStates) {
      myContextStates.put(contextType.getContextId(), value);
    }
  }

  // used during initialization => no sync
  @VisibleForTesting
  public void setDefaultContext(@NotNull TemplateContext defContext) {
    Map<String, Boolean> copy = new THashMap<>(myContextStates);
    myContextStates.clear();
    myContextStates.putAll(defContext.myContextStates);
    myContextStates.putAll(copy);
  }

  // used during initialization => no sync
  @VisibleForTesting
  public void readTemplateContext(@NotNull Element element) {
    Map<String, String> internMap = INTERN_MAP.getValue();
    for (Element option : element.getChildren("option")) {
      String name = option.getAttributeValue("name");
      String value = option.getAttributeValue("value");
      if (name != null && value != null) {
        myContextStates.put(ContainerUtil.getOrElse(internMap, name, name), Boolean.parseBoolean(value));
      }
    }

    myContextStates.putAll(makeInheritanceExplicit());
  }

  /**
   * Mark contexts explicitly as excluded which are excluded because some of their bases is explicitly marked as excluded.
   * Otherwise that `excluded` status will be forgotten if the base context is enabled.
   */
  @NotNull
  private Map<String, Boolean> makeInheritanceExplicit() {
    Map<String, Boolean> explicitStates = new THashMap<>();
    for (TemplateContextType type : ContainerUtil.filter(TemplateManagerImpl.getAllContextTypes(), this::isDisabledByInheritance)) {
      explicitStates.put(type.getContextId(), false);
    }
    return explicitStates;
  }

  private boolean isDisabledByInheritance(TemplateContextType type) {
    return !hasOwnValue(type) &&
           !isEnabled(type) &&
           JBIterable.generate(type, TemplateContextType::getBaseContextType).filter(this::hasOwnValue).first() != null;
  }

  private boolean hasOwnValue(TemplateContextType t) {
    return getOwnValue(t) != null;
  }

  @TestOnly
  public Element writeTemplateContext(@Nullable TemplateContext defaultContext) {
    return writeTemplateContext(defaultContext, getIdToType());
  }

  @VisibleForTesting
  @Nullable
  public Element writeTemplateContext(@Nullable TemplateContext defaultContext, @NotNull Lazy<? extends Map<String, TemplateContextType>> idToType) {
    if (myContextStates.isEmpty()) {
      return null;
    }

    Element element = new Element(TemplateSettings.CONTEXT);
    List<Map.Entry<String, Boolean>> entries = new ArrayList<>(myContextStates.entrySet());
    entries.sort(Map.Entry.comparingByKey());
    for (Map.Entry<String, Boolean> entry : entries) {
      Boolean ownValue = entry.getValue();
      if (ownValue == null) {
        continue;
      }

      TemplateContextType type = idToType.getValue().get(entry.getKey());
      if (type == null) {
        // https://youtrack.jetbrains.com/issue/IDEA-155623#comment=27-1721029
        JdomKt.addOptionTag(element, entry.getKey(), ownValue.toString());
      }
      else if (isValueChanged(ownValue, type, defaultContext)) {
        JdomKt.addOptionTag(element, type.getContextId(), ownValue.toString());
      }
    }
    return element;
  }

  @NotNull
  public static Lazy<Map<String, TemplateContextType>> getIdToType() {
    return LazyKt.lazy(new Function0<Map<String, TemplateContextType>>() {
      @Override
      public Map<String, TemplateContextType> invoke() {
        Map<String, TemplateContextType> idToType = new THashMap<>();
        for (TemplateContextType type : TemplateManagerImpl.getAllContextTypes()) {
          idToType.put(type.getContextId(), type);
        }
        return idToType;
      }
    });
  }

  /**
   * Default value for GROOVY_STATEMENT is `true` (defined in the `plugins/groovy/groovy-psi/resources/liveTemplates/Groovy.xml`).
   * Base value is `false`.
   * <p>
   * If default value is defined (as in our example)  we must not take base value in account.
   * Because on init `setDefaultContext` will be called and we will have own value.
   * Otherwise it will be not possible to set value for `GROOVY_STATEMENT` neither to `true` (equals to default), nor to `false` (equals to base).
   * See TemplateSchemeTest.
   */
  private boolean isValueChanged(@NotNull Boolean ownValue, @NotNull TemplateContextType type, @Nullable TemplateContext defaultContext) {
    Boolean defaultValue = defaultContext == null ? null : defaultContext.getOwnValue(type);
    if (defaultValue == null) {
      TemplateContextType base = type.getBaseContextType();
      boolean baseEnabled = base != null && isEnabled(base);
      return ownValue != baseEnabled;
    }
    return !ownValue.equals(defaultValue);
  }

  @Override
  public String toString() {
    return myContextStates.toString();
  }
}
