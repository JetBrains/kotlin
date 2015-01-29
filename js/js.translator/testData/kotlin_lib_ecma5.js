/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

var Kotlin = {};

(function (Kotlin) {
    'use strict';

    function toArray(obj) {
        var array;
        if (obj == null) {
            array = [];
        }
        else if (!Array.isArray(obj)) {
            array = [obj];
        }
        else {
            array = obj;
        }
        return array;
    }

    function copyProperties(to, from) {
        if (to == null || from == null) {
            return;
        }
        for (var p in from) {
            if (from.hasOwnProperty(p)) {
                to[p] = from[p];
            }
        }
    }

    function getClass(basesArray) {
        for (var i = 0; i < basesArray.length; i++) {
            if (isNativeClass(basesArray[i]) || basesArray[i].$metadata$.type === Kotlin.TYPE.CLASS) {
                return basesArray[i];
            }
        }
        return null;
    }

    var emptyFunction = function () {
        return function() {};
    };

    Kotlin.TYPE = {
        CLASS: "class",
        TRAIT: "trait",
        OBJECT: "object",
        INIT_FUN: "init fun"
    };

    Kotlin.classCount = 0;
    Kotlin.newClassIndex = function () {
        var tmp = Kotlin.classCount;
        Kotlin.classCount++;
        return tmp;
    };

    function isNativeClass(obj) {
        return !(obj == null) && obj.$metadata$ == null;
    }

    function applyExtension(current, bases, baseGetter) {
        for (var i = 0; i < bases.length; i++) {
            if (isNativeClass(bases[i])) {
                continue;
            }
            var base = baseGetter(bases[i]);
            for (var p in  base) {
                if (base.hasOwnProperty(p)) {
                    if (!current.hasOwnProperty(p) || current[p].$classIndex$ < base[p].$classIndex$) {
                        current[p] = base[p];
                    }
                }
            }
        }
    }

    function computeMetadata(bases, properties) {
        var metadata = {};

        metadata.baseClasses = toArray(bases);
        metadata.baseClass = getClass(metadata.baseClasses);
        metadata.classIndex = Kotlin.newClassIndex();
        metadata.functions = {};
        metadata.properties = {};

        if (!(properties == null)) {
            for (var p in properties) {
                if (properties.hasOwnProperty(p)) {
                    var property = properties[p];
                    property.$classIndex$ = metadata.classIndex;
                    if (typeof property === "function") {
                        metadata.functions[p] = property;
                    }
                    else {
                        metadata.properties[p] = property;
                    }
                }
            }
        }
        applyExtension(metadata.functions, metadata.baseClasses, function (it) {
            return it.$metadata$.functions
        });
        applyExtension(metadata.properties, metadata.baseClasses, function (it) {
            return it.$metadata$.properties
        });

        return metadata;
    }

    /**
     * @this {{object_initializer$: (function(): Object)}}
     * @returns {Object}
     */
    function class_object() {
        var object = this.object_initializer$();
        Object.defineProperty(this, "object", {value: object});
        return object;
    }

    /**
     * @param {(Array|Object|null)=} bases
     * @param {(function(new: T, ?, ?, ?, ?, ?, ?, ?): T)|null=} constructor
     * @param {Object=} properties
     * @param {Object=} staticProperties
     * @returns {function(new: T): T}
     * @template T
     */
    Kotlin.createClassNow = function (bases, constructor, properties, staticProperties) {
        if (constructor == null) {
            constructor = emptyFunction();
        }
        copyProperties(constructor, staticProperties);

        var metadata = computeMetadata(bases, properties);
        metadata.type = Kotlin.TYPE.CLASS;

        var prototypeObj;
        if (metadata.baseClass !== null) {
            prototypeObj = Object.create(metadata.baseClass.prototype);
        }
        else {
            prototypeObj = {};
        }
        Object.defineProperties(prototypeObj, metadata.properties);
        copyProperties(prototypeObj, metadata.functions);
        prototypeObj.constructor = constructor;

        if (metadata.baseClass != null) {
            constructor.baseInitializer = metadata.baseClass;
        }

        constructor.$metadata$ = metadata;
        constructor.prototype = prototypeObj;
        Object.defineProperty(constructor, "object", {get: class_object, configurable: true});
        return constructor;
    };

    Kotlin.createObjectNow = function (bases, constructor, functions) {
        var noNameClass = Kotlin.createClassNow(bases, constructor, functions);
        var obj = new noNameClass();
        obj.$metadata$ = {
            type: Kotlin.TYPE.OBJECT
        };
        return  obj;
    };

    Kotlin.createTraitNow = function (bases, properties, staticProperties) {
        var obj = function () {};
        copyProperties(obj, staticProperties);

        obj.$metadata$ = computeMetadata(bases, properties);
        obj.$metadata$.type = Kotlin.TYPE.TRAIT;

        obj.prototype = {};
        Object.defineProperties(obj.prototype, obj.$metadata$.properties);
        copyProperties(obj.prototype, obj.$metadata$.functions);
        Object.defineProperty(obj, "object", {get: class_object, configurable: true});
        return obj;
    };

    function getBases(basesFun) {
        if (typeof basesFun === "function") {
            return basesFun();
        }
        else {
            return basesFun;
        }
    }

    /**
     * @param {(function():Array.<*>)|null} basesFun
     * @param {?=} constructor
     * @param {Object=} properties
     * @param {Object=} staticProperties
     * @returns {*}
     */
    Kotlin.createClass = function (basesFun, constructor, properties, staticProperties) {
        function $o() {
            var klass = Kotlin.createClassNow(getBases(basesFun), constructor, properties, staticProperties);
            Object.defineProperty(this, $o.className, {value: klass});
            return klass;
        }

        $o.type = Kotlin.TYPE.INIT_FUN;
        return $o;
    };

    /**
     * @param {(function():Array.<*>)|null} basesFun
     * @param {?=} constructor
     * @param {function():Object} enumEntries
     * @param {Object=} properties
     * @param {Object=} staticProperties
     * @returns {*}
     */
    Kotlin.createEnumClass = function (basesFun, constructor, enumEntries, properties, staticProperties) {
        staticProperties = staticProperties || {};

        // TODO use Object.assign
        staticProperties.object_initializer$ = function () {
            var enumEntryList = enumEntries();
            var i = 0;
            var values = [];
            for (var entryName in enumEntryList) {
                if (enumEntryList.hasOwnProperty(entryName)) {
                    var entryObject = enumEntryList[entryName];
                    values[i] = entryObject;
                    entryObject.ordinal$ = i;
                    entryObject.name$ = entryName;
                    i++;
                }
            }
            enumEntryList.values$ = values;
            return enumEntryList;
        };

        staticProperties.values = function () {
            return this.object.values$;
        };

        staticProperties.valueOf_61zpoe$ = function (name) {
            return this.object[name];
        };

        return Kotlin.createClass(basesFun, constructor, properties, staticProperties)
    };

    /**
     * @param {(function():Array.<*>)|null} basesFun
     * @param {Object=} properties
     * @param {Object=} staticProperties
     * @returns {*}
     */
    Kotlin.createTrait = function (basesFun, properties, staticProperties) {
        function $o() {
            var klass = Kotlin.createTraitNow(getBases(basesFun), properties, staticProperties);
            Object.defineProperty(this, $o.className, {value: klass});
            return klass;
        }

        $o.type = Kotlin.TYPE.INIT_FUN;
        return $o;
    };

    /**
     * @param {function()|null} basesFun
     * @param {(function(new: T): T)|null=} constructor
     * @param {Object=} functions
     * @returns {Object}
     * @template T
     */
    Kotlin.createObject = function (basesFun, constructor, functions) {
        return Kotlin.createObjectNow(getBases(basesFun), constructor, functions);
    };

    Kotlin.callGetter = function (thisObject, klass, propertyName) {
        return klass.$metadata$.properties[propertyName].get.call(thisObject);
    };

    Kotlin.callSetter = function (thisObject, klass, propertyName, value) {
        klass.$metadata$.properties[propertyName].set.call(thisObject, value);
    };

    function isInheritanceFromTrait(objConstructor, trait) {
        if (isNativeClass(objConstructor) || objConstructor.$metadata$.classIndex < trait.$metadata$.classIndex) {
            return false;
        }
        var baseClasses = objConstructor.$metadata$.baseClasses;
        var i;
        for (i = 0; i < baseClasses.length; i++) {
            if (baseClasses[i] === trait) {
                return true;
            }
        }
        for (i = 0; i < baseClasses.length; i++) {
            if (isInheritanceFromTrait(baseClasses[i], trait)) {
                return true;
            }
        }
        return false;
    }

    Kotlin.isType = function (object, klass) {
        if (object == null || klass == null) {
            return false;
        }
        else {
            if (object instanceof klass) {
                return true;
            }
            else if (isNativeClass(klass) || klass.$metadata$.type == Kotlin.TYPE.CLASS) {
                return false;
            }
            else {
                return isInheritanceFromTrait(object.constructor, klass);
            }
        }
    };

    // TODO Store callable references for members in class
    Kotlin.getCallableRefForMemberFunction = function (klass, memberName) {
        return function () {
            return this[memberName].apply(this, arguments);
        };
    };

    // TODO Store callable references for extension functions in class
    // extFun expected receiver as the first argument
    Kotlin.getCallableRefForExtensionFunction = function (extFun) {
        return function () {
          var args = [this];
          Array.prototype.push.apply(args, arguments);
          return extFun.apply(null, args);
        };
    };

    Kotlin.getCallableRefForConstructor = function (klass) {
        return function () {
            var obj = Object.create(klass.prototype);
            klass.apply(obj, arguments);
            return obj;
        };
    };

    Kotlin.getCallableRefForTopLevelProperty = function(packageName, name, isVar) {
      var obj = {};
      obj.name = name;
      obj.get = function() { return packageName[name]; };
      if (isVar) {
          obj.set_za3rmp$ = function(value) { packageName[name] = value; };
      }
      return obj;
    };

    Kotlin.getCallableRefForMemberProperty = function(name, isVar) {
      var obj = {};
      obj.name = name;
      obj.get_za3rmp$ = function(receiver) { return receiver[name]; };
      if (isVar) {
          obj.set_wn2jw4$ = function(receiver, value) { receiver[name] = value; };
      }
      return obj;
    };

    Kotlin.getCallableRefForExtensionProperty = function(name, getFun, setFun) {
      var obj = {};
      obj.name = name;
      obj.get_za3rmp$ = getFun;
      if (typeof setFun === "function") {
          obj.set_wn2jw4$ = setFun;
      }
      return obj;
    };
////////////////////////////////// packages & modules //////////////////////////////

    Kotlin.modules = {};

    function createPackageGetter(instance, initializer) {
        return function () {
            if (initializer !== null) {
                var tmp = initializer;
                initializer = null;
                tmp.call(instance);
            }

            return instance;
        };
    }

    function createDefinition(members, definition) {
        if (typeof definition === "undefined") {
            definition = {}
        }
        if (members == null) {
            return definition;
        }
        for (var p in members) {
            if (members.hasOwnProperty(p)) {
                if ((typeof members[p]) === "function") {
                    if (members[p].type === Kotlin.TYPE.INIT_FUN) {
                        members[p].className = p;
                        Object.defineProperty(definition, p, {
                            get: members[p],
                            configurable: true
                        });
                    }
                    else {
                        definition[p] = members[p];
                    }
                }
                else {
                    Object.defineProperty(definition, p, members[p]);
                }
            }
        }
        return definition;
    }

    Kotlin.createDefinition = createDefinition;

    /**
     * @param {function()|null=} initializer
     * @param {Object=} members
     * @returns {Object}
     */
    Kotlin.definePackage = function (initializer, members) {
        var definition = createDefinition(members);
        if (initializer === null) {
            return {value: definition};
        }
        else {
            var getter = createPackageGetter(definition, initializer);
            return {get: getter};
        }
    };

    Kotlin.defineRootPackage = function (initializer, members) {
        var definition = createDefinition(members);

        if (initializer === null) {
            definition.$initializer$ = emptyFunction();
        }
        else {
            definition.$initializer$ = initializer;
        }
        return definition;
    };

    /**
     * @param {string} id
     * @param {Object} declaration
     */
    Kotlin.defineModule = function (id, declaration) {
        if (id in Kotlin.modules) {
            throw new Error("Module " + id + " is already defined");
        }
        declaration.$initializer$.call(declaration); // TODO: temporary hack
        Object.defineProperty(Kotlin.modules, id, {value: declaration});
    };

    Kotlin.defineInlineFunction = function(startTag, fun, metadataArgs) {
        return fun;
    };

})(Kotlin);
