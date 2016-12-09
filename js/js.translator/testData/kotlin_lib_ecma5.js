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

(function () {
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

    Kotlin.callGetter = function (thisObject, klass, propertyName) {
        var propertyDescriptor = Object.getOwnPropertyDescriptor(klass, propertyName);
        if (propertyDescriptor != null) {
            if (propertyDescriptor.get != null) {
                return propertyDescriptor.get.call(thisObject);
            }
            else if ("value" in propertyDescriptor) {
                return propertyDescriptor.value;
            }
        }
        else {
            return Kotlin.callGetter(thisObject, Object.getPrototypeOf(klass), propertyName);
        }
        return null;
    };

    Kotlin.callSetter = function (thisObject, klass, propertyName, value) {
        var propertyDescriptor = Object.getOwnPropertyDescriptor(klass, propertyName);
        if (propertyDescriptor != null) {
            if (propertyDescriptor.set != null) {
                propertyDescriptor.set.call(thisObject, value);
            }
            else if ("value" in propertyDescriptor) {
                throw new Error("Assertion failed: Kotlin compiler should not generate simple JavaScript properties for overridable " +
                                "Kotlin properties.");
            }
        }
        else {
            return Kotlin.callSetter(thisObject, Object.getPrototypeOf(klass), propertyName, value);
        }
    };

    function isInheritanceFromTrait(metadata, trait) {
        // TODO: return this optimization
        /*if (metadata == null || metadata.classIndex < trait.$metadata$.classIndex) {
            return false;
        }*/
        var baseClasses = metadata.baseClasses;
        var i;
        for (i = 0; i < baseClasses.length; i++) {
            if (baseClasses[i] === trait) {
                return true;
            }
        }
        for (i = 0; i < baseClasses.length; i++) {
            if (isInheritanceFromTrait(baseClasses[i].$metadata$, trait)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param {*} object
     * @param {Function|Object} klass
     * @returns {Boolean}
     */
    Kotlin.isType = function (object, klass) {
        if (klass === Object) {
            switch (typeof object) {
                case "string":
                case "number":
                case "boolean":
                case "function":
                    return true;
                default:
                    return object instanceof Object;
            }
        }

        if (object == null || klass == null || (typeof object !== 'object' && typeof object !== 'function')) {
            return false;
        }

        if (typeof klass === "function" && object instanceof klass) {
            return true;
        }

        var proto = Object.getPrototypeOf(klass);
        var constructor = proto != null ? proto.constructor : null;
        if (constructor != null && "$metadata$" in constructor) {
            var metadata = constructor.$metadata$;
            if (metadata.type === Kotlin.TYPE.OBJECT) {
                return object === klass;
            }
        }

        // In WebKit (JavaScriptCore) for some interfaces from DOM typeof returns "object", nevertheless they can be used in RHS of instanceof
        if (isNativeClass(klass)) {
            return object instanceof klass;
        }

        if (isTrait(klass) && object.constructor != null) {
            metadata = object.constructor.$metadata$;
            if (metadata != null) {
                return isInheritanceFromTrait(metadata, klass);
            }
        }

        return false;
    };

    function isTrait(klass) {
        var metadata = klass.$metadata$;
        return metadata != null && metadata.type === Kotlin.TYPE.TRAIT;
    }

    // TODO Store callable references for members in class
    Kotlin.getCallableRefForMemberFunction = function (memberName) {
        return function () {
            var args = [].slice.call(arguments);
            var instance = args.shift();
            return instance[memberName].apply(instance, args);
        };
    };

    Kotlin.getBoundCallableRefForMemberFunction = function (receiver, memberName) {
        return function () {
            return receiver[memberName].apply(receiver, arguments);
        };
    };

    // TODO Store callable references for extension functions in class
    // extFun expected receiver as the first argument
    Kotlin.getCallableRefForExtensionFunction = function (extFun) {
        return function () {
            return extFun.apply(null, arguments);
        };
    };

    Kotlin.getBoundCallableRefForExtensionFunction = function (receiver, extFun) {
        return function () {
            var args = [].slice.call(arguments);
            args.unshift(receiver);
            return extFun.apply(null, args);
        };
    };

    Kotlin.getCallableRefForLocalExtensionFunction = function (extFun) {
        return function () {
            var args = [].slice.call(arguments);
            var instance = args.shift();
            return extFun.apply(instance, args);
        };
    };

    Kotlin.getBoundCallableRefForLocalExtensionFunction = function (receiver, extFun) {
        return function () {
            return extFun.apply(receiver, arguments);
        };
    };

    Kotlin.getCallableRefForConstructor = function (klass) {
        return function () {
            var obj = Object.create(klass.prototype);
            klass.apply(obj, arguments);
            return obj;
        };
    };

    Kotlin.getCallableRefForTopLevelProperty = function(getter, setter, name) {
        var getFun = Function("getter", "return function " + name + "() { return getter(); }")(getter, setter);
        return getPropertyRefClass(getFun, "get", setter, "set_za3rmp$", propertyRefClassMetadataCache.zeroArg);
    };

    Kotlin.getCallableRefForMemberProperty = function(name, isVar) {
        var getFun = Function("return function " + name + "(receiver) { return receiver['" + name + "']; }")();
        var setFun = isVar ? function(receiver, value) { receiver[name] = value; } : null;
        return getPropertyRefClass(getFun, "get_za3rmp$", setFun, "set_wn2jw4$", propertyRefClassMetadataCache.oneArg);
    };

    Kotlin.getBoundCallableRefForMemberProperty = function(receiver, name, isVar) {
        var getFun = Function("receiver", "return function " + name + "() { return receiver['" + name + "']; }")(receiver);
        var setFun = isVar ? function(value) { receiver[name] = value; } : null;
        return getPropertyRefClass(getFun, "get", setFun, "set_za3rmp$", propertyRefClassMetadataCache.oneArg);
    };

    Kotlin.getCallableRefForExtensionProperty = function(name, getFun, setFun) {
        var getFunWrapper = Function("getFun", "return function " + name + "(receiver, extensionReceiver) { return getFun(receiver, extensionReceiver) }")(getFun);
        return getPropertyRefClass(getFunWrapper, "get_za3rmp$", setFun, "set_wn2jw4$", propertyRefClassMetadataCache.oneArg);
    };

    Kotlin.getBoundCallableRefForExtensionProperty = function(receiver, name, getFun, setFun) {
        var getFunWrapper = Function("receiver", "getFun", "return function " + name + "(extensionReceiver) { return getFun(receiver, extensionReceiver) }")(receiver, getFun);
        if (setFun) {
            setFun = setFun.bind(null, receiver);
        }
        return getPropertyRefClass(getFunWrapper, "get", setFun, "set_za3rmp$", propertyRefClassMetadataCache.oneArg);
    };

    function getPropertyRefClass(getFun, getName, setFun, setName, cache) {
        var obj = getFun;
        var isMutable = typeof setFun === "function";
        obj.$metadata$ = getPropertyRefMetadata(isMutable ? cache.mutable : cache.immutable);
        obj[getName] = getFun;
        if (isMutable) {
            obj[setName] = setFun;
        }
        obj.constructor = obj;
        return obj;
    }

    var propertyRefClassMetadataCache = {
        zeroArg: {
            mutable: { value: null, implementedInterface: function () {
                return Kotlin.kotlin.reflect.KMutableProperty0 }
            },
            immutable: { value: null, implementedInterface: function () {
                return Kotlin.kotlin.reflect.KProperty0 }
            }
        },
        oneArg: {
            mutable: { value: null, implementedInterface: function () {
                return Kotlin.kotlin.reflect.KMutableProperty1 }
            },
            immutable: { value: null, implementedInterface: function () {
                return Kotlin.kotlin.reflect.KProperty1 }
            }
        }
    };

    function getPropertyRefMetadata(cache) {
        if (cache.value === null) {
            cache.value = {
                baseClasses: [cache.implementedInterface()],
                baseClass: null,
                classIndex: Kotlin.newClassIndex(),
                functions: {},
                properties: {},
                types: {},
                staticMembers: {}
            };
        }
        return cache.value;
    }

    Kotlin.jsTypeOf = function(a) { return typeof a; };

////////////////////////////////// packages & modules //////////////////////////////

    Kotlin.modules = {};

    /**
     * @param {string} id
     * @param {Object} declaration
     */
    Kotlin.defineModule = function (id, declaration) {
        Kotlin.modules[id] = declaration;
    };

    Kotlin.defineInlineFunction = function(tag, fun) {
        return fun;
    };

    Kotlin.isTypeOf = function(type) {
        return function (object) {
            return typeof object === type;
        }
    };

    Kotlin.isInstanceOf = function (klass) {
        return function (object) {
            return Kotlin.isType(object, klass);
        }
    };

    Kotlin.orNull = function (fn) {
        return function (object) {
            return object == null || fn(object);
        }
    };
    
    Kotlin.andPredicate = function (a, b) {
        return function (object) {
            return a(object) && b(object);
        }
    };

    Kotlin.kotlinModuleMetadata = function (abiVersion, moduleName, data) {
    };

})();
