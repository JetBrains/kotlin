package org.jetbrains.plugins.gradle.util;

import com.intellij.testFramework.SkipInHeadlessEnvironment;
import org.jetbrains.plugins.gradle.ui.RichTextControlBuilder;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.swing.*;

/**
 * @author Denis Zhdanov
 */
@RunWith(JMock.class)
@SkipInHeadlessEnvironment
public class RichTextControlBuilderTest {

  private static final String META_KEY = "my-key";
  
  private RichTextControlBuilder myBuilder;
  private Mockery myMockery;
  private RichTextControlBuilder.RichTextProcessor myProcessor;
  
  @Before
  public void setUp() {
    myBuilder = new RichTextControlBuilder();
    myMockery = new JUnit4Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
    myProcessor = myMockery.mock(RichTextControlBuilder.RichTextProcessor.class);
    myMockery.checking(new Expectations() {{
      allowing(myProcessor).getKey(); will(returnValue(META_KEY));
    }});
    myBuilder.registerProcessor(myProcessor);
  }
  
  @After
  public void checkExpectations() {
    myMockery.assertIsSatisfied();
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void invalidMetaKeyDefinition() {
    myBuilder.setText("test {@ invalid-key data}");
  }

  @Test(expected = IllegalArgumentException.class)
  public void noProcessorForMetaKey() {
    myBuilder.setText("{@key data}");
  }
  
  @Test
  public void completeMetaInfo() {
    String text = String.format("this is a test text with two inline meta-datas: {@%s meta \t text} and {@%s}", META_KEY, META_KEY);
    myMockery.checking(new Expectations() {{
      one(myProcessor).process("meta \t text"); will(returnValue(new JLabel("")));
      one(myProcessor).process(""); will(returnValue(new JLabel("")));
    }});
    myBuilder.setText(text);
  }
}
