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
    var FilterIterator_0 = Kotlin.Class.create(Kotlin.ArrayIterator, {initialize:function(original, filter_0){
      this.$original = original;
      this.$filter = filter_0;
      this.$state = 0;
      this.$nextElement = null;
    }
    , get_original:function(){
      return this.$original;
    }
    , get_filter:function(){
      return this.$filter;
    }
    , get_state:function(){
      return this.$state;
    }
    , set_state:function(tmp$0){
      this.$state = tmp$0;
    }
    , get_nextElement:function(){
      return this.$nextElement;
    }
    , set_nextElement:function(tmp$0){
      this.$nextElement = tmp$0;
    }
    , get_hasNext:function(){
      {
        var tmp$1;
        var tmp$0;
        if (this.get_state() == 1) {
          return true;
        }
        tmp$0;
        if (this.get_state() == 2) {
          return false;
        }
        tmp$1;
        while (this.get_original().get_hasNext()) {
          var tmp$2;
          var candidate = this.get_original().next();
          if (this.get_filter()(candidate)) {
            this.set_nextElement(candidate);
            this.set_state(1);
            return true;
          }
          tmp$2;
        }
        this.set_state(2);
        return false;
      }
    }
    , next:function(){
      {
        var res = this.get_nextElement();
        this.set_nextElement(null);
        this.set_state(0);
        return res;
      }
    }
    });
    var NoSuchElementException_0 = Kotlin.Class.create(Kotlin.Exception, {initialize:function(){
      this.super_init();
    }
    });
    return {NoSuchElementException:NoSuchElementException_0, FilterIterator:FilterIterator_0};
  }
  ();
  var foo = Kotlin.Namespace.create({initialize:function(){
  }
  , filter:function(receiver, f){
    {
      return new foo.FilterIterator(receiver, f);
    }
  }
  , filterTo:function(receiver, container, filter){
    {
      var tmp$0;
      {
        tmp$0 = receiver.iterator();
        while (tmp$0.hasNext()) {
          var element = tmp$0.next();
          {
            var tmp$1;
            if (filter(element))
              tmp$1 = container.add(element);
            tmp$1;
          }
        }
      }
      return container;
    }
  }
  }, classes);
  foo.initialize();
}
