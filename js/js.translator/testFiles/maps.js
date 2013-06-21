/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

"use strict";
(function () {
    var FUNCTION = "function";
    var arrayRemoveAt = (typeof Array.prototype.splice == FUNCTION) ?
                        function (arr, idx) {
                            arr.splice(idx, 1);
                        } :

                        function (arr, idx) {
                            var itemsAfterDeleted, i, len;
                            if (idx === arr.length - 1) {
                                arr.length = idx;
                            }
                            else {
                                itemsAfterDeleted = arr.slice(idx + 1);
                                arr.length = idx;
                                for (i = 0, len = itemsAfterDeleted.length; i < len; ++i) {
                                    arr[idx + i] = itemsAfterDeleted[i];
                                }
                            }
                        };

    function hashObject(obj) {
        var hashCode;
        if (typeof obj == "string") {
            return obj;
        }
        else if (typeof obj.hashCode == FUNCTION) {
            // Check the hashCode method really has returned a string
            hashCode = obj.hashCode();
            return (typeof hashCode == "string") ? hashCode : hashObject(hashCode);
        }
        else if (typeof obj.toString == FUNCTION) {
            return obj.toString();
        }
        else {
            try {
                return String(obj);
            }
            catch (ex) {
                // For host objects (such as ActiveObjects in IE) that have no toString() method and throw an error when
                // passed to String()
                return Object.prototype.toString.call(obj);
            }
        }
    }

    function equals_fixedValueHasEquals(fixedValue, variableValue) {
        return fixedValue.equals(variableValue);
    }

    function equals_fixedValueNoEquals(fixedValue, variableValue) {
        return (typeof variableValue.equals == FUNCTION) ?
               variableValue.equals(fixedValue) : (fixedValue === variableValue);
    }

    function createKeyValCheck(kvStr) {
        return function (kv) {
            if (kv === null) {
                throw new Error("null is not a valid " + kvStr);
            }
            else if (typeof kv == "undefined") {
                throw new Error(kvStr + " must not be undefined");
            }
        };
    }

    var checkKey = createKeyValCheck("key"), checkValue = createKeyValCheck("value");

    function Bucket(hash, firstKey, firstValue, equalityFunction) {
        this[0] = hash;
        this.entries = [];
        this.addEntry(firstKey, firstValue);

        if (equalityFunction !== null) {
            this.getEqualityFunction = function () {
                return equalityFunction;
            };
        }
    }

    var EXISTENCE = 0, ENTRY = 1, ENTRY_INDEX_AND_VALUE = 2;

    function createBucketSearcher(mode) {
        return function (key) {
            var i = this.entries.length, entry, equals = this.getEqualityFunction(key);
            while (i--) {
                entry = this.entries[i];
                if (equals(key, entry[0])) {
                    switch (mode) {
                        case EXISTENCE:
                            return true;
                        case ENTRY:
                            return entry;
                        case ENTRY_INDEX_AND_VALUE:
                            return [ i, entry[1] ];
                    }
                }
            }
            return false;
        };
    }

    function createBucketLister(entryProperty) {
        return function (aggregatedArr) {
            var startIndex = aggregatedArr.length;
            for (var i = 0, len = this.entries.length; i < len; ++i) {
                aggregatedArr[startIndex + i] = this.entries[i][entryProperty];
            }
        };
    }

    Bucket.prototype = {
        getEqualityFunction: function (searchValue) {
            return (typeof searchValue.equals == FUNCTION) ? equals_fixedValueHasEquals : equals_fixedValueNoEquals;
        },

        getEntryForKey: createBucketSearcher(ENTRY),

        getEntryAndIndexForKey: createBucketSearcher(ENTRY_INDEX_AND_VALUE),

        removeEntryForKey: function (key) {
            var result = this.getEntryAndIndexForKey(key);
            if (result) {
                arrayRemoveAt(this.entries, result[0]);
                return result[1];
            }
            return null;
        },

        addEntry: function (key, value) {
            this.entries[this.entries.length] = [key, value];
        },

        keys: createBucketLister(0),

        values: createBucketLister(1),

        getEntries: function (entries) {
            var startIndex = entries.length;
            for (var i = 0, len = this.entries.length; i < len; ++i) {
                // Clone the entry stored in the bucket before adding to array
                entries[startIndex + i] = this.entries[i].slice(0);
            }
        },

        containsKey: createBucketSearcher(EXISTENCE),

        containsValue: function (value) {
            var i = this.entries.length;
            while (i--) {
                if (value === this.entries[i][1]) {
                    return true;
                }
            }
            return false;
        }
    };

    /*----------------------------------------------------------------------------------------------------------------*/

    // Supporting functions for searching hashtable buckets

    function searchBuckets(buckets, hash) {
        var i = buckets.length, bucket;
        while (i--) {
            bucket = buckets[i];
            if (hash === bucket[0]) {
                return i;
            }
        }
        return null;
    }

    function getBucketForHash(bucketsByHash, hash) {
        var bucket = bucketsByHash[hash];

        // Check that this is a genuine bucket and not something inherited from the bucketsByHash's prototype
        return ( bucket && (bucket instanceof Bucket) ) ? bucket : null;
    }

    /*----------------------------------------------------------------------------------------------------------------*/

    var Hashtable = function (hashingFunctionParam, equalityFunctionParam) {
        var that = this;
        var buckets = [];
        var bucketsByHash = {};

        var hashingFunction = (typeof hashingFunctionParam == FUNCTION) ? hashingFunctionParam : hashObject;
        var equalityFunction = (typeof equalityFunctionParam == FUNCTION) ? equalityFunctionParam : null;

        this.put = function (key, value) {
            checkKey(key);
            checkValue(value);
            var hash = hashingFunction(key), bucket, bucketEntry, oldValue = null;

            // Check if a bucket exists for the bucket key
            bucket = getBucketForHash(bucketsByHash, hash);
            if (bucket) {
                // Check this bucket to see if it already contains this key
                bucketEntry = bucket.getEntryForKey(key);
                if (bucketEntry) {
                    // This bucket entry is the current mapping of key to value, so replace old value and we're done.
                    oldValue = bucketEntry[1];
                    bucketEntry[1] = value;
                }
                else {
                    // The bucket does not contain an entry for this key, so add one
                    bucket.addEntry(key, value);
                }
            }
            else {
                // No bucket exists for the key, so create one and put our key/value mapping in
                bucket = new Bucket(hash, key, value, equalityFunction);
                buckets[buckets.length] = bucket;
                bucketsByHash[hash] = bucket;
            }
            return oldValue;
        };

        this.get = function (key) {
            checkKey(key);

            var hash = hashingFunction(key);

            // Check if a bucket exists for the bucket key
            var bucket = getBucketForHash(bucketsByHash, hash);
            if (bucket) {
                // Check this bucket to see if it contains this key
                var bucketEntry = bucket.getEntryForKey(key);
                if (bucketEntry) {
                    // This bucket entry is the current mapping of key to value, so return the value.
                    return bucketEntry[1];
                }
            }
            return null;
        };

        this.containsKey = function (key) {
            checkKey(key);
            var bucketKey = hashingFunction(key);

            // Check if a bucket exists for the bucket key
            var bucket = getBucketForHash(bucketsByHash, bucketKey);

            return bucket ? bucket.containsKey(key) : false;
        };

        this.containsValue = function (value) {
            checkValue(value);
            var i = buckets.length;
            while (i--) {
                if (buckets[i].containsValue(value)) {
                    return true;
                }
            }
            return false;
        };

        this.clear = function () {
            buckets.length = 0;
            bucketsByHash = {};
        };

        this.isEmpty = function () {
            return !buckets.length;
        };

        var createBucketAggregator = function (bucketFuncName) {
            return function () {
                var aggregated = [], i = buckets.length;
                while (i--) {
                    buckets[i][bucketFuncName](aggregated);
                }
                return aggregated;
            };
        };

        this._keys = createBucketAggregator("keys");
        this._values = createBucketAggregator("values");
        this._entries = createBucketAggregator("getEntries");

        this.values = function () {
            var values = this._values();
            var i = values.length;
            var result = Kotlin.$new(Kotlin.ArrayList)();
            while (i--) {
                result.add(values[i]);
            }
            return result;
        };

        this.remove = function (key) {
            checkKey(key);

            var hash = hashingFunction(key), bucketIndex, oldValue = null;

            // Check if a bucket exists for the bucket key
            var bucket = getBucketForHash(bucketsByHash, hash);

            if (bucket) {
                // Remove entry from this bucket for this key
                oldValue = bucket.removeEntryForKey(key);
                if (oldValue !== null) {
                    // Entry was removed, so check if bucket is empty
                    if (!bucket.entries.length) {
                        // Bucket is empty, so remove it from the bucket collections
                        bucketIndex = searchBuckets(buckets, hash);
                        arrayRemoveAt(buckets, bucketIndex);
                        delete bucketsByHash[hash];
                    }
                }
            }
            return oldValue;
        };

        this.size = function () {
            var total = 0, i = buckets.length;
            while (i--) {
                total += buckets[i].entries.length;
            }
            return total;
        };

        this.each = function (callback) {
            var entries = that._entries(), i = entries.length, entry;
            while (i--) {
                entry = entries[i];
                callback(entry[0], entry[1]);
            }
        };


        this.putAll = function (hashtable, conflictCallback) {
            var entries = hashtable._entries();
            var entry, key, value, thisValue, i = entries.length;
            var hasConflictCallback = (typeof conflictCallback == FUNCTION);
            while (i--) {
                entry = entries[i];
                key = entry[0];
                value = entry[1];

                // Check for a conflict. The default behaviour is to overwrite the value for an existing key
                if (hasConflictCallback && (thisValue = that.get(key))) {
                    value = conflictCallback(key, thisValue, value);
                }
                that.put(key, value);
            }
        };

        this.clone = function () {
            var clone = new Hashtable(hashingFunctionParam, equalityFunctionParam);
            clone.putAll(that);
            return clone;
        };

        this.keySet = function () {
            var res = Kotlin.$new(Kotlin.ComplexHashSet)();
            var keys = this._keys();
            var i = keys.length;
            while (i--) {
                res.add(keys[i]);
            }
            return res;
        };
    };


    Kotlin.HashTable = Hashtable;
})();

