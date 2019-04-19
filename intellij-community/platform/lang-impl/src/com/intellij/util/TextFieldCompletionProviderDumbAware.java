package com.intellij.util;

import com.intellij.openapi.project.DumbAware;

/**
 * @author sergey.evdokimov
 */
public abstract class TextFieldCompletionProviderDumbAware extends TextFieldCompletionProvider implements DumbAware {

  protected TextFieldCompletionProviderDumbAware() {
  }

  protected TextFieldCompletionProviderDumbAware(boolean caseInsensitivity) {
    super(caseInsensitivity);
  }
}
