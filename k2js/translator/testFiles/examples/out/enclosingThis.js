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
    var Point_0 = Kotlin.Class.create({initialize:function(x, y){
      this.$x = x;
      this.$y = y;
    }
    , get_x:function(){
      return this.$x;
    }
    , get_y:function(){
      return this.$y;
    }
    , mul:function(){
      {
        var tmp$0;
        return tmp$0 = this , function(scalar){
          {
            return new Anonymous.Point(tmp$0.get_x() * scalar, tmp$0.get_y() * scalar);
          }
        }
        ;
      }
    }
    });
    return {Point:Point_0};
  }
  ();
  var Anonymous = Kotlin.Namespace.create({initialize:function(){
    Anonymous.$m = (new Anonymous.Point(2, 3)).mul();
  }
  , get_m:function(){
    return Anonymous.$m;
  }
  , box:function(){
    {
      var answer = Anonymous.get_m()(5);
      return answer.get_x() == 10 && answer.get_y() == 15?'OK':'FAIL';
    }
  }
  }, classes);
  Anonymous.initialize();
}
