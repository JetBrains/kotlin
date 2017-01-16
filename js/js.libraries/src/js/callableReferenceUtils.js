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

Kotlin.getCallableRefForConstructor = function (klass) {
    return function () {
        var obj = Object.create(klass.prototype);
        klass.apply(obj, arguments);
        return obj;
    };
};

Kotlin.getCallableRefZeroArg = function(name, getter, setter) {
    getter.get = getter;
    getter.set = setter;
    getter.callableName = name;
    return getPropertyRefClass(getter, setter, propertyRefClassMetadataCache.zeroArg);
};

Kotlin.getCallableRefOneArg = function(name, getter, setter) {
    getter.get = getter;
    getter.set = setter;
    getter.callableName = name;
    return getPropertyRefClass(getter, setter, propertyRefClassMetadataCache.oneArg);
};

function getPropertyRefClass(obj, setter, cache) {
    obj.$metadata$ = getPropertyRefMetadata(typeof setter === "function" ? cache.mutable : cache.immutable);
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
            functions: {},
            properties: {},
            types: {},
            staticMembers: {}
        };
    }
    return cache.value;
}
