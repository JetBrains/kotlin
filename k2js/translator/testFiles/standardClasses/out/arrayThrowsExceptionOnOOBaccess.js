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
    var A_0 = Kotlin.Class.create({initialize:function(){
    }
    });
    return {A:A_0};
  }
  ();
  var foo = Kotlin.Namespace.create({initialize:function(){
    foo.$a1 = Kotlin.nullArray(3);
    foo.$a2 = Kotlin.nullArray(2);
  }
  , get_a1:function(){
    return foo.$a1;
  }
  , get_a2:function(){
    return foo.$a2;
  }
  , box:function(){
    {
      return foo.get_a1()[4] == null;
    }
  }
  }, classes);
  foo.initialize();
}
