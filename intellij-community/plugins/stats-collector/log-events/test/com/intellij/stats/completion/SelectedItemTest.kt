/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.stats.completion

import com.intellij.stats.validation.CompletionValidationState
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class SelectedItemTest {

    lateinit var state: CompletionValidationState

    @Before
    fun setUp() {
        state = CompletionValidationState(LogEventFixtures.completion_started_3_items_shown)
    }

    @Test
    fun `explicit select`() {
        state.accept(LogEventFixtures.explicit_select_position_0)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(true)
    }

    @Test
    fun `explicit select of incorrect item`() {
        state.accept(LogEventFixtures.type_event_current_pos_0_left_ids_0_1)
        state.accept(LogEventFixtures.explicit_select_position_1)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(false)
    }

    @Test
    fun `completion cancelled`() {
        state.accept(LogEventFixtures.completion_cancelled)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(true)
    }
}
