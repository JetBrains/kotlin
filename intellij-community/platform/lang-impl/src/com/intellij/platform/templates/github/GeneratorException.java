package com.intellij.platform.templates.github;

/**
 * @author Sergey Simonchik
 */
public class GeneratorException extends Exception {
  public GeneratorException(String message) {
    super(message);
  }

  public GeneratorException(String message, Throwable cause) {
    super(message, cause);
  }
}
