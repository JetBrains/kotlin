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
  classes = function(){
    var A = Class.create({initialize:function(){
    }
    , p:function(){
      return true;
    }
    });
    return {A:A};
  }
  ();
  foo = Namespace.create({initialize:function(){
  }
  , box:function(){
    var a = 0;
    for (var tmp$0 = 0; tmp$0 < 4; ++tmp$0) {
      if (tmp$0 == 0)
        if ((new foo.A).p() === true) {
          a--;
          break;
        }
      if (tmp$0 == 1)
        if (new foo.A === null || isType(new foo.A, foo.A)) {
          a++;
          break;
        }
      if (tmp$0 == 2)
        if (isType(new foo.A, foo.A)) {
          a++;
          break;
        }
      if (tmp$0 == 3)
        a++;
    }
    return a === -1;
  }
  }, classes);
  foo.initialize();
}
