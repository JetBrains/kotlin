// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.completion

import com.intellij.stats.completion.events.*

@Suppress("unused")
object LogEventFixtures {

    private const val sessionId = "session-id-xxx"
    private const val userId = "user-id-xxx"
    private const val bucket = "0"
    private val TEST_STATE = Fixtures.initialState
    private val NO_NEW_ITEMS_TEST_STATE = TEST_STATE.withoutNewItems()

    val completion_started_3_items_shown = CompletionStartedEvent("", "", "", userId, sessionId, "Java", true, 1, Fixtures.initialState,
                                                                  Fixtures.userFactors, Fixtures.contextFactors, 0, bucket, System.currentTimeMillis())

    val completion_cancelled = CompletionCancelledEvent(userId, sessionId, Fixtures.performance, bucket, System.currentTimeMillis())

    val type_event_current_pos_0_left_ids_1_2 = TypeEvent(userId, sessionId, lookupState(listOf(1, 2), 0), 1, bucket, System.currentTimeMillis())
    val type_event_current_pos_0_left_ids_0_1 = TypeEvent(userId, sessionId, lookupState(listOf(0, 1), 0), 1, bucket, System.currentTimeMillis())
    val type_event_current_pos_0_left_id_0 = TypeEvent(userId, sessionId, lookupState(listOf(0), 0), 1, bucket, System.currentTimeMillis())
    val type_event_no_items = TypeEvent(userId, sessionId, lookupState(emptyList(), -1), 1, bucket, System.currentTimeMillis())

    val up_pressed_new_pos_0 = UpPressedEvent(userId, sessionId, TEST_STATE.withSelected(0), bucket, System.currentTimeMillis())
    val up_pressed_new_pos_1 = UpPressedEvent(userId, sessionId, TEST_STATE.withSelected(1), bucket, System.currentTimeMillis())
    val up_pressed_new_pos_2 = UpPressedEvent(userId, sessionId, TEST_STATE.withSelected(2), bucket, System.currentTimeMillis())

    val down_event_new_pos_0 = DownPressedEvent(userId, sessionId, TEST_STATE.withSelected(0), bucket, System.currentTimeMillis())
    val down_event_new_pos_1 = DownPressedEvent(userId, sessionId, TEST_STATE.withSelected(1), bucket, System.currentTimeMillis())
    val down_event_new_pos_2 = DownPressedEvent(userId, sessionId, TEST_STATE.withSelected(2), bucket, System.currentTimeMillis())

    val backspace_event_pos_0_left_0_1_2 = BackspaceEvent(userId, sessionId, NO_NEW_ITEMS_TEST_STATE, 1, bucket, System.currentTimeMillis())
    val backspace_event_pos_0_left_1 = BackspaceEvent(userId, sessionId, lookupState(listOf(1), 0), 0,
                                                      bucket, System.currentTimeMillis())

    val explicit_select_position_0 = ExplicitSelectEvent(userId, sessionId, TEST_STATE.withSelected(0), 0, Fixtures.performance,
                                                         bucket, System.currentTimeMillis())
    val explicit_select_position_2 = ExplicitSelectEvent(userId, sessionId, TEST_STATE.withSelected(2), 2, Fixtures.performance,
                                                         bucket, System.currentTimeMillis())
    val explicit_select_position_1 = ExplicitSelectEvent(userId, sessionId, TEST_STATE.withSelected(1), 1, Fixtures.performance,
                                                         bucket, System.currentTimeMillis())

    val selected_by_typing_0 = TypedSelectEvent(userId, sessionId, TEST_STATE.withSelected(0), 0, Fixtures.performance, bucket, System.currentTimeMillis())
    val selected_by_typing_1 = TypedSelectEvent(userId, sessionId, TEST_STATE.withSelected(0), 1, Fixtures.performance, bucket, System.currentTimeMillis())

    private fun lookupState(ids: List<Int>, @Suppress("SameParameterValue") selectedPosition: Int): LookupState {
        return LookupState(ids, emptyList(), emptyList(), selectedPosition, emptyMap())
    }
}