/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

interface Interface {
    fun interfaceFun()
}

abstract class AbstractClass: Interface {
    override fun interfaceFun() {}
    abstract fun abstractFun()
}

open class OpenClass: AbstractClass() {
    override fun abstractFun() {}
    open fun openFun1() {}
    open fun openFun2() {}
    fun finalFun() {}
}

class FinalClass: OpenClass() {
    override fun openFun1() {}
    final override fun openFun2() {}
}