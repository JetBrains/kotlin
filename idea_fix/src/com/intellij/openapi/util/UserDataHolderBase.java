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
package com.intellij.openapi.util;


//import com.intellij.util.concurrency.AtomicFieldUpdater;

import com.intellij.util.containers.StripedLockConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class UserDataHolderBase implements UserDataHolderEx, Cloneable {
    private static final Key<Map<Key, Object>> COPYABLE_USER_MAP_KEY = Key.create("COPYABLE_USER_MAP_KEY");

    /**
     * Concurrent writes to this field are via CASes only, using the {@link #}
     * When map becomes empty, this field set to null atomically
     * <p/>
     * Basic state transitions are as follows:
     * <p/>
     * (adding keyvalue)            (putUserData(key,value))
     * [myUserMap=null]                  ->              [myUserMap=(key->value)]
     * <p/>
     * (adding another)             (putUserData(key2,value2))
     * [myUserMap=(key->value)]          ->              [myUserMap=(key->value, key2->value2)]
     * <p/>
     * (removing keyvalue)          (putUserData(k2,null))
     * [myUserMap=(key->value, k2->v2)]  ->              [myUserMap=(key->value)]
     * <p/>
     * (removing last entry)        (putUserData(key,null))
     * [myUserMap=(key->value)]          ->              [myUserMap=null]
     */
    private volatile ConcurrentMap<Key, Object> myUserMap = null;

    protected Object clone() {
        try {
            UserDataHolderBase clone = (UserDataHolderBase) super.clone();
            clone.myUserMap = null;
            copyCopyableDataTo(clone);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);

        }
    }

    @TestOnly
    public String getUserDataString() {
        final ConcurrentMap<Key, Object> userMap = myUserMap;
        if (userMap == null) {
            return "";
        }
        final Map copyableMap = getUserData(COPYABLE_USER_MAP_KEY);
        return userMap.toString() + (copyableMap == null ? "" : copyableMap.toString());
    }

    public void copyUserDataTo(UserDataHolderBase other) {
        ConcurrentMap<Key, Object> map = myUserMap;
        if (map == null) {
            other.myUserMap = null;
        } else {
            ConcurrentMap<Key, Object> fresh = createDataMap(map.size());
            fresh.putAll(map);
            other.myUserMap = fresh;
        }
    }

    public <T> T getUserData(@NotNull Key<T> key) {
        final Map<Key, Object> map = myUserMap;
        //noinspection unchecked
        return map == null ? null : (T) map.get(key);
    }

    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        while (true) {
            try {
                if (value == null) {
                    ConcurrentMap<Key, Object> map = myUserMap;
                    if (map == null) break;
                    @SuppressWarnings("unchecked")
                    T previous = (T) map.remove(key);
                    boolean removed = previous != null;
                    if (removed) {
                        nullifyMapFieldIfEmpty();
                    }
                } else {
                    getOrCreateMap().put(key, value);
                }
                break;
            } catch (ConcurrentModificationException ignored) {
            }
        }
    }

    private static ConcurrentMap<Key, Object> createDataMap(int initialCapacity) {
        return new StripedLockConcurrentHashMap<Key, Object>(initialCapacity);
    }

    public <T> T getCopyableUserData(Key<T> key) {
        return getCopyableUserDataImpl(key);
    }

    protected final <T> T getCopyableUserDataImpl(Key<T> key) {
        Map map = getUserData(COPYABLE_USER_MAP_KEY);
        //noinspection unchecked
        return map == null ? null : (T) map.get(key);
    }

    public <T> void putCopyableUserData(Key<T> key, T value) {
        putCopyableUserDataImpl(key, value);
    }

    private Map<Key, Object> getOrCreateCopyableMap(boolean create) {
        Map<Key, Object> copyMap = getUserData(COPYABLE_USER_MAP_KEY);
        if (copyMap == null && create) {
            copyMap = createDataMap(1);
            copyMap = putUserDataIfAbsent(COPYABLE_USER_MAP_KEY, copyMap);
        }

        return copyMap;
    }

    protected final <T> void putCopyableUserDataImpl(Key<T> key, T value) {
        while (true) {
            try {
                Map<Key, Object> copyMap = getOrCreateCopyableMap(value != null);
                if (copyMap == null) break;

                if (value == null) {
                    copyMap.remove(key);
                    if (copyMap.isEmpty()) {
                        ((StripedLockConcurrentHashMap<Key, Object>) copyMap).blockModification();
                        ConcurrentMap<Key, Object> newCopyMap;
                        if (copyMap.isEmpty()) {
                            newCopyMap = null;
                        } else {
                            newCopyMap = createDataMap(copyMap.size());
                            newCopyMap.putAll(copyMap);
                        }
                        boolean replaced = replace(COPYABLE_USER_MAP_KEY, copyMap, newCopyMap);
                        if (!replaced) continue;
                    }
                } else {
                    copyMap.put(key, value);
                }
                break;
            } catch (ConcurrentModificationException ignored) {
                // someone blocked modification, retry
            }
        }
    }

    private ConcurrentMap<Key, Object> getOrCreateMap() {
        while (true) {
            ConcurrentMap<Key, Object> map = myUserMap;
            if (map != null) return map;
            map = createDataMap(2);
            boolean updated = true;
            //throw new UnsupportedOperationException("Exception in (my) UserDataHolderBase");
//      boolean updated = updater.compareAndSet(this, null, map);
            if (updated) {
                return map;
            }
        }
    }

    public <T> boolean replace(@NotNull Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
        while (true) {
            try {
                ConcurrentMap<Key, Object> map = getOrCreateMap();
                if (oldValue == null) {
                    return newValue == null || map.putIfAbsent(key, newValue) == null;
                }
                if (newValue == null) {
                    boolean removed = map.remove(key, oldValue);
                    if (removed) {
                        nullifyMapFieldIfEmpty();
                    }
                    return removed;
                }
                return map.replace(key, oldValue, newValue);
            } catch (ConcurrentModificationException ignored) {
                // someone blocked modification, retry
            }
        }
    }

    @NotNull
    public <T> T putUserDataIfAbsent(@NotNull final Key<T> key, @NotNull final T value) {
        Object v = getOrCreateMap().get(key);
        if (v != null) {
            //noinspection unchecked
            return (T) v;
        }
        while (true) {
            try {
                @SuppressWarnings("unchecked")
                T prev = (T) getOrCreateMap().putIfAbsent(key, value);
                return prev == null ? value : prev;
            } catch (ConcurrentModificationException ignored) {
                // someone blocked modification, retry
            }
        }
    }

    public void copyCopyableDataTo(@NotNull UserDataHolderBase clone) {
        Map<Key, Object> copyableMap = getUserData(COPYABLE_USER_MAP_KEY);
        if (copyableMap != null) {
            ConcurrentMap<Key, Object> copy = createDataMap(copyableMap.size());
            copy.putAll(copyableMap);
            copyableMap = copy;
        }
        clone.putUserData(COPYABLE_USER_MAP_KEY, copyableMap);
    }

    protected void clearUserData() {
        myUserMap = null;
    }

