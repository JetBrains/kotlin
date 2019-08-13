/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.notification;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Vladislav.Soroka
 */
public class MessageCounterTest {

  private static final String GROUP1 = "group1";
  private static final String GROUP2 = "group2";
  private static final ProjectSystemId OTHER_BUILD_SYSTEM = new ProjectSystemId("SomeOtherBuildSystem");

  @Test
  public void testIncrement() {
    MessageCounter counter = new MessageCounter();
    counter.increment(GROUP1, NotificationSource.PROJECT_SYNC, NotificationCategory.ERROR, OTHER_BUILD_SYSTEM);
    assertEquals(counter.toString(), 1, counter.getCount(GROUP1, NotificationSource.PROJECT_SYNC, NotificationCategory.ERROR, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 1, counter.getCount(null, NotificationSource.PROJECT_SYNC, NotificationCategory.ERROR, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 1, counter.getCount(null, NotificationSource.PROJECT_SYNC, null, OTHER_BUILD_SYSTEM));

    counter.increment(GROUP1, NotificationSource.PROJECT_SYNC, NotificationCategory.INFO, OTHER_BUILD_SYSTEM);
    assertEquals(counter.toString(), 1, counter.getCount(GROUP1, NotificationSource.PROJECT_SYNC, NotificationCategory.INFO, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 1, counter.getCount(null, NotificationSource.PROJECT_SYNC, NotificationCategory.INFO, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 2, counter.getCount(null, NotificationSource.PROJECT_SYNC, null, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 2, counter.getCount(GROUP1, NotificationSource.PROJECT_SYNC, null, OTHER_BUILD_SYSTEM));

    counter.increment(GROUP2, NotificationSource.PROJECT_SYNC, NotificationCategory.WARNING, OTHER_BUILD_SYSTEM);
    assertEquals(counter.toString(), 1, counter.getCount(GROUP1, NotificationSource.PROJECT_SYNC, NotificationCategory.INFO, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 1, counter.getCount(GROUP1, NotificationSource.PROJECT_SYNC, NotificationCategory.ERROR, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 0, counter.getCount(GROUP1, NotificationSource.PROJECT_SYNC, NotificationCategory.WARNING, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 0, counter.getCount(GROUP2, NotificationSource.PROJECT_SYNC, NotificationCategory.INFO, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 0, counter.getCount(GROUP2, NotificationSource.PROJECT_SYNC, NotificationCategory.ERROR, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 1, counter.getCount(GROUP2, NotificationSource.PROJECT_SYNC, NotificationCategory.WARNING, OTHER_BUILD_SYSTEM));

    assertEquals(counter.toString(), 1, counter.getCount(null, NotificationSource.PROJECT_SYNC, NotificationCategory.INFO, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 1, counter.getCount(null, NotificationSource.PROJECT_SYNC, NotificationCategory.ERROR, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 1, counter.getCount(null, NotificationSource.PROJECT_SYNC, NotificationCategory.WARNING, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 3, counter.getCount(null, NotificationSource.PROJECT_SYNC, null, OTHER_BUILD_SYSTEM));

    counter.increment(GROUP2, NotificationSource.PROJECT_SYNC, NotificationCategory.SIMPLE, new ProjectSystemId("anotherBuildSystem"));

    assertEquals(counter.toString(), 2, counter.getCount(GROUP1, NotificationSource.PROJECT_SYNC, null, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 1, counter.getCount(GROUP2, NotificationSource.PROJECT_SYNC, null, OTHER_BUILD_SYSTEM));

    counter.remove(GROUP2, NotificationSource.PROJECT_SYNC, OTHER_BUILD_SYSTEM);
    assertEquals(counter.toString(), 0, counter.getCount(GROUP2, NotificationSource.PROJECT_SYNC, null, OTHER_BUILD_SYSTEM));
    assertEquals(counter.toString(), 2, counter.getCount(null, NotificationSource.PROJECT_SYNC, null, OTHER_BUILD_SYSTEM));
    counter.remove(null, NotificationSource.PROJECT_SYNC, OTHER_BUILD_SYSTEM);
    assertEquals(counter.toString(), 0, counter.getCount(null, NotificationSource.PROJECT_SYNC, null, OTHER_BUILD_SYSTEM));
  }
}
