// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.newclass;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Trinity;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.BooleanFunction;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import static com.intellij.ide.ui.laf.darcula.DarculaUIUtil.Outline;

public class CreateWithTemplatesDialogPanel extends JBPanel implements Disposable {

  private final ExtendableTextField myNameField;
  private final JList<Trinity<String, Icon, String>> myTemplatesList;

  private JBPopup myErrorPopup;
  private RelativePoint myErrorShowPoint;

  private Consumer<? super InputEvent> myApplyAction;

  public CreateWithTemplatesDialogPanel(@NotNull List<Trinity<String, Icon, String>> templates, @Nullable String selectedItem) {
    super(new BorderLayout());
    setBackground(JBUI.CurrentTheme.NewClassDialog.panelBackground());

    myNameField = createNameField();
    myTemplatesList = createTemplatesList(templates);
    myErrorShowPoint = new RelativePoint(myNameField, new Point(0, myNameField.getHeight()));

    ScrollingUtil.installMoveUpAction(myTemplatesList, myNameField);
    ScrollingUtil.installMoveDownAction(myTemplatesList, myNameField);

    selectTemplate(selectedItem);
    add(myNameField, BorderLayout.NORTH);

    if (templates.size() > 1) {
      JBScrollPane scrollPane = new JBScrollPane(myTemplatesList);
      scrollPane.setBorder(JBUI.Borders.empty());
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      Box listHolder = new Box(BoxLayout.Y_AXIS);
      listHolder.setBorder(JBUI.Borders.emptyTop(JBUI.CurrentTheme.NewClassDialog.fieldsSeparatorWidth()));
      listHolder.add(scrollPane);
      add(listHolder, BorderLayout.CENTER);
    }
  }

  public JTextField getNameField() {
    return myNameField;
  }

  @NotNull
  public String getEnteredName() {
    return myNameField.getText().trim();
  }

  @NotNull
  public String getSelectedTemplate() {
    return myTemplatesList.getSelectedValue().third;
  }

  public void setError(String error) {
    myNameField.putClientProperty("JComponent.outline", error != null ? "error" : null);

    if (myErrorPopup != null && !myErrorPopup.isDisposed()) Disposer.dispose(myErrorPopup);
    if (error == null) return;

    ComponentPopupBuilder popupBuilder = ComponentValidator.createPopupBuilder(new ValidationInfo(error, myNameField), errorHint -> {
      Insets insets = myNameField.getInsets();
      Dimension hintSize = errorHint.getPreferredSize();
      Point point = new Point(0, insets.top - JBUIScale.scale(6) - hintSize.height);
      myErrorShowPoint = new RelativePoint(myNameField, point);
    }).setCancelOnWindowDeactivation(false)
      .setCancelOnClickOutside(true)
      .addUserData("SIMPLE_WINDOW");

    myErrorPopup = popupBuilder.createPopup();
    myErrorPopup.show(myErrorShowPoint);
  }

  public void setApplyAction(@NotNull Consumer<? super InputEvent> applyAction) {
    myApplyAction = applyAction;
  }

  @Override
  public void dispose() {
    if (myErrorPopup != null && !myErrorPopup.isDisposed()) Disposer.dispose(myErrorPopup);
  }

