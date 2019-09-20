// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.options;

import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.Validator;
import com.intellij.openapi.compiler.options.ExcludedEntriesConfigurable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.NullableFunction;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

/**
 * @author Dmitry Avdeev
 */
public class ValidationConfigurable implements SearchableConfigurable, Configurable.NoScroll {

  private JPanel myPanel;
  private JCheckBox myValidateBox;
  private ElementsChooser<Compiler> myValidators;
  private JPanel myExcludedEntriesPanel;
  private JPanel myValidatorsPanel;
  private final Project myProject;
  private final ValidationConfiguration myConfiguration;
  private final ExcludedEntriesConfigurable myExcludedConfigurable;

  public ValidationConfigurable(final Project project) {
    myProject = project;
    myConfiguration = ValidationConfiguration.getInstance(myProject);
    myExcludedConfigurable = createExcludedConfigurable(project);

    myValidatorsPanel.setBorder(IdeBorderFactory.createTitledBorder("Validators:", false, JBUI.insetsTop(8)).setShowLine(false));
    myValidators.getEmptyText().setText(CompilerBundle.message("no.validators"));

    myExcludedEntriesPanel.setBorder(IdeBorderFactory.createTitledBorder("Exclude from validation:", false, JBUI.insetsTop(8)).setShowLine(false));
  }

  private static ExcludedEntriesConfigurable createExcludedConfigurable(@NotNull Project project) {
    ProjectFileIndex index = project.isDefault() ? null : ProjectRootManager.getInstance(project).getFileIndex();
    final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, true) {
      @Override
      public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        return super.isFileVisible(file, showHiddenFiles) && (index == null || !index.isExcluded(file));
      }
    };

    List<VirtualFile> allContentRoots = new ArrayList<>();
    for (final Module module: ModuleManager.getInstance(project).getModules()) {
      final VirtualFile[] moduleContentRoots = ModuleRootManager.getInstance(module).getContentRoots();
      Collections.addAll(allContentRoots, moduleContentRoots);
    }
    descriptor.setRoots(allContentRoots);
    return new ExcludedEntriesConfigurable(project, descriptor, ValidationConfiguration.getExcludedEntriesConfiguration(project));
  }

  @Override
  @NotNull
  public String getId() {
    return "project.validation";
  }

  @Override
  public String getDisplayName() {
    return CompilerBundle.message("validation.display.name");
  }

  @Override
  public String getHelpTopic() {
    return "reference.projectsettings.compiler.validation";
  }

  @Override
  public JComponent createComponent() {
    final GridConstraints constraints = new GridConstraints();
    constraints.setFill(GridConstraints.FILL_BOTH);
    myExcludedEntriesPanel.add(myExcludedConfigurable.createComponent(), constraints);
    return myPanel;
  }

  @Override
  public boolean isModified() {
    List<Compiler> selectedElements = myValidators.getMarkedElements();
    List<Compiler> markedValidators = getMarkedValidators();
    if (markedValidators.size() != selectedElements.size()) {
      return true;
    }
    Set<Compiler> set = new THashSet<>(selectedElements, new TObjectHashingStrategy<Compiler>() {
      @Override
      public int computeHashCode(Compiler object) {
        return object.getDescription().hashCode();
      }

      @Override
      public boolean equals(Compiler o1, Compiler o2) {
        return o1.getDescription().equals(o2.getDescription());
      }
    });
    return myConfiguration.isValidateOnBuild() != myValidateBox.isSelected() ||
        set.retainAll(markedValidators) ||
        myExcludedConfigurable.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myConfiguration.setValidateOnBuild(myValidateBox.isSelected());
    for (int i = 0; i < myValidators.getElementCount(); i++) {
      final Compiler validator = myValidators.getElementAt(i);
      myConfiguration.setSelected(validator,  myValidators.isElementMarked(validator));
    }
    myExcludedConfigurable.apply();
  }

  @Override
  public void reset() {
    myValidateBox.setSelected(myConfiguration.isValidateOnBuild());
    final List<Compiler> validators = getValidators();
    Collections.sort(validators, Comparator.comparing(Compiler::getDescription));
    myValidators.setElements(validators, false);
    myValidators.markElements(getMarkedValidators());
    myExcludedConfigurable.reset();
  }

  private List<Compiler> getMarkedValidators() {
    final List<Compiler> validators = getValidators();
    return ContainerUtil.mapNotNull(validators, (NullableFunction<Compiler, Compiler>)validator -> myConfiguration.isSelected(validator) ? validator : null);
  }

  private List<Compiler> getValidators() {
    final CompilerManager compilerManager = CompilerManager.getInstance(myProject);
    return new SmartList<>(compilerManager.getCompilers(Validator.class));
  }

  @Override
  public void disposeUIResources() {
    myExcludedConfigurable.disposeUIResources();
  }

  private void createUIComponents() {
    myValidators = new ElementsChooser<Compiler>(true) {
      @Override
      protected String getItemText(@NotNull final Compiler validator) {
        final String description = validator.getDescription();
        return description.replace(" Validator", "").replace(" validator", "");
      }
    };
  }
}
