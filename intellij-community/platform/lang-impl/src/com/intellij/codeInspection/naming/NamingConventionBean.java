// Copyright 2000-2017 JetBrains s.r.o.
// Use of this source code is governed by the Apache 2.0 license that can be
// found in the LICENSE file.
package com.intellij.codeInspection.naming;

import com.intellij.codeInspection.ui.ConventionOptionsPanel;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see NamingConventionWithFallbackBean for default fallbacks
 *
 * When beans have custom fields, ensure to provide {@link #equals(Object)}/{@link #hashCode()} for correct serialization
 */
public class NamingConventionBean {
  public String m_regex;
  public int m_minLength;
  public int m_maxLength;

  private final Set<String> myPredefinedNames = new HashSet<>();

  public NamingConventionBean(String regex, int minLength, int maxLength, String... predefinedNames2Ignore) {
    m_regex = regex;
    m_minLength = minLength;
    m_maxLength = maxLength;
    myPredefinedNames.addAll(Arrays.asList(predefinedNames2Ignore));
    initPattern();
  }

  protected Pattern m_regexPattern;

  public boolean isValid(String name) {
    final int length = name.length();
    if (length < m_minLength) {
      return false;
    }
    if (m_maxLength > 0 && length > m_maxLength) {
      return false;
    }
    if (myPredefinedNames.contains(name)) {
      return true;
    }
    final Matcher matcher = m_regexPattern.matcher(name);
    return matcher.matches();
  }

  public void initPattern() {
    m_regexPattern = Pattern.compile(m_regex);
  }

  public JComponent createOptionsPanel() {
    return new ConventionOptionsPanel(this, "m_minLength", "m_maxLength", "m_regex", "m_regexPattern");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NamingConventionBean bean = (NamingConventionBean)o;
    return m_minLength == bean.m_minLength &&
           m_maxLength == bean.m_maxLength &&
           Objects.equals(m_regex, bean.m_regex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_regex, m_minLength, m_maxLength);
  }
}
