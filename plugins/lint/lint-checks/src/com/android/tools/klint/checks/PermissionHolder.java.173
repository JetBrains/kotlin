/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.tools.klint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.sdklib.AndroidVersion;

import java.util.Collections;
import java.util.Set;

/**
 * A {@linkplain PermissionHolder} knows which permissions are held/granted and can look up
 * individual permissions and respond to queries about whether they are held or not.
 */
public interface PermissionHolder {

    /** Returns true if the permission holder has been granted the given permission */
    boolean hasPermission(@NonNull String permission);

    /** Returns true if the given permission is known to be revocable for targetSdkVersion &ge; M */
    boolean isRevocable(@NonNull String permission);

    @NonNull
    AndroidVersion getMinSdkVersion();

    @NonNull
    AndroidVersion getTargetSdkVersion();

    /**
     * A convenience implementation of {@link PermissionHolder} backed by a set
     */
    class SetPermissionLookup implements PermissionHolder {
        private final Set<String> mGrantedPermissions;
        private final Set<String> mRevocablePermissions;
        private final AndroidVersion mMinSdkVersion;
        private final AndroidVersion mTargetSdkVersion;

        public SetPermissionLookup(
                @NonNull Set<String> grantedPermissions,
                @NonNull Set<String> revocablePermissions,
                @NonNull AndroidVersion minSdkVersion,
                @NonNull AndroidVersion targetSdkVersion) {
            mGrantedPermissions = grantedPermissions;
            mRevocablePermissions = revocablePermissions;
            mMinSdkVersion = minSdkVersion;
            mTargetSdkVersion = targetSdkVersion;
        }

        @VisibleForTesting
        public SetPermissionLookup(@NonNull Set<String> grantedPermissions,
                @NonNull Set<String> revocablePermissions) {
            this(grantedPermissions, revocablePermissions, AndroidVersion.DEFAULT,
                    AndroidVersion.DEFAULT);
        }

        @VisibleForTesting
        public SetPermissionLookup(@NonNull Set<String> grantedPermissions) {
            this(grantedPermissions, Collections.<String>emptySet());
        }

        @Override
        public boolean hasPermission(@NonNull String permission) {
            return mGrantedPermissions.contains(permission);
        }

        @Override
        public boolean isRevocable(@NonNull String permission) {
            return mRevocablePermissions.contains(permission);
        }

        @NonNull
        @Override
        public AndroidVersion getMinSdkVersion() {
            return mMinSdkVersion;
        }

        @NonNull
        @Override
        public AndroidVersion getTargetSdkVersion() {
            return mTargetSdkVersion;
        }

        /**
         * Creates a {@linkplain PermissionHolder} which combines the permissions
         * held by the given holder, with the permissions implied by the given
         * {@link PermissionRequirement}
         */
        @NonNull
        public static PermissionHolder join(@NonNull PermissionHolder lookup,
                                            @NonNull PermissionRequirement requirement) {
            SetPermissionLookup empty = new SetPermissionLookup(Collections.<String>emptySet(),
                    Collections.<String>emptySet(), lookup.getMinSdkVersion(),
                    lookup.getTargetSdkVersion());
            return join(lookup, requirement.getMissingPermissions(empty));
        }

        /**
         * Creates a {@linkplain PermissionHolder} which combines the permissions
         * held by the given holder, along with a set of additional permission names
         */
        @NonNull
        public static PermissionHolder join(@NonNull final PermissionHolder lookup,
                @Nullable final Set<String> permissions) {
            if (permissions != null && !permissions.isEmpty()) {
                return new PermissionHolder() {
                    @Override
                    public boolean hasPermission(@NonNull String permission) {
                        return lookup.hasPermission(permission)
                                || permissions.contains(permission);
                    }

                    @Override
                    public boolean isRevocable(@NonNull String permission) {
                        return lookup.isRevocable(permission);
                    }

                    @NonNull
                    @Override
                    public AndroidVersion getMinSdkVersion() {
                        return lookup.getMinSdkVersion();
                    }

                    @NonNull
                    @Override
                    public AndroidVersion getTargetSdkVersion() {
                        return lookup.getTargetSdkVersion();
                    }
                };
            }
            return lookup;
        }
    }
}
