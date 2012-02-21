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
    var Slot = Kotlin.Class.create({initialize:function(){
      this.$vitality = 10000;
    }
    , get_vitality:function(){
      return this.$vitality;
    }
    , set_vitality:function(tmp$0){
      this.$vitality = tmp$0;
    }
    , increaseVitality:function(delta){
      {
        this.set_vitality(this.get_vitality() + delta);
        if (this.get_vitality() > 65535)
          this.set_vitality(65535);
      }
    }
    });
    return {Slot_0:Slot};
  }
  ();
  var foo = Kotlin.Namespace.create({initialize:function(){
  }
  , box:function(){
    {
      var s = new foo.Slot_0;
      s.increaseVitality(1000);
      if (s.get_vitality() == 11000)
        return 'OK';
      else 
        return 'fail';
    }
  }
  }, classes);
  foo.initialize();
}
