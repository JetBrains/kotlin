/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.inspections.klint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceItem;
import com.android.tools.idea.rendering.LocalResourceRepository;
import com.android.tools.klint.checks.ViewTypeDetector;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Scope;

import java.util.Collection;
import java.util.Collections;

public class IntellijViewTypeDetector extends ViewTypeDetector {
  static final Implementation IMPLEMENTATION = new Implementation(
    IntellijViewTypeDetector.class,
    Scope.JAVA_FILE_SCOPE);

  @Nullable
  @Override
  protected Collection<String> getViewTags(@NonNull Context context, @NonNull ResourceItem item) {
    AbstractResourceRepository projectResources = context.getClient().getProjectResources(context.getMainProject(), true);
    assert projectResources instanceof LocalResourceRepository : projectResources;
    LocalResourceRepository repository = (LocalResourceRepository)projectResources;
    String viewTag = repository.getViewTag(item);
    if (viewTag != null) {
      return Collections.singleton(viewTag);
    }

    return super.getViewTags(context, item);
  }
}
