package com.intellij.platform.templates.github;

import com.intellij.openapi.util.NlsContexts.DialogMessage;

/**
 * @author Sergey Simonchik
 */
public class GeneratorException extends Exception {
  public GeneratorException(@DialogMessage String message) {
    super(message);
  }

  public GeneratorException(@DialogMessage String message, Throwable cause) {
    super(message, cause);
  }
}
