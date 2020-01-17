// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class DefaultNewRunConfigurationTreePopupFactory extends NewRunConfigurationTreePopupFactory {
  private NodeDescriptor<?> root;
  private List<GroupDescriptor> myGroups;
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
    myGroups = createGroups(project, myTypesToShow);
  }

  @NotNull
  @Override
  public NodeDescriptor<?> getRootElement() {
    return root;
  }

  protected List<GroupDescriptor> createGroups(@NotNull Project project, @NotNull List<ConfigurationType> fullList) {
    return Arrays.asList(
      //it's just an example, 'how-to'
      //new GroupDescriptor(project, getRootElement(), AllIcons.FileTypes.Java, "Java", getTypes(fullList, "Application", "JarApplication")),
      //new GroupDescriptor(project, getRootElement(), AllIcons.RunConfigurations.TestPassed, "Tests", getTypes(fullList, "JUnit", "TestNG"))
    );
  }

  @SuppressWarnings("unused")
  protected static List<ConfigurationType> getTypes(List<ConfigurationType> fullList, String... ids) {
    List<String> idsList = Arrays.asList(ids);
    List<ConfigurationType> result = new ArrayList<>();
    for (Iterator<ConfigurationType> iterator = fullList.iterator(); iterator.hasNext(); ) {
      ConfigurationType type = iterator.next();
      if (idsList.contains(type.getId())) {
        result.add(type);
        //Note, here we remove an element from full 'plain' list as we put the element into some group
        iterator.remove();
      }
    }
    return result;
  }

  @Override
  public NodeDescriptor<?>[] createChildElements(@NotNull Project project, @NotNull NodeDescriptor nodeDescriptor) {
    Object nodeDescriptorElement = nodeDescriptor.getElement();
    if (root.equals(nodeDescriptor)) {
      ArrayList<NodeDescriptor> list = new ArrayList<>();
      for (GroupDescriptor group : myGroups) {
        if (!group.myTypes.isEmpty()) {
          list.add(group);
        }
      }
      list.addAll(Arrays.asList(convertToDescriptors(project, nodeDescriptor, myTypesToShow.toArray())));
      if (!myOtherTypes.isEmpty()) {
        list.add(other);
      }
      return list.toArray(NodeDescriptor.EMPTY_ARRAY);
    }
    else if (nodeDescriptor instanceof GroupDescriptor) {
      ArrayList<NodeDescriptor> list = new ArrayList<>();
      for (ConfigurationType type : ((GroupDescriptor)nodeDescriptor).myTypes) {
        list.add(createDescriptor(project, type, nodeDescriptor));
      }
      return list.toArray(NodeDescriptor.EMPTY_ARRAY);
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


  protected static class GroupDescriptor extends NodeDescriptor<String> {
    private final List<ConfigurationType> myTypes;

    protected GroupDescriptor(@NotNull Project project,
                            @NotNull NodeDescriptor parent,
                            @Nullable Icon icon,
                            @NotNull String name,
                            List<ConfigurationType> types) {
      super(project, parent);
      myTypes = types;
      myClosedIcon = icon;
      myName = name;
    }

    @Override
    public boolean update() {
      return false;
    }

    @Override
    public String getElement() {
      return myName;
    }

    @Override
    public int getWeight() {
      return DEFAULT_WEIGHT - 1;
    }
  }
}
