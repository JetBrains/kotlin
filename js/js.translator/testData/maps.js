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

(function (Kotlin) {
    "use strict";

    /**
     * @class
     * @constructor
     * @param {K} key
     * @param {V} value
     * @template K, V
     */
    // TODO: Make Entry implement kotlin.Map.Entry interface
    function Entry(key, value) {
        this.key = key;
        this.value = value;
    }

    Entry.prototype.getKey = function () {
        return this.key;
    };

    Entry.prototype.getValue = function () {
        return this.value;
    };

    Entry.prototype.hashCode = function() {
        return mapEntryHashCode(this.key, this.value);
    };

    Entry.prototype.equals_za3rmp$ = function(o) {
        // TODO: Check if o is instance of kotlin.Map.Entry
        return o instanceof Entry && Kotlin.equals(this.key, o.getKey()) && Kotlin.equals(this.value, o.getValue());
    };

    Entry.prototype.toString = function() {
        return Kotlin.toString(this.key) + "=" + Kotlin.toString(this.value);
    };

    function hashMapPutAll (fromMap) {
        var entries = fromMap.entries;
        var it = entries.iterator();
        while (it.hasNext()) {
            var e = it.next();
            this.put_wn2jw4$(e.getKey(), e.getValue());
        }
    }

    function hashSetEquals(o) {
        if (o == null || this.size !== o.size) return false;

        return this.containsAll_wtfk93$(o);
    }

    function hashSetHashCode() {
        var h = 0;
        var i = this.iterator();
        while (i.hasNext()) {
            var obj = i.next();
            h += Kotlin.hashCode(obj);
        }
        return h;
    }

    function convertKeyToString(key) { return key; }
    function convertKeyToNumber(key) { return +key; }
    function convertKeyToBoolean(key) { return key == "true"; }

    /** @const */
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
        if (obj == null) return "";

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

    function mapEntryHashCode(key, value) {
        return Kotlin.hashCode(key) ^ Kotlin.hashCode(value);
    }

    function equals_fixedValueHasEquals(fixedValue, variableValue) {
        return fixedValue.equals_za3rmp$(variableValue);
    }

    function equals_fixedValueNoEquals(fixedValue, variableValue) {
        return (variableValue != null && typeof variableValue.equals_za3rmp$ == FUNCTION) ?
               // TODO: test this case
               variableValue.equals_za3rmp$(fixedValue) : (fixedValue === variableValue);
    }

    /**
     * @constructor
     * @param {string} hash
     * @param {Key} firstKey
     * @param {Value} firstValue
     * @param {(function(Key, Key): boolean)|null|undefined} equalityFunction
     * @template  Key, Value
     */
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
            return (searchValue != null && typeof searchValue.equals_za3rmp$ == FUNCTION) ? equals_fixedValueHasEquals : equals_fixedValueNoEquals;
        },

        getEntryForKey: createBucketSearcher(ENTRY),

        getEntryAndIndexForKey: createBucketSearcher(ENTRY_INDEX_AND_VALUE),

        removeEntryForKey: function (key) {
            var result = this.getEntryAndIndexForKey(key);
            if (result) {
                arrayRemoveAt(this.entries, result[0]);
                return result;
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

        containsKey_za3rmp$: createBucketSearcher(EXISTENCE),

        containsValue_za3rmp$: function (value) {
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

    /**
     * @class
     *
     * @constructor
     * @param {(function(Key): string)=} hashingFunctionParam
     * @param {(function(Key, Key): boolean)=} equalityFunctionParam
     * @template Key, Value
     */
    var Hashtable = function (hashingFunctionParam, equalityFunctionParam) {
        var that = this;
        var buckets = [];
        var bucketsByHash = {};

        var hashingFunction = (typeof hashingFunctionParam == FUNCTION) ? hashingFunctionParam : hashObject;
        var equalityFunction = (typeof equalityFunctionParam == FUNCTION) ? equalityFunctionParam : null;

        this.put_wn2jw4$ = function (key, value) {
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

        this.get_za3rmp$ = function (key) {
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

        this.containsKey_za3rmp$ = function (key) {
            var bucketKey = hashingFunction(key);

            // Check if a bucket exists for the bucket key
            var bucket = getBucketForHash(bucketsByHash, bucketKey);

            return bucket ? bucket.containsKey_za3rmp$(key) : false;
        };

        this.containsValue_za3rmp$ = function (value) {
            var i = buckets.length;
            while (i--) {
                if (buckets[i].containsValue_za3rmp$(value)) {
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

        Object.defineProperty(this, "values", {
            get: function () {
                var values = this._values();
                var i = values.length;
                var result = new Kotlin.ArrayList();
                while (i--) {
                    result.add_za3rmp$(values[i]);
                }
                return result;
            },
            configurable: true
        });


        this.remove_za3rmp$ = function (key) {
            var hash = hashingFunction(key), bucketIndex, oldValue = null, result = null;

            // Check if a bucket exists for the bucket key
            var bucket = getBucketForHash(bucketsByHash, hash);

            if (bucket) {
                // Remove entry from this bucket for this key
                result = bucket.removeEntryForKey(key);
                if (result !== null) {
                    oldValue = result[1];

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

        Object.defineProperty(this, "size", {
            get: function () {
                var total = 0, i = buckets.length;
                while (i--) {
                    total += buckets[i].entries.length;
                }
                return total;
            }
        });

        this.each = function (callback) {
            var entries = that._entries(), i = entries.length, entry;
            while (i--) {
                entry = entries[i];
                callback(entry[0], entry[1]);
            }
        };

        /**
         * @param {Hashtable.<Key, Value>} hashtable
         */
        this.putAll_r12sna$ = hashMapPutAll;

        this.clone = function () {
            var clone = new Hashtable(hashingFunctionParam, equalityFunctionParam);
            clone.putAll_r12sna$(that);
            return clone;
        };

        Object.defineProperty(this, "keys", {
            get: function () {
                 var res = new Kotlin.ComplexHashSet();
                 var keys = this._keys();
                 var i = keys.length;
                 while (i--) {
                     res.add_za3rmp$(keys[i]);
                 }
                 return res;
            },
            configurable: true
        });

        Object.defineProperty(this, "entries", {
            get: function () {
                 var result = new Kotlin.ComplexHashSet();
                 var entries = this._entries();
                 var i = entries.length;
                 while (i--) {
                     var entry = entries[i];
                     result.add_za3rmp$(new Entry(entry[0], entry[1]));
                 }

                 return result;
            },
            configurable: true
        });

        this.hashCode = function() {
            var h = 0;
            var entries = this._entries();
            var i = entries.length;
            while (i--) {
                var entry = entries[i];
                h += mapEntryHashCode(entry[0], entry[1]);
            }
            return h;
        };

        this.equals_za3rmp$ = function(o) {
            if (o == null || this.size !== o.size) return false;

            var entries = this._entries();
            var i = entries.length;
            while (i--) {
                var entry = entries[i];
                var key = entry[0];
                var value = entry[1];
                if (value == null) {
                    if (!(o.get_za3rmp$(key) == null && o.contains_za3rmp$(key))) return false;
                }
                else {
                    if (!Kotlin.equals(value, o.get_za3rmp$(key))) return false;
                }
            }
            return true;
        };

        this.toString = function() {
            var entries = this._entries();
            var length = entries.length;
            if (length === 0) return "{}";
            var builder = "{";
            for (var i = 0;;) {
                var entry = entries[i];
                var key = entry[0];
                var value = entry[1];
                builder +=
                    (key === this ? "(this Map)" : Kotlin.toString(key)) +
                    "=" +
                    (value === this ? "(this Map)" : Kotlin.toString(value));
                if (++i >= length) return builder + "}";
                builder += ", "
            }
        };
    };

    Kotlin.HashTable = Hashtable;

    var lazyInitClasses = {};

    lazyInitClasses.HashMap = Kotlin.createClass(
        function () {
            return [Kotlin.modules['builtins'].kotlin.collections.MutableMap];
        },
        function () {
            Kotlin.HashTable.call(this);
        }
    );

    Object.defineProperty(Kotlin, "ComplexHashMap", { get : function () { return Kotlin.HashMap; }});

    /**
     * @class
     * @implements Kotlin.Iterator.<Value>
     *
     * @constructor
     * @param {Kotlin.Map.<Key, Value>} map
     * @param {Array.<Value>} keys
     * @template Key, Value
     */
    lazyInitClasses.PrimitiveHashMapValuesIterator = Kotlin.createClass(
        function () {
            return [Kotlin.modules['builtins'].kotlin.collections.Iterator];
        },
        function (map, keys) {
            this.map = map;
            this.keys = keys;
            this.size = keys.length;
            this.index = 0;
        }, {
            next: function () {
                if (!this.hasNext()) {
                    throw new Kotlin.NoSuchElementException();
                }
                return this.map[this.keys[this.index++]];
            },
            hasNext: function () {
                return this.index < this.size;
            }
    });

    /**
     * @class
     * @implements Kotlin.Collection.<Value>
     *
     * @constructor
     * @param {Kotlin.PrimitiveHashMap.<Key, Value>} map
     * @template Key, Value
     */
    lazyInitClasses.PrimitiveHashMapValues = Kotlin.createClass(
        function () {
            return [Kotlin.AbstractCollection];
        },
        function (map) {
            this.map = map;
        }, {
            iterator: function () {
                return new Kotlin.PrimitiveHashMapValuesIterator(this.map.map, Object.keys(this.map.map));
            },
            isEmpty: function () {
                return this.map.isEmpty();
            },
            size: {
                get: function () {
                    return this.map.size;
                }
            },
            contains_za3rmp$: function (o) {
                return this.map.containsValue_za3rmp$(o);
            }
    });

    /**
     * @class
     * @implements Kotlin.Map.<Key, Value>
     * @constructor
     * @template Key, Value
     */
    lazyInitClasses.AbstractPrimitiveHashMap = Kotlin.createClass(
        function () {
            return [Kotlin.HashMap];
        },
        function () {
            this.$size = 0;
            this.map = Object.create(null);
        }, {
            size: {
                get: function () {
                    return this.$size;
                }
            },
            isEmpty: function () {
                return this.$size === 0;
            },
            containsKey_za3rmp$: function (key) {
                // TODO: should process "__proto__" separately?
                return this.map[key] !== void 0;
            },
            containsValue_za3rmp$: function (value) {
                var map = this.map;
                for (var key in map) {
                    //noinspection JSUnfilteredForInLoop
                    if (map[key] === value) {
                        return true;
                    }
                }

                return false;
            },
            get_za3rmp$: function (key) {
                return this.map[key];
            },
            put_wn2jw4$: function (key, value) {
                var prevValue = this.map[key];
                this.map[key] = value === void 0 ? null : value;
                if (prevValue === void 0) {
                    this.$size++;
                }
                return prevValue;
            },
            remove_za3rmp$: function (key) {
                var prevValue = this.map[key];
                if (prevValue !== void 0) {
                    delete this.map[key];
                    this.$size--;
                }
                return prevValue;
            },
            clear: function () {
                this.$size = 0;
                this.map = {};
            },
            putAll_r12sna$: hashMapPutAll,
            entries: {
                get: function () {
                    var result = new Kotlin.ComplexHashSet();
                    var map = this.map;
                    for (var key in map) {
                        //noinspection JSUnfilteredForInLoop
                        result.add_za3rmp$(new Entry(this.convertKeyToKeyType(key), map[key]));
                    }

                    return result;
                }
            },
            getKeySetClass: function () {
                throw new Error("Kotlin.AbstractPrimitiveHashMap.getKetSetClass is abstract");
            },
            convertKeyToKeyType: function(key) {
                throw new Error("Kotlin.AbstractPrimitiveHashMap.convertKeyToKeyType is abstract");
            },
            keys: {
                get: function () {
                    var result = new (this.getKeySetClass())();
                    var map = this.map;
                    for (var key in map) {
                        //noinspection JSUnfilteredForInLoop
                        result.add_za3rmp$(key);
                    }

                    return result;
                }
            },
            values: {
                get: function () {
                    return new Kotlin.PrimitiveHashMapValues(this);
                }
            },
            toJSON: function () {
                return this.map;
            },
            toString: function() {
                if (this.isEmpty()) return "{}";
                var map = this.map;
                var isFirst = true;
                var builder = "{";
                for (var key in map) {
                    var value = map[key];
                    builder +=
                        (isFirst ? "": ", ") +
                        Kotlin.toString(key) +
                        "=" +
                        (value === this ? "(this Map)" : Kotlin.toString(value));
                    isFirst = false;
                }
                return builder + "}";
            },
            equals_za3rmp$: function(o) {
                if (o == null || this.size !== o.size) return false;
                var map = this.map;
                for (var key in map) {
                    var key_ = this.convertKeyToKeyType(key);
                    var value = map[key];
                    if (value == null) {
                        if (!(o.get_za3rmp$(key_) == null && o.contains_za3rmp$(key_))) return false;
                    }
                    else {
                        if (!Kotlin.equals(value, o.get_za3rmp$(key_))) return false;
                    }
                }
                return true;
            },
            hashCode: function() {
                var h = 0;
                var map = this.map;
                for (var key in map) {
                    //noinspection JSUnfilteredForInLoop
                    h += mapEntryHashCode(this.convertKeyToKeyType(key), map[key]);
                }
                return h;
            }
    });

    lazyInitClasses.DefaultPrimitiveHashMap = Kotlin.createClass(
        function () {
            return [Kotlin.AbstractPrimitiveHashMap];
        },
        function () {
            Kotlin.AbstractPrimitiveHashMap.call(this);
        }, {
            getKeySetClass: function () {
                return Kotlin.DefaultPrimitiveHashSet;
            },
            convertKeyToKeyType: convertKeyToString
    });

    lazyInitClasses.PrimitiveNumberHashMap = Kotlin.createClass(
        function () {
            return [Kotlin.AbstractPrimitiveHashMap];
        },
        function () {
            Kotlin.AbstractPrimitiveHashMap.call(this);
            this.$keySetClass$ = Kotlin.PrimitiveNumberHashSet;
        }, {
            getKeySetClass: function () {
                return Kotlin.PrimitiveNumberHashSet;
            },
            convertKeyToKeyType: convertKeyToNumber
    });

    lazyInitClasses.PrimitiveBooleanHashMap = Kotlin.createClass(
        function () {
            return [Kotlin.AbstractPrimitiveHashMap];
        },
        function () {
            Kotlin.AbstractPrimitiveHashMap.call(this);
        }, {
            getKeySetClass: function () {
                return Kotlin.PrimitiveBooleanHashSet;
            },
            convertKeyToKeyType: convertKeyToBoolean
    });

    function LinkedHashMap() {
        Kotlin.ComplexHashMap.call(this);
        this.orderedKeys = [];

        this.super_put_wn2jw4$ = this.put_wn2jw4$;
        this.put_wn2jw4$ = function(key, value) {
            if (!this.containsKey_za3rmp$(key)) {
                this.orderedKeys.push(key);
            }

            return this.super_put_wn2jw4$(key, value);
        };

        this.super_remove_za3rmp$ = this.remove_za3rmp$;
        this.remove_za3rmp$ = function(key) {
            var i = this.orderedKeys.indexOf(key);
            if (i != -1) {
                this.orderedKeys.splice(i, 1);
            }

            return this.super_remove_za3rmp$(key);
        };

        this.super_clear = this.clear;
        this.clear = function() {
            this.super_clear();
            this.orderedKeys = [];
        };

        Object.defineProperty(this, "keys", {
            get: function () {
                // TODO return special Set which unsupported adding
                var set = new Kotlin.LinkedHashSet();
                set.map = this;
                return set;
            }
        });

        Object.defineProperty(this, "entries", {
            get: function () {
                var result = new Kotlin.ArrayList();

                for (var i = 0, c = this.orderedKeys, l = c.length; i < l; i++) {
                    result.add_za3rmp$(this.get_za3rmp$(c[i]));
                }

                return result;
            }
        });

        Object.defineProperty(this, "entries", {
            get: function () {
                var set = new Kotlin.LinkedHashSet();

                for (var i = 0, c = this.orderedKeys, l = c.length; i < l; i++) {
                    set.add_za3rmp$(new Entry(c[i], this.get_za3rmp$(c[i])));
                }

                return set;
            }
        });
    }

    lazyInitClasses.LinkedHashMap = Kotlin.createClass(
        function () {
            return [Kotlin.ComplexHashMap];
        },
        function () {
            LinkedHashMap.call(this);
        }
    );

    lazyInitClasses.LinkedHashSet = Kotlin.createClass(
        function () {
            return [Kotlin.modules['builtins'].kotlin.collections.MutableSet, Kotlin.HashSet];
        },
        /** @constructs */
        function () {
            this.map = new Kotlin.LinkedHashMap();
        },
        /** @lends {Kotlin.LinkedHashSet.prototype} */
        {
            equals_za3rmp$: hashSetEquals,
            hashCode: hashSetHashCode,
            size: {
                get: function () {
                    return this.map.size;
                }
            },
            contains_za3rmp$: function (element) {
                return this.map.containsKey_za3rmp$(element);
            },
            iterator: function () {
                return new Kotlin.SetIterator(this);
            },
            add_za3rmp$: function (element) {
                return this.map.put_wn2jw4$(element, true) == null;
            },
            remove_za3rmp$: function (element) {
                return this.map.remove_za3rmp$(element) != null;
            },
            clear: function () {
                this.map.clear();
            },
            toArray: function () {
                return this.map.orderedKeys.slice();
            }
    });

    /**
     * @class
     * @constructor
     * @param {Kotlin.Set} set
     */
    lazyInitClasses.SetIterator = Kotlin.createClass(
        function () {
            return [Kotlin.modules['builtins'].kotlin.collections.MutableIterator];
        },
        function (set) {
            this.set = set;
            this.keys = set.toArray();
            this.index = 0;
        },
        /** @lends SetIterator.prototype */
        {
            next: function () {
                if (!this.hasNext()) {
                    throw new Kotlin.NoSuchElementException();
                }
                return this.keys[this.index++];
            },
            hasNext: function () {
                return this.index < this.keys.length;
            },
            remove: function () {
                if (this.index === 0) {
                    throw Kotlin.IllegalStateException();
                }
                this.set.remove_za3rmp$(this.keys[this.index - 1]);
            }
    });

    /**
     * @class
     * @constructor
     * @extends {Kotlin.Collection.<T>}
     * @template T
     */
    lazyInitClasses.AbstractPrimitiveHashSet = Kotlin.createClass(
        function () {
            return [Kotlin.HashSet];
        },
        /** @constructs */
        function () {
            this.$size = 0;
            this.map = Object.create(null);
        },
        /** @lends {Kotlin.AbstractPrimitiveHashSet.prototype} */
        {
            equals_za3rmp$: hashSetEquals,
            hashCode: hashSetHashCode,
            size: {
                get: function () {
                    return this.$size;
                }
            },
            contains_za3rmp$: function (key) {
                return this.map[key] === true;
            },
            iterator: function () {
                return new Kotlin.SetIterator(this);
            },
            add_za3rmp$: function (element) {
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
            remove_za3rmp$: function (element) {
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
            convertKeyToKeyType: function (key) {
                throw new Error("Kotlin.AbstractPrimitiveHashSet.convertKeyToKeyType is abstract");
            },
            toArray: function () {
                var result = Object.keys(this.map);
                for(var i=0; i<result.length; i++) {
                    result[i] = this.convertKeyToKeyType(result[i]);
                }
                return result;
            }
    });

    lazyInitClasses.DefaultPrimitiveHashSet = Kotlin.createClass(
        function () {
            return [Kotlin.AbstractPrimitiveHashSet];
        },
        /** @constructs */
        function () {
            Kotlin.AbstractPrimitiveHashSet.call(this);
        },
        /** @lends {Kotlin.DefaultPrimitiveHashSet.prototype} */
        {
            toArray: function () {
                return Object.keys(this.map);
            },
            convertKeyToKeyType: convertKeyToString
    });

    lazyInitClasses.PrimitiveNumberHashSet = Kotlin.createClass(
        function () {
            return [Kotlin.AbstractPrimitiveHashSet];
        },
        /** @constructs */
        function () {
            Kotlin.AbstractPrimitiveHashSet.call(this);
        },
        /** @lends {Kotlin.PrimitiveNumberHashSet.prototype} */
        {
            convertKeyToKeyType: convertKeyToNumber
    });

    lazyInitClasses.PrimitiveBooleanHashSet = Kotlin.createClass(
        function () {
            return [Kotlin.AbstractPrimitiveHashSet];
        },
        /** @constructs */
        function () {
            Kotlin.AbstractPrimitiveHashSet.call(this);
        },
        /** @lends {Kotlin.PrimitiveBooleanHashSet.prototype} */
        {
            convertKeyToKeyType: convertKeyToBoolean
    });

    /**
     * @class
     * @constructor
     * @param {(function(Key): string)=} hashingFunction
     * @param {(function(Key, Key): boolean)=} equalityFunction
     * @template Key, Value
     */
    function HashSet(hashingFunction, equalityFunction) {
        var hashTable = new Kotlin.HashTable(hashingFunction, equalityFunction);

        this.addAll_wtfk93$ = Kotlin.AbstractCollection.prototype.addAll_wtfk93$;
        this.removeAll_wtfk93$ = Kotlin.AbstractCollection.prototype.removeAll_wtfk93$;
        this.retainAll_wtfk93$ = Kotlin.AbstractCollection.prototype.retainAll_wtfk93$;
        this.containsAll_wtfk93$ = Kotlin.AbstractCollection.prototype.containsAll_wtfk93$;

        this.add_za3rmp$ = function (o) {
            return !hashTable.put_wn2jw4$(o, true);
        };

        this.toArray = function () {
            return hashTable._keys();
        };

        /** @suppress {checkTypes} */
        this.iterator = function () {
            return new Kotlin.SetIterator(this);
        };

        this.remove_za3rmp$ = function (o) {
            return hashTable.remove_za3rmp$(o) != null;
        };

        this.contains_za3rmp$ = function (o) {
            return hashTable.containsKey_za3rmp$(o);
        };

        this.clear = function () {
            hashTable.clear();
        };

        Object.defineProperty(this, "size", { get: function () {
            return hashTable.size;
        }});

        this.isEmpty = function () {
            return hashTable.isEmpty();
        };

        this.clone = function () {
            var h = new HashSet(hashingFunction, equalityFunction);
            h.addAll_wtfk93$(hashTable.keys());
            return h;
        };

        this.equals_za3rmp$ = hashSetEquals;

        this.toString = function () {
            var builder = "[";
            var iter = this.iterator();
            var first = true;
            while (iter.hasNext()) {
                if (first) {
                    first = false;
                }
                else {
                    builder += ", ";
                }
                builder += iter.next();
            }
            builder += "]";
            return builder;
        };

        this.intersection = function (hashSet) {
            var intersection = new HashSet(hashingFunction, equalityFunction);
            var values = hashSet.values, i = values.length, val;
            while (i--) {
                val = values[i];
                if (hashTable.containsKey_za3rmp$(val)) {
                    intersection.add_za3rmp$(val);
                }
            }
            return intersection;
        };

        this.union = function (hashSet) {
            var union = this.clone();
            var values = hashSet.values, i = values.length, val;
            while (i--) {
                val = values[i];
                if (!hashTable.containsKey_za3rmp$(val)) {
                    union.add_za3rmp$(val);
                }
            }
            return union;
        };

        this.isSubsetOf = function (hashSet) {
            var values = hashTable.keys(), i = values.length;
            while (i--) {
                if (!hashSet.contains_za3rmp$(values[i])) {
                    return false;
                }
            }
            return true;
        };

        this.hashCode = hashSetHashCode;
    }

    lazyInitClasses.HashSet = Kotlin.createClass(
        function () {
            return [Kotlin.modules['builtins'].kotlin.collections.MutableSet, Kotlin.AbstractCollection];
        },
        function () {
            HashSet.call(this);
        }
    );

    Object.defineProperty(Kotlin, "ComplexHashSet", { get : function () { return Kotlin.HashSet; }});

    Kotlin.createDefinition(lazyInitClasses, Kotlin);
}(Kotlin));
