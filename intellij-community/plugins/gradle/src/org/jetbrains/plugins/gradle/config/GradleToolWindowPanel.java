package org.jetbrains.plugins.gradle.config;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettingsListener;
import org.jetbrains.plugins.gradle.ui.RichTextControlBuilder;
import org.jetbrains.plugins.gradle.util.GradleBundle;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * Base class for high-level Gradle GUI controls used at the Gradle tool window. The basic idea is to encapsulate the same features in
 * this class and allow to extend it via {@code Template Method} pattern. The shared features are listed below:
 * <pre>
 * <ul>
 *   <li>provide common actions at the toolbar;</li>
 *   <li>show info control when no gradle project is linked to the current IntelliJ IDEA project;</li>
 * </ul>
 * </pre>
 * <p/>
 * Not thread-safe.
 * 
 * @author Denis Zhdanov
 */
public abstract class GradleToolWindowPanel extends SimpleToolWindowPanel {

  private static final String NON_LINKED_CARD_NAME   = "NON_LINKED";
  private static final String CONTENT_CARD_NAME      = "CONTENT";
  private static final String TOOL_WINDOW_TOOLBAR_ID = "Gradle.ChangeActionsToolbar";

  /** Show info control when no gradle project is linked, using {@link CardLayout} for that. */
  private final CardLayout myLayout  = new CardLayout();
  /** Top-level container, managed by the card layout. */
  private final JPanel     myContent = new JPanel(myLayout);

  private final Project myProject;
  private final String  myPlace;

  protected GradleToolWindowPanel(@NotNull Project project, @NotNull String place) {
    super(true);
    myProject = project;
    myPlace = place;
    setContent(myContent);

    MessageBusConnection connection = project.getMessageBus().connect(project);
    connection.subscribe(GradleSettingsListener.TOPIC, new GradleSettingsListenerAdapter() {
      // TODO den implement
//      @Override public void onLinkedProjectConfigChange(@Nullable String oldPath, @Nullable String newPath) {
//        if (StringUtil.isEmpty(newPath)) {
//          myLayout.show(myContent, NON_LINKED_CARD_NAME);
//          return;
//        }
//        if (StringUtil.isEmpty(oldPath) && !StringUtil.isEmpty(newPath)) {
//          myLayout.show(myContent, CONTENT_CARD_NAME);
//        }
//      }
    });
  }

  public void initContent() {
    final ActionManager actionManager = ActionManager.getInstance();
    final ActionGroup actionGroup = (ActionGroup)actionManager.getAction(TOOL_WINDOW_TOOLBAR_ID);
    ActionToolbar actionToolbar = actionManager.createActionToolbar(myPlace, actionGroup, true);
    JPanel toolbarControl = new JPanel(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    toolbarControl.add(actionToolbar.getComponent(), constraints);
    for (JComponent component : getToolbarControls()) {
      component.setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
      toolbarControl.add(component, constraints);
    }
    setToolbar(toolbarControl);
    
    final JComponent payloadControl = buildContent();
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(payloadControl);
    JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
    myContent.add(scrollPane, CONTENT_CARD_NAME);
    RichTextControlBuilder builder = new RichTextControlBuilder();
    builder.setBackgroundColor(payloadControl.getBackground());
    builder.setForegroundColor(UIUtil.getInactiveTextColor());
    builder.setFont(payloadControl.getFont());
    builder.setText(GradleBundle.message("gradle.toolwindow.text.no.linked.project"));
    final JComponent noLinkedProjectControl = builder.build();
    myContent.add(noLinkedProjectControl, NON_LINKED_CARD_NAME);
    update();
  }

  /**
   * @return    list of UI controls to be displayed vertically at the toolbar
   */
  @NotNull
  protected List<JComponent> getToolbarControls() {
    return Collections.emptyList();
  }
  
  /**
   * Asks current control to update its state.
   */
  public void update() {
    final GradleSettings settings = GradleSettings.getInstance(myProject);
    // TODO den implement
    String cardToShow = "sf";
//    String cardToShow = StringUtil.isEmpty(settings.getLinkedExternalProjectPath()) ? NON_LINKED_CARD_NAME : CONTENT_CARD_NAME;
    myLayout.show(myContent, cardToShow);
    boolean showToolbar = cardToShow != NON_LINKED_CARD_NAME;
    for (JComponent component : getToolbarControls()) {
      component.setVisible(showToolbar);
    }
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  /**
   * @return    GUI control to be displayed at the current tab
   */
  @NotNull
  protected abstract JComponent buildContent();
  
  /**
   * Callback for asking content control to update its state.
   */
  protected abstract void updateContent();
}
