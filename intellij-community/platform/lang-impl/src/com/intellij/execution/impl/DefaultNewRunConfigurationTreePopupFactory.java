// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DefaultNewRunConfigurationTreePopupFactory extends NewRunConfigurationTreePopupFactory {
  private final NodeDescriptor<?> root = createDescriptor("<invisible-templates-root>", null);
  private final NodeDescriptor<?> other =
    createDescriptor(ExecutionBundle.message("add.new.run.configuration.other.types.node.text"), root, NodeDescriptor.DEFAULT_WEIGHT + 1);
  private final List<? extends ConfigurationType> myTypesToShow;
  private final List<ConfigurationType> myOtherTypes;

  public DefaultNewRunConfigurationTreePopupFactory(@NotNull Project project, List<? extends ConfigurationType> typesToShow, List<ConfigurationType> otherTypes) {
    super(project);
    myTypesToShow = typesToShow;
    myOtherTypes = otherTypes;
  }

  @NotNull
  @Override
  public NodeDescriptor<?> getRootElement() {
    return root;
  }

  @Override
  public NodeDescriptor<?>[] createChildElements(@NotNull NodeDescriptor nodeDescriptor) {
    Object nodeDescriptorElement = nodeDescriptor.getElement();
    if (root.equals(nodeDescriptor)) {
      return ArrayUtil.append(convertToDescriptors(nodeDescriptor, myTypesToShow.toArray()), other);
    }
    else if (other.equals(nodeDescriptor)) {
      return convertToDescriptors(nodeDescriptor, myOtherTypes.toArray());
    }
    else if (nodeDescriptorElement instanceof ConfigurationType) {
      ConfigurationFactory[] factories = ((ConfigurationType)nodeDescriptorElement).getConfigurationFactories();
      if (factories.length > 1) {
        return convertToDescriptors(nodeDescriptor, factories);
      }
    }
    return NodeDescriptor.EMPTY_ARRAY;
  }
}
