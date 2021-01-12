/*
 * Copyright 2000-2005 JetBrains s.r.o.
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
package com.intellij.compiler;

import com.intellij.compiler.impl.javaCompiler.javac.JavacOutputParser;
import com.intellij.compiler.impl.javaCompiler.javac.JavacParserAction;
import com.intellij.openapi.diagnostic.Logger;
import junit.framework.TestCase;

import java.util.ResourceBundle;
import java.util.regex.Matcher;

/**
 * @author Eugene Zhuravlev
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public class JavacOutputParserTest extends TestCase {
  private static final Logger LOG = Logger.getInstance(JavacOutputParserTest.class);
  private static final ResourceBundle ourBundle;

  static {
    ResourceBundle bundle = null;
    try {
      bundle = ResourceBundle.getBundle("com.sun.tools.javac.resources.compiler");
    }
    catch (Throwable e) {
      LOG.info(e);
    }
    ourBundle = bundle;
  }
  /*
  compiler.misc.verbose.parsing.started->[parsing started {0}]
  compiler.misc.verbose.parsing.done->[parsing completed {0}ms]
  compiler.misc.verbose.loading->[loading {0}]
  compiler.misc.verbose.checking.attribution->[checking {0}]
  compiler.misc.verbose.wrote.file->[wrote {0}]
  */


  /*
LIne read: #[parsing started C:\temp\rmiTest\source\mycompany\TTT.java]#
LIne read: #[parsing completed 47ms]#
LIne read: #[search path for source files: []]#
LIne read: #[search path for class files: [C:\java\jdk150_04\jre\lib\charsets.jar, C:\java\jdk150_04\jre\lib\deploy.jar, C:\java\jdk150_04\jre\lib\javaws.jar, C:\java\jdk150_04\jre\lib\jce.jar, C:\java\jdk150_04\jre\lib\jsse.jar, C:\java\jdk150_04\jre\lib\plugin.jar, C:\java\jdk150_04\jre\lib\rt.jar, C:\java\jdk150_04\jre\lib\ext\dnsns.jar, C:\java\jdk150_04\jre\lib\ext\localedata.jar, C:\java\jdk150_04\jre\lib\ext\sunjce_provider.jar, C:\java\jdk150_04\jre\lib\ext\sunpkcs11.jar, C:\temp\rmiTest\classes]]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/lang/Object.class)]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/lang/InterruptedException.class)]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/lang/String.class)]#
LIne read: #[checking mycompany.TTT]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/lang/Exception.class)]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/lang/Throwable.class)]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/lang/System.class)]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/io/PrintStream.class)]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/io/FilterOutputStream.class)]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/io/OutputStream.class)]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/lang/Thread.class)]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/lang/Error.class)]#
LIne read: #[loading C:\java\jdk150_04\jre\lib\rt.jar(java/lang/RuntimeException.class)]#
LIne read: #[wrote C:\temp\rmiTest\classes\mycompany\TTT.class]#
  */
  public void testFilePathMatcher() {
    if (ourBundle != null) {
      MyJavacParserAction action = new MyJavacParserAction(JavacOutputParser.createMatcher(ourBundle.getString("compiler.misc.verbose.parsing.started")));
      action.setExpectedString("C:/temp/rmiTest/source/mycompany/TTT.java").execute("[parsing started C:/temp/rmiTest/source/mycompany/TTT.java]", null);

      action = new MyJavacParserAction(JavacOutputParser.createMatcher(ourBundle.getString("compiler.misc.verbose.loading")));
      action.setExpectedString("C:/java/jdk150_04/jre/lib/rt.jar(java/lang/Object.class)").execute("[loading C:/java/jdk150_04/jre/lib/rt.jar(java/lang/Object.class)]", null);

      action = new MyJavacParserAction(JavacOutputParser.createMatcher(ourBundle.getString("compiler.misc.verbose.checking.attribution")));
      action.setExpectedString("mycompany.TTT").execute("[checking mycompany.TTT]", null);

      action = new MyJavacParserAction(JavacOutputParser.createMatcher(ourBundle.getString("compiler.misc.verbose.wrote.file")));
      action.setExpectedString("C:/temp/rmiTest/classes/mycompany/TTT.class").execute("[wrote C:/temp/rmiTest/classes/mycompany/TTT.class]", null);

      action = new MyJavacParserAction(JavacOutputParser.createMatcher(ourBundle.getString("compiler.misc.verbose.parsing.done")));
      action.setExpectedString("47").execute("[parsing completed 47ms]", null);
    }
    else {
      System.out.println("JavacOutputParserTest.testFilePathMatcher SKIPPED");
    }
  }

  private static class MyJavacParserAction extends JavacParserAction {
    private String myExpected;

    MyJavacParserAction(Matcher matcher) {
      super(matcher);
    }

    public JavacParserAction setExpectedString(String expected) {
      myExpected = expected;
      return this;
    }

    @Override
    protected void doExecute(final String line, final String dta, final OutputParser.Callback callback) {
      assertEquals("Expected: #" + myExpected + "#, but was: #" + dta + "#", myExpected, dta);
    }
  }
}
