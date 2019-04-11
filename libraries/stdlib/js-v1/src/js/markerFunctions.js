/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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