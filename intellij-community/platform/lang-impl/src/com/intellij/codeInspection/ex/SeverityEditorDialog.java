// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ex;

import com.intellij.application.options.colors.ColorAndFontDescriptionPanel;
import com.intellij.application.options.colors.InspectionColorSettingsPage;
import com.intellij.application.options.colors.TextAttributesDescription;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInsight.daemon.impl.SeverityUtil;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.ide.DataManager;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.ui.SingleInspectionProfilePanel;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;
import java.util.*;

import static com.intellij.application.options.colors.ColorAndFontOptions.selectOrEditColor;
import static com.intellij.codeInsight.daemon.impl.SeverityRegistrar.SeverityBasedTextAttributes;

public class SeverityEditorDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(SeverityEditorDialog.class);

  private final JPanel myPanel;

  private final JList<SeverityBasedTextAttributes> myOptionsList = new JBList<>();
  private final ColorAndFontDescriptionPanel myOptionsPanel = new ColorAndFontDescriptionPanel();

  private SeverityBasedTextAttributes myCurrentSelection;
  private final SeverityRegistrar mySeverityRegistrar;
  private final boolean myCloseDialogWhenSettingsShown;
  private final CardLayout myCard;
  private final JPanel myRightPanel;
  @NonNls private static final String DEFAULT = "DEFAULT";
  @NonNls private static final String EDITABLE = "EDITABLE";

  public static void show(@NotNull Project project,
                          @Nullable HighlightSeverity selectedSeverity,
                          @NotNull SeverityRegistrar severityRegistrar,
                          boolean closeDialogWhenSettingsShown,
                          @Nullable Consumer<? super HighlightSeverity> chosenSeverityCallback) {
    final SeverityEditorDialog dialog = new SeverityEditorDialog(project, selectedSeverity, severityRegistrar, closeDialogWhenSettingsShown);
    if (dialog.showAndGet()) {
      final HighlightInfoType type = dialog.getSelectedType();
      if (type != null) {
        final HighlightSeverity severity = type.getSeverity(null);
        if (chosenSeverityCallback != null) {
          chosenSeverityCallback.consume(severity);
        }
      }
    }
  }

  private SeverityEditorDialog(@NotNull Project project,
                               @Nullable HighlightSeverity selectedSeverity,
                               @NotNull SeverityRegistrar severityRegistrar,
                               boolean closeDialogWhenSettingsShown) {
    super(project, true);
    mySeverityRegistrar = severityRegistrar;
    myCloseDialogWhenSettingsShown = closeDialogWhenSettingsShown;
    myOptionsList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        final Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setText(SingleInspectionProfilePanel.renderSeverity(((SeverityBasedTextAttributes)value).getSeverity()));
        return rendererComponent;
      }
    });
    myOptionsList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (myCurrentSelection != null) {
          apply(myCurrentSelection);
        }
        myCurrentSelection = myOptionsList.getSelectedValue();
        if (myCurrentSelection != null) {
          reset(myCurrentSelection);
          myCard.show(myRightPanel, SeverityRegistrar.isDefaultSeverity(myCurrentSelection.getSeverity()) ? DEFAULT : EDITABLE);
        }
      }
    });
    TreeUIHelper.getInstance().installListSpeedSearch(myOptionsList, attrs -> attrs.getSeverity().getName());
    myOptionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JPanel leftPanel = ToolbarDecorator.createDecorator(myOptionsList)
                                       .setAddAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          final String name = Messages.showInputDialog(myPanel, InspectionsBundle.message("highlight.severity.create.dialog.name.label"),
                                                       InspectionsBundle.message("highlight.severity.create.dialog.title"),
                                                       Messages.getQuestionIcon(),
                                                       "", new InputValidator() {
            @Override
            public boolean checkInput(final String inputString) {
              return checkNameExist(inputString);
            }

            @Override
            public boolean canClose(final String inputString) {
              return checkInput(inputString);
            }
          });
          if (name == null) return;
          SeverityBasedTextAttributes newSeverityBasedTextAttributes = createSeverity(name,
                                                                                      CodeInsightColors.WARNINGS_ATTRIBUTES.getDefaultAttributes());
          ((DefaultListModel<SeverityBasedTextAttributes>)myOptionsList.getModel()).addElement(newSeverityBasedTextAttributes);

          select(newSeverityBasedTextAttributes);
        }
      }).setMoveUpAction(button -> {
        apply(myCurrentSelection);
        ListUtil.moveSelectedItemsUp(myOptionsList);
      }).setMoveDownAction(button -> {
        apply(myCurrentSelection);
        ListUtil.moveSelectedItemsDown(myOptionsList);
      }).setEditAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          String oldName = myCurrentSelection.getSeverity().getName();
          String newName = Messages.showInputDialog(myPanel, InspectionsBundle.message("highlight.severity.create.dialog.name.label"), "Edit Severity Name", null, oldName, new InputValidator() {
            @Override
            public boolean checkInput(String inputString) {
              return checkNameExist(inputString);
            }

            @Override
            public boolean canClose(String inputString) {
              return checkInput(inputString);
            }
          });
          if (newName != null && !oldName.equals(newName)) {
            SeverityBasedTextAttributes newSeverityBasedTextAttributes = createSeverity(newName, myCurrentSelection.getAttributes());
            int index = myOptionsList.getSelectedIndex();
            ((DefaultListModel<SeverityBasedTextAttributes>)myOptionsList.getModel()).set(index, newSeverityBasedTextAttributes);

            select(newSeverityBasedTextAttributes);
          }
        }
      }).setEditActionUpdater(e -> myCurrentSelection != null && !SeverityRegistrar.isDefaultSeverity(myCurrentSelection.getSeverity())).setEditActionName("Rename").createPanel();
    ToolbarDecorator.findRemoveButton(leftPanel).addCustomUpdater(
      e -> !SeverityRegistrar.isDefaultSeverity(myOptionsList.getSelectedValue().getSeverity()));
    ToolbarDecorator.findUpButton(leftPanel).addCustomUpdater(e -> {
      boolean canMove = ListUtil.canMoveSelectedItemsUp(myOptionsList);
      if (canMove) {
        SeverityBasedTextAttributes pair =
          myOptionsList.getSelectedValue();
        if (pair != null && SeverityRegistrar.isDefaultSeverity(pair.getSeverity())) {
          final int newPosition = myOptionsList.getSelectedIndex() - 1;
          pair = myOptionsList.getModel().getElementAt(newPosition);
          if (SeverityRegistrar.isDefaultSeverity(pair.getSeverity())) {
            canMove = false;
          }
        }
      }

      return canMove;
    });
    ToolbarDecorator.findDownButton(leftPanel).addCustomUpdater(e -> {
      boolean canMove = ListUtil.canMoveSelectedItemsDown(myOptionsList);
      if (canMove) {
        SeverityBasedTextAttributes pair =
          myOptionsList.getSelectedValue();
        if (pair != null && SeverityRegistrar.isDefaultSeverity(pair.getSeverity())) {
          final int newPosition = myOptionsList.getSelectedIndex() + 1;
          pair = myOptionsList.getModel().getElementAt(newPosition);
          if (SeverityRegistrar.isDefaultSeverity(pair.getSeverity())) {
            canMove = false;
          }
        }
      }

      return canMove;
    });

    myPanel = new JPanel(new BorderLayout());
    myPanel.add(leftPanel, BorderLayout.CENTER);
    myCard = new CardLayout();
    myRightPanel = new JPanel(myCard);
    final JPanel disabled = new JPanel(new GridBagLayout());
    final JButton button = new JButton(InspectionsBundle.message("severities.default.settings.message"));
    button.addActionListener(e -> editColorsAndFonts());
    disabled.add(button,
                 new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, JBUI.emptyInsets(), 0,
                                        0));
    myRightPanel.add(DEFAULT, disabled);
    myRightPanel.add(EDITABLE, myOptionsPanel);
    myCard.show(myRightPanel, EDITABLE);
    myPanel.add(myRightPanel, BorderLayout.EAST);
    fillList(selectedSeverity);
    init();
    setTitle(InspectionsBundle.message("severities.editor.dialog.title"));
    reset(myOptionsList.getSelectedValue());
  }

  @NotNull
  public SeverityBasedTextAttributes createSeverity(@NotNull String name, @NotNull TextAttributes parent) {
    HighlightInfoType.HighlightInfoTypeImpl info = new HighlightInfoType.HighlightInfoTypeImpl(new HighlightSeverity(name, 50),
                                                                                               TextAttributesKey
                                                                                                 .createTextAttributesKey(name));
    return new SeverityBasedTextAttributes(parent.clone(), info);
  }

  public void select(SeverityBasedTextAttributes newSeverityBasedTextAttributes) {
    myOptionsList.clearSelection();
    ScrollingUtil.selectItem(myOptionsList, newSeverityBasedTextAttributes);
  }

  private boolean checkNameExist(@NotNull String newName) {
    if (StringUtil.isEmpty(newName)) return false;
    final ListModel listModel = myOptionsList.getModel();
    for (int i = 0; i < listModel.getSize(); i++) {
      final String severityName = ((SeverityBasedTextAttributes)listModel.getElementAt(i)).getSeverity().myName;
      if (Comparing.strEqual(severityName, newName, false)) return false;
    }
    return true;
  }

  private void editColorsAndFonts() {
    final String toConfigure = Objects.requireNonNull(getSelectedType()).getSeverity(null).myName;
    if (myCloseDialogWhenSettingsShown) {
      doOKAction();
    }
    myOptionsList.clearSelection();
    final DataContext dataContext = DataManager.getInstance().getDataContext(myPanel);
    selectOrEditColor(dataContext, toConfigure, InspectionColorSettingsPage.class);
  }

  private void fillList(final @Nullable HighlightSeverity severity) {
    DefaultListModel<SeverityBasedTextAttributes> model = new DefaultListModel<>();
    final List<SeverityBasedTextAttributes> infoTypes =
      new ArrayList<>(SeverityUtil.getRegisteredHighlightingInfoTypes(mySeverityRegistrar));
    SeverityBasedTextAttributes preselection = null;
    for (SeverityBasedTextAttributes type : infoTypes) {
      model.addElement(type);
      if (type.getSeverity().equals(severity)) {
        preselection = type;
      }
    }
    if (preselection == null && !infoTypes.isEmpty()) {
      preselection = infoTypes.get(0);
    }
    myOptionsList.setModel(model);
    myOptionsList.setSelectedValue(preselection, true);
  }


  private void apply(SeverityBasedTextAttributes info) {
    if (info == null) {
      return;
    }
    MyTextAttributesDescription description = new MyTextAttributesDescription(info.getType().toString(), null, new TextAttributes(), info.getType().getAttributesKey());
    myOptionsPanel.apply(description, null);
    Element textAttributes = new Element("temp");
    try {
      description.getTextAttributes().writeExternal(textAttributes);
      info.getAttributes().readExternal(textAttributes);
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  private void reset(SeverityBasedTextAttributes info) {
    if (info == null) {
      return;
    }
    final MyTextAttributesDescription description =
      new MyTextAttributesDescription(info.getType().toString(), null, info.getAttributes(), info.getType().getAttributesKey());
    @NonNls Element textAttributes = new Element("temp");
    try {
      info.getAttributes().writeExternal(textAttributes);
      description.getTextAttributes().readExternal(textAttributes);
    }
    catch (Exception e) {
      LOG.error(e);
    }
    myOptionsPanel.reset(description);
  }

  @Override
  protected void doOKAction() {
    apply(myOptionsList.getSelectedValue());
    final Collection<SeverityBasedTextAttributes> infoTypes =
      new HashSet<>(SeverityUtil.getRegisteredHighlightingInfoTypes(mySeverityRegistrar));
    final ListModel listModel = myOptionsList.getModel();
    final List<HighlightSeverity> order = new ArrayList<>();
    for (int i = listModel.getSize() - 1; i >= 0; i--) {
      SeverityBasedTextAttributes info = (SeverityBasedTextAttributes)listModel.getElementAt(i);
      order.add(info.getSeverity());
      if (!SeverityRegistrar.isDefaultSeverity(info.getSeverity())) {
        infoTypes.remove(info);
        final Color stripeColor = info.getAttributes().getErrorStripeColor();
        final boolean exists = mySeverityRegistrar.getSeverity(info.getSeverity().getName()) != null;
        if (exists) {
          info.getType().getAttributesKey().getDefaultAttributes().setErrorStripeColor(stripeColor);
        } else {
          HighlightInfoType.HighlightInfoTypeImpl type = info.getType();
          TextAttributesKey key = type.getAttributesKey();
          final TextAttributes defaultAttributes = key.getDefaultAttributes().clone();
          defaultAttributes.setErrorStripeColor(stripeColor);
          key = TextAttributesKey.createTextAttributesKey(key.getExternalName(), defaultAttributes);
          type = new HighlightInfoType.HighlightInfoTypeImpl(type.getSeverity(null), key);
          info = new SeverityBasedTextAttributes(info.getAttributes(), type);
        }

        mySeverityRegistrar.registerSeverity(info, stripeColor != null ? stripeColor : LightColors.YELLOW);
      }
    }
    for (SeverityBasedTextAttributes info : infoTypes) {
      mySeverityRegistrar.unregisterSeverity(info.getSeverity());
    }
    mySeverityRegistrar.setOrder(order);
    super.doOKAction();
  }

  @Override
  @Nullable
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Nullable
  public HighlightInfoType getSelectedType() {
    final SeverityBasedTextAttributes selection =  myOptionsList.getSelectedValue();
    return selection != null ? selection.getType() : null;
  }

  private static class MyTextAttributesDescription extends TextAttributesDescription {
    MyTextAttributesDescription(final String name,
                                       final String group,
                                       final TextAttributes attributes,
                                       final TextAttributesKey type) {
      super(name, group, attributes, type, null, null, null);
    }

    @Override
    public boolean isErrorStripeEnabled() {
      return true;
    }


    @Override
    public TextAttributes getTextAttributes() {
      return super.getTextAttributes();
    }
  }
}
