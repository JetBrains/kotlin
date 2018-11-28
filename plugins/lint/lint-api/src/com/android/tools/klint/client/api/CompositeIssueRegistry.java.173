/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.tools.klint.client.api;

import com.android.annotations.NonNull;
import com.android.tools.klint.detector.api.Issue;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Registry which merges many issue registries into one, and presents a unified list
 * of issues.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
class CompositeIssueRegistry extends IssueRegistry {
    private final List<IssueRegistry> myRegistries;
    private List<Issue> myIssues;

    public CompositeIssueRegistry(@NonNull List<IssueRegistry> registries) {
        myRegistries = registries;
    }

    @NonNull
    @Override
    public List<Issue> getIssues() {
        if (myIssues == null) {
            List<Issue> issues = Lists.newArrayListWithExpectedSize(200);
            for (IssueRegistry registry : myRegistries) {
                issues.addAll(registry.getIssues());
            }
            myIssues = issues;
        }

        return myIssues;
    }
}
