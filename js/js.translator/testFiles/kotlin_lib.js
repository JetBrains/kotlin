/**
 * Copyright 2010 Tim Down.
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

// todo org.jetbrains.k2js.test.semantics.WebDemoExamples2Test#testBuilder
var kotlin = {set:function (receiver, key, value) {
    return receiver.put(key, value);
}};

(function () {
    "use strict";

    Kotlin.equals = function (obj1, obj2) {
        if ((obj1 === null)|| (obj1 === undefined)) return obj2 === null;
        if (typeof obj1 == "object") {
            if (obj1.equals !== undefined) {
                return obj1.equals(obj2);
            }
        }
        return obj1 === obj2;
    };

    Kotlin.array = function (args) {
        var answer = [];
        if (args !== null && args !== undefined) {
            for (var i = 0, n = args.length; i < n; ++i) {
                answer[i] = args[i]
            }
        }
        return answer;
    };

    Kotlin.upto = function (from, limit, reversed) {
        return Kotlin.$new(Kotlin.NumberRange)(from, limit - from, reversed).iterator();
    };

    Kotlin.modules = {};
    Kotlin.Exceptions = {};
    Kotlin.Exception = Kotlin.$createClass();
    Kotlin.RuntimeException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.Exceptions.IndexOutOfBounds = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.Exceptions.NullPointerException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.Exceptions.NoSuchElementException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.Exceptions.IllegalArgumentException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.Exceptions.IllegalStateException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.Exceptions.IndexOutOfBoundsException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.Exceptions.UnsupportedOperationException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.Exceptions.IOException = Kotlin.$createClass(Kotlin.Exception);

    Kotlin.throwNPE = function () {
        throw Kotlin.$new(Kotlin.Exceptions.NullPointerException)();
    };

    function throwAbstractFunctionInvocationError() {
        throw new TypeError("Function is abstract");
    }

    Kotlin.Iterator = Kotlin.$createClass({
        initialize: function () {
        },
        next: throwAbstractFunctionInvocationError,
        get_hasNext: throwAbstractFunctionInvocationError
    });

    var ArrayIterator = Kotlin.$createClass(Kotlin.Iterator, {
        initialize: function (array) {
            this.array = array;
            this.size = array.length;
            this.index = 0;
        },
        next: function () {
            return this.array[this.index++];
        },
        get_hasNext: function () {
            return this.index < this.size;
        }
    });

    var ListIterator = Kotlin.$createClass(ArrayIterator, {
        initialize: function (list) {
            this.list = list;
            this.size = list.size();
            this.index = 0;
        },
        next: function () {
            return this.list.get(this.index++);
        },
        get_hasNext: function () {
            return this.index < this.size;
        }
    });

    Kotlin.AbstractList = Kotlin.$createClass({
        iterator: function () {
            return Kotlin.$new(ListIterator)(this);
        },
        isEmpty: function () {
            return this.size() == 0;
        },
        addAll: function (collection) {
            var it = collection.iterator();
            while (it.get_hasNext()) {
                this.add(it.next());
            }
        },
        remove: function (o) {
            var index = this.indexOf(o);
            if (index != -1) {
                this.removeAt(index);
            }
        },
        contains: function (o) {
            return this.indexOf(o) != -1;
        },
        equals: function (o) {
            if (this.$size === o.$size) {
                var iter1 = this.iterator();
                var iter2 = o.iterator();
                while (true) {
                    var hn1 = iter1.get_hasNext();
                    var hn2 = iter2.get_hasNext();
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
        },
        toString: function() {
            var builder = "[";
            var iter = this.iterator();
            var first = true;
            while (iter.get_hasNext()) {
                if (first)
                    first = false;
                else
                    builder += ", ";
                builder += iter.next();
            }
            builder += "]";
            return builder;
        }
    });

    Kotlin.ArrayList = Kotlin.$createClass(Kotlin.AbstractList, {
        initialize: function () {
            this.array = [];
            this.$size = 0;
        },
        get: function (index) {
            if (index < 0 || index >= this.$size) {
                throw Kotlin.Exceptions.IndexOutOfBounds;
            }
            return this.array[index];
        },
        set: function (index, value) {
            if (index < 0 || index >= this.$size) {
                throw Kotlin.Exceptions.IndexOutOfBounds;
            }
            this.array[index] = value;
        },
        toArray: function () {
            return this.array.slice(0, this.$size);
        },
        size: function () {
            return this.$size;
        },
        iterator: function () {
            return Kotlin.arrayIterator(this.array);
        },
        add: function (element) {
            this.array[this.$size++] = element;
        },
        addAt: function (index, element) {
            this.array.splice(index, 0, element);
        },
        removeAt: function (index) {
            this.array.splice(index, 1);
            this.$size--;
        },
        clear: function () {
            this.array.length = 0;
            this.$size = 0;
        },
        indexOf: function (o) {
            for (var i = 0, n = this.$size; i < n; ++i) {
                if (Kotlin.equals(this.array[i], o)) {
                    return i;
                }
            }
            return -1;
        }
    });

    Kotlin.Runnable = Kotlin.$createClass({
        initialize: function () {
        },
        run: throwAbstractFunctionInvocationError
    });

    Kotlin.Comparable = Kotlin.$createClass({
        initialize: function () {
        },
        compareTo: throwAbstractFunctionInvocationError
    });

    Kotlin.Appendable = Kotlin.$createClass({
        initialize: function () {
        },
        append: throwAbstractFunctionInvocationError
    });

    Kotlin.parseInt = function (str) {
        return parseInt(str, 10);
    };

    Kotlin.safeParseInt = function(str) {
        var r = parseInt(str, 10);
        return isNaN(r) ? null : r;
    };

    Kotlin.safeParseDouble = function(str) {
        var r = parseFloat(str, 10);
        return isNaN(r) ? null : r;
    };

    Kotlin.System = function () {
        var output = "";

        var print = function (obj) {
            if (obj !== undefined) {
                if (obj === null || typeof obj !== "object") {
                    output += obj;
                }
                else {
                    output += obj.toString();
                }
            }
        };
        var println = function (obj) {
            this.print(obj);
            output += "\n";
        };

        return {
            out:function () {
                return {
                    print:print,
                    println:println
                };
            },
            output:function () {
                return output;
            },
            flush:function () {
                output = "";
            }
        };
    }();

    Kotlin.println = function (s) {
        Kotlin.System.out().println(s);
    };

    Kotlin.print = function (s) {
        Kotlin.System.out().print(s);
    };

    Kotlin.RangeIterator = Kotlin.$createClass(Kotlin.Iterator, {
        initialize: function (start, count, reversed) {
            this.$start = start;
            this.$count = count;
            this.$reversed = reversed;
            this.$i = this.get_start();
        },
        get_start: function () {
            return this.$start;
        },
        get_count: function () {
            return this.$count;
        },
        set_count: function (tmp$0) {
            this.$count = tmp$0;
        },
        get_reversed: function () {
            return this.$reversed;
        },
        get_i: function () {
            return this.$i;
        },
        set_i: function (tmp$0) {
            this.$i = tmp$0;
        },
        next: function () {
            this.set_count(this.get_count() - 1);
            if (this.get_reversed()) {
                this.set_i(this.get_i() - 1);
                return this.get_i() + 1;
            }
            else {
                this.set_i(this.get_i() + 1);
                return this.get_i() - 1;
            }
        },
        get_hasNext: function () {
            return this.get_count() > 0;
        }
    });

    Kotlin.NumberRange = Kotlin.$createClass({
        initialize: function (start, size, reversed) {
            this.$start = start;
            this.$size = size;
            this.$reversed = reversed;
        },
        get_start: function () {
            return this.$start;
        },
        get_size: function () {
            return this.$size;
        },
        get_reversed: function () {
            return this.$reversed;
        },
        get_end: function () {
            return this.get_reversed() ? this.get_start() - this.get_size() + 1 : this.get_start() + this.get_size() - 1;
        },
        contains: function (number) {
            if (this.get_reversed()) {
                return number <= this.get_start() && number > this.get_start() - this.get_size();
            }
            else {
                return number >= this.get_start() && number < this.get_start() + this.get_size();
            }
        },
        iterator: function () {
            return Kotlin.$new(Kotlin.RangeIterator)(this.get_start(), this.get_size(), this.get_reversed());
        }
    });

    Kotlin.Comparator = Kotlin.$createClass({
        initialize: function () {
        },
        compare: throwAbstractFunctionInvocationError
    });

    var ComparatorImpl = Kotlin.$createClass(Kotlin.Comparator, {
        initialize: function (comparator) {
            this.compare = comparator;
        }
    });

    Kotlin.comparator = function (f) {
        return Kotlin.$new(ComparatorImpl)(f);
    };

    Kotlin.collectionsMax = function (col, comp) {
        var it = col.iterator();
        if (col.isEmpty()) {
            //TODO: which exception?
            throw Kotlin.Exception();
        }
        var max = it.next();
        while (it.get_hasNext()) {
            var el = it.next();
            if (comp.compare(max, el) < 0) {
                max = el;
            }
        }
        return max;
    };

    Kotlin.StringBuilder = Kotlin.$createClass(
            {
                initialize:function () {
                    this.string = "";
                },
                append:function (obj) {
                    this.string = this.string + obj.toString();
                },
                toString:function () {
                    return this.string;
                }
            }
    );

    Kotlin.splitString = function (str, regex) {
        return str.split(regex);
    };

    Kotlin.nullArray = function (size) {
        var res = [];
        var i = size;
        while (i > 0) {
            res[--i] = null;
        }
        return res;
    };

    Kotlin.arrayFromFun = function (size, initFun) {
        var res = [];
        var i = size;
        while (i > 0) {
            res[--i] = initFun(i);
        }
        return res;
    };

    Kotlin.arrayIndices = function (arr) {
        return Kotlin.$new(Kotlin.NumberRange)(0, arr.length);
    };

    Kotlin.arrayIterator = function (array) {
        return Kotlin.$new(ArrayIterator)(array);
    };

    Kotlin.toString = function (obj) {
        return obj.toString();
    };

    Kotlin.jsonFromTuples = function (pairArr) {
        var i = pairArr.length;
        var res = {};
        while (i > 0) {
            --i;
            res[pairArr[i][0]] = pairArr[i][1];
        }
        return res;
    };

    //TODO: use intrinsic
    Kotlin.jsonSet = function (obj, attrName, value) {
        obj[attrName] = value;
    };

    Kotlin.jsonGet = function (obj, attrName) {
        return obj[attrName];
    };


    Kotlin.jsonAddProperties = function (obj1, obj2) {
        for (var p in obj2) {
            if (obj2.hasOwnProperty(p)) {
                obj1[p] = obj2[p];
            }
        }
        return obj1;
    };

    //TODO: use intrinsic
    Kotlin.sure = function (obj) {
        return obj;
    };

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
            } else if (typeof obj.hashCode == FUNCTION) {
                // Check the hashCode method really has returned a string
                hashCode = obj.hashCode();
                return (typeof hashCode == "string") ? hashCode : hashObject(hashCode);
            } else if (typeof obj.toString == FUNCTION) {
                return obj.toString();
            }
            else {
                try {
                    return String(obj);
                } catch (ex) {
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
                } else if (typeof kv == "undefined") {
                    throw new Error(kvStr + " must not be undefined");
                }
            };
        }

        var checkKey = createKeyValCheck("key"), checkValue = createKeyValCheck("value");

        /*----------------------------------------------------------------------------------------------------------------*/

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
            getEqualityFunction:function (searchValue) {
                return (typeof searchValue.equals == FUNCTION) ? equals_fixedValueHasEquals : equals_fixedValueNoEquals;
            },

            getEntryForKey:createBucketSearcher(ENTRY),

            getEntryAndIndexForKey:createBucketSearcher(ENTRY_INDEX_AND_VALUE),

            removeEntryForKey:function (key) {
                var result = this.getEntryAndIndexForKey(key);
                if (result) {
                    arrayRemoveAt(this.entries, result[0]);
                    return result[1];
                }
                return null;
            },

            addEntry:function (key, value) {
                this.entries[this.entries.length] = [key, value];
            },

            keys:createBucketLister(0),

            values:createBucketLister(1),

            getEntries:function (entries) {
                var startIndex = entries.length;
                for (var i = 0, len = this.entries.length; i < len; ++i) {
                    // Clone the entry stored in the bucket before adding to array
                    entries[startIndex + i] = this.entries[i].slice(0);
                }
            },

            containsKey:createBucketSearcher(EXISTENCE),

            containsValue:function (value) {
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

            this.values = function() {
                var values = this._values();
                var i = values.length
                var result = Kotlin.$new(Kotlin.ArrayList)();
                while (--i) {
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
                var entries = that.entries(), i = entries.length, entry;
                while (i--) {
                    entry = entries[i];
                    callback(entry[0], entry[1]);
                }
            };


            this.putAll = function (hashtable, conflictCallback) {
                var entries = hashtable.entries();
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
                var res = Kotlin.$new(Kotlin.HashSet)();
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

    Kotlin.HashMap = Kotlin.$createClass({
                                             initialize: function () {
                                                 Kotlin.HashTable.call(this);
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

        Kotlin.HashSet = Kotlin.$createClass({initialize: function () {
            HashSet.call(this);
        }});
    }());
})();
