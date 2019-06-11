// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight;

import com.intellij.codeInsight.editorActions.SmartBackspaceMode;
import com.intellij.configurationStore.XmlSerializer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.DifferenceFilter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.serialization.SerializationException;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Transient;
import com.intellij.util.xmlb.annotations.XCollection;
import org.intellij.lang.annotations.MagicConstant;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@State(
  name = "CodeInsightSettings",
  storages = @Storage("editor.codeinsight.xml")
)
public class CodeInsightSettings implements PersistentStateComponent<Element>, Cloneable {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.CodeInsightSettings");
  private final List<PropertyChangeListener> myListeners = new CopyOnWriteArrayList<>();

  public static CodeInsightSettings getInstance() {
    return ServiceManager.getService(CodeInsightSettings.class);
  }

  public CodeInsightSettings() {
    Application application = ApplicationManager.getApplication();
    if (Registry.is("java.completion.argument.hints") ||
        application != null && application.isInternal() && !application.isUnitTestMode() && Registry.is("java.completion.argument.hints.internal")) {
      SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION = true;
      Registry.get("java.completion.argument.hints").setValue(false);
      Registry.get("java.completion.argument.hints.internal").setValue(false);
    }
  }

  public void addPropertyChangeListener(PropertyChangeListener listener, Disposable parentDisposable) {
    myListeners.add(listener);
    Disposer.register(parentDisposable, () -> myListeners.remove(listener));
  }

  @Override
  @Nullable
  public CodeInsightSettings clone() {
    try {
      return (CodeInsightSettings)super.clone();
    }
    catch (CloneNotSupportedException e) {
      return null;
    }
  }

  public boolean SHOW_EXTERNAL_ANNOTATIONS_INLINE = true;
  public boolean SHOW_INFERRED_ANNOTATIONS_INLINE;


  public boolean SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION;
  public boolean AUTO_POPUP_PARAMETER_INFO = true;
  public int PARAMETER_INFO_DELAY = 1000;
  public boolean AUTO_POPUP_JAVADOC_INFO;
  public int JAVADOC_INFO_DELAY = 1000;
  public boolean AUTO_POPUP_COMPLETION_LOOKUP = true;

  @MagicConstant(intValues = {ALL, NONE, FIRST_LETTER})
  public int COMPLETION_CASE_SENSITIVE = FIRST_LETTER;
  public static final int ALL = 1;
  public static final int NONE = 2;
  public static final int FIRST_LETTER = 3;

  /**
   * @deprecated use accessors instead
   */
  public boolean SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS;

  public boolean isSelectAutopopupSuggestionsByChars() {
    return SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS;
  }

  public void setSelectAutopopupSuggestionsByChars(boolean value) {
    boolean oldValue = SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS;
    if (oldValue != value) {
      SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS = value;
      for (PropertyChangeListener listener : myListeners) {
        listener.propertyChange(new PropertyChangeEvent(this, "SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS", oldValue, value));
      }
    }
  }

  public boolean AUTOCOMPLETE_ON_CODE_COMPLETION = true;
  public boolean AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = true;

  @Deprecated
  public boolean AUTOCOMPLETE_COMMON_PREFIX = true;

  public boolean SHOW_FULL_SIGNATURES_IN_PARAMETER_INFO;

  @OptionTag("SMART_BACKSPACE") // explicit name makes it work also for obfuscated private field's name
  private int SMART_BACKSPACE = SmartBackspaceMode.AUTOINDENT.ordinal();

  @Transient
  @NotNull
  public SmartBackspaceMode getBackspaceMode() {
    SmartBackspaceMode[] values = SmartBackspaceMode.values();
    return SMART_BACKSPACE >= 0 && SMART_BACKSPACE < values.length ? values[SMART_BACKSPACE] : SmartBackspaceMode.OFF;
  }

  @Transient
  public void setBackspaceMode(@NotNull SmartBackspaceMode mode) {
    SMART_BACKSPACE = mode.ordinal();
  }

