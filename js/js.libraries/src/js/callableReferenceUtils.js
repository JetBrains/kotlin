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

Kotlin.getCallableRef = function(name, f) {
    f.callableName = name;
    return f;
};

Kotlin.getPropertyCallableRef = function(name, paramCount, getter, setter) {
    getter.get = getter;
    getter.set = setter;
    getter.callableName = name;
    return getPropertyRefClass(getter, setter, propertyRefClassMetadataCache[paramCount]);
};

function getPropertyRefClass(obj, setter, cache) {
    obj.$metadata$ = getPropertyRefMetadata(typeof setter === "function" ? cache.mutable : cache.immutable);
    obj.constructor = obj;
    return obj;
}

var propertyRefClassMetadataCache = [
    {
        mutable: { value: null, implementedInterface: function () {
            return Kotlin.kotlin.reflect.KMutableProperty0 }
        },
        immutable: { value: null, implementedInterface: function () {
            return Kotlin.kotlin.reflect.KProperty0 }
        }
    },
    {
        mutable: { value: null, implementedInterface: function () {
            return Kotlin.kotlin.reflect.KMutableProperty1 }
        },
        immutable: { value: null, implementedInterface: function () {
            return Kotlin.kotlin.reflect.KProperty1 }
        }
    }
];

function getPropertyRefMetadata(cache) {
    if (cache.value === null) {
        cache.value = {
            interfaces: [cache.implementedInterface()],
            baseClass: null,
            functions: {},
            properties: {},
            types: {},
            staticMembers: {}
        };
    }
    return cache.value;
}
