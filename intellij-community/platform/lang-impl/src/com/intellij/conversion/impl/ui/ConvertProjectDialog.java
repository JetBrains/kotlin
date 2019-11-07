// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.conversion.impl.ui;

import com.intellij.CommonBundle;
import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.impl.ConversionContextImpl;
import com.intellij.conversion.impl.ConversionRunner;
import com.intellij.conversion.impl.ProjectConversionUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public class ConvertProjectDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(ConvertProjectDialog.class);
  private JPanel myMainPanel;
  private JTextPane myTextPane;
  private boolean myConverted;
  private final ConversionContextImpl myContext;
  private final List<? extends ConversionRunner> myConversionRunners;
  private final File myBackupDir;
  private final Set<Path> myAffectedFiles;
  private boolean myNonExistingFilesMessageShown;

  public ConvertProjectDialog(ConversionContextImpl context, final List<? extends ConversionRunner> conversionRunners) {
    super(true);
    setTitle(IdeBundle.message("dialog.title.convert.project"));
    setModal(true);
    myContext = context;
    myConversionRunners = conversionRunners;
    myAffectedFiles = new HashSet<>();
    for (ConversionRunner conversionRunner : conversionRunners) {
      myAffectedFiles.addAll(conversionRunner.getAffectedFiles());
    }

    myBackupDir = ProjectConversionUtil.getBackupDir(context.getProjectBaseDir());
    myTextPane.setSize(new Dimension(350, Integer.MAX_VALUE));
    StringBuilder message = new StringBuilder();
    if (myConversionRunners.size() == 1 && myConversionRunners.get(0).getProvider().getConversionDialogText(context) != null) {
      message.append(myConversionRunners.get(0).getProvider().getConversionDialogText(context));
    }
    else {
      message.append(IdeBundle.message("conversion.dialog.text.1", context.getProjectFile().getName(),
                                       ApplicationNamesInfo.getInstance().getFullProductName()));
    }
    message.append(IdeBundle.message("conversion.dialog.text.2", myBackupDir.getAbsolutePath()));
    Messages.configureMessagePaneUi(myTextPane, XmlStringUtil.wrapInHtml(message), null);

    myTextPane.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          @NonNls StringBuilder descriptions = new StringBuilder("<html>The following conversions will be performed:<br>");
          for (ConversionRunner runner : conversionRunners) {
            descriptions.append(runner.getProvider().getConversionDescription()).append("<br>");
          }
          descriptions.append("</html>");
          Messages.showInfoMessage(descriptions.toString(), IdeBundle.message("dialog.title.convert.project"));
        }
      }
    });
    init();
    setOKButtonText("Convert");
  }

  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  @Override
  protected void doOKAction() {
    final List<Path> nonexistentFiles = myContext.getNonExistingModuleFiles();
    if (!nonexistentFiles.isEmpty() && !myNonExistingFilesMessageShown) {
      final String filesString = getFilesString(nonexistentFiles);
      final String message = IdeBundle.message("message.text.files.do.not.exist", filesString);
      final int res = Messages.showYesNoDialog(getContentPane(), message, IdeBundle.message("dialog.title.convert.project"), Messages.getQuestionIcon());
      if (res != Messages.YES) {
        super.doOKAction();
        return;
      }
      myNonExistingFilesMessageShown = false;
    }


    try {
      if (!checkReadOnlyFiles()) {
        return;
      }

      ProjectConversionUtil.backupFiles(myAffectedFiles, myContext.getProjectBaseDir(), myBackupDir);
      List<ConversionRunner> usedRunners = new ArrayList<>();
      for (ConversionRunner runner : myConversionRunners) {
        if (runner.isConversionNeeded()) {
          runner.preProcess();
          runner.process();
          runner.postProcess();
          usedRunners.add(runner);
        }
      }
      myContext.saveFiles(myAffectedFiles, usedRunners);
      myConverted = true;
      super.doOKAction();
    }
    catch (CannotConvertException | IOException e) {
      LOG.info(e);
      showErrorMessage(IdeBundle.message("error.cannot.convert.project", e.getMessage()));
    }
  }

  private static String getFilesString(List<? extends Path> files) {
    StringBuilder buffer = new StringBuilder();
    for (Path file : files) {
      buffer.append(file.toAbsolutePath().toString()).append("<br>");
    }
    return buffer.toString();
  }

  private boolean checkReadOnlyFiles() {
    List<Path> files = getReadOnlyFiles();
    if (!files.isEmpty()) {
      final String message = IdeBundle.message("message.text.unlock.read.only.files",
                                               ApplicationNamesInfo.getInstance().getFullProductName(),
                                               getFilesString(files));
      final String[] options = {CommonBundle.getContinueButtonText(), CommonBundle.getCancelButtonText()};
      if (Messages.showOkCancelDialog(myMainPanel, message, IdeBundle.message("dialog.title.convert.project"), options[0], options[1], null) != Messages.OK) {
        return false;
      }
      unlockFiles(files);

      files = getReadOnlyFiles();
      if (!files.isEmpty()) {
        showErrorMessage(IdeBundle.message("error.message.cannot.make.files.writable", getFilesString(files)));
        return false;
      }
    }
    return true;
  }

  private List<Path> getReadOnlyFiles() {
    return ConversionRunner.getReadOnlyFiles(myAffectedFiles);
  }

  private static void unlockFiles(final List<? extends Path> files) {
    for (Path file : files) {
      FileUtil.setReadOnlyAttribute(file.toAbsolutePath().toString(), false);
    }
  }

  private void showErrorMessage(final String message) {
    Messages.showErrorDialog(myMainPanel, message, IdeBundle.message("dialog.title.convert.project"));
  }

  public boolean isConverted() {
    return myConverted;
  }
}
