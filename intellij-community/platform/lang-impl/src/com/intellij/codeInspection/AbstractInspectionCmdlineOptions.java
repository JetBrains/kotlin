package com.intellij.codeInspection;

import com.intellij.codeInspection.ex.PlainTextFormatter;
import com.intellij.openapi.diagnostic.Logger;
import com.sampullara.cli.Args;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author Roman.Chernyatchik
 */
public abstract class AbstractInspectionCmdlineOptions implements InspectionToolCmdlineOptions {
  private static final Logger LOG = Logger.getInstance(AbstractInspectionCmdlineOptions.class);

  @Nullable
  protected abstract String getProfileNameOrPathProperty();
  @Nullable
  protected abstract String getProjectPathProperty();
  @Nullable
  protected abstract String getOutputPathProperty();
  @Nullable
  protected abstract String getDirToInspectProperty();
  @Nullable
  protected abstract String getOutputFormatProperty();
  @Nullable
  protected abstract String getXSLTSchemePathProperty();
  @Nullable
  protected abstract Boolean getErrorCodeRequiredProperty();
  @Nullable
  protected abstract Boolean getRunWithEditorSettingsProperty();

  @NotNull
  protected abstract String[] optionsBanner();

  @Override
  public void initApplication(InspectionApplication app) {
    app.myHelpProvider = this;
    app.myProjectPath = determineProjectPath();
    app.myProfileName = getProfileNameOrPathProperty();
    app.myOutPath = determineOutputPath();
    app.mySourceDirectory = determineDirectoryToInspect(app.myProjectPath);
    app.setVerboseLevel(getVerboseLevelProperty());

    final Boolean errorCodeRequired = getErrorCodeRequiredProperty();
    if (errorCodeRequired != null) {
      app.myErrorCodeRequired = errorCodeRequired;
    }
    final Boolean runWithEditorSettings = getRunWithEditorSettingsProperty();
    if (runWithEditorSettings != null) {
      app.myRunWithEditorSettings = runWithEditorSettings;
    }

    final String xsltSchemePath = getXSLTSchemePathProperty();
    if (xsltSchemePath != null) {
      app.myOutputFormat = xsltSchemePath;
    } else {
      final String outputFormat = getOutputFormatProperty();
      if (outputFormat != null) {
        app.myOutputFormat = outputFormat.toLowerCase();
      }
    }
  }

  @Override
  public void validate() throws CmdlineArgsValidationException {
    // project path
    final String projectPath = determineProjectPath();
    if (projectPath == null) {
      throw new CmdlineArgsValidationException("Project not found.");
    }
    else if (!new File(projectPath).exists()) {
      throw new CmdlineArgsValidationException("Project '" + projectPath + "' not found.");
    }

    // Dir to inspect: check only if set
    if (getDirToInspectProperty() != null) {
      final String dirToInspect = determineDirectoryToInspect(projectPath);
      if (dirToInspect == null) {
        throw new CmdlineArgsValidationException("Directory '" + dirToInspect + "' not found.");
      }
    }

    final String xsltSchemePath = getXSLTSchemePathProperty();
    if (xsltSchemePath != null) {
      final File xsltScheme = new File(xsltSchemePath);
      if (!xsltScheme.exists()) {
        throw new CmdlineArgsValidationException("XSLT scheme '" + xsltSchemePath + "' not found.");
      }
    }

    final String outputFormat = getOutputFormatProperty();
    if (outputFormat != null) {
      StringBuilder builder = new StringBuilder();
      for (InspectionsReportConverter converter : InspectionsReportConverter.EP_NAME.getExtensions()) {
        final String converterFormat = converter.getFormatName();
        if (outputFormat == converterFormat) {
          builder = null;
          break;
        } else {
          if (builder.length() != 0) {
            builder.append(", ");
          }
          builder.append(converterFormat);
        }
      }
      // report error if converter isn't registered.
      if (builder != null) {
        throw new CmdlineArgsValidationException("Unsupported format option '" + outputFormat + "'. Please use one of: " + builder.toString());
      }
    }
  }

  @Nullable
  protected String determineOutputPath() {
    final String outputPathProperty = getOutputPathProperty();

    // if plain formatter and output path not specified - use STDOUT
    // otherwise specified output path or a default one
    return outputPathProperty != null ? outputPathProperty
                                      : getOutputFormatProperty() == PlainTextFormatter.NAME ? null
                                                                                             : getDefaultOutputPath();
  }

  @Override
  public void printHelpAndExit() {
    final String[] bannerLines = optionsBanner();
    for (String line : bannerLines) {
      System.out.println(line);
    }
    Args.usage(this);
    System.exit(1);
  }

  @Nullable
  protected String determineProjectPath() {
    final String projectPath = getProjectPathProperty();
    return projectPath != null ? projectPath : getDefaultProjectPath();
  }

  @Nullable
  protected String determineDirectoryToInspect(@Nullable final String projectPath) {
    final String dirToInspect = getDirToInspectProperty();

    if (dirToInspect == null) {
      return null;
    }

    try {
      // 1. By relative path
      final File relativePathFile = new File(projectPath + File.separatorChar + dirToInspect);
      if (relativePathFile.exists()) {
        return relativePathFile.getCanonicalPath();
      }

      // 2. try absolute path
      final File absPathFile = new File(dirToInspect);
      if (absPathFile.exists()) {
        return absPathFile.getCanonicalPath();
      }
    }
    catch (IOException e) {
      LOG.warn(e);
    }
    return null;
  }

  protected String getDefaultOutputPath() {
    return getOutputPathProperty() + "/results";
  }

  @Nullable
  protected String getDefaultProjectPath() {
    return null;
  }
}
