// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultNewRunConfigurationTreePopupFactory extends NewRunConfigurationTreePopupFactory {
  private NodeDescriptor<?> root;
  private NodeDescriptor<?> other;
  private List<ConfigurationType> myTypesToShow;
  private List<ConfigurationType> myOtherTypes;

  @Override
  public void initStructure(@NotNull Project project) {
    root = createDescriptor(project, "<invisible-templates-root>", null);
    other = createDescriptor(project, ExecutionBundle.message("add.new.run.configuration.other.types.node.text"), root,
                             NodeDescriptor.DEFAULT_WEIGHT + 1);
    myTypesToShow = new ArrayList<>(
      RunConfigurable.Companion.configurationTypeSorted(project, true,
                                                        ConfigurationType.CONFIGURATION_TYPE_EP.getExtensionList()));
    myOtherTypes = new ArrayList<>(ConfigurationType.CONFIGURATION_TYPE_EP.getExtensionList());
    Collections.sort(myOtherTypes, (o1, o2) -> RunConfigurationListManagerHelperKt.compareTypesForUi(o1, o2));
    myOtherTypes.removeAll(myTypesToShow);
  }

  @NotNull
  @Override
  public NodeDescriptor<?> getRootElement() {
    return root;
  }

  @Override
  public NodeDescriptor<?>[] createChildElements(@NotNull Project project, @NotNull NodeDescriptor nodeDescriptor) {
    Object nodeDescriptorElement = nodeDescriptor.getElement();
    if (root.equals(nodeDescriptor)) {
      return ArrayUtil.append(convertToDescriptors(project, nodeDescriptor, myTypesToShow.toArray()), other);
    }
    else if (other.equals(nodeDescriptor)) {
      return convertToDescriptors(project, nodeDescriptor, myOtherTypes.toArray());
    }
    else if (nodeDescriptorElement instanceof ConfigurationType) {
      ConfigurationFactory[] factories = ((ConfigurationType)nodeDescriptorElement).getConfigurationFactories();
      if (factories.length > 1) {
        return convertToDescriptors(project, nodeDescriptor, factories);
      }
    }
    return NodeDescriptor.EMPTY_ARRAY;
  }
}
