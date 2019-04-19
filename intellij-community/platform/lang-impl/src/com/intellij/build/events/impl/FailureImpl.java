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
package com.intellij.build.events.impl;

import com.intellij.build.events.Failure;
import com.intellij.notification.Notification;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class FailureImpl implements Failure {

  private final String myMessage;
  private final String myDescription;
  private final List<? extends Failure> myCauses;
  private final Throwable myError;
  @Nullable
  private final Notification myNotification;
  @Nullable
  private final Navigatable myNavigatable;

  public FailureImpl(String message, Throwable error) {
    this(message, null, Collections.emptyList(), error, null, null);
  }

  public FailureImpl(String message, Throwable error, @Nullable Notification notification, @Nullable Navigatable navigatable) {
    this(message, null, Collections.emptyList(), error, notification, navigatable);
  }

  public FailureImpl(String message, String description) {
    this(message, description, Collections.emptyList(), null, null, null);
  }

  public FailureImpl(String message, String description, List<? extends Failure> causes) {
    this(message, description, causes, null, null, null);
  }

  private FailureImpl(String message,
                      String description,
                      List<? extends Failure> causes,
                      Throwable error,
                      @Nullable Notification notification,
                      @Nullable Navigatable navigatable) {
    myMessage = message;
    myDescription = description;
    myCauses = causes;
    myError = error;
    myNotification = notification;
    myNavigatable = navigatable;
  }

  @Nullable
  @Override
  public String getMessage() {
    return myMessage;
  }

  @Nullable
  @Override
  public String getDescription() {
    return myDescription;
  }

  @Nullable
  @Override
  public Throwable getError() {
    return myError;
  }

  @Override
  public List<? extends Failure> getCauses() {
    return myCauses;
  }

  @Nullable
  @Override
  public Notification getNotification() {
    return myNotification;
  }

  @Nullable
  @Override
  public Navigatable getNavigatable() {
    return myNavigatable;
  }
}
