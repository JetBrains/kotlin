/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * @author: Dmitry Batkovich
 */
function navigate(an_id) {
  problem_div = document.getElementById("d" + an_id);
  preview_div = document.getElementById("preview");
  preview_div.innerHTML = problem_div != null ? problem_div.innerHTML : "Select a problem element in tree";
}