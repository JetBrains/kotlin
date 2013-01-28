/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

/**
 * Copyright 2010 Tim Down.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
function HashSet(c,a){var b=new Hashtable(c,a);this.add=function(d){b.put(d,true)};this.addAll=function(d){var e=d.length;while(e--){b.put(d[e],true)}};this.values=function(){return b.keys()};this.remove=function(d){return b.remove(d)?d:null};this.contains=function(d){return b.containsKey(d)};this.clear=function(){b.clear()};this.size=function(){return b.size()};this.isEmpty=function(){return b.isEmpty()};this.clone=function(){var d=new HashSet(c,a);d.addAll(b.keys());return d};this.intersection=function(d){var h=new HashSet(c,a);var e=d.values(),f=e.length,g;while(f--){g=e[f];if(b.containsKey(g)){h.add(g)}}return h};this.union=function(d){var g=this.clone();var e=d.values(),f=e.length,h;while(f--){h=e[f];if(!b.containsKey(h)){g.add(h)}}return g};this.isSubsetOf=function(d){var e=b.keys(),f=e.length;while(f--){if(!d.contains(e[f])){return false}}return true}};