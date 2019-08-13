// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.lookup;

import com.intellij.codeInsight.CharTailType;
import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.impl.ElementLookupRenderer;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.ClassConditionKey;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class represents an item of a lookup list.
 */
public class LookupItem<T> extends MutableLookupElement<T> implements Comparable {
  public static final ClassConditionKey<LookupItem> CLASS_CONDITION_KEY = ClassConditionKey.create(LookupItem.class);

  public static final Object HIGHLIGHTED_ATTR = Key.create("highlighted");
  public static final Object ICON_ATTR = Key.create("icon");
  public static final Object TYPE_TEXT_ATTR = Key.create("typeText");
  public static final Object TAIL_TEXT_ATTR = Key.create("tailText");
  public static final Object TAIL_TEXT_SMALL_ATTR = Key.create("tailTextSmall");

  public static final Object FORCE_QUALIFY = Key.create("FORCE_QUALIFY");

  public static final Object CASE_INSENSITIVE = Key.create("CASE_INSENSITIVE");

  public static final Key<TailType> TAIL_TYPE_ATTR = Key.create("myTailType"); // one of constants defined in SimpleTailType interface

  private Object myObject;
  private String myLookupString;
  private InsertHandler myInsertHandler;
  private double myPriority;
  private Map<Object,Object> myAttributes = null;
  public static final LookupItem[] EMPTY_ARRAY = new LookupItem[0];
  private final Set<String> myAllLookupStrings = new HashSet<>();
  private String myPresentable;
  private AutoCompletionPolicy myAutoCompletionPolicy = AutoCompletionPolicy.SETTINGS_DEPENDENT;

  /**
   * @deprecated use {@link LookupElementBuilder}
   */
  @Deprecated
  public LookupItem(T o, @NotNull @NonNls String lookupString) {
    setObject(o);
    setLookupString(lookupString);
  }

  /**
   * @deprecated use {@link LookupElementBuilder}
   */
  @Deprecated
  public static LookupItem fromString(String s) {
    return new LookupItem<>(s, s);
  }

  public void setObject(@NotNull T o) {
    myObject = o;

    if (o instanceof LookupValueWithPriority) {
      setPriority(((LookupValueWithPriority)o).getPriority());
    }
  }

  public boolean equals(Object o){
    if (o == this) return true;
    if (o instanceof LookupItem){
      LookupItem item = (LookupItem)o;
      return Comparing.equal(myObject, item.myObject)
             && Comparing.equal(myLookupString, item.myLookupString)
             && Comparing.equal(myAllLookupStrings, item.myAllLookupStrings)
             && Comparing.equal(myAttributes, item.myAttributes);
    }
    return false;
  }

  public int hashCode() {
    final Object object = getObject();
    assert object != this: getClass().getName();
    return myAllLookupStrings.hashCode() * 239 + object.hashCode();
  }

  public String toString() {
    return getLookupString();
  }

  /**
   * Returns a data object.  This object is used e.g. for rendering the node.
   */
  @Override
  @NotNull
  public T getObject() {
    return (T)myObject;
  }

  /**
   * Returns a string which will be inserted to the editor when this item is
   * choosen.
   */
  @Override
  @NotNull
  public String getLookupString() {
    return myLookupString;
  }

  public void setLookupString(@NotNull String lookupString) {
    myAllLookupStrings.remove("");
    myLookupString = lookupString;
    myAllLookupStrings.add(lookupString);
  }

  public Object getAttribute(Object key){
    if (myAttributes != null){
      return myAttributes.get(key);
    }
    else{
      return null;
    }
  }

  public <T> T getAttribute(Key<T> key) {
    if (myAttributes != null){
      //noinspection unchecked
      return (T)myAttributes.get(key);
    }
    else {
      return null;
    }
  }

  public void setAttribute(Object key, Object value){
    if (value == null && myAttributes != null) {
      myAttributes.remove(key);
      return;
    }

    if (myAttributes == null){
      myAttributes = new HashMap<>(5);
    }
    myAttributes.put(key, value);
  }

  public <T> void setAttribute(Key<T> key, T value){
    if (value == null && myAttributes != null) {
      myAttributes.remove(key);
      return;
    }

    if (myAttributes == null){
      myAttributes = new HashMap<>(5);
    }
    myAttributes.put(key, value);
  }

  @Override
  public InsertHandler<? extends LookupItem> getInsertHandler(){
    return myInsertHandler;
  }

  @Override
  public boolean isBold() {
    return getAttribute(HIGHLIGHTED_ATTR) != null;
  }