Kotlin.Map = Kotlin.$createClass();

Kotlin.HashMap = Kotlin.$createClass(Kotlin.Map, {initialize: function () {
    Kotlin.HashTable.call(this);
}});

Kotlin.ComplexHashMap = Kotlin.HashMap;

(function () {
    var PrimitiveHashMapValuesIterator = Kotlin.$createClass(Kotlin.Iterator, {
        initialize: function (map, keys) {
            this.map = map;
            this.keys = keys;
            this.size = keys.length;
            this.index = 0;
        },
        next: function () {
            return this.map[this.keys[this.index++]];
        },
        hasNext: function () {
            return this.index < this.size;
        }
    });

    var PrimitiveHashMapValues = Kotlin.$createClass(Kotlin.Collection, {
        initialize: function (map) {
            this.map = map;
        },
        iterator: function () {
            return Kotlin.$new(PrimitiveHashMapValuesIterator)(this.map.map, Kotlin.keys(this.map.map));
        },
        isEmpty: function () {
            return this.map.$size === 0;
        },
        contains: function (o) {
            return this.map.containsValue(o);
        }
    });

    Kotlin.PrimitiveHashMap = Kotlin.$createClass(Kotlin.Map, {
        initialize: function () {
            this.$size = 0;
            this.map = {};
        },
        size: function () {
            return this.$size;
        },
        isEmpty: function () {
            return this.$size === 0;
        },
        containsKey: function (key) {
            return this.map[key] !== undefined;
        },
        containsValue: function (value) {
            var map = this.map;
            for (var key in map) {
                if (map.hasOwnProperty(key) && map[key] === value) {
                    return true;
                }
            }

            return false;
        },
        get: function (key) {
            return this.map[key];
        },
        put: function (key, value) {
            var prevValue = this.map[key];
            this.map[key] = value === undefined ? null : value;
            if (prevValue === undefined) {
                this.$size++;
            }
            return prevValue;
        },
        remove: function (key) {
            var prevValue = this.map[key];
            if (prevValue !== undefined) {
                delete this.map[key];
                this.$size--;
            }
            return prevValue;
        },
        clear: function () {
            this.$size = 0;
            this.map = {};
        },
        putAll: function (fromMap) {
            var map = fromMap.map;
            for (var key in map) {
                if (map.hasOwnProperty(key)) {
                    this.map[key] = map[key];
                    this.$size++;
                }
            }
        },
        keySet: function () {
            var result = Kotlin.$new(Kotlin.PrimitiveHashSet)();
            var map = this.map;
            for (var key in map) {
                if (map.hasOwnProperty(key)) {
                    result.add(key);
                }
            }

            return result;
        },
        values: function () {
            return Kotlin.$new(PrimitiveHashMapValues)(this);
        },
        toJSON: function () {
            return this.map;
        }
    });
}());

