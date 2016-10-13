/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.tools.klint.detector.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.annotations.Beta;

/**
 * A category is a container for related issues.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public final class Category implements Comparable<Category> {
    private final String mName;
    private final int mPriority;
    private final Category mParent;

    /**
     * Creates a new {@link Category}.
     *
     * @param parent the name of a parent category, or null
     * @param name the name of the category
     * @param priority a sorting priority, with higher being more important
     */
    private Category(
            @Nullable Category parent,
            @NonNull String name,
            int priority) {
        mParent = parent;
        mName = name;
        mPriority = priority;
    }

    /**
     * Creates a new top level {@link Category} with the given sorting priority.
     *
     * @param name the name of the category
     * @param priority a sorting priority, with higher being more important
     * @return a new category
     */
    @NonNull
    public static Category create(@NonNull String name, int priority) {
        return new Category(null, name, priority);
    }

    /**
     * Creates a new top level {@link Category} with the given sorting priority.
     *
     * @param parent the name of a parent category, or null
     * @param name the name of the category
     * @param priority a sorting priority, with higher being more important
     * @return a new category
     */
    @NonNull
    public static Category create(@Nullable Category parent, @NonNull String name, int priority) {
        return new Category(parent, name, priority);
    }

    /**
     * Returns the parent category, or null if this is a top level category
     *
     * @return the parent category, or null if this is a top level category
     */
    public Category getParent() {
        return mParent;
    }

    /**
     * Returns the name of this category
     *
     * @return the name of this category
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns a full name for this category. For a top level category, this is just
     * the {@link #getName()} value, but for nested categories it will include the parent
     * names as well.
     *
     * @return a full name for this category
     */
    public String getFullName() {
        if (mParent != null) {
            return mParent.getFullName() + ':' + mName;
        } else {
            return mName;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Category category = (Category) o;

        //noinspection SimplifiableIfStatement
        if (!mName.equals(category.mName)) {
            return false;
        }
        return mParent != null ? mParent.equals(category.mParent) : category.mParent == null;

    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public int hashCode() {
        return mName.hashCode();
    }

    @Override
    public int compareTo(@NonNull Category other) {
        if (other.mPriority == mPriority) {
            if (mParent == other) {
                return 1;
            } else if (other.mParent == this) {
                return -1;
            }
        }

        int delta = other.mPriority - mPriority;
        if (delta != 0) {
            return delta;
        }

        return mName.compareTo(other.mName);
    }

    /** Issues related to running lint itself */
    public static final Category LINT = create("Lint", 110);

    /** Issues related to correctness */
    public static final Category CORRECTNESS = create("Correctness", 100);

    /** Issues related to security */
    public static final Category SECURITY = create("Security", 90);

    /** Issues related to performance */
    public static final Category PERFORMANCE = create("Performance", 80);

    /** Issues related to usability */
    public static final Category USABILITY = create("Usability", 70);

    /** Issues related to accessibility */
    public static final Category A11Y = create("Accessibility", 60);

    /** Issues related to internationalization */
    public static final Category I18N = create("Internationalization", 50);

    // Sub categories

    /** Issues related to icons */
    public static final Category ICONS = create(USABILITY, "Icons", 73);

    /** Issues related to typography */
    public static final Category TYPOGRAPHY = create(USABILITY, "Typography", 76);

    /** Issues related to messages/strings */
    public static final Category MESSAGES = create(CORRECTNESS, "Messages", 95);

    /** Issues related to right to left and bidirectional text support */
    public static final Category RTL = create(I18N, "Bidirectional Text", 40);
}
