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
    var A = Kotlin.Class.create({initialize:function(i){
      this.$a = i;
    }
    , get_a:function(){
      return this.$a;
    }
    , plusAssign:function(other){
      {
        return new foo.A_0(this.get_a() + other.get_a());
      }
    }
    });
    return {A_0:A};
  }
  ();
  var foo = Kotlin.Namespace.create({initialize:function(){
  }
  , box:function(){
    {
      var c = new foo.A_0(2);
      var d = c;
      c = c.plusAssign(new foo.A_0(3));
      return c.get_a() == 5 && d.get_a() == 2 && d != c;
    }
  }
  }, classes);
  foo.initialize();
}
