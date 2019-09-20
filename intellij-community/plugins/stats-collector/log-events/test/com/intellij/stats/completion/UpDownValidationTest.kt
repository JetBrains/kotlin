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

class UpDownValidationTest {

    lateinit var state: CompletionValidationState

    @Before
    fun setUp() {
        state = CompletionValidationState(LogEventFixtures.completion_started_3_items_shown)
    }

    @Test
    fun `down pressed, new position 1, state is valid`() {
        state.accept(LogEventFixtures.down_event_new_pos_1)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(true)
    }

    @Test
    fun `down pressed, new pos 2, invalid`() {
        state.accept(LogEventFixtures.down_event_new_pos_2)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(false)
    }

    @Test
    fun `down pressed, new pos 0, invalid`() {
        state.accept(LogEventFixtures.down_event_new_pos_0)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(false)
    }

    @Test
    fun `down pressed, new position 0, state is not valid`() {
        state.accept(LogEventFixtures.down_event_new_pos_0)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(false)
    }

    @Test
    fun `sequence of downs cycles back to start`() {
        state.accept(LogEventFixtures.down_event_new_pos_1)
        state.accept(LogEventFixtures.down_event_new_pos_2)
        state.accept(LogEventFixtures.down_event_new_pos_0)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(true)
    }

    @Test
    fun `up pressed, new position is 2, state is valid`() {
        state.accept(LogEventFixtures.up_pressed_new_pos_2)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(true)
    }

    @Test
    fun `up pressed twice, new position 1, state is valid`() {
        state.accept(LogEventFixtures.up_pressed_new_pos_2)
        state.accept(LogEventFixtures.up_pressed_new_pos_1)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(true)
    }

    @Test
    fun `up cycles back to 0, state is valid`() {
        state.accept(LogEventFixtures.up_pressed_new_pos_2)
        state.accept(LogEventFixtures.up_pressed_new_pos_1)
        state.accept(LogEventFixtures.up_pressed_new_pos_0)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(true)
    }

    @Test
    fun `up pressed, new pos 1, invalid`() {
        state.accept(LogEventFixtures.up_pressed_new_pos_1)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(false)
    }

    @Test
    fun `up pressed, new pos 0, invalid`() {
        state.accept(LogEventFixtures.up_pressed_new_pos_0)
        Assertions.assertThat(state.isCurrentlyValid()).isEqualTo(false)
    }

}