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
    return {};
  }
  ();
  var foo = Kotlin.Namespace.create({initialize:function(){
  }
  , box:function(){
    {
      var tmp$1;
      var tmp$0;
      for (tmp$0 = 0; tmp$0 < 3; ++tmp$0) {
        if (tmp$0 == 0)
          if (1 == 3) {
            {
              tmp$1 = 3;
            }
            break;
          }
        if (tmp$0 == 1)
          if (1 == 1) {
            {
              throw new Kotlin.Exception;
            }
            break;
          }
        if (tmp$0 == 2) {
          return false;
        }
      }
      tmp$1;
      return false;
    }
  }
  }, classes);
  foo.initialize();
}
