// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ex;

import com.intellij.codeInspection.InspectionApplication;
import com.intellij.codeInspection.InspectionsReportConverter;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.URLUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @author Roman.Chernyatchik
 */
public class PlainTextFormatter implements InspectionsReportConverter {
  public static final String NAME = "plain";
  private static final String FILE_ELEMENT = "file";
  private static final String LINE_ELEMENT = "line";
  private static final String PROBLEM_ELEMENT = "problem";
  private static final String DESCRIPTION_ELEMENT = "description";
  private static final String PROBLEM_CLASS_ELEMENT = "problem_class";
  private static final String SEVERITY_ATTRIBUTE = "severity";

  @Override
  public String getFormatName() {
    return NAME;
  }

  @Override
  public boolean useTmpDirForRawData() {
    return true;
  }

  @Override
  public void convert(@NotNull final String rawDataDirectoryPath,
                      @Nullable final String outputPath,
                      @NotNull final Map<String, Tools> tools,
                      @NotNull final List<? extends File> inspectionsResults) throws ConversionException {

    final SAXTransformerFactory transformerFactory = (SAXTransformerFactory)TransformerFactory.newInstance();

    final URL descrExtractorXsltUrl = getClass().getResource("description-text.xsl");
    final Source xslSource;
    final Transformer transformer;
    try {
      xslSource = new StreamSource(URLUtil.openStream(descrExtractorXsltUrl));
      transformer = transformerFactory.newTransformer(xslSource);
    }
    catch (IOException e) {
      throw new ConversionException("Cannot find inspection descriptions converter.");
    }
    catch (TransformerConfigurationException e) {
      throw new ConversionException("Fail to load inspection descriptions converter.");
    }


    // write to file/stdout:
    final Writer w;
    if (outputPath != null) {
      final File outputFile = new File(outputPath);
      try {
        w = new FileWriter(outputFile);
      }
      catch (IOException e) {
        throw new ConversionException("Cannot edit file: " + outputFile.getPath());
      }
    }
    else {
      w = new PrintWriter(System.out);
    }

    try {
      for (File inspectionData : inspectionsResults) {
        if (inspectionData.isDirectory()) {
          warn("Folder isn't expected here: " + inspectionData.getName());
          continue;
        }
        final String fileNameWithoutExt = FileUtilRt.getNameWithoutExtension(inspectionData.getName());
        if (InspectionApplication.DESCRIPTIONS.equals(fileNameWithoutExt)) {
          continue;
        }

        InspectionToolWrapper toolWrapper = tools.get(fileNameWithoutExt).getTool();

        // Tool name and group
        w.append(getToolPresentableName(toolWrapper)).append("\n");

        // Description is HTML based, need to be converted in plain text
        writeInspectionDescription(w, toolWrapper, transformer);

        // separator before file list
        w.append("\n");

        // parse xml and output results
        final SAXBuilder builder = new SAXBuilder();

        try {
          final Document doc = builder.build(inspectionData);
          final Element root = doc.getRootElement();

          final List problems = root.getChildren(PROBLEM_ELEMENT);

          // let's count max file path & line_number length to align problem descriptions
          final int maxFileColonLineLength = getMaxFileColonLineNumLength(inspectionData, toolWrapper, problems);

          for (Object problem : problems) {
            // Format:
            //   file_path:line_num   [severity] problem description

            final Element fileElement = ((Element)problem).getChild(FILE_ELEMENT);
            final String filePath = getPath(fileElement);

            // skip suppressed results
            if (resultsIgnored(inspectionData, toolWrapper)) {
              continue;
            }

            final Element lineElement = ((Element)problem).getChild(LINE_ELEMENT);
            final Element problemDescrElement = ((Element)problem).getChild(DESCRIPTION_ELEMENT);
            final String severity = ((Element)problem).getChild(PROBLEM_CLASS_ELEMENT).getAttributeValue(SEVERITY_ATTRIBUTE);

            final String fileLineNum = lineElement.getText();
            w.append("  ").append(filePath).append(':').append(fileLineNum);
            // align descriptions
            for (int i = maxFileColonLineLength - 1 - filePath.length() - fileLineNum.length() + 4; i >= 0; i--) {
              w.append(' ');
            }
            w.append("[").append(severity).append("] ");
            w.append(problemDescrElement.getText()).append('\n');
          }
        }
        catch (JDOMException e) {
          throw new ConversionException("Unknown results format, file = " + inspectionData.getPath() + ". Error: " + e.getMessage());
        }

        // separator between neighbour inspections
        w.append("\n");
      }
    }
    catch (IOException e) {
      throw new ConversionException("Cannot write inspection results: " + e.getMessage());
    } finally {
      if (w != null) {
        try {
          w.close();
        }
        catch (IOException e) {
          warn("Cannot save inspection results: " + e.getMessage());
        }
      }
    }
  }

  private int getMaxFileColonLineNumLength(@NotNull final File inspectionResultData,
                                           @NotNull final InspectionToolWrapper toolWrapper,
                                           @NotNull final List problems) {
    int maxFileColonLineLength = 0;
    for (Object problem : problems) {
      final Element fileElement = ((Element)problem).getChild(FILE_ELEMENT);
      final Element lineElement = ((Element)problem).getChild(LINE_ELEMENT);

      final String filePath = getPath(fileElement);
      // skip suppressed results
      if (resultsIgnored(inspectionResultData, toolWrapper)) {
        continue;
      }

      maxFileColonLineLength = Math.max(maxFileColonLineLength, filePath.length() + 1 + lineElement.getText().length());
    }
    return maxFileColonLineLength;
  }

  private void warn(String msg) {
    System.err.println(msg);
  }

  private boolean resultsIgnored(@NotNull final File file,
                                 @NotNull final InspectionToolWrapper toolWrapper) {
    // TODO: check according to config
    return false;
  }

  @NotNull
  protected String getPath(@NotNull final Element fileElement) {
    return fileElement.getText().replace("file://$PROJECT_DIR$", ".");
  }

  protected void writeInspectionDescription(@NotNull final Writer w,
                                            @NotNull final InspectionToolWrapper toolWrapper,
                                            @NotNull final Transformer transformer)
    throws IOException, ConversionException {

    final StringWriter descrWriter = new StringWriter();
    String descr = toolWrapper.loadDescription();
    if (descr == null) {
      return;
    }
    // convert line ends to xml form
    descr = descr.replace("<br>", "<br/>");

    try {

      transformer.transform(new StreamSource(new StringReader(descr)), new StreamResult(descrWriter));
    }
    catch (TransformerException e) {
      // Not critical problem, just inspection error cannot be loaded
      warn("ERROR:  Cannot load description for inspection: " + getToolPresentableName(toolWrapper) + ".\n        Error message: " + e.getMessage());
      return;
    }

    final String trimmedDesc = descrWriter.toString().trim();
    final String[] descLines = StringUtil.splitByLines(trimmedDesc);
    if (descLines.length > 0) {
      for (String descLine : descLines) {
        w.append("  ").append(descLine.trim()).append("\n");
      }
    }
  }

  @NotNull
  protected String getToolPresentableName(@NotNull final InspectionToolWrapper toolWrapper) throws IOException {
    final StringBuilder buff = new StringBuilder();

    // inspection name
    buff.append(toolWrapper.getDisplayName()).append(" (");

    // group name
    final String[] groupPath = toolWrapper.getGroupPath();
    for (int i = 0, groupPathLength = groupPath.length; i < groupPathLength; i++) {
      if (i != 0) {
        buff.append(" | ");
      }
      buff.append(groupPath[i]);
    }
    buff.append(")");

    return buff.toString();
  }
}