  public boolean SMART_INDENT_ON_ENTER = true;
  public boolean INSERT_BRACE_ON_ENTER = true;
  public boolean INSERT_SCRIPTLET_END_ON_ENTER = true;
  public boolean JAVADOC_STUB_ON_ENTER = true;
  public boolean SMART_END_ACTION = true;
  public boolean JAVADOC_GENERATE_CLOSING_TAG = true;

  public boolean SURROUND_SELECTION_ON_QUOTE_TYPED = true;

  public boolean AUTOINSERT_PAIR_BRACKET = true;
  public boolean AUTOINSERT_PAIR_QUOTE = true;
  public boolean REFORMAT_BLOCK_ON_RBRACE = true;

  @MagicConstant(intValues = {NO_REFORMAT, INDENT_BLOCK, INDENT_EACH_LINE, REFORMAT_BLOCK})
  public int REFORMAT_ON_PASTE = INDENT_EACH_LINE;
  public static final int NO_REFORMAT = 1;
  public static final int INDENT_BLOCK = 2;
  public static final int INDENT_EACH_LINE = 3;
  public static final int REFORMAT_BLOCK = 4;

  public boolean INDENT_TO_CARET_ON_PASTE;

  @MagicConstant(intValues = {YES, NO, ASK})
  public int ADD_IMPORTS_ON_PASTE = YES;
  public static final int YES = 1;
  public static final int NO = 2;
  public static final int ASK = 3;

  public boolean HIGHLIGHT_BRACES = true;
  public boolean HIGHLIGHT_SCOPE;

  public boolean USE_INSTANCEOF_ON_EQUALS_PARAMETER;
  public boolean USE_ACCESSORS_IN_EQUALS_HASHCODE;

  public boolean HIGHLIGHT_IDENTIFIER_UNDER_CARET = true;

  /**
   * @deprecated use {@link CodeInsightWorkspaceSettings#optimizeImportsOnTheFly}
   */
  @SuppressWarnings("MissingDeprecatedAnnotation") public boolean OPTIMIZE_IMPORTS_ON_THE_FLY;

  public boolean ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY;
  public boolean ADD_MEMBER_IMPORTS_ON_THE_FLY = true;
  public boolean JSP_ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY;

  public boolean TAB_EXITS_BRACKETS_AND_QUOTES = true;

  /**
   * Names of classes and packages excluded from (Java) auto-import and completion. These are only IDE-specific settings
   * and don't take project-specific settings into account.
   * So please don't reference this field directly, use JavaProjectCodeInsightSettings instead.
   */
  @Property(surroundWithTag = false)
  @XCollection(elementName = "EXCLUDED_PACKAGE", valueAttributeName = "NAME")
  @NotNull
  public String[] EXCLUDED_PACKAGES = ArrayUtilRt.EMPTY_STRING_ARRAY;

  @Override
  public void loadState(@NotNull Element state) {
    // 'Write' save only diff from default. Before load do reset to default values.
    setDefaults();

    try {
      XmlSerializer.deserializeInto(state, this);
    }
    catch (SerializationException e) {
      LOG.info(e);
    }
  }

  private void setDefaults() {
    try {
      ReflectionUtil.copyFields(CodeInsightSettings.class.getDeclaredFields(), new CodeInsightSettings(), this,
                                new DifferenceFilter<Object>(null, null) {
                                  @Override
                                  public boolean isAccept(@NotNull Field field) {
                                    return !field.getName().equals("EXCLUDED_PACKAGES");
                                  }
                                });
    }
    catch (Throwable e) {
      LOG.info(e);
    }

    EXCLUDED_PACKAGES = ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  @Override
  public Element getState() {
    Element element = new Element("state");
    writeExternal(element);
    return element;
  }

  public void writeExternal(@NotNull Element element) {
    try {
      XmlSerializer.serializeObjectInto(this, element);
    }
    catch (SerializationException e) {
      LOG.info(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return ReflectionUtil.comparePublicNonFinalFields(this, o);
  }
}