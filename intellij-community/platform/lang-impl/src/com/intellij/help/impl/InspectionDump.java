// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.help.impl;

import com.intellij.codeInspection.ex.InspectionToolRegistrar;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;

public class InspectionDump implements ApplicationStarter {
  @Override
  public String getCommandName() {
    return "inspections";
  }

  @Override
  public void main(@NotNull String[] args) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.newDocument();
      Element inspections = document.createElement("Inspections");
      document.appendChild(inspections);
      List<InspectionToolWrapper> tools = InspectionToolRegistrar.getInstance().createTools();
      for (InspectionToolWrapper tool : tools) {
        Element inspection = document.createElement("Inspection");
        inspection.setAttribute("groupPath", StringUtil.join(tool.getGroupPath(), ";"));
        inspection.setAttribute("group", tool.getGroupDisplayName());
        inspection.setAttribute("name", tool.getDisplayName());
        inspection.setAttribute("level", tool.getDefaultLevel().getName());
        if (tool.getLanguage() != null) {
          inspection.setAttribute("language", tool.getLanguage());
        }
        Element description = document.createElement("description");
        CDATASection descriptionSection = document.createCDATASection(escapeCDATA(tool.loadDescription()));
        description.appendChild(descriptionSection);
        inspection.appendChild(description);
        inspections.appendChild(inspection);
      }

      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      DOMSource source = new DOMSource(document);
      final String path = args.length == 2 ? args[1] : PathManager.getHomePath() + File.separator + "AllInspections.xml";
      StreamResult console = new StreamResult(new File(path));
      transformer.transform(source, console);

      System.exit(0);
    }
    catch (ParserConfigurationException | TransformerException e) {
      // noinspection CallToPrintStackTrace
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static String escapeCDATA(String cData) {
    return cData.replaceAll("\\]", "&#x005D;").replaceAll("\\[", "&#x005B;");
  }
}
