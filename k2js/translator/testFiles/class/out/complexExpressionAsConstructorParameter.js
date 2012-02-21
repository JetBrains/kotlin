/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
  var classes = function(){
    var Test = Kotlin.Class.create({initialize:function(a, b){
      this.$c = a;
      this.$d = b;
    }
    , get_c:function(){
      return this.$c;
    }
    , get_d:function(){
      return this.$d;
    }
    });
    return {Test_0:Test};
  }
  ();
  var foo = Kotlin.Namespace.create({initialize:function(){
  }
  , box:function(){
    {
      var test = new foo.Test_0(1 + 6 * 3, 10 % 2);
      return test.get_c() == 19 && test.get_d() == 0;
    }
  }
  }, classes);
  foo.initialize();
}
