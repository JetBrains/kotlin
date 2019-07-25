/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.facet.impl;

import com.intellij.facet.impl.invalid.InvalidFacet;
import com.intellij.facet.impl.invalid.InvalidFacetManager;
import com.intellij.openapi.module.ConfigurationErrorDescription;
import com.intellij.openapi.module.ConfigurationErrorType;
import com.intellij.openapi.project.ProjectBundle;

/**
 * @author nik
 */
public class FacetLoadingErrorDescription extends ConfigurationErrorDescription {
  private static final ConfigurationErrorType INVALID_FACET = new ConfigurationErrorType(ProjectBundle.message("element.kind.name.facet"), true);
  private final InvalidFacet myFacet;

  public FacetLoadingErrorDescription(final InvalidFacet facet) {
    super(facet.getName() + " (" + facet.getModule().getName() + ")", facet.getErrorMessage(), INVALID_FACET);
    myFacet = facet;
  }

  @Override
  public String getIgnoreConfirmationMessage() {
    return ProjectBundle.message("confirmation.message.would.you.like.to.ignore.facet", myFacet.getName(), myFacet.getModule().getName());
  }

  @Override
  public void ignoreInvalidElement() {
    InvalidFacetManager.getInstance(myFacet.getModule().getProject()).setIgnored(myFacet, true);
  }

  @Override
  public boolean isValid() {
    return !myFacet.isDisposed();
  }
}
