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

/*  Prototype JavaScript framework, version 1.6.1
 *  (c) 2005-2009 Sam Stephenson
 *
 *  Prototype is freely distributable under the terms of an MIT-style license.
 *  For details, see the Prototype web site: http://www.prototypejs.org/
 *
 *--------------------------------------------------------------------------*/
var Kotlin;
(function () {
    "use strict";
    function $A(iterable) {
        if (!iterable) return [];
        if ('toArray' in Object(iterable)) return iterable.toArray();
        var length = iterable.length || 0, results = new Array(length);
        while (length--) results[length] = iterable[length];
        return results;
    }

    (function () {


        function extend(destination, source) {
            for (var property in source)
                destination[property] = source[property];
            return destination;
        }


        function keys(object) {
            var results = [];
            for (var property in object) {
                if (object.hasOwnProperty(property)) {
                    results.push(property);
                }
            }
            return results;
        }

        function values(object) {
            var results = [];
            for (var property in object)
                results.push(object[property]);
            return results;
        }

        extend(Object, {
            extend:extend,
            keys:Object.keys || keys,
            values:values
        });
    })();


    Object.extend(Function.prototype, (function () {
        var slice = Array.prototype.slice;

        function update(array, args) {
            var arrayLength = array.length, length = args.length;
            while (length--) array[arrayLength + length] = args[length];
            return array;
        }

        function merge(array, args) {
            array = slice.call(array, 0);
            return update(array, args);
        }

        function argumentNames() {
            var names = this.toString().match(/^[\s\(]*function[^(]*\(([^)]*)\)/)[1]
                    .replace(/\/\/.*?[\r\n]|\/\*(?:.|[\r\n])*?\*\//g, '')
                    .replace(/\s+/g, '').split(',');
            return names.length == 1 && !names[0] ? [] : names;
        }

        function bind(context) {
            if (arguments.length < 2 && Object.isUndefined(arguments[0])) return this;
            var __method = this, args = slice.call(arguments, 1);
            return function () {
                var a = merge(args, arguments);
                return __method.apply(context, a);
            };
        }

        function bindAsEventListener(context) {
            var __method = this, args = slice.call(arguments, 1);
            return function (event) {
                var a = update([event || window.event], args);
                return __method.apply(context, a);
            };
        }

        function wrap(wrapper) {
            var __method = this;
            return function () {
                var a = update([__method.bind(this)], arguments);
                return wrapper.apply(this, a);
            };
        }

        return {
            argumentNames:argumentNames,
            bind:bind,
            bindAsEventListener:bindAsEventListener,
            wrap:wrap
        };
    })());

    var isType = function (object, klass) {
        if (object === null) {
            return false;
        }
        var current = object.get_class();
        while (current !== klass) {
            if (current === null) {
                return false;
            }
            current = current.superclass;
        }
        return true;
    };

    var emptyFunction = function () {
    };

    var Class = (function () {

        function subclass() {
        }

        function create() {
            var parent = null, properties = $A(arguments);
            if (typeof (properties[0]) == "function") {
                parent = properties.shift();
            }

            function klass() {
                this.initializing = klass;
                this.initialize.apply(this, arguments);
            }

            Object.extend(klass, Class.Methods);
            klass.superclass = parent;
            klass.subclasses = [];

            if (parent) {
                subclass.prototype = parent.prototype;
                klass.prototype = new subclass();
                parent.subclasses.push(klass);
            }

            klass.addMethods(
                    {
                        get_class:function () {
                            return klass;
                        }
                    });

            if (parent !== null) {
                klass.addMethods(
                        {
                            super_init:function () {
                                this.initializing = this.initializing.superclass;
                                this.initializing.prototype.initialize.apply(this, arguments);
                            }
                        });
            }

            for (var i = 0, length = properties.length; i < length; i++)
                klass.addMethods(properties[i]);

            if (!klass.prototype.initialize) {
                klass.prototype.initialize = emptyFunction;
            }

            klass.prototype.constructor = klass;
            return klass;
        }

        function addMethods(source) {
            var ancestor = this.superclass && this.superclass.prototype,
                    properties = Object.keys(source);


            for (var i = 0, length = properties.length; i < length; i++) {
                var property = properties[i], value = source[property];
                if (ancestor && (typeof (value) == "function") &&
                    value.argumentNames()[0] == "$super") {
                    var method = value;
                    value = (function (m) {
                        return function () {
                            return ancestor[m].apply(this, arguments);
                        };
                    })(property).wrap(method);

                }
                this.prototype[property] = value;
            }

            return this;
        }

        return {
            create:create,
            Methods:{
                addMethods:addMethods
            }
        };
    })();

    var Trait = (function () {

        function add(object, source) {
            var properties = Object.keys(source);
            for (var i = 0, length = properties.length; i < length; i++) {
                var property = properties[i];
                var value = source[property];
                object[property] = value;
            }
            return this;
        }

        function create() {
            var result = {};
            for (var i = 0, length = arguments.length; i < length; i++) {
                add(result, arguments[i]);
            }
            return result;
        }

        return {
            create:create
        };
    })();


    var Namespace = (function () {

        function create() {
            return Trait.create.apply(Trait, arguments);
        }

        return {
            create:create
        };
    })();

    var object = (function () {
        function create() {
            var singletonClass = Class.create.apply(Class, arguments);
            return new singletonClass();
        }

        return {
            create:create
        };
    })();

    Kotlin = {};
    Kotlin.Class = Class;
    Kotlin.Namespace = Namespace;
    Kotlin.Trait = Trait;
    Kotlin.isType = isType;
    Kotlin.object = object;

    Kotlin.equals = function (obj1, obj2) {
        if (typeof obj1 == "object") {
            if (obj1.equals !== undefined) {
                return obj1.equals(obj2);
            }
        }
        return (obj1 === obj2);
    };

    Kotlin.Exceptions = {};
    Kotlin.Exception = Kotlin.Class.create();
    Kotlin.RuntimeException = Kotlin.Class.create(Kotlin.Exception);
    Kotlin.Exceptions.IndexOutOfBounds = Kotlin.Class.create(Kotlin.Exception);
    Kotlin.Exceptions.NullPointerException = Kotlin.Class.create(Kotlin.Exception);
    Kotlin.Exceptions.NoSuchElementException = Kotlin.Class.create(Kotlin.Exception);
    Kotlin.Exceptions.IllegalArgumentException = Kotlin.Class.create(Kotlin.Exception);
    Kotlin.Exceptions.IllegalStateException = Kotlin.Class.create(Kotlin.Exception);
    Kotlin.Exceptions.IndexOutOfBoundsException = Kotlin.Class.create(Kotlin.Exception);
    Kotlin.Exceptions.UnsupportedOperationException = Kotlin.Class.create(Kotlin.Exception);

    Kotlin.throwNPE = function() {
        throw new Kotlin.Exceptions.NullPointerException();
    };

    Kotlin.ArrayList = Class.create({
        initialize:function () {
            this.array = [];
            this.$size = 0;
        },
        get:function (index) {
            if ((index < 0) || (index >= this.$size)) {
                throw Kotlin.Exceptions.IndexOutOfBounds;
            }
            return (this.array)[index];
        },
        set:function (index, value) {
            if ((index < 0) || (index >= this.$size)) {
                throw Kotlin.Exceptions.IndexOutOfBounds;
            }
            (this.array)[index] = value;
        },
        size:function () {
            return this.$size;
        },
        iterator:function () {
            return new Kotlin.ArrayIterator(this);
        },
        isEmpty:function () {
            return (this.$size === 0);
        },
        add:function (element) {
            this.array[this.$size++] = element;
        },
        addAll:function (collection) {
            var it = collection.iterator();
            while (it.hasNext()) {
                this.add(it.next());
            }
        },
        remove:function(value) {
            for (var i = 0; i < this.$size; ++i) {
                if (this.array[i] == value) {
                    this.removeByIndex(i);
                    return;
                }
            }
        },
        removeByIndex:function (index) {
            for (var i = index; i < this.$size - 1; ++i) {
                this.array[i] = this.array[i + 1];
            }
            this.$size--;
        },
        clear:function () {
            this.array = [];
            this.$size = 0;
        },
        contains:function (obj) {
            for (var i = 0; i < this.$size; ++i) {
                if (Kotlin.equals(this.array[i], obj)) {
                    return true;
                }
            }
            return false;
        }
    });


    Kotlin.AbstractList = Class.create({
        set:function (index, value) {
            throw new Kotlin.Exceptions.UnsupportedOperationException();
        },
        iterator:function () {
            return new Kotlin.ArrayIterator(this);
        },
        isEmpty:function () {
            return (this.size() === 0);
        },
        add:function (element) {
            throw new Kotlin.Exceptions.UnsupportedOperationException();
        },
        addAll:function (collection) {
            var it = collection.iterator();
            while (it.hasNext()) {
                this.add(it.next());
            }
        },
        remove:function(value) {
            for (var i = 0; i < this.$size; ++i) {
                if (this.array[i] == value) {
                    this.removeByIndex(i);
                    return;
                }
            }
        },
        removeByIndex:function (index) {
            throw new Kotlin.Exceptions.UnsupportedOperationException();
        },
        clear:function () {
            throw new Kotlin.Exceptions.UnsupportedOperationException();
        },
        contains:function (obj) {
            for (var i = 0; i < this.$size; ++i) {
                if (Kotlin.equals(this.array[i], obj)) {
                    return true;
                }
            }
            return false;
        }
    });


    Kotlin.parseInt = function (str) {
        return parseInt(str, 10);
    }
    ;

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

    Kotlin.AbstractFunctionInvocationError = Class.create();

    Kotlin.Iterator = Class.create({
        initialize:function () {
        },
        next:function () {
            throw new Kotlin.AbstractFunctionInvocationError();
        },
        hasNext:function () {
            throw new Kotlin.AbstractFunctionInvocationError();
        }
    });

    Kotlin.ArrayIterator = Class.create(Kotlin.Iterator, {
        initialize:function (array) {
            this.array = array;
            this.index = 0;
        },
        next:function () {
            return this.array.get(this.index++);
        },
        hasNext:function () {
            return (this.array.size() > this.index);
        },
        get_hasNext:function () {
            return this.hasNext();
        }
    });


    Kotlin.RangeIterator = Kotlin.Class.create(Kotlin.Iterator, {
        initialize:function (start, count, reversed) {
            this.$start = start;
            this.$count = count;
            this.$reversed = reversed;
            this.$i = this.get_start();
        }, get_start:function () {
            return this.$start;
        }, get_count:function () {
            return this.$count;
        }, set_count:function (tmp$0) {
            this.$count = tmp$0;
        }, get_reversed:function () {
            return this.$reversed;
        }, get_i:function () {
            return this.$i;
        }, set_i:function (tmp$0) {
            this.$i = tmp$0;
        }, next:function () {
            this.set_count(this.get_count() - 1);
            if (this.get_reversed()) {
                this.set_i(this.get_i() - 1);
                return this.get_i() + 1;
            }
            else {
                this.set_i(this.get_i() + 1);
                return this.get_i() - 1;
            }
        }, hasNext:function () {
            return this.get_count() > 0;
        }, get_hasNext:function () {
            return this.hasNext();
        }
    });

    Kotlin.NumberRange = Kotlin.Class.create({initialize:function (start, size, reversed) {
        this.$start = start;
        this.$size = size;
        this.$reversed = reversed;
    }, get_start:function () {
        return this.$start;
    }, get_size:function () {
        return this.$size;
    }, get_reversed:function () {
        return this.$reversed;
    }, get_end:function () {
        return this.get_reversed() ? this.get_start() - this.get_size() + 1 : this.get_start() + this.get_size() - 1;
    }, contains:function (number) {
        if (this.get_reversed()) {
            return number <= this.get_start() && number > this.get_start() - this.get_size();
        }
        else {
            return number >= this.get_start() && number < this.get_start() + this.get_size();
        }
    }, iterator:function () {
        return new Kotlin.RangeIterator(this.get_start(), this.get_size(), this.get_reversed());
    }
    });

    Kotlin.Comparator = Kotlin.Class.create(
            {
                initialize:function () {
                },
                compare:function (el1, el2) {
                    throw new Kotlin.AbstractFunctionInvocationError();
                }
            }
    );

    Kotlin.comparator = function (f) {
        var result = new Kotlin.Comparator();
        result.compare = function (el1, el2) {
            return f(el1, el2);
        };
        return result;
    };

    Kotlin.collectionsMax = function (col, comp) {
        var it = col.iterator();
        if (col.isEmpty()) {
            //TODO: which exception?
            throw Kotlin.Exception();
        }
        var max = it.next();
        while (it.hasNext()) {
            var el = it.next();
            if (comp.compare(max, el) < 0) {
                max = el;
            }
        }
        return max;
    };

    Kotlin.StringBuilder = Kotlin.Class.create(
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
        return new Kotlin.NumberRange(0, arr.length);
    };

    var intrinsicArrayIterator = Kotlin.Class.create(
            Kotlin.Iterator,
            {
                initialize:function (arr) {
                    this.arr = arr;
                    this.len = arr.length;
                    this.i = 0;
                },
                hasNext:function () {
                    return (this.i < this.len);
                },
                next:function () {
                    return this.arr[this.i++];
                },
                get_hasNext:function () {
                    return this.hasNext();
                }
            }
    );

    Kotlin.arrayIterator = function (arr) {
        return new intrinsicArrayIterator(arr);
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
                var result = new Kotlin.ArrayList();
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
                var res = new Kotlin.HashSet();
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

    Kotlin.HashMap = Kotlin.Class.create(
            {
                initialize:function () {
                    Kotlin.HashTable.call(this);
                }
            }
    );


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
                var list = new Kotlin.ArrayList();
                var values = this.values();
                var i = values.length;
                while (i--) {
                    list.add(values[i]);
                }
                return list.iterator();
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

        Kotlin.HashSet = Kotlin.Class.create(
                {
                    initialize:function () {
                        HashSet.call(this);
                    }
                }
        );
    }());

})();
