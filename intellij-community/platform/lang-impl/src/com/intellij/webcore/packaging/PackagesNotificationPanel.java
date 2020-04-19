package com.intellij.webcore.packaging;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.util.ui.SwingHelper;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yole
 */
public class PackagesNotificationPanel {
  private final JEditorPane myHtmlViewer;
  private final Map<String, Runnable> myLinkHandlers = new HashMap<>();
  private String myErrorTitle;
  private PackageManagementService.ErrorDescription myErrorDescription;

  public PackagesNotificationPanel() {
    myHtmlViewer = SwingHelper.createHtmlViewer(true, null, null, null);
    myHtmlViewer.setVisible(false);
    myHtmlViewer.setOpaque(true);
    myHtmlViewer.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        final Runnable handler = myLinkHandlers.get(e.getDescription());
        if (handler != null) {
          handler.run();
        }
        else if (myErrorTitle != null && myErrorDescription != null) {
          showError(myErrorTitle, myErrorDescription);
        }
      }
    });
  }

  public static void showError(@NotNull @NlsContexts.DialogTitle String title,
                               @NotNull PackageManagementService.ErrorDescription description) {
    final PackagingErrorDialog dialog = new PackagingErrorDialog(title, description);
    dialog.show();
  }

  public void showResult(String packageName, @Nullable PackageManagementService.ErrorDescription errorDescription) {
    if (errorDescription == null) {
      String message = IdeBundle.message("package.installed.successfully");
      if (packageName != null) {
        message = IdeBundle.message("package.0.installed.successfully", packageName);
      }
      showSuccess(message);
    }
    else {
      String title = IdeBundle.message("failed.to.install.packages.dialog.title");
      if (packageName != null) {
        title = IdeBundle.message("failed.to.install.package.dialog.title", packageName);
      }
      String firstLine = IdeBundle.message("install.package.failure", packageName);
      showError(firstLine + "<a href=\"xxx\">" + IdeBundle.message("install.packages.failure.details") + "</a>", title,
                errorDescription);
    }
  }

  public void addLinkHandler(String key, Runnable handler) {
    myLinkHandlers.put(key, handler);
  }

  public void removeAllLinkHandlers() {
    myLinkHandlers.clear();
  }

  public JComponent getComponent() {
    return myHtmlViewer;
  }

  public void showSuccess(String text) {
    showContent(text, MessageType.INFO.getPopupBackground());
  }

  private void showContent(@NotNull String text, @NotNull Color background) {
    String htmlText = text.startsWith("<html>") ? text : UIUtil.toHtml(text);
    myHtmlViewer.setText(htmlText);
    myHtmlViewer.setBackground(background);
    setVisibleEditorPane(true);
    myErrorTitle = null;
    myErrorDescription = null;
  }

  public void showError(String text,
                        @Nullable @NlsContexts.DialogTitle String detailsTitle,
                        PackageManagementService.ErrorDescription errorDescription) {
    showContent(text, MessageType.ERROR.getPopupBackground());
    myErrorTitle = detailsTitle;
    myErrorDescription = errorDescription;
  }

  public void showWarning(String text) {
    showContent(text, MessageType.WARNING.getPopupBackground());
  }

  public void hide() {
    setVisibleEditorPane(false);
  }

  private void setVisibleEditorPane(boolean visible) {
    boolean oldVisible = myHtmlViewer.isVisible();
    myHtmlViewer.setVisible(visible);
    if (oldVisible != visible) {
      myHtmlViewer.revalidate();
      myHtmlViewer.repaint();
    }
  }

  public boolean hasLinkHandler(String key) {
    return myLinkHandlers.containsKey(key);
  }

  public void removeLinkHandler(String key) {
    myLinkHandlers.remove(key);
  }
}
