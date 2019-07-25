/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.execution.test.runner.events;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

/**
 * {@link TestEventXmlXPathView} utility class for query of XML using XPath expressions.
 *
 * @author Vladislav.Soroka
 */
public class TestEventXmlXPathView implements TestEventXmlView {
  private final XPath xpath;
  private final Document xmlDocument;

  /**
   * Parses the given XML string as a DOM document, using the JDK parser. The parser does not
   * validate, and is namespace aware.
   *
   * @param xml            the XML content to be parsed (must be well formed)
   * @throws XmlParserException
   */
  public TestEventXmlXPathView(String xml) throws XmlParserException {
    xpath = XPathFactory.newInstance().newXPath();
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      InputSource is = new InputSource(new StringReader(xml));
      factory.setNamespaceAware(false);
      factory.setValidating(false);
      xmlDocument = factory.newDocumentBuilder().parse(is);
    }
    catch (Exception ex) {
      throw new XmlParserException(ex);
    }
  }

  @NotNull
  @Override
  public String getTestEventType() throws TestEventXmlXPathView.XmlParserException {
    return queryXml("/ijLog/event/@type");
  }

  @NotNull
  @Override
  public String getTestName() throws XmlParserException {
    return queryXml("/ijLog/event/test/descriptor/@name");
  }

  @NotNull
  @Override
  public String getTestParentId() throws XmlParserException {
    return queryXml("/ijLog/event/test/@parentId");
  }

  @NotNull
  @Override
  public String getTestId() throws XmlParserException {
    return queryXml("/ijLog/event/test/@id");
  }

  @NotNull
  @Override
  public String getTestClassName() throws XmlParserException {
    return queryXml("/ijLog/event/test/descriptor/@className");
  }

  @NotNull
  @Override
  public String getTestEventResultType() throws XmlParserException {
    return queryXml("/ijLog/event/test/result/@resultType");
  }

  @NotNull
  @Override
  public String getEventTitle() throws XmlParserException {
    return queryXml("/ijLog/event/title");
  }

  @Override
  public boolean isEventOpenSettings() throws XmlParserException {
    return Boolean.parseBoolean(queryXml("/ijLog/event/@openSettings"));
  }

  @NotNull
  @Override
  public String getEventMessage() throws XmlParserException {
    return queryXml("/ijLog/event/message");
  }

  @NotNull
  @Override
  public String getTestEventTest() throws XmlParserException {
    return queryXml("/ijLog/event/test/event");
  }

  @NotNull
  @Override
  public String getTestEventTestDescription() throws XmlParserException {
    return queryXml("/ijLog/event/test/event/@destination");
  }

  @NotNull
  @Override
  public String getEventTestReport() throws XmlParserException {
    return queryXml("/ijLog/event/@testReport");
  }

  @NotNull
  @Override
  public String getEventTestResultActualFilePath() throws XmlParserException {
    return queryXml("/ijLog/event/test/result/actualFilePath");
  }

  @NotNull
  @Override
  public String getEventTestResultFilePath() throws XmlParserException {
    return queryXml("/ijLog/event/test/result/filePath");
  }

  @NotNull
  @Override
  public String getEventTestResultExpected() throws XmlParserException {
    return queryXml("/ijLog/event/test/result/expected");
  }

  @NotNull
  @Override
  public String getEventTestResultActual() throws XmlParserException {
    return queryXml("/ijLog/event/test/result/actual");
  }

  @NotNull
  @Override
  public String getEventTestResultFailureType() throws XmlParserException {
    return queryXml("/ijLog/event/test/result/failureType");
  }

  @NotNull
  @Override
  public String getEventTestResultStackTrace() throws XmlParserException {
    return queryXml("/ijLog/event/test/result/stackTrace");
  }

  @NotNull
  @Override
  public String getEventTestResultErrorMsg() throws XmlParserException {
    return queryXml("/ijLog/event/test/result/errorMsg");
  }

  @NotNull
  @Override
  public String getEventTestResultEndTime() throws XmlParserException {
    return queryXml("/ijLog/event/test/result/@endTime");
  }

  @NotNull
  @Override
  public String getEventTestResultStartTime() throws XmlParserException {
    return queryXml("/ijLog/event/test/result/@startTime");
  }

  @NotNull
  private String queryXml(final String xpathExpr) throws XmlParserException {
    try {
      return xmlDocument == null ? "" : xpath.evaluate(xpathExpr, xmlDocument);
    }
    catch (XPathExpressionException ex) {
      throw new XmlParserException(ex);
    }
  }
}
