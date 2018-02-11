/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

/**
 * @param {string} id
 * @param {Object} declaration
 */
Kotlin.defineModule = function (id, declaration) {
};

Kotlin.defineInlineFunction = function(tag, fun) {
    return fun;
};

Kotlin.wrapFunction = function(fun) {
    var f = function() {
        f = fun();
        return f.apply(this, arguments);
    };
    return function() {
        return f.apply(this, arguments);
    };
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

Kotlin.suspendCall = function(value) {
    return value;
};

Kotlin.coroutineResult = function(qualifier) {
    throwMarkerError();
};

Kotlin.coroutineController = function(qualifier) {
    throwMarkerError();
};

Kotlin.coroutineReceiver = function(qualifier) {
    throwMarkerError();
};

Kotlin.setCoroutineResult = function(value, qualifier) {
    throwMarkerError();
};

function throwMarkerError() {
    throw new Error(
        "This marker function should never been called. " +
        "Looks like compiler did not eliminate it properly. " +
        "Please, report an issue if you caught this exception.");
}

Kotlin.getFunctionById = function(id, defaultValue) {
    return function() {
        return defaultValue;
    }
};