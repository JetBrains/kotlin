// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.colors;

import com.intellij.application.options.EditorFontsConstants;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.FontPreferences;
import com.intellij.openapi.editor.colors.ModifiableFontPreferences;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.FontComboBox;
import com.intellij.ui.FontInfoRenderer;
import com.intellij.ui.TooltipWithClickableLinks;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class AbstractFontOptionsPanel extends JPanel implements OptionsPanel {
  private static final FontInfoRenderer RENDERER = new FontInfoRenderer() {
    @Override
    protected boolean isEditorFont() {
      return true;
    }
  };

  private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);

  @NotNull private final JTextField myEditorFontSizeField = new JTextField(4);
  @NotNull private final JTextField myLineSpacingField = new JTextField(4);
  private final FontComboBox myPrimaryCombo = new FontComboBox();
  private final JCheckBox myEnableLigaturesCheckbox = new JCheckBox(ApplicationBundle.message("use.ligatures"));
  private final FontComboBox mySecondaryCombo = new FontComboBox(false, false, true);

  @NotNull private final JBCheckBox myOnlyMonospacedCheckBox =
    new JBCheckBox(ApplicationBundle.message("checkbox.show.only.monospaced.fonts"));

  private boolean myIsInSchemeChange;
  private JLabel myPrimaryLabel;
  private JLabel mySizeLabel;

  protected final static int ADDITIONAL_VERTICAL_GAP = 12;
  protected final static int BASE_INSET = 5;
  private JLabel mySecondaryFontLabel;
  private JLabel myLineSpacingLabel;

  protected AbstractFontOptionsPanel() {
    setLayout(new FlowLayout(FlowLayout.LEFT));
    add(createControls());
  }

  protected JComponent createControls() {
    return createFontSettingsPanel();
  }

  @SuppressWarnings("unchecked")
  protected final JPanel createFontSettingsPanel() {
    Insets baseInsets = getInsets(0, 0);

    JPanel fontPanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.anchor = GridBagConstraints.WEST;
    c.insets = baseInsets;

    c.gridx = 0;
    c.gridy = 0;
    myPrimaryLabel = new JLabel(ApplicationBundle.message("primary.font"));
    fontPanel.add(myPrimaryLabel, c);

    c.gridx = 1;
    fontPanel.add(myPrimaryCombo, c);

    c.gridx = 2;
    c.insets = getInsets(0, BASE_INSET);
    fontPanel.add(myOnlyMonospacedCheckBox, c);

    c.gridx = 0;
    c.gridy = 1;
    c.insets = baseInsets;
    mySizeLabel = new JLabel(ApplicationBundle.message("editbox.font.size"));
    fontPanel.add(mySizeLabel, c);

    c.gridx = 1;
    fontPanel.add(myEditorFontSizeField, c);

    c.gridx = 0;
    c.gridy = 2;
    myLineSpacingLabel = new JLabel(ApplicationBundle.message("editbox.line.spacing"));
    myLineSpacingLabel.setLabelFor(myLineSpacingField);
    fontPanel.add(myLineSpacingLabel, c);
    c.gridx = 1;
    fontPanel.add(myLineSpacingField,c);

    c.gridy = 3;
    c.gridx = 0;
    c.insets = getInsets(ADDITIONAL_VERTICAL_GAP, 0);
    mySecondaryFontLabel = new JLabel(ApplicationBundle.message("secondary.font"));
    mySecondaryFontLabel.setLabelFor(mySecondaryCombo);
    fontPanel.add(mySecondaryFontLabel, c);
    c.gridx = 1;
    fontPanel.add(mySecondaryCombo, c);
    c.gridx = 2;
    c.insets = getInsets(ADDITIONAL_VERTICAL_GAP, BASE_INSET);
    JLabel fallbackLabel = new JLabel(ApplicationBundle.message("label.fallback.fonts.list.description"));
    fallbackLabel.setEnabled(false);
    fontPanel.add(fallbackLabel, c);

    JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    myEnableLigaturesCheckbox.setBorder(null);
    panel.add(myEnableLigaturesCheckbox);
    JLabel warningIcon = new JLabel(AllIcons.General.BalloonWarning);
    IdeTooltipManager.getInstance().setCustomTooltip(
      warningIcon,
      new TooltipWithClickableLinks.ForBrowser(warningIcon,
                                               ApplicationBundle.message("ligatures.jre.warning",
                                                                         ApplicationNamesInfo.getInstance().getFullProductName())));
    warningIcon.setBorder(JBUI.Borders.emptyLeft(5));
    warningIcon.setVisible(!SystemInfo.isJetBrainsJvm);
    panel.add(warningIcon);
    c.gridx = 0;
    c.gridy = 4;
    c.gridwidth = 2;
    c.insets = getInsets(ADDITIONAL_VERTICAL_GAP, 0);
    c.insets.bottom = BASE_INSET;
    fontPanel.add(panel, c);

    myOnlyMonospacedCheckBox.setBorder(null);
    mySecondaryCombo.setEnabled(false);

    myOnlyMonospacedCheckBox.setSelected(EditorColorsManager.getInstance().isUseOnlyMonospacedFonts());
    myOnlyMonospacedCheckBox.addActionListener(e -> {
      EditorColorsManager.getInstance().setUseOnlyMonospacedFonts(myOnlyMonospacedCheckBox.isSelected());
      myPrimaryCombo.setMonospacedOnly(myOnlyMonospacedCheckBox.isSelected());
      mySecondaryCombo.setMonospacedOnly(myOnlyMonospacedCheckBox.isSelected());
    });
    myPrimaryCombo.setMonospacedOnly(myOnlyMonospacedCheckBox.isSelected());
    myPrimaryCombo.setRenderer(RENDERER);

    mySecondaryCombo.setMonospacedOnly(myOnlyMonospacedCheckBox.isSelected());
    mySecondaryCombo.setRenderer(RENDERER);

    ItemListener itemListener = this::syncFontFamilies;
    myPrimaryCombo.addItemListener(itemListener);
    mySecondaryCombo.addItemListener(itemListener);

    ActionListener actionListener = this::syncFontFamilies;
    myPrimaryCombo.addActionListener(actionListener);
    mySecondaryCombo.addActionListener(actionListener);

    myEditorFontSizeField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void textChanged(@NotNull DocumentEvent event) {
        if (myIsInSchemeChange || !SwingUtilities.isEventDispatchThread()) return;
        String selectedFont = myPrimaryCombo.getFontName();
        if (selectedFont != null) {
          setFontSize(getFontSizeFromField());
        }
        updateDescription(true);
      }
    });
    myEditorFontSizeField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.VK_UP && e.getKeyCode() != KeyEvent.VK_DOWN) return;
        boolean up = e.getKeyCode() == KeyEvent.VK_UP;
        try {
          int value = Integer.parseInt(myEditorFontSizeField.getText());
          value += (up ? 1 : -1);
          value = Math.min(EditorFontsConstants.getMaxEditorFontSize(), Math.max(EditorFontsConstants.getMinEditorFontSize(), value));
          myEditorFontSizeField.setText(String.valueOf(value));
        }
        catch (NumberFormatException ignored) {
        }
      }
    });

    myLineSpacingField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void textChanged(@NotNull DocumentEvent event) {
        if (myIsInSchemeChange) return;
        float lineSpacing = getLineSpacingFromField();
        if (getLineSpacing() != lineSpacing) {
          setCurrentLineSpacing(lineSpacing);
        }
        updateDescription(true);
      }
    });
    myLineSpacingField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.VK_UP && e.getKeyCode() != KeyEvent.VK_DOWN) return;
        boolean up = e.getKeyCode() == KeyEvent.VK_UP;
        try {
          float value = Float.parseFloat(myLineSpacingField.getText());
          value += (up ? 1 : -1) * .1F;
          value = Math.min(EditorFontsConstants.getMaxEditorLineSpacing(), Math.max(EditorFontsConstants.getMinEditorLineSpacing(), value));
          myLineSpacingField.setText(String.format(Locale.ENGLISH, "%.1f", value));
        }
        catch (NumberFormatException ignored) {
        }
      }
    });
    myEnableLigaturesCheckbox.addActionListener(e -> {
      FontPreferences preferences = getFontPreferences();
      if (preferences instanceof ModifiableFontPreferences) {
        ((ModifiableFontPreferences)preferences).setUseLigatures(myEnableLigaturesCheckbox.isSelected());
        updateDescription(true);
      }
    });
    return fontPanel;
  }

  private static Insets getInsets(int extraTopSpacing, int extraLeftSpacing) {
    return JBUI.insets(BASE_INSET + extraTopSpacing, BASE_INSET + extraLeftSpacing, 0, 0);
  }

  protected void setDelegatingPreferences(boolean isDelegating) {
  }

  private int getFontSizeFromField() {
    try {
      return Math.min(EditorFontsConstants.getMaxEditorFontSize(),
                      Math.max(EditorFontsConstants.getMinEditorFontSize(), Integer.parseInt(myEditorFontSizeField.getText())));
    }
    catch (NumberFormatException e) {
      return EditorFontsConstants.getDefaultEditorFontSize();
    }
  }

  private float getLineSpacingFromField() {
    try {
      return Math.min(EditorFontsConstants.getMaxEditorLineSpacing(),
                      Math.max(EditorFontsConstants.getMinEditorLineSpacing(), Float.parseFloat(myLineSpacingField.getText())));
    }
    catch (NumberFormatException e) {
      return EditorFontsConstants.getDefaultEditorLineSpacing();
    }
  }

  /**
   * Processes an event from {@code FontComboBox}
   * if it is enabled and its item is selected.
   *
   * @param event the event to process
   */
  private void syncFontFamilies(AWTEvent event) {
    Object source = event.getSource();
    if (source instanceof FontComboBox) {
      FontComboBox combo = (FontComboBox)source;
      if (combo.isEnabled() && combo.isShowing() && combo.getSelectedItem() != null) {
        syncFontFamilies();
      }
    }
  }

  private void syncFontFamilies() {
    if (myIsInSchemeChange) {
      return;
    }
    FontPreferences fontPreferences = getFontPreferences();
    if (fontPreferences instanceof ModifiableFontPreferences) {
      ModifiableFontPreferences modifiableFontPreferences = (ModifiableFontPreferences)fontPreferences;
      modifiableFontPreferences.clearFonts();
      modifiableFontPreferences.setUseLigatures(myEnableLigaturesCheckbox.isSelected());
      String primaryFontFamily = myPrimaryCombo.getFontName();
      String secondaryFontFamily = mySecondaryCombo.isNoFontSelected() ? null : mySecondaryCombo.getFontName();
      int fontSize = getFontSizeFromField();
      if (primaryFontFamily != null) {
        if (!FontPreferences.DEFAULT_FONT_NAME.equals(primaryFontFamily)) {
          modifiableFontPreferences.addFontFamily(primaryFontFamily);
        }
        modifiableFontPreferences.register(primaryFontFamily, fontSize);
      }
      if (secondaryFontFamily != null) {
        if (!FontPreferences.DEFAULT_FONT_NAME.equals(secondaryFontFamily)) {
          modifiableFontPreferences.addFontFamily(secondaryFontFamily);
        }
        modifiableFontPreferences.register(secondaryFontFamily, fontSize);
      }
      updateDescription(true);
    }
  }

  @Override
  public void updateOptionsList() {
    myIsInSchemeChange = true;

    myLineSpacingField.setText(Float.toString(getLineSpacing()));
    FontPreferences fontPreferences = getFontPreferences();
    List<String> fontFamilies = fontPreferences.getEffectiveFontFamilies();
    myPrimaryCombo.setFontName(fontPreferences.getFontFamily());
    boolean isThereSecondaryFont = fontFamilies.size() > 1;
    mySecondaryCombo.setFontName(isThereSecondaryFont ? fontFamilies.get(1) : null);
    myEditorFontSizeField.setText(String.valueOf(fontPreferences.getSize(fontPreferences.getFontFamily())));

    boolean isReadOnlyColorScheme = isReadOnly();
    updateCustomOptions();
    boolean readOnly = isReadOnlyColorScheme || !(getFontPreferences() instanceof ModifiableFontPreferences);
    myPrimaryCombo.setEnabled(!readOnly);
    myPrimaryLabel.setEnabled(!readOnly);
    mySecondaryCombo.setEnabled(!readOnly);
    mySecondaryFontLabel.setEnabled(!readOnly);
    myOnlyMonospacedCheckBox.setEnabled(!readOnly);
    myLineSpacingField.setEnabled(!readOnly);
    myLineSpacingLabel.setEnabled(!readOnly);
    myEditorFontSizeField.setEnabled(!readOnly);
    mySizeLabel.setEnabled(!readOnly);

    myEnableLigaturesCheckbox.setEnabled(!readOnly && SystemInfo.isJetBrainsJvm);
    myEnableLigaturesCheckbox.setSelected(fontPreferences.useLigatures());

    myIsInSchemeChange = false;
  }

  protected void updateCustomOptions() {
  }

  protected abstract boolean isReadOnly();

  protected abstract boolean isDelegating();

  @NotNull
  protected abstract FontPreferences getFontPreferences();

  protected abstract void setFontSize(int fontSize);

  protected abstract float getLineSpacing();

  protected abstract void setCurrentLineSpacing(float lineSpacing);

  @Override
  @Nullable
  public Runnable showOption(final String option) {
    return null;
  }

  @Override
  public void applyChangesToScheme() {
  }

  @Override
  public void selectOption(final String typeToSelect) {
  }

  public boolean updateDescription(boolean modified) {
    if (modified && isReadOnly()) {
      return false;
    }
    fireFontChanged();
    return true;
  }

  @Override
  public void addListener(ColorAndFontSettingsListener listener) {
    myDispatcher.addListener(listener);
  }

  public void fireFontChanged() {
    myDispatcher.getMulticaster().fontChanged();
  }

  @Override
  public JPanel getPanel() {
    return this;
  }

  @Override
  public Set<String> processListOptions() {
    return new HashSet<>();
  }

}
