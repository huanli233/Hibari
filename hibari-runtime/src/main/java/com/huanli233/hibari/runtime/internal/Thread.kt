/*
 * Copyright 2024 The Android Open Source Project
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

package com.huanli233.hibari.runtime.internal

import android.os.Looper

internal val MainThreadId: Long =
    try {
        @Suppress("DEPRECATION") Looper.getMainLooper().thread.id
    } catch (e: Exception) {
        // When linked against Android SDK stubs and running host-side tests, APIs such as
        // Looper.getMainLooper() can throw or return null
        // This branch intercepts that exception and returns default value for such cases.
        -1
    }

@Suppress("DEPRECATION")
internal fun currentThreadId(): Long = Thread.currentThread().id

internal fun currentThreadName(): String = Thread.currentThread().name