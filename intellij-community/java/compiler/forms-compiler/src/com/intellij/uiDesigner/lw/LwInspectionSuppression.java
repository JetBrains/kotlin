/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.uiDesigner.lw;

/**
 * @author yole
 */
public class LwInspectionSuppression {
  public static final LwInspectionSuppression[] EMPTY_ARRAY = new LwInspectionSuppression[0];

  private final String myInspectionId;
  private final String myComponentId;

  public LwInspectionSuppression(final String inspectionId, final String componentId) {
    myInspectionId = inspectionId;
    myComponentId = componentId;
  }

  public String getInspectionId() {
    return myInspectionId;
  }

  public String getComponentId() {
    return myComponentId;
  }
}
