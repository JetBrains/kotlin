// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner.events;

import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.jetbrains.annotations.NotNull;

import java.io.StringReader;

public class TestEventXPPXmlView implements TestEventXmlView {
  private static final HierarchicalStreamDriver DRIVER = new XppDriver();

  private String myTestEventType;
  private String myTestParentId;
  private String myTestId;
  private String myTestClassName;
  private String myTestEventResultType;
  private String myEventTitle;
  private String myEventOpenSettings;
  private String myEventMessage;
  private String myTestEventTest;
  private String myTestEventTestDescription;
  private String myEventTestReport;
  private String myEventTestResultActionFilePath;
  private String myEventTestResultFilePath;
  private String myEventTestResultExpected;
  private String myEventTestResultActual;
  private String myEventTestResultFailureType;
  private String myEventTestResultStackTrace;
  private String myEventTestResultErrorMsg;
  private String myEventTestResultEndTime;
  private String myEventTestResultStartTime;
  private String myTestName;

  public TestEventXPPXmlView(@NotNull final String xml) {
    final HierarchicalStreamReader parser = DRIVER.createReader(new StringReader(xml));

    if (!"ijLog".equals(parser.getNodeName())) throw new RuntimeException("root element must be 'ijLog'");

    while(parser.hasMoreChildren()) {
      parser.moveDown();

      if ("event".equals(parser.getNodeName())) {
        myTestEventType = parser.getAttribute("type");                //queryXml("/ijLog/event/@type");
        myEventOpenSettings = parser.getAttribute("openSettings");    //queryXml("/ijLog/event/@openSettings");
        myEventTestReport = parser.getAttribute("testReport");        //queryXml("/ijLog/event/@testReport");

        while (parser.hasMoreChildren()) {
          parser.moveDown();

          if ("title".equals(parser.getNodeName())) {
            myEventTitle = parser.getValue();                               //queryXml("/ijLog/event/title");
          } else if ("message".equals(parser.getNodeName())) {
            myEventMessage = parser.getValue();                             //queryXml("/ijLog/event/message");
          } else if ("test".equals(parser.getNodeName())) {
            myTestParentId = parser.getAttribute("parentId");        //queryXml("/ijLog/event/test/@parentId");
            myTestId = parser.getAttribute("id");                    //queryXml("/ijLog/event/test/@id");

            while (parser.hasMoreChildren()) {
              parser.moveDown();

              if ("descriptor".equals(parser.getNodeName())) {
                myTestName = parser.getAttribute("name");            //queryXml("/ijLog/event/test/descriptor/@name");
                myTestClassName = parser.getAttribute("className");  //queryXml("/ijLog/event/test/descriptor/@className");
              } else if ("event".equals(parser.getNodeName())) {
                myTestEventTestDescription = parser.getAttribute("destination"); //queryXml("/ijLog/event/test/event/@destination");
                myTestEventTest = parser.getValue();                                    //queryXml("/ijLog/event/test/event");
              } else if ("result".equals(parser.getNodeName())) {
                myTestEventResultType = parser.getAttribute("resultType");       //queryXml("/ijLog/event/test/result/@resultType");
                myEventTestResultEndTime = parser.getAttribute("endTime");       //queryXml("/ijLog/event/test/result/@endTime");
                myEventTestResultStartTime = parser.getAttribute("startTime");   //queryXml("/ijLog/event/test/result/@startTime");

                while(parser.hasMoreChildren()) {
                  parser.moveDown();

                  if ("actualFilePath".equals(parser.getNodeName())){
                    myEventTestResultActionFilePath = parser.getValue();             //queryXml("/ijLog/event/test/result/actualFilePath");
                  } else if ("filePath".equals(parser.getNodeName())){
                    myEventTestResultFilePath = parser.getValue();                   //queryXml("/ijLog/event/test/result/filePath");
                  } else if ("expected".equals(parser.getNodeName())){
                    myEventTestResultExpected = parser.getValue();                   //queryXml("/ijLog/event/test/result/expected");
                  } else if ("actual".equals(parser.getNodeName())){
                    myEventTestResultActual = parser.getValue();                     //queryXml("/ijLog/event/test/result/actual");
                  } else if ("failureType".equals(parser.getNodeName())){
                    myEventTestResultFailureType = parser.getValue();                //queryXml("/ijLog/event/test/result/failureType");
                  } else if ("stackTrace".equals(parser.getNodeName())){
                    myEventTestResultStackTrace = parser.getValue();                 //queryXml("/ijLog/event/test/result/stackTrace");
                  } else if ("errorMsg".equals(parser.getNodeName())){
                    myEventTestResultErrorMsg = parser.getValue();                   //queryXml("/ijLog/event/test/result/errorMsg");
                  }
                  parser.moveUp();
                }
              }

              parser.moveUp();
            }
          }

          parser.moveUp();
        }
      }

      parser.moveUp();
    }
  }

