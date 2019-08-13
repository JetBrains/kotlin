// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.profile.codeInspection.ui;

import com.intellij.codeInspection.ex.Descriptor;
import com.intellij.codeInspection.ex.InspectionProfileModifiableModel;
import com.intellij.codeInspection.ex.ScopeToolState;
import com.intellij.openapi.project.Project;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Dmitry Batkovich
 */
public class ToolDescriptors {

  @NotNull
  private final Descriptor myDefaultDescriptor;
  @NotNull
  private final List<Descriptor> myNonDefaultDescriptors;

  private ToolDescriptors(final @NotNull Descriptor defaultDescriptor,
                          final @NotNull List<Descriptor> nonDefaultDescriptors) {
    myDefaultDescriptor = defaultDescriptor;
    myNonDefaultDescriptors = nonDefaultDescriptors;
  }

  public static ToolDescriptors fromScopeToolState(final ScopeToolState state,
                                                   @NotNull InspectionProfileModifiableModel profile,
                                                   final Project project) {
    List<ScopeToolState> nonDefaultTools = profile.getNonDefaultTools(state.getTool().getShortName(), project);
    ArrayList<Descriptor> descriptors = new ArrayList<>(nonDefaultTools.size());
    for (final ScopeToolState nonDefaultToolState : nonDefaultTools) {
      descriptors.add(new Descriptor(nonDefaultToolState, profile, project));
    }
    return new ToolDescriptors(new Descriptor(state, profile, project), descriptors);
  }

  @NotNull
  public Descriptor getDefaultDescriptor() {
    return myDefaultDescriptor;
  }

  @NotNull
  public List<Descriptor> getNonDefaultDescriptors() {
    return myNonDefaultDescriptors;
  }

  @NotNull
  public Stream<Descriptor> getDescriptors() {
    return StreamEx.of(getNonDefaultDescriptors()).prepend(getDefaultDescriptor());
  }

  @NotNull
  public ScopeToolState getDefaultScopeToolState() {
    return myDefaultDescriptor.getState();
  }
}
