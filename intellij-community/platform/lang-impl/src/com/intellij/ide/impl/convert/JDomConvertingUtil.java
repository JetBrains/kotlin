// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.impl.convert;

import com.intellij.conversion.CannotConvertException;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.JDomSerializationUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * @author nik
 */
public final class JDomConvertingUtil extends JDomSerializationUtil {
  private JDomConvertingUtil() {
  }

  /**
   * @deprecated Use {@link #load(Path)}
   */
  @Deprecated
  public static Document loadDocument(File file) throws CannotConvertException {
    try {
      return JDOMUtil.loadDocument(file);
    }
    catch (JDOMException | IOException e) {
      throw new CannotConvertException(file.getAbsolutePath() + ": " + e.getMessage(), e);
    }
  }

  public static Element load(Path file) throws CannotConvertException {
    try {
      return JDOMUtil.load(file);
    }
    catch (JDOMException | IOException e) {
      throw new CannotConvertException(file.toAbsolutePath() + ": " + e.getMessage(), e);
    }
  }

  public static String getOptionValue(Element element, String optionName) {
    return JDOMExternalizerUtil.readField(element, optionName);
  }

  public static Condition<Element> createAttributeValueFilter(@NonNls final String name, @NonNls final String value) {
    return createAttributeValueFilter(name, Collections.singleton(value));
  }

  public static Condition<Element> createAttributeValueFilter(@NonNls final String name, @NonNls final Collection<String> value) {
    return element -> value.contains(element.getAttributeValue(name));
  }

  public static Condition<Element> createElementWithAttributeFilter(final String elementName, final String attributeName, final String attributeValue) {
    return Conditions.and(createElementNameFilter(elementName),
                          createAttributeValueFilter(attributeName, attributeValue));
  }

  public static Condition<Element> createElementNameFilter(@NonNls final String elementName) {
    return element -> elementName.equals(element.getName());
  }

  public static List<Element> removeChildren(final Element element, final Condition<? super Element> filter) {
    List<Element> toRemove = new ArrayList<>();
    final List<Element> list = element.getChildren();
    for (Element e : list) {
      if (filter.value(e)) {
        toRemove.add(e);
      }
    }
    for (Element e : toRemove) {
      element.removeContent(e);
    }
    return toRemove;
  }

  @Nullable
  public static Element findChild(Element parent, final Condition<? super Element> filter) {
    final List<Element> list = parent.getChildren();
    for (Element e : list) {
      if (filter.value(e)) {
        return e;
      }
    }
    return null;
  }

  public static void removeDuplicatedOptions(final Element element) {
    List<Element> children = new ArrayList<>(element.getChildren(OPTION_ELEMENT));
    Set<String> names = new HashSet<>();
    for (Element child : children) {
      if (!names.add(child.getAttributeValue(NAME_ATTRIBUTE))) {
        element.removeContent(child);
      }
    }
  }
}