Kotlin.Set = Kotlin.$createClass(Kotlin.Collection);

Kotlin.PrimitiveHashSet = Kotlin.$createClass(Kotlin.AbstractCollection, {
    initialize: function () {
        this.$size = 0;
        this.map = {};
    },
    contains: function (key) {
        return this.map[key] === true;
    },
    add: function (element) {
        var prevElement = this.map[element];
        this.map[element] = true;
        if (prevElement === true) {
            return false;
        }
        else {
            this.$size++;
            return true;
        }
    },
    remove: function (element) {
        if (this.map[element] === true) {
            delete this.map[element];
            this.$size--;
            return true;
        }
        else {
            return false;
        }
    },
    clear: function () {
        this.$size = 0;
        this.map = {};
    },
    toArray: function () {
        return Kotlin.keys(this.map);
    }
});

(function () {
    function HashSet(hashingFunction, equalityFunction) {
        var hashTable = new Kotlin.HashTable(hashingFunction, equalityFunction);

        this.add = function (o) {
            hashTable.put(o, true);
        };

        this.addAll = function (arr) {
            var i = arr.length;
            while (i--) {
                hashTable.put(arr[i], true);
            }
        };

        this.values = function () {
            return hashTable._keys();
        };

        this.iterator = function () {
            return Kotlin.arrayIterator(this.values());
        };

        this.remove = function (o) {
            return hashTable.remove(o) ? o : null;
        };

        this.contains = function (o) {
            return hashTable.containsKey(o);
        };

        this.clear = function () {
            hashTable.clear();
        };

        this.size = function () {
            return hashTable.size();
        };

        this.isEmpty = function () {
            return hashTable.isEmpty();
        };

        this.clone = function () {
            var h = new HashSet(hashingFunction, equalityFunction);
            h.addAll(hashTable.keys());
            return h;
        };

        this.equals = function (o) {
            if (o === null || o === undefined) return false;
            if (this.size() === o.size()) {
                var iter1 = this.iterator();
                var iter2 = o.iterator();
                while (true) {
                    var hn1 = iter1.hasNext();
                    var hn2 = iter2.hasNext();
                    if (hn1 != hn2) return false;
                    if (!hn2)
                        return true;
                    else {
                        var o1 = iter1.next();
                        var o2 = iter2.next();
                        if (!Kotlin.equals(o1, o2)) return false;
                    }
                }
            }
            return false;
        };

        this.toString = function() {
            var builder = "[";
            var iter = this.iterator();
            var first = true;
            while (iter.hasNext()) {
                if (first)
                    first = false;
                else
                    builder += ", ";
                builder += iter.next();
            }
            builder += "]";
            return builder;
        };

        this.intersection = function (hashSet) {
            var intersection = new HashSet(hashingFunction, equalityFunction);
            var values = hashSet.values(), i = values.length, val;
            while (i--) {
                val = values[i];
                if (hashTable.containsKey(val)) {
                    intersection.add(val);
                }
            }
            return intersection;
        };

        this.union = function (hashSet) {
            var union = this.clone();
            var values = hashSet.values(), i = values.length, val;
            while (i--) {
                val = values[i];
                if (!hashTable.containsKey(val)) {
                    union.add(val);
                }
            }
            return union;
        };

        this.isSubsetOf = function (hashSet) {
            var values = hashTable.keys(), i = values.length;
            while (i--) {
                if (!hashSet.contains(values[i])) {
                    return false;
                }
            }
            return true;
        };
    }

    Kotlin.HashSet = Kotlin.$createClass(Kotlin.Set, {initialize: function () {
        HashSet.call(this);
    }});

    Kotlin.ComplexHashSet = Kotlin.HashSet;
}());
