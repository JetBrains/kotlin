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

{
    var items = function () {
        var A = Kotlin.createClass(null, {initialize: function () {
            this.$order = '';
            {
                this.set_order(this.get_order() + 'A');
            }
        }, set_order: function (tmp$0) {
            this.$order = tmp$0;
        }, get_order: function () {
            return this.$order;
        }
                                   });
        var B = Kotlin.createClass(A, {initialize: function () {
            this.super_init();
            {
                this.set_order(this.get_order() + 'B');
            }
        }
        });
        var C = Kotlin.createClass(B, {initialize: function () {
            this.super_init();
            {
                this.set_order(this.get_order() + 'C');
            }
        }
        });
        return {A: A, B: B, C: C};
    }
        ();

    items.box = function () {
        return (new foo.C).get_order() === 'ABC' && (new foo.B).get_order() === 'AB' && (new foo.A).get_order() === 'A';
    };

    var foo = Kotlin.definePackage(items);
}

function test() {
    return foo.box()
}
