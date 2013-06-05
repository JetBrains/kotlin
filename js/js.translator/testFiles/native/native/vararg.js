/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

function paramCount() {
    return arguments.length
}

function Bar(size, order) {
    this.size = size;
    Bar.checkOrder(order);
}

Bar.order = 0;
Bar.hasOrderProblem = false;
Bar.checkOrder = function (expectedOrder) {
    var curOrder = Bar.order++;
    Bar.hasOrderProblem = Bar.hasOrderProblem || curOrder !== expectedOrder;
};

Bar.startNewTest = function () {
    Bar.hasOrderProblem = false;
    Bar.order = 0;
    return true;
};


Bar.prototype.test = function (order, dummy /*, args */) {
    Bar.checkOrder(order);
    return dummy === 1 && (arguments.length - 2) === this.size;
};

var obj = {
    test : function (size /*, args */) {
        return (arguments.length - 1) === size;
    }
};

function testNativeVarargWithFunLit(/* args, f */) {
    var args = Array.prototype.slice.call(arguments, 0, arguments.length - 1);
    var f = arguments[arguments.length - 1];
    return typeof f === "function" && f(args);
}