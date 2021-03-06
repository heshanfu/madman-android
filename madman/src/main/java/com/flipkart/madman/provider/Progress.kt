/*
 *
 *  * Copyright (C) 2019 Flipkart Internet Pvt Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.flipkart.madman.provider

/**
 * returns the current time and the duration of the media (ads or content)
 */
class Progress(currentTimeInMs: Long, durationInMs: Long) {
    var currentTime: Float = currentTimeInMs / 1000.0F
    var duration: Float = durationInMs / 1000.0F

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as Progress

        if (currentTime != other.currentTime) return false
        if (duration != other.duration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = currentTime.hashCode()
        result = 31 * result + duration.hashCode()
        return result
    }

    companion object {
        val UNDEFINED = Progress(-1, -1)
    }
}
