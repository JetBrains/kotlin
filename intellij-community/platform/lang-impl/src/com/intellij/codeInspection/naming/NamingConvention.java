// Copyright 2000-2017 JetBrains s.r.o.
// Use of this source code is governed by the Apache 2.0 license that can be
// found in the LICENSE file.
package com.intellij.codeInspection.naming;

import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.psi.PsiNameIdentifierOwner;

public abstract class NamingConvention<T extends PsiNameIdentifierOwner> {
  /**
   * @return true if member can be processed by this convention. The first convention which returns {@code true}, wins.
   */
  public abstract boolean isApplicable(T member);

  /**
   * @return Text presentation which will be shown in check box UI
   */
  public abstract String getElementDescription();

  /**
   * @return unique short name;
   *         if tool was already present and merging of settings is required ({@link AbstractNamingConventionMerger}),
   *         short name should be equal to the old tool name
   */
  public abstract String getShortName();

  /**
   * @return default settings for the convention
   */
  public abstract NamingConventionBean createDefaultBean();

  /**
   * @return true, if newly created inspection should contain this convention on.
   *         false, if convention should be disabled for newly created inspection or if inspection is merged from inspections, disabled by default.
   */
  public boolean isEnabledByDefault() {
    return false;
  }

  public String createErrorMessage(String name, NamingConventionBean bean) {
    final int length = name.length();
    if (length < bean.m_minLength) {
      return InspectionsBundle.message("naming.convention.problem.descriptor.short", getElementDescription(),
                                             Integer.valueOf(length), Integer.valueOf(bean.m_minLength));
    }
    else if (bean.m_maxLength > 0 && length > bean.m_maxLength) {
      return InspectionsBundle.message("naming.convention.problem.descriptor.long", getElementDescription(),
                                             Integer.valueOf(length), Integer.valueOf(bean.m_maxLength));
    }
    return InspectionsBundle.message("naming.convention.problem.descriptor.regex.mismatch", getElementDescription(), bean.m_regex);
  }

  public boolean isValid(T member, NamingConventionBean bean) {
    String name = member.getName();
    return name != null && bean.isValid(name);
  }
}
