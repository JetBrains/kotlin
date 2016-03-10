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

    function computeMetadata(bases, properties, staticProperties) {
        var metadata = {};
        var p, property;

        metadata.baseClasses = toArray(bases);
        metadata.baseClass = getClass(metadata.baseClasses);
        metadata.classIndex = Kotlin.newClassIndex();
        metadata.functions = {};
        metadata.properties = {};
        metadata.types = {};
        metadata.staticMembers = {};

        if (!(properties == null)) {
            for (p in properties) {
                if (properties.hasOwnProperty(p)) {
                    property = properties[p];
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
        if (typeof staticProperties !== 'undefined') {
            for (p in staticProperties) {
                //noinspection JSUnfilteredForInLoop
                property = staticProperties[p];
                if (typeof property === "function" && property.type === Kotlin.TYPE.INIT_FUN) {
                    //noinspection JSUnfilteredForInLoop
                    metadata.types[p] = property;
                }
                else {
                    //noinspection JSUnfilteredForInLoop
                    metadata.staticMembers[p] = property;
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

        var metadata = computeMetadata(bases, properties, staticProperties);
        metadata.type = Kotlin.TYPE.CLASS;
        copyProperties(constructor, metadata.staticMembers);

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
        defineNestedTypes(constructor, metadata.types);

        if (metadata.baseClass != null) {
            constructor.baseInitializer = metadata.baseClass;
        }

        constructor.$metadata$ = metadata;
        constructor.prototype = prototypeObj;
        return constructor;
    };

    function defineNestedTypes(constructor, types) {
        for (var innerTypeName in types) {
            // since types object does not inherit from anything, it's just a map
            //noinspection JSUnfilteredForInLoop
            var innerType = types[innerTypeName];
            innerType.className = innerTypeName;
            //noinspection JSUnfilteredForInLoop
            Object.defineProperty(constructor, innerTypeName, {
                get: innerType,
                configurable: true
            });
        }
    }

    Kotlin.createTraitNow = function (bases, properties, staticProperties) {
        var obj = function () {};

        obj.$metadata$ = computeMetadata(bases, properties, staticProperties);
        obj.$metadata$.type = Kotlin.TYPE.TRAIT;
        copyProperties(obj, obj.$metadata$.staticMembers);

        obj.prototype = {};
        Object.defineProperties(obj.prototype, obj.$metadata$.properties);
        copyProperties(obj.prototype, obj.$metadata$.functions);

        defineNestedTypes(obj, obj.$metadata$.types);
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
            if (staticProperties && staticProperties.object_initializer$) {
                staticProperties.object_initializer$(klass);
            }
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
        staticProperties.object_initializer$ = function (cls) {
            var enumEntryList = enumEntries();
            var i = 0;
            var values = [];
            for (var entryName in enumEntryList) {
                if (enumEntryList.hasOwnProperty(entryName)) {
                    var entryObject = enumEntryList[entryName];
                    values.push(entryObject);
                    entryObject.ordinal$ = i++;
                    entryObject.name$ = entryName;
                    cls[entryName] = entryObject;
                }
            }
            enumEntryList.values$ = values;
        };

        staticProperties.values = function () {
            return this.values$;
        };

        staticProperties.valueOf_61zpoe$ = function (name) {
            return this[name];
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
     * @param {Object=} staticProperties
     * @returns {Object}
     * @template T
     */
    Kotlin.createObject = function (basesFun, constructor, functions, staticProperties) {
        constructor = constructor || function() {};
        function $o() {
            var klass = Kotlin.createClassNow(getBases(basesFun), null, functions, staticProperties);
            var obj = new klass();
            var metadata = klass.$metadata$;
            metadata.type = Kotlin.TYPE.OBJECT;
            Object.defineProperty(this, $o.className, {value: obj});
            defineNestedTypes(obj, klass.$metadata$.types);
            if (metadata.baseClass != null) {
                constructor.baseInitializer = metadata.baseClass;
            }
            constructor.apply(obj);
            return obj;
        }

        $o.type = Kotlin.TYPE.INIT_FUN;
        return $o;
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
            var args = [].slice.call(arguments);
            var instance = args.shift();
            return instance[memberName].apply(instance, args);
        };
    };

    // TODO Store callable references for extension functions in class
    // extFun expected receiver as the first argument
    Kotlin.getCallableRefForExtensionFunction = function (extFun) {
        return function () {
            return extFun.apply(null, arguments);
        };
    };

    Kotlin.getCallableRefForLocalExtensionFunction = function (extFun) {
        return function () {
            var args = [].slice.call(arguments);
            var instance = args.shift();
            return extFun.apply(instance, args);
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

    function defineInlineFunction(tag, fun) {
        return fun;
    }

    Kotlin.defineInlineFunction = defineInlineFunction;

    Kotlin.isTypeOf = defineInlineFunction('stdlib.kotlin.isTypeOf', function (type) {
        return function (object) {
            return typeof object === type;
        }
    });

    Kotlin.isInstanceOf = defineInlineFunction('stdlib.kotlin.isInstanceOf', function (klass) {
        return function (object) {
            return Kotlin.isType(object, klass);
        }
    });

    Kotlin.kotlinModuleMetadata = function (abiVersion, moduleName, data) {
    };

})(Kotlin);
