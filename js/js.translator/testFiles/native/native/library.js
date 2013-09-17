/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
    var c = 0;

    Kotlin.A = Kotlin.createClass(null,
            function () {
                this.f = function(i) {
                    if (i === undefined && c === 0) {
                        c = 1;
                    }
                    if (i === 2 && c === 1) {
                        c = 2;
                    }
                }
            }
        );
    Kotlin.getResult = function () {
        return c === 2;
    };
})();