  @NotNull
  @Override
  public String getTestEventType() {
    return myTestEventType == null ? "" : myTestEventType;
  }

  @NotNull
  @Override
  public String getTestName() {
    return myTestName == null ? "" : myTestName;
  }

  @NotNull
  @Override
  public String getTestParentId() {
    return myTestParentId == null ? "" : myTestParentId;
  }

  @NotNull
  @Override
  public String getTestId() {
    return myTestId == null ? "" : myTestId;
  }

  @NotNull
  @Override
  public String getTestClassName() {
    return myTestClassName == null ? "" : myTestClassName;
  }

  @NotNull
  @Override
  public String getTestEventResultType() {
    return myTestEventResultType == null ? "" : myTestEventResultType;
  }

  @NotNull
  @Override
  public String getEventTitle() {
    return myEventTitle == null ? "" : myEventTitle;
  }

  @Override
  public boolean isEventOpenSettings() {
    return Boolean.parseBoolean(myEventOpenSettings == null ? "" : myEventOpenSettings);
  }

  @NotNull
  @Override
  public String getEventMessage() {
    return myEventMessage == null ? "" : myEventMessage;
  }

  @NotNull
  @Override
  public String getTestEventTest() {
    return myTestEventTest == null ? "" : myTestEventTest;
  }

  @NotNull
  @Override
  public String getTestEventTestDescription() {
    return myTestEventTestDescription == null ? "" : myTestEventTestDescription;
  }

  @NotNull
  @Override
  public String getEventTestReport() {
    return myEventTestReport == null ? "" : myEventTestReport;
  }

  @NotNull
  @Override
  public String getEventTestResultActualFilePath() {
    return myEventTestResultActionFilePath == null ? "" : myEventTestResultActionFilePath;
  }

  @NotNull
  @Override
  public String getEventTestResultFilePath() {
    return myEventTestResultFilePath == null ? "" : myEventTestResultFilePath;
  }

  @NotNull
  @Override
  public String getEventTestResultExpected() {
    return myEventTestResultExpected == null ? "" : myEventTestResultExpected;
  }

  @NotNull
  @Override
  public String getEventTestResultActual() {
    return myEventTestResultActual == null ? "" : myEventTestResultActual;
  }

  @NotNull
  @Override
  public String getEventTestResultFailureType() {
    return myEventTestResultFailureType == null ? "" : myEventTestResultFailureType;
  }

  @NotNull
  @Override
  public String getEventTestResultStackTrace() {
    return myEventTestResultStackTrace == null ? "" : myEventTestResultStackTrace;
  }

  @NotNull
  @Override
  public String getEventTestResultErrorMsg() {
    return myEventTestResultErrorMsg == null ? "" : myEventTestResultErrorMsg;
  }

  @NotNull
  @Override
  public String getEventTestResultEndTime() {
    return myEventTestResultEndTime == null ? "" : myEventTestResultEndTime;
  }

  @NotNull
  @Override
  public String getEventTestResultStartTime() {
    return myEventTestResultStartTime == null ? "" : myEventTestResultStartTime;
  }
}
