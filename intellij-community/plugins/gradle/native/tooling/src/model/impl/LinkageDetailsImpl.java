// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.DefaultExternalTask;
import org.jetbrains.plugins.gradle.model.ExternalTask;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.LinkageDetails;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class LinkageDetailsImpl implements LinkageDetails {
  private ExternalTask myLinkTask;
  private File myOutputLocation;
  @NotNull
  private List<String> myAdditionalArgs;

  public LinkageDetailsImpl() {
    myAdditionalArgs = Collections.emptyList();
  }

  public LinkageDetailsImpl(LinkageDetails details) {
    myLinkTask = new DefaultExternalTask(details.getLinkTask());
    myOutputLocation = details.getOutputLocation();
    myAdditionalArgs = new ArrayList<String>(details.getAdditionalArgs());
  }

  @Override
  public ExternalTask getLinkTask() {
    return myLinkTask;
  }

  public void setLinkTask(ExternalTask linkTask) {
    myLinkTask = linkTask;
  }

  @Override
  public File getOutputLocation() {
    return myOutputLocation;
  }

  public void setOutputLocation(File outputLocation) {
    myOutputLocation = outputLocation;
  }

  @NotNull
  @Override
  public List<String> getAdditionalArgs() {
    return myAdditionalArgs;
  }

  public void setAdditionalArgs(@NotNull List<String> additionalArgs) {
    myAdditionalArgs = additionalArgs;
  }
}
