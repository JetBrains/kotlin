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
    var A = Kotlin.Class.create({initialize:function(b, a){
      this.$b = b;
      this.$a = a;
    }
    , get_b:function(){
      return this.$b;
    }
    , set_b:function(tmp$0){
      this.$b = tmp$0;
    }
    , get_a:function(){
      return this.$a;
    }
    , set_a:function(tmp$0){
      this.$a = tmp$0;
    }
    });
    return {A_0:A};
  }
  ();
  var foo = Kotlin.Namespace.create({initialize:function(){
  }
  , box:function(){
    {
      var c = new foo.A_0(2, '2');
      return c.get_b() == 2 && c.get_a() == '2';
    }
  }
  }, classes);
  foo.initialize();
}
