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
    var Viewable = Kotlin.Class.create({initialize:function(){
      this.$items = new Kotlin.ArrayList;
    }
    , get_items:function(){
      return this.$items;
    }
    , add:function(item_0){
      this.get_items().add(item_0);
    }
    , remove:function(item_0){
      this.get_items().remove(item_0);
    }
    , view:function(lifetime, viewer){
      {
        tmp$0 = this.get_items().iterator();
        while (tmp$0.hasNext()) {
          item = tmp$0.next();
          {
            viewer(lifetime, item);
          }
        }
      }
    }
    });
    var Lifetime = Kotlin.Class.create({initialize:function(){
      this.$attached = new Kotlin.ArrayList;
    }
    , get_attached:function(){
      return this.$attached;
    }
    , attach:function(action){
      this.get_attached().add(action);
    }
    , close_0:function(){
      {
        tmp$0 = this.get_attached().iterator();
        while (tmp$0.hasNext()) {
          x = tmp$0.next();
          {
            x();
          }
        }
      }
      this.get_attached().clear();
    }
    });
    return {Lifetime_0:Lifetime, Viewable_0:Viewable};
  }
  ();
  Anonymous = Kotlin.Namespace.create({initialize:function(){
  }
  , lifetime_0:function(body){
    var l = new Anonymous.Lifetime_0;
    body(l);
    l.close_0();
  }
  , Dump:function(items){
    {
      tmp$0 = items.iterator();
      while (tmp$0.hasNext()) {
        item = tmp$0.next();
        {
          Kotlin.System.out() != null?Kotlin.System.out().print(Kotlin.toString(item) + ', '):null;
        }
      }
    }
    Kotlin.System.out() != null?Kotlin.System.out().println():null;
  }
  , main:function(args){
    var v = new Anonymous.Viewable_0;
    var x = new Kotlin.ArrayList;
    v.add(1);
    v.add(2);
    Anonymous.lifetime_0((tmp$0_0 = this , function(it){
      return v.view(it, (tmp$0 = this , function(itemLifetime, item){
        x.add(item);
        Anonymous.Dump(x);
        return itemLifetime.attach();
      }
      ));
    }
    ));
  }
  }, classes);
  Anonymous.initialize();
}
