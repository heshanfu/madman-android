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

package com.flipkart.madman.manager.data.providers

import com.flipkart.madman.component.enums.AdErrorType
import com.flipkart.madman.component.model.vmap.AdBreak
import com.flipkart.madman.manager.data.VastAdProvider

/**
 * Implementation of [VastAdProvider] which reads the string vast data in the given ad break.
 *
 * It assumes the model already contains the VAST embedded.
 */
class StringVastAdProvider : VastAdProvider {
    override fun getVASTAd(adBreak: AdBreak, listener: VastAdProvider.Listener) {
        adBreak.adSource?.vastAdData?.let {
            if (it.ads?.isNotEmpty() == true) {
                listener.onVastFetchSuccess(it)
            } else {
                listener.onVastFetchError(AdErrorType.VAST_ERROR, "No ad to play for $adBreak")
            }
        } ?: run {
            listener.onVastFetchError(AdErrorType.VAST_ERROR, "No vast ad to play for $adBreak")
        }
    }
}
