// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeEditor.printing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.FontComboBox;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class PrintDialog extends DialogWrapper {
  private JRadioButton myRbCurrentFile = null;
  private JRadioButton myRbSelectedText = null;
  private JRadioButton myRbCurrentPackage = null;
  private JCheckBox myCbIncludeSubpackages = null;

  private JComboBox<Object> myPaperSizeCombo = null;

  private JCheckBox myCbColorPrinting = null;
  private JCheckBox myCbSyntaxPrinting = null;
  private JCheckBox myCbPrintAsGraphics = null;

  private JRadioButton myRbPortrait = null;
  private JRadioButton myRbLandscape = null;

  private FontComboBox myFontNameCombo = null;
  private JComboBox<Integer> myFontSizeCombo = null;

  private JCheckBox myCbLineNumbers = null;

  private JRadioButton myRbNoWrap = null;
  private JRadioButton myRbWrapAtWordBreaks = null;

  private JTextField myTopMarginField = null;
  private JTextField myBottomMarginField = null;
  private JTextField myLeftMarginField = null;
  private JTextField myRightMarginField = null;

  private JCheckBox myCbDrawBorder = null;
  private JCheckBox myCbEvenNumberOfPages = null;

  private JTextField myLineTextField1 = null;
  private JComboBox<PrintSettings.Placement> myLinePlacementCombo1 = null;
  private JComboBox<PrintSettings.Alignment> myLineAlignmentCombo1 = null;
  private JTextField myLineTextField2 = null;
  private JComboBox<PrintSettings.Placement> myLinePlacementCombo2 = null;
  private JComboBox<PrintSettings.Alignment> myLineAlignmentCombo2 = null;
  private JComboBox<Integer> myFooterFontSizeCombo = null;
  private FontComboBox myFooterFontNameCombo = null;
  private final String myFileName;
  private final String myDirectoryName;
  private final boolean isSelectedTextEnabled;
  private final int mySelectedFileCount;
  private final String mySelectedText;

  PrintDialog(String fileName, String directoryName, String selectedText, int selectedFileCount, Project project) {
    super(project, true);
    mySelectedText = selectedText;
    setOKButtonText(CodeEditorBundle.message("print.print.button"));
    myFileName = fileName;
    myDirectoryName = directoryName;
    isSelectedTextEnabled = selectedText != null;
    mySelectedFileCount = selectedFileCount;
    setTitle(CodeEditorBundle.message("print.title"));
    init();
  }


  @Override
  protected JComponent createNorthPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(4,8,8,4));
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;

    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.insets = JBUI.emptyInsets();

    myRbCurrentFile = new JRadioButton(mySelectedFileCount > 1 ? CodeEditorBundle.message("print.files.radio", mySelectedFileCount)
                                                               : CodeEditorBundle.message("print.file.name.radio",
                                                                                          (myFileName != null ? myFileName : "")));
    panel.add(myRbCurrentFile, gbConstraints);

    myRbSelectedText = new JRadioButton(mySelectedText != null ? mySelectedText : CodeEditorBundle.message("print.selected.text.radio"));
    gbConstraints.gridy++;
    gbConstraints.insets = JBUI.emptyInsets();
    panel.add(myRbSelectedText, gbConstraints);

    myRbCurrentPackage = new JRadioButton(
      CodeEditorBundle.message("print.all.files.in.directory.radio", (myDirectoryName != null ? myDirectoryName : "")));
    gbConstraints.gridy++;
    gbConstraints.insets = JBUI.emptyInsets();
    panel.add(myRbCurrentPackage, gbConstraints);

    myCbIncludeSubpackages = new JCheckBox(CodeEditorBundle.message("print.include.subdirectories.checkbox"));
    gbConstraints.gridy++;
    gbConstraints.insets = JBUI.insetsLeft(20);
    panel.add(myCbIncludeSubpackages, gbConstraints);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(myRbCurrentFile);
    buttonGroup.add(myRbSelectedText);
    buttonGroup.add(myRbCurrentPackage);

    ActionListener actionListener = e -> updateDependentComponents();

    myRbCurrentFile.addActionListener(actionListener);
    myRbSelectedText.addActionListener(actionListener);
    myRbCurrentPackage.addActionListener(actionListener);

    return panel;
  }

  private void updateDependentComponents() {
    myCbIncludeSubpackages.setEnabled(myRbCurrentPackage.isSelected());
    myCbEvenNumberOfPages.setVisible(myRbCurrentFile.isSelected() && mySelectedFileCount > 1 || myRbCurrentPackage.isSelected());
  }

  @Override
  protected JComponent createCenterPanel() {
    TabbedPaneWrapper tabbedPaneWrapper = new TabbedPaneWrapper(myDisposable);
    tabbedPaneWrapper.addTab(CodeEditorBundle.message("print.settings.tab"), createPrintSettingsPanel());
    tabbedPaneWrapper.addTab(CodeEditorBundle.message("print.header.footer.tab"), createHeaderAndFooterPanel());
    tabbedPaneWrapper.addTab(CodeEditorBundle.message("print.advanced.tab"), createAdvancedPanel());
    return tabbedPaneWrapper.getComponent();
  }

  private JPanel createPrintSettingsPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    panel.setBorder(BorderFactory.createEmptyBorder(8,8,4,4));
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.weighty = 0;
    gbConstraints.insets = JBUI.insets(0, 8, 6, 4);
    gbConstraints.fill = GridBagConstraints.BOTH;

    JLabel paperSizeLabel = new MyLabel(CodeEditorBundle.message("print.settings.paper.size.label"));
    panel.add(paperSizeLabel, gbConstraints);
    myPaperSizeCombo = createPageSizesCombo();
    gbConstraints.gridx = 1;
    gbConstraints.gridwidth = 2;
    panel.add(myPaperSizeCombo, gbConstraints);

    JLabel fontLabel = new MyLabel(CodeEditorBundle.message("print.settings.font.label"));
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridy++;
    panel.add(fontLabel, gbConstraints);

    myFontNameCombo = new FontComboBox(true);
    gbConstraints.gridx = 1;
    panel.add(myFontNameCombo, gbConstraints);

    myFontSizeCombo = createFontSizesComboBox();
    gbConstraints.gridx = 2;
    panel.add(myFontSizeCombo, gbConstraints);

    myCbLineNumbers = new JCheckBox(CodeEditorBundle.message("print.settings.show.line.numbers.checkbox"));
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 3;
    gbConstraints.gridy++;
    panel.add(myCbLineNumbers, gbConstraints);

    myCbDrawBorder = new JCheckBox(CodeEditorBundle.message("print.settings.draw.border.checkbox"));
    gbConstraints.gridy++;
    panel.add(myCbDrawBorder, gbConstraints);

    myCbEvenNumberOfPages = new JCheckBox(CodeEditorBundle.message("print.settings.even.number.of.pages"));
    gbConstraints.gridy++;
    panel.add(myCbEvenNumberOfPages, gbConstraints);

    gbConstraints.insets = JBUI.insets(0, 0, 6, 4);
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 3;
    gbConstraints.gridy++;
    panel.add(createStyleAndLayoutPanel(), gbConstraints);

    gbConstraints.gridy++;
    gbConstraints.weighty = 1;
    panel.add(new MyTailPanel(), gbConstraints);
    return panel;
  }

  private JPanel createAdvancedPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    panel.setBorder(BorderFactory.createEmptyBorder(8,8,4,4));
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.weighty = 0;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.insets = JBUI.insets(0, 0, 6, 4);

    panel.add(createWrappingPanel(), gbConstraints);

    gbConstraints.gridy++;
    panel.add(createMarginsPanel(), gbConstraints);

    gbConstraints.gridy++;
    gbConstraints.weighty = 1;
    panel.add(new MyTailPanel(), gbConstraints);

    return panel;
  }

  private JPanel createStyleAndLayoutPanel() {
    JPanel panel = new JPanel(new GridLayout(1, 2));
    panel.add(createOrientationPanel());
    panel.add(createStylePanel());
    return panel;
  }

  private JPanel createOrientationPanel() {
    JPanel panel = new JPanel();
    panel.setBorder(IdeBorderFactory.createTitledBorder(CodeEditorBundle.message("print.orientation.group")));
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;

    myRbPortrait = new JRadioButton(CodeEditorBundle.message("print.orientation.portrait.radio"));
    panel.add(myRbPortrait, gbConstraints);

    myRbLandscape = new JRadioButton(CodeEditorBundle.message("print.orientation.landscape.radio"));
    gbConstraints.gridy++;
    panel.add(myRbLandscape, gbConstraints);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(myRbPortrait);
    buttonGroup.add(myRbLandscape);

    return panel;
  }

  private JPanel createStylePanel() {
    JPanel panel = new JPanel();
    panel.setBorder(IdeBorderFactory.createTitledBorder(CodeEditorBundle.message("print.style.group")));
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;

    myCbColorPrinting = new JCheckBox(CodeEditorBundle.message("print.style.color.printing.checkbox"));
    panel.add(myCbColorPrinting, gbConstraints);

    myCbSyntaxPrinting = new JCheckBox(CodeEditorBundle.message("print.style.syntax.printing.checkbox"));
    gbConstraints.gridy++;
    panel.add(myCbSyntaxPrinting, gbConstraints);

    myCbPrintAsGraphics = new JCheckBox(CodeEditorBundle.message("print.style.print.as.graphics.checkbox"));
    gbConstraints.gridy++;
    panel.add(myCbPrintAsGraphics, gbConstraints);

    return panel;
  }

  private JPanel createWrappingPanel() {
    JPanel panel = new JPanel();
    panel.setBorder(IdeBorderFactory.createTitledBorder(CodeEditorBundle.message("print.wrapping.group")));
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;

    myRbNoWrap = new JRadioButton(CodeEditorBundle.message("print.wrapping.none.radio"));
    panel.add(myRbNoWrap, gbConstraints);

    myRbWrapAtWordBreaks = new JRadioButton(CodeEditorBundle.message("print.wrapping.word.breaks.radio"));
    gbConstraints.gridy++;
    panel.add(myRbWrapAtWordBreaks, gbConstraints);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(myRbNoWrap);
    buttonGroup.add(myRbWrapAtWordBreaks);

    return panel;
  }

  private JPanel createMarginsPanel() {
    JPanel panel = new JPanel();
    panel.setBorder(IdeBorderFactory.createTitledBorder(CodeEditorBundle.message("print.margins.group")));
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;

    panel.add(new MyLabel(CodeEditorBundle.message("print.margins.top.label")), gbConstraints);
    myTopMarginField = new MyTextField(6);
    gbConstraints.weightx = 0;
    gbConstraints.gridx = 1;
    panel.add(myTopMarginField, gbConstraints);

    gbConstraints.weightx = 1;
    gbConstraints.gridx = 2;
    panel.add(new MyLabel(CodeEditorBundle.message("print.margins.bottom.label")), gbConstraints);
    myBottomMarginField = new MyTextField(6);
    gbConstraints.weightx = 0;
    gbConstraints.gridx = 3;
    panel.add(myBottomMarginField, gbConstraints);

    gbConstraints.weightx = 1;
    gbConstraints.gridx = 0;
    gbConstraints.gridy++;
    panel.add(new MyLabel(CodeEditorBundle.message("print.margins.left.label")), gbConstraints);
    myLeftMarginField = new MyTextField(6);
    gbConstraints.weightx = 0;
    gbConstraints.gridx = 1;
    panel.add(myLeftMarginField, gbConstraints);

    gbConstraints.weightx = 1;
    gbConstraints.gridx = 2;
    panel.add(new MyLabel(CodeEditorBundle.message("print.margins.right.label")), gbConstraints);
    myRightMarginField = new MyTextField(6);
    gbConstraints.weightx = 0;
    gbConstraints.gridx = 3;
    panel.add(myRightMarginField, gbConstraints);

    return panel;
  }

  private JPanel createHeaderAndFooterPanel() {
//    JPanel panel = createGroupPanel("Header");
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(8,8,4,4));
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.insets = JBUI.insets(0, 0, 6, 4);

    gbConstraints.gridwidth = 3;
    myLineTextField1 = new MyTextField(30);
    myLinePlacementCombo1 = new ComboBox<>();
    myLineAlignmentCombo1 = new ComboBox<>();
    JPanel linePanel1 = createLinePanel(CodeEditorBundle.message("print.header.line.1.label"), myLineTextField1, myLinePlacementCombo1, myLineAlignmentCombo1);
    panel.add(linePanel1, gbConstraints);

    myLineTextField2 = new MyTextField(30);
    myLinePlacementCombo2 = new ComboBox<>();
    myLineAlignmentCombo2 = new ComboBox<>();
    JPanel linePanel2 = createLinePanel(CodeEditorBundle.message("print.header.line.2.label"), myLineTextField2, myLinePlacementCombo2, myLineAlignmentCombo2);
    gbConstraints.gridy++;
    panel.add(linePanel2, gbConstraints);

    gbConstraints.insets = JBUI.insets(0, 8, 6, 4);
    gbConstraints.gridy++;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridx = 0;
    panel.add(new MyLabel(CodeEditorBundle.message("print.header.font.label")), gbConstraints);
    myFooterFontNameCombo = new FontComboBox(true);
    gbConstraints.gridx = 1;
    panel.add(myFooterFontNameCombo, gbConstraints);

    myFooterFontSizeCombo = createFontSizesComboBox();
    gbConstraints.gridx = 2;
    panel.add(myFooterFontSizeCombo, gbConstraints);

    return panel;
  }

  private static JPanel createLinePanel(String name,
                                        JTextField lineTextField,
                                        JComboBox<PrintSettings.Placement> linePlacementCombo,
                                        JComboBox<PrintSettings.Alignment> lineAlignmentCombo) {
    JPanel panel = new JPanel();
    panel.setBorder(IdeBorderFactory.createTitledBorder(name));
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 0;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.insets = JBUI.insetsBottom(6);

    panel.add(new MyLabel(CodeEditorBundle.message("print.header.text.line.editbox")), gbConstraints);
    gbConstraints.gridx = 1;
    gbConstraints.gridwidth = 4;
    gbConstraints.weightx = 1;
    panel.add(lineTextField, gbConstraints);

    gbConstraints.gridwidth = 1;
    gbConstraints.gridy++;
    gbConstraints.gridx = 0;
    gbConstraints.weightx = 0;
    panel.add(new MyLabel(CodeEditorBundle.message("print.header.placement.combobox")), gbConstraints);
    linePlacementCombo.addItem(PrintSettings.Placement.Header);
    linePlacementCombo.addItem(PrintSettings.Placement.Footer);
    gbConstraints.gridx = 1;
    gbConstraints.weightx = 0;
    panel.add(linePlacementCombo, gbConstraints);

    gbConstraints.gridx = 2;
    gbConstraints.weightx = 1;
    panel.add(new MyTailPanel(), gbConstraints);

    gbConstraints.gridx = 3;
    gbConstraints.weightx = 0;
    panel.add(new MyLabel(CodeEditorBundle.message("print.header.alignment.combobox")), gbConstraints);
    lineAlignmentCombo.addItem(PrintSettings.Alignment.Left);
    lineAlignmentCombo.addItem(PrintSettings.Alignment.Center);
    lineAlignmentCombo.addItem(PrintSettings.Alignment.Right);
    gbConstraints.gridx = 4;
    gbConstraints.weightx = 0;
    panel.add(lineAlignmentCombo, gbConstraints);

    return panel;
  }

  private static JComboBox<Integer> createFontSizesComboBox() {
    JComboBox<Integer> comboBox = new ComboBox<>();
    for (int i = 6; i < 40; i++) {
      comboBox.addItem(Integer.valueOf(i));
    }
    return comboBox;
  }

  private static JComboBox<Object> createPageSizesCombo() {
    JComboBox<Object> pageSizesCombo = new ComboBox<>();
    for (String name : PageSizes.getNames()) {
      pageSizesCombo.addItem(PageSizes.getItem(name));
    }
    return pageSizesCombo;
  }

  private static class MyTailPanel extends JPanel {
    MyTailPanel(){
      setFocusable(false);
    }

    @Override
    public Dimension getMinimumSize() {
      return JBUI.emptySize();
    }
    @Override
    public Dimension getPreferredSize() {
      return JBUI.emptySize();
    }
  }

  public void reset() {
    PrintSettings printSettings = PrintSettings.getInstance();

    myRbSelectedText.setEnabled(isSelectedTextEnabled);
    myRbSelectedText.setSelected(isSelectedTextEnabled);
    myRbCurrentFile.setEnabled(myFileName != null || mySelectedFileCount > 1);
    myRbCurrentFile.setSelected(myFileName != null && !isSelectedTextEnabled || mySelectedFileCount > 1);
    myRbCurrentPackage.setEnabled(myDirectoryName != null);
    myRbCurrentPackage.setSelected(myDirectoryName != null && !isSelectedTextEnabled && myFileName == null);

    myCbIncludeSubpackages.setSelected(printSettings.isIncludeSubdirectories());

    updateDependentComponents();

    Object selectedPageSize = PageSizes.getItem(printSettings.PAPER_SIZE);
    if(selectedPageSize != null) {
      myPaperSizeCombo.setSelectedItem(selectedPageSize);
    }
    myCbColorPrinting.setSelected(printSettings.COLOR_PRINTING);
    myCbSyntaxPrinting.setSelected(printSettings.SYNTAX_PRINTING);
    myCbPrintAsGraphics.setSelected(printSettings.PRINT_AS_GRAPHICS);

    if(printSettings.PORTRAIT_LAYOUT) {
      myRbPortrait.setSelected(true);
    }
    else {
      myRbLandscape.setSelected(true);
    }
    myFontNameCombo.setFontName(printSettings.FONT_NAME);
    myFontSizeCombo.setSelectedItem(Integer.valueOf(printSettings.FONT_SIZE));

    myCbLineNumbers.setSelected(printSettings.PRINT_LINE_NUMBERS);

    if(printSettings.WRAP) {
      myRbWrapAtWordBreaks.setSelected(true);
    }
    else {
      myRbNoWrap.setSelected(true);
    }

    myTopMarginField.setText(String.valueOf(printSettings.TOP_MARGIN));
    myBottomMarginField.setText(String.valueOf(printSettings.BOTTOM_MARGIN));
    myLeftMarginField.setText(String.valueOf(printSettings.LEFT_MARGIN));
    myRightMarginField.setText(String.valueOf(printSettings.RIGHT_MARGIN));

    myCbDrawBorder.setSelected(printSettings.DRAW_BORDER);
    myCbEvenNumberOfPages.setSelected(printSettings.EVEN_NUMBER_OF_PAGES);

    myLineTextField1.setText(printSettings.FOOTER_HEADER_TEXT1);
    myLinePlacementCombo1.setSelectedItem(printSettings.FOOTER_HEADER_PLACEMENT1);
    myLineAlignmentCombo1.setSelectedItem(printSettings.FOOTER_HEADER_ALIGNMENT1);

    myLineTextField2.setText(printSettings.FOOTER_HEADER_TEXT2);
    myLinePlacementCombo2.setSelectedItem(printSettings.FOOTER_HEADER_PLACEMENT2);
    myLineAlignmentCombo2.setSelectedItem(printSettings.FOOTER_HEADER_ALIGNMENT2);

    myFooterFontSizeCombo.setSelectedItem(Integer.valueOf(printSettings.FOOTER_HEADER_FONT_SIZE));
    myFooterFontNameCombo.setFontName(printSettings.FOOTER_HEADER_FONT_NAME);
  }

  public void apply() {
    PrintSettings printSettings = PrintSettings.getInstance();

    if (myRbCurrentFile.isSelected()){
      printSettings.setPrintScope(PrintSettings.PRINT_FILE);
    }
    else if (myRbSelectedText.isSelected()){
      printSettings.setPrintScope(PrintSettings.PRINT_SELECTED_TEXT);
    }
    else if (myRbCurrentPackage.isSelected()){
      printSettings.setPrintScope(PrintSettings.PRINT_DIRECTORY);
    }
    printSettings.setIncludeSubdirectories(myCbIncludeSubpackages.isSelected());

    printSettings.PAPER_SIZE = PageSizes.getName(myPaperSizeCombo.getSelectedItem());
    printSettings.COLOR_PRINTING = myCbColorPrinting.isSelected();
    printSettings.SYNTAX_PRINTING = myCbSyntaxPrinting.isSelected();
    printSettings.PRINT_AS_GRAPHICS = myCbPrintAsGraphics.isSelected();

    printSettings.PORTRAIT_LAYOUT = myRbPortrait.isSelected();

    printSettings.FONT_NAME = myFontNameCombo.getFontName();
    printSettings.FONT_SIZE = ObjectUtils.notNull((Integer)myFontSizeCombo.getSelectedItem(), printSettings.FONT_SIZE);

    printSettings.PRINT_LINE_NUMBERS = myCbLineNumbers.isSelected();

    printSettings.WRAP = myRbWrapAtWordBreaks.isSelected();


    try {
      printSettings.TOP_MARGIN = Float.parseFloat(myTopMarginField.getText());
    }
    catch(NumberFormatException ignored) { }

    try {
      printSettings.BOTTOM_MARGIN = Float.parseFloat(myBottomMarginField.getText());
    }
    catch(NumberFormatException ignored) { }

    try {
      printSettings.LEFT_MARGIN = Float.parseFloat(myLeftMarginField.getText());
    }
    catch(NumberFormatException ignored) { }

    try {
      printSettings.RIGHT_MARGIN = Float.parseFloat(myRightMarginField.getText());
    }
    catch(NumberFormatException ignored) { }

    printSettings.DRAW_BORDER = myCbDrawBorder.isSelected();
    printSettings.EVEN_NUMBER_OF_PAGES = myCbEvenNumberOfPages.isSelected();

    printSettings.FOOTER_HEADER_TEXT1 = myLineTextField1.getText();
    printSettings.FOOTER_HEADER_ALIGNMENT1 = (PrintSettings.Alignment)myLineAlignmentCombo1.getSelectedItem();
    printSettings.FOOTER_HEADER_PLACEMENT1 = (PrintSettings.Placement)myLinePlacementCombo1.getSelectedItem();

    printSettings.FOOTER_HEADER_TEXT2 = myLineTextField2.getText();
    printSettings.FOOTER_HEADER_ALIGNMENT2 = (PrintSettings.Alignment)myLineAlignmentCombo2.getSelectedItem();
    printSettings.FOOTER_HEADER_PLACEMENT2 = (PrintSettings.Placement)myLinePlacementCombo2.getSelectedItem();

    printSettings.FOOTER_HEADER_FONT_NAME = myFooterFontNameCombo.getFontName();
    printSettings.FOOTER_HEADER_FONT_SIZE =
      ObjectUtils.notNull((Integer)myFooterFontSizeCombo.getSelectedItem(), printSettings.FOOTER_HEADER_FONT_SIZE);
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), new ApplyAction(), getHelpAction()};
  }

  @Override
  protected String getHelpId() {
    return HelpID.PRINT;
  }

  private class ApplyAction extends AbstractAction{
    ApplyAction(){
      putValue(Action.NAME, CodeEditorBundle.message("print.apply.button"));
    }

    @Override
    public void actionPerformed(ActionEvent e){
      apply();
    }
  }

  private static class MyTextField extends JTextField {
    MyTextField(int size) {
     super(size);
    }

    @Override
    public Dimension getMinimumSize() {
      return super.getPreferredSize();
    }
  }

  private static class MyLabel extends JLabel {
    MyLabel(String text) {
     super(text);
    }

    @Override
    public Dimension getMinimumSize() {
      return super.getPreferredSize();
    }
  }
}