  @Override
  public void handleInsert(@NotNull final InsertionContext context) {
    final InsertHandler<? extends LookupElement> handler = getInsertHandler();
    if (handler != null) {
      //noinspection unchecked
      ((InsertHandler)handler).handleInsert(context, this);
    }
    if (getTailType() != TailType.UNKNOWN && myInsertHandler == null) {
      context.setAddCompletionChar(false);
      final TailType type = handleCompletionChar(context.getEditor(), this, context.getCompletionChar());
      type.processTail(context.getEditor(), context.getTailOffset());
    }
  }

  @Nullable
  public static TailType getDefaultTailType(final char completionChar) {
    switch(completionChar){
      case '.': return new CharTailType('.', false);
      case ',': return TailType.COMMA;
      case ';': return TailType.SEMICOLON;
      case '=': return TailType.EQ;
      case ' ': return TailType.SPACE;
      case ':': return TailType.CASE_COLON; //?
    }
    return null;
  }

  @NotNull
  public static TailType handleCompletionChar(@NotNull final Editor editor, @NotNull final LookupElement lookupElement, final char completionChar) {
    final TailType type = getDefaultTailType(completionChar);
    if (type != null) {
      return type;
    }

    if (lookupElement instanceof LookupItem) {
      final LookupItem<?> item = (LookupItem)lookupElement;
      final TailType attr = item.getAttribute(TAIL_TYPE_ATTR);
      if (attr != null) {
        return attr;
      }
    }
    return TailType.NONE;
  }


  @NotNull
  public TailType getTailType(){
    final TailType tailType = getAttribute(TAIL_TYPE_ATTR);
    return tailType != null ? tailType : TailType.UNKNOWN;
  }

  @Override
  @NotNull
  public LookupItem<T> setTailType(@NotNull TailType type) {
    setAttribute(TAIL_TYPE_ATTR, type);
    return this;
  }

  @Override
  public int compareTo(@NotNull Object o){
    if(o instanceof String){
      return getLookupString().compareTo((String)o);
    }
    if(!(o instanceof LookupItem)){
      throw new RuntimeException("Trying to compare LookupItem with " + o.getClass() + "!!!");
    }
    return getLookupString().compareTo(((LookupItem)o).getLookupString());
  }

  @Override
  public LookupItem<T> setInsertHandler(@NotNull final InsertHandler<? extends LookupElement> handler) {
    myInsertHandler = handler;
    return this;
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    for (final ElementLookupRenderer renderer : ElementLookupRenderer.EP_NAME.getExtensionList()) {
      if (renderer.handlesItem(getObject())) {
        renderer.renderElement(this, getObject(), presentation);
        return;
      }
    }
    DefaultLookupItemRenderer.INSTANCE.renderElement(this, presentation);
  }

  @Override
  public LookupItem<T> setBold() {
    setAttribute(HIGHLIGHTED_ATTR, "");
    return this;
  }

  public LookupItem<T> forceQualify() {
    setAttribute(FORCE_QUALIFY, "");
    return this;
  }

  @Override
  public LookupItem<T> setAutoCompletionPolicy(final AutoCompletionPolicy policy) {
    myAutoCompletionPolicy = policy;
    return this;
  }

  @Override
  public AutoCompletionPolicy getAutoCompletionPolicy() {
    return myAutoCompletionPolicy;
  }

  @Override
  @NotNull
  public LookupItem<T> setIcon(Icon icon) {
    setAttribute(ICON_ATTR, icon);
    return this;
  }

  @Override
  @NotNull
  public LookupItem<T> setPriority(double priority) {
    myPriority = priority;
    return this;
  }

  public final double getPriority() {
    return myPriority;
  }

  @Override
  @NotNull
  public LookupItem<T> setPresentableText(@NotNull final String displayText) {
    myPresentable = displayText;
    return this;
  }

  @Nullable
  public String getPresentableText() {
    return myPresentable;
  }

  @NotNull
  public MutableLookupElement<T> setTailText(final String text, final boolean grayed) {
    setAttribute(TAIL_TEXT_ATTR, text);
    setAttribute(TAIL_TEXT_SMALL_ATTR, Boolean.TRUE);
    return this;
  }

  @Override
  public LookupItem<T> addLookupStrings(@NonNls final String... additionalLookupStrings) {
    ContainerUtil.addAll(myAllLookupStrings, additionalLookupStrings);
    return this;
  }

  @Override
  public Set<String> getAllLookupStrings() {
    return myAllLookupStrings;
  }

  @Override
  public boolean isCaseSensitive() {
    return !Boolean.TRUE.equals(getAttribute(CASE_INSENSITIVE));
  }
}
