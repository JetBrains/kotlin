package com.intellij.openapi.externalSystem.service.execution.cmd;

/**
 * @author Sergey Evdokimov
 */
public class ParametersListLexer {

  private final String myText;

  private int myTokenStart = -1;

  private int index = 0;

  public ParametersListLexer(String text) {
    myText = text;
  }

  public int getTokenStart() {
    assert myTokenStart >= 0;
    return myTokenStart;
  }

  public int getTokenEnd() {
    assert myTokenStart >= 0;
    return index;
  }

  public String getCurrentToken() {
    return myText.substring(myTokenStart, index);
  }

  public boolean nextToken() {
    int i = index;

    while (i < myText.length() && Character.isWhitespace(myText.charAt(i))) {
      i++;
    }

    if (i == myText.length()) {
      return false;
    }

    myTokenStart = i;

    boolean isInQuote = false;

    do {
      char a = myText.charAt(i);

      if (!isInQuote && Character.isWhitespace(a)) {
        break;
      }

      if (a == '\\' && i + 1 < myText.length() && myText.charAt(i + 1) == '"') {
        i += 2;
      }
      else if (a == '"') {
        i++;
        isInQuote = !isInQuote;
      }
      else {
        i++;
      }

    } while (i < myText.length());

    index = i;

    return true;
  }
}
