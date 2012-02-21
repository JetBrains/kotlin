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
  var Anonymous = Kotlin.Namespace.create({initialize:function(){
  }
  , findAll:function(receiver, predicate){
    {
      var tmp$0;
      var result = new Kotlin.ArrayList(0);
      {
        tmp$0 = this.iterator();
        while (tmp$0.hasNext()) {
          var t = tmp$0.next();
          {
            if (predicate(t))
              result.add(t);
          }
        }
      }
      return result;
    }
  }
  , box:function(){
    {
      var list = new Kotlin.ArrayList(0);
      list.add(2);
      list.add(3);
      list.add(5);
      var m = list.Anonymous.findAll(list, function(name_0){
        {
          return name_0 < 4;
        }
      }
      );
      return m.size() == 2?'OK':'fail';
    }
  }
  }, classes);
  Anonymous.initialize();
}
