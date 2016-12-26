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