//    private static final AtomicFieldUpdater<UserDataHolderBase, ConcurrentMap> updater = AtomicFieldUpdater.forFieldOfType(UserDataHolderBase.class, ConcurrentMap.class);

    private void nullifyMapFieldIfEmpty() {
        try {
            while (true) {
                StripedLockConcurrentHashMap<Key, Object> map = (StripedLockConcurrentHashMap<Key, Object>) myUserMap;
                if (map == null || !map.isEmpty()) break;
                map.blockModification(); // we block the map and either replace it with null or fail with replace, in both cases the map is thrown away
                ConcurrentMap<Key, Object> newMap;
                if (map.isEmpty()) {
                    newMap = null;
                } else {
                    // someone managed to add something in the meantime
                    // atomically replace the blocked map with newly created map filled with the data sneaked in
                    newMap = createDataMap(map.size());
                    newMap.putAll(map);
                }
                boolean replaced = true;
                //throw new UnsupportedOperationException("Exception in (my) UserDataHolderBase");
                //boolean replaced = updater.compareAndSet(this, map, newMap);
                if (replaced) break;
                // else someone has replaced map already and pushing back the changes is his responsibility
            }
        } catch (ConcurrentModificationException ignored) {
            // somebody has already blocked the map, back off  
        }
    }
}