  @NotNull
  private ExtendableTextField createNameField() {
    ExtendableTextField res = new ExtendableTextField();

    Dimension minSize = res.getMinimumSize();
    Dimension prefSize = res.getPreferredSize();
    minSize.height = JBUIScale.scale(28);
    prefSize.height = JBUIScale.scale(28);
    res.setMinimumSize(minSize);
    res.setPreferredSize(prefSize);
    res.setColumns(30);

    Border border = JBUI.Borders.customLine(JBUI.CurrentTheme.NewClassDialog.bordersColor(), 1, 0, 1, 0);
    Border errorBorder = new ErrorBorder(res.getBorder());
    res.setBorder(JBUI.Borders.merge(border, errorBorder, false));
    res.setBackground(JBUI.CurrentTheme.NewClassDialog.searchFieldBackground());

    res.putClientProperty("StatusVisibleFunction", (BooleanFunction<JBTextField>) field -> field.getText().isEmpty());
    res.getEmptyText().setText(IdeBundle.message("action.create.new.class.name.field"));
    res.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          if (myApplyAction != null) myApplyAction.consume(e);
        }
      }
    });

    res.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        setError(null);
      }
    });

    return res;
  }

  @NotNull
  private JBList<Trinity<String, Icon, String>> createTemplatesList(@NotNull List<Trinity<String, Icon, String>> templates) {
    JBList<Trinity<String, Icon, String>> list = new JBList<>(templates);
    MouseAdapter mouseListener = new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (myApplyAction != null && e.getClickCount() > 1) myApplyAction.consume(e);
      }
    };

    list.addMouseListener(mouseListener);
    list.setCellRenderer(LIST_RENDERER);
    list.setFocusable(false);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    list.addListSelectionListener(e -> {
      Trinity<String, Icon, String> selectedValue = list.getSelectedValue();
      if (selectedValue != null) {
        setTextFieldIcon(selectedValue.second);
      }
    });

    Border border = JBUI.Borders.merge(
      JBUI.Borders.emptyLeft(JBUIScale.scale(5)),
      JBUI.Borders.customLine(JBUI.CurrentTheme.NewClassDialog.bordersColor(), 1, 0, 0, 0),
      true
    );
    list.setBorder(border);
    return list;
  }

  private void setTextFieldIcon(Icon icon) {
    myNameField.setExtensions(new TemplateIconExtension(icon));
    myNameField.repaint();
  }

  private void selectTemplate(@Nullable String selectedItem) {
    if (selectedItem == null) {
      myTemplatesList.setSelectedIndex(0);
      return;
    }

    ListModel<Trinity<String, Icon, String>> model = myTemplatesList.getModel();
    for (int i = 0; i < model.getSize(); i++) {
      String templateID = model.getElementAt(i).getThird();
      if (selectedItem.equals(templateID)) {
        myTemplatesList.setSelectedIndex(i);
        return;
      }
    }
  }

  private static final ListCellRenderer<Trinity<String, Icon, String>> LIST_RENDERER =
    new ListCellRenderer<Trinity<String, Icon, String>>() {

      private final ListCellRenderer<Trinity<String, Icon, String>> delegateRenderer =
        SimpleListCellRenderer.create((label, value, index) -> {
          if (value != null) {
            label.setText(value.first);
            label.setIcon(value.second);
          }
        });

      @Override
      public Component getListCellRendererComponent(JList<? extends Trinity<String, Icon, String>> list,
                                                    Trinity<String, Icon, String> value,
                                                    int index,
                                                    boolean isSelected,
                                                    boolean cellHasFocus) {
        JComponent delegate = (JComponent) delegateRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        delegate.setBorder(JBUI.Borders.empty(JBUIScale.scale(3), JBUIScale.scale(1)));
        return delegate;
      }
    };

  private static class TemplateIconExtension implements ExtendableTextComponent.Extension {
    private final Icon icon;

    private TemplateIconExtension(Icon icon) {this.icon = icon;}

    @Override
    public Icon getIcon(boolean hovered) {
      return icon;
    }

    @Override
    public boolean isIconBeforeText() {
      return true;
    }
  }

  private static class ErrorBorder implements Border {
    private final Border errorDelegateBorder;

    private ErrorBorder(Border delegate) {errorDelegateBorder = delegate;}

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      if (checkError(c)) {
        errorDelegateBorder.paintBorder(c, g, x, y, width, height);
      }
    }

    @Override
    public Insets getBorderInsets(Component c) {
      return checkError(c) ? errorDelegateBorder.getBorderInsets(c) : JBUI.emptyInsets();
    }

    @Override
    public boolean isBorderOpaque() {
      return false;
    }

    private static boolean checkError(Component c) {
      Object outlineObj = ((JComponent)c).getClientProperty("JComponent.outline");
      if (outlineObj == null) return false;

      Outline outline = outlineObj instanceof Outline ? (Outline) outlineObj : Outline.valueOf(outlineObj.toString());
      return outline == Outline.error || outline == Outline.warning;
    }
  }
}
