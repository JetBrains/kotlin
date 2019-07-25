// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.ui;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.MultiRowFlowPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Allows to build controls that show target user text with 'reach info' (e.g. inline icon button).
 * <p/>
 * Not thread-safe.
 * 
 * @author Denis Zhdanov
 */
public class RichTextControlBuilder {
  
  private static final String RICH_TEXT_TOKEN_START = "{@";
  private static final String RICH_TEXT_TOKEN_END = "}";

  private final Map<String, RichTextProcessor> myProcessors = new HashMap<>();
  private final JPanel                                 myResult     = new JPanel(new GridBagLayout());
  private final Collection<JComponent>                 myComponents = new ArrayList<>();
  
  private Color myForegroundColor;
  private Color myBackgroundColor;
  private Font  myFont;

  public RichTextControlBuilder() {
    myForegroundColor = myResult.getForeground();
    myBackgroundColor = myResult.getBackground();
    myFont = myResult.getFont();

    registerProcessor(new RichTextActionProcessor());
  }

  /**
   * Defines target text to show by the UI component built by the current builder.
   * <p/>
   * The text can be 'reach', i.e. contain text like '{@value #RICH_TEXT_TOKEN_START}key data{@value #RICH_TEXT_TOKEN_END}'.
   * Then 'data' will be delivered to the {@link RichTextProcessor} registered for the
   * {@link RichTextProcessor#getKey() target key}. {@link RichTextProcessor#process(String) Returned component} (if any) will be
   * shown within the resulting UI control.
   * <p/>
   * For example, there is a predefined {@link RichTextActionProcessor} that resolves references to the actions (shows its icon at
   * the resulting component). 
   * 
   * @param text  target text to show by the resulting control
   * @throws IllegalArgumentException   if given text is malformed
   */
  public void setText(@NotNull String text) throws IllegalArgumentException {
    myResult.removeAll();
    myComponents.clear();
    List<JComponent> rowComponents = new ArrayList<>();
    RichTextProcessor metaDataProcessor = null;
    StringBuilder metaTokenData = new StringBuilder();
    boolean ignoreNext = false;
    for (String s : StringUtil.tokenize(new StringTokenizer(text, " \n", true))) {
      if (ignoreNext || s.isEmpty()) {
        ignoreNext = false;
        continue;
      }

      if (metaDataProcessor != null) {
        // Inside meta-token.
        final int i = s.indexOf(RICH_TEXT_TOKEN_END);
        if (i >= 0) {
          // Meta-token ends within the current string.
          metaTokenData.append(s, 0, i);
          final JComponent component = metaDataProcessor.process(metaTokenData.toString());
          if (component != null) {
            rowComponents.add(component);
          }
          metaTokenData.setLength(0);
          metaDataProcessor = null;
          if (i + RICH_TEXT_TOKEN_END.length() < s.length()) {
            s = s.substring(i + RICH_TEXT_TOKEN_END.length());
          }
          else {
            continue;
          } 
        }
        else {
          // Meta-token data continues here.
          metaTokenData.append(s);
          continue;
        }
      }
      
      final int start = s.indexOf(RICH_TEXT_TOKEN_START);

      // Check if meta data starts here.
      if (start >= 0) {
        if (start + RICH_TEXT_TOKEN_START.length() >= s.length()) {
          throw new IllegalArgumentException(String.format(
            "Invalid rich text detected. Meta data key is assumed to directly follow '%s' (no white spaces between them). "
            + "Given text: '%s'", RICH_TEXT_TOKEN_START, text));
        }

        // Define meta-key end offset.
        int end = s.indexOf(RICH_TEXT_TOKEN_END);
        boolean metaDataComplete = true;
        if (end < start) {
          end = s.length();
          metaDataComplete = false;
        }
        String metaKey = s.substring(start + RICH_TEXT_TOKEN_START.length(), end);
        metaDataProcessor = myProcessors.get(metaKey);
        if (metaDataProcessor == null) {
          throw new IllegalArgumentException(String.format(
            "No processor is registered for the meta-key '%s' (processors are available only for these keys - %s). Rich text: '%s'",
            metaKey, myProcessors.keySet(), text
          ));
        }

        // There is a possible case like {@key}. We can complete meta-processing here than.
        if (metaDataComplete) {
          final JComponent component = metaDataProcessor.process("");
          if (component != null) {
            rowComponents.add(component);
          }
          metaDataProcessor = null;
          if (end < s.length()) {
            // Handle situation like '{@key}text', i.e. there is no white space between the meta-data and the text that follows it.
            s = s.substring(end);
          }
          else {
            continue;
          }
        }
        else {
          ignoreNext = true;
          continue;
        }
      }
      if (s.contains("\n")) {
        addRow(rowComponents);
        rowComponents.clear();
      }
      else {
        final JLabel label = new JLabel(s);
        label.setForeground(myForegroundColor);
        label.setBackground(myBackgroundColor);
        label.setFont(myFont);
        rowComponents.add(label);
      }
    }
    if (!rowComponents.isEmpty()) {
      addRow(rowComponents);
    }
  }
  
  public void setForegroundColor(@NotNull Color foregroundColor) {
    myForegroundColor = foregroundColor;
  }

  public void setBackgroundColor(@NotNull Color backgroundColor) {
    myBackgroundColor = backgroundColor;
  }

  public void setFont(@NotNull Font font) {
    myFont = font;
  }

  /**
   * Registers given processor within the current builder, i.e. it will be
   * {@link RichTextProcessor#process(String) asked to process} meta data for the
   * {@link RichTextProcessor#getKey target key} (if any).
   * 
   * @param processor  processor to register
   */
  public void registerProcessor(@NotNull RichTextProcessor processor) {
    myProcessors.put(processor.getKey(), processor);
  }
  
  /**
   * @return    component built within the provided information
   */
  @NotNull
  public JComponent build() {
    for (JComponent component : myComponents) {
      component.setForeground(myForegroundColor);
      component.setBackground(myBackgroundColor);
      component.setFont(myFont);
    }
    myResult.setForeground(myForegroundColor);
    myResult.setBackground(myBackgroundColor);
    myResult.setFont(myFont);
    
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.weighty = 1;
    constraints.fill = GridBagConstraints.VERTICAL;
    myResult.add(Box.createVerticalStrut(1), constraints);
    return myResult;
  }
  
  private void addRow(@NotNull Collection<JComponent> rowComponents) {
    JPanel row = new MultiRowFlowPanel(FlowLayout.CENTER, 0, 3);
    row.setBackground(myBackgroundColor);
    myComponents.add(row);
    if (rowComponents.isEmpty()) {
      row.add(new JLabel(" "));
    }
    else {
      for (JComponent component : rowComponents) {
        row.add(component);
      }
    }
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets.top = 3;
    myResult.add(row, constraints);
  }

  /**
   * Encapsulates functionality for showing particular {@link RichTextControlBuilder#setText(String) rich text}.
   */
  public interface RichTextProcessor {

    /**
     * @return    flavor of the rich text that can be managed by the current processor
     */
    @NotNull
    String getKey();
    
    /**
     * Callback that receives target rich text data and returns UI control that represents it.
     * <p/>
     * For example, it can receive action id and return action's icon button.
     * 
     * @param text  target rich text
     * @return      UI control that represents given rich text in a way specific to the current processor (if any)
     */
    @Nullable
    JComponent process(@NotNull String text);
  }
}
