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

package com.flipkart.madman.manager

import com.flipkart.madman.component.enums.AdErrorType
import com.flipkart.madman.component.enums.AdEventType
import com.flipkart.madman.component.model.vast.VASTData
import com.flipkart.madman.component.model.vmap.VMAPData
import com.flipkart.madman.listener.AdErrorListener
import com.flipkart.madman.listener.AdEventListener
import com.flipkart.madman.listener.impl.AdError
import com.flipkart.madman.listener.impl.AdEvent
import com.flipkart.madman.loader.AdLoader
import com.flipkart.madman.manager.data.VastAdProvider
import com.flipkart.madman.manager.data.providers.NetworkVastAdProvider
import com.flipkart.madman.manager.data.providers.StringVastAdProvider
import com.flipkart.madman.manager.data.providers.VastAdProviderImpl
import com.flipkart.madman.network.NetworkLayer
import com.flipkart.madman.network.model.NetworkAdRequest
import com.flipkart.madman.provider.ContentProgressProvider
import com.flipkart.madman.provider.Progress
import com.flipkart.madman.renderer.AdRenderer
import com.flipkart.madman.renderer.player.AdPlayer
import com.flipkart.madman.renderer.settings.DefaultRenderingSettings
import com.flipkart.madman.testutils.VMAPUtil
import com.flipkart.madman.testutils.anyObject
import com.flipkart.madman.testutils.capture
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.Mockito.*
import org.mockito.stubbing.Answer
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Test for [AdManager]
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21])
class DefaultAdManagerTest {

    @Mock
    private lateinit var mockAdLoader: AdLoader<NetworkAdRequest>

    @Mock
    private lateinit var mockNetworkLayer: NetworkLayer

    @Mock
    private lateinit var mockAdEventListener: AdEventListener

    @Mock
    private lateinit var mockAdErrorListener: AdErrorListener

    @Mock
    private lateinit var mockNetworkVastAdProvider: NetworkVastAdProvider

    @Mock
    private lateinit var mockStringVastAdProvider: StringVastAdProvider

    @Spy
    private lateinit var mockAdPlayer: AdPlayer

    @Spy
    private lateinit var mockAdRenderer: AdRenderer

    @Spy
    private lateinit var mockContentProgressProvider: ContentProgressProvider

    @Captor
    private lateinit var adEventCaptor: ArgumentCaptor<AdEvent>

    @Captor
    private lateinit var adErrorCaptor: ArgumentCaptor<AdError>

    private var adManager: AdManager? = null

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        /** return undefined progress in start **/
        `when`(mockContentProgressProvider.getContentProgress()).thenReturn(Progress.UNDEFINED)

        /** return undefined progress in start **/
        `when`(mockAdPlayer.getAdProgress()).thenReturn(Progress.UNDEFINED)

        /** return default rendering settings **/
        `when`(mockAdRenderer.getRenderingSettings()).thenReturn(DefaultRenderingSettings())

        /** return default rendering settings **/
        `when`(mockAdRenderer.getAdPlayer()).thenReturn(mockAdPlayer)
    }

    @After
    fun tearDown() {
        adManager?.destroy()
    }

    /**
     * Test to verify the behaviour of the ad manager with a pre roll ad break
     */
    @Test
    fun testIfCorrectEventsAreTriggeredDuringAdPlayback() {
        val vmap = VMAPUtil.createVMAP(true)

        adManager = DefaultAdManager(
            vmap,
            mockAdLoader,
            mockNetworkLayer,
            mockAdRenderer,
            mockAdEventListener,
            mockAdErrorListener
        )

        /** initialise the ad manager **/
        adManager?.init(mockContentProgressProvider)

        /** ad event listener is called with LOADED event as we have a pre-roll to play **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.LOADED)
        verify(mockAdPlayer, times(1)).loadAd(anyObject())
        reset(mockAdEventListener)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        /** ad event listener is called with CONTENT_PAUSE_REQUESTED event **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.CONTENT_PAUSE_REQUESTED)
        verify(mockAdPlayer, times(1)).playAd()
        reset(mockAdEventListener)

        /** start the ad play **/
        `when`(mockAdPlayer.getAdProgress()).thenReturn(Progress(1, 10))
        (adManager as? DefaultAdManager)?.onPlay()

        /** ad event listener is called with STARTED event **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.STARTED)
        reset(mockAdEventListener)

        `when`(mockAdPlayer.getAdProgress()).thenReturn(Progress(2, 10))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        /** ad event listener is called with PROGRESS event **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.PROGRESS)
        reset(mockAdEventListener)

        `when`(mockAdPlayer.getAdProgress()).thenReturn(Progress(3, 10))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        /** ad event listener is called with FIRST_QUARTILE event **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.FIRST_QUARTILE)
        reset(mockAdEventListener)

        `when`(mockAdPlayer.getAdProgress()).thenReturn(Progress(6, 10))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        /** ad event listener is called with MIDPOINT event **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.MIDPOINT)
        reset(mockAdEventListener)

        `when`(mockAdPlayer.getAdProgress()).thenReturn(Progress(10, 10))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        /** ad event listener is called with THIRD_QUARTILE event **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.THIRD_QUARTILE)

        /** mark the ad as completed **/
        (adManager as? DefaultAdManager)?.onEnded()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        /** ad event listener is called 3 times with AD_STOPPED, AD_COMPLETED and CONTENT_RESUME_REQUESTED event **/
        verify(mockAdEventListener, times(3)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.CONTENT_RESUME_REQUESTED)
        verify(mockAdPlayer, times(1)).stopAd()
    }

    /**
     * Test to verify the behaviour when no pre-roll is present
     */
    @Test
    fun testBehaviourWithoutPreRoll() {
        val vmap = VMAPUtil.createVMAP(false)

        adManager = DefaultAdManager(
            vmap,
            mockAdLoader,
            mockNetworkLayer,
            mockAdRenderer,
            mockAdEventListener,
            mockAdErrorListener
        )

        /** initialise the ad manager **/
        adManager?.init(mockContentProgressProvider)

        /** ad event listener is called with content resume requested as no pre-roll is present **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.CONTENT_RESUME_REQUESTED)
    }

    /**
     * Test to verify the behaviour when the ad is paused and resumed
     */
    @Test
    fun testBehaviourWhenAdIsPausedAndResumed() {
        val vmap = VMAPUtil.createVMAP(true)

        adManager = DefaultAdManager(
            vmap,
            mockAdLoader,
            mockNetworkLayer,
            mockAdRenderer,
            mockAdEventListener,
            mockAdErrorListener
        )

        /** initialise the ad manager **/
        adManager?.init(mockContentProgressProvider)

        /** ad event listener is called with LOADED event **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.LOADED)
        verify(mockAdPlayer, times(1)).loadAd(anyObject())

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        reset(mockAdEventListener)

        `when`(mockAdPlayer.getAdProgress()).thenReturn(Progress(3, 10))
        /** pause the ad **/
        adManager?.pause()

        /** ad event listener is called with PAUSED event **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.PAUSED)
        verify(mockAdPlayer, times(1)).pauseAd()
        reset(mockAdEventListener)
        reset(mockAdPlayer)

        `when`(mockAdPlayer.getAdProgress()).thenReturn(Progress(6, 10))
        /** resume the ad now **/
        adManager?.resume()

        /** ad event listener is called with RESUMED and STARTED event **/
        verify(mockAdEventListener, times(2)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.STARTED)
        verify(mockAdPlayer, times(1)).playAd()
    }

    /**
     * Test to verify the behaviour when the vast has no media files to play
     */
    @Test
    fun testWhenVASTHasNoMediaFiles() {
        val vmap = VMAPUtil.createVMAP(true)

        adManager = MockedAdManager(
            vmap,
            mockAdLoader,
            mockNetworkLayer,
            mockAdRenderer,
            mockAdEventListener,
            mockAdErrorListener,
            VastAdProviderImpl(mockStringVastAdProvider, mockNetworkVastAdProvider)
        )

        val answer = Answer { invocation ->
            val listener = invocation.getArgument<VastAdProvider.Listener>(1)
            // mimics return empty vast data
            listener.onVastFetchSuccess(VASTData())
        }
        doAnswer(answer).`when`(mockStringVastAdProvider).getVASTAd(anyObject(), anyObject())

        /** initialise the ad manager **/
        adManager?.init(mockContentProgressProvider)

        /** ad event listener is not called **/
        verify(mockAdEventListener, times(0)).onAdEvent(capture(adEventCaptor))

        /** ad error listener is called with NO_MEDIA_URL error **/
        verify(mockAdErrorListener, times(1)).onAdError(capture(adErrorCaptor))
        assert(adErrorCaptor.value.getType() == AdErrorType.NO_MEDIA_URL)
    }

    /**
     * Test to verify the behaviour when the vast fetch fails with an error
     */
    @Test
    fun testWhenVastFetchFails() {
        val vmap = VMAPUtil.createVMAP(true)

        adManager = MockedAdManager(
            vmap,
            mockAdLoader,
            mockNetworkLayer,
            mockAdRenderer,
            mockAdEventListener,
            mockAdErrorListener,
            VastAdProviderImpl(mockStringVastAdProvider, mockNetworkVastAdProvider)
        )

        val answer = Answer { invocation ->
            val listener = invocation.getArgument<VastAdProvider.Listener>(1)
            // mimics a vast fetch error callback
            listener.onVastFetchError(AdErrorType.VAST_ERROR, "Error while fetching")
        }
        doAnswer(answer).`when`(mockStringVastAdProvider).getVASTAd(anyObject(), anyObject())

        /** initialise the ad manager **/
        adManager?.init(mockContentProgressProvider)

        /** ad event listener is not called **/
        verify(mockAdEventListener, times(0)).onAdEvent(capture(adEventCaptor))

        /** ad error listener is called with VAST_ERROR error **/
        verify(mockAdErrorListener, times(1)).onAdError(capture(adErrorCaptor))
        assert(adErrorCaptor.value.getType() == AdErrorType.VAST_ERROR)
    }

    /**
     * Test to verify the behaviour when ad is skipped
     */
    @Test
    fun testWhenAdIsSkipped() {
        val vmap = VMAPUtil.createVMAP(true)

        adManager = DefaultAdManager(
            vmap,
            mockAdLoader,
            mockNetworkLayer,
            mockAdRenderer,
            mockAdEventListener,
            mockAdErrorListener
        )

        /** initialise the ad manager **/
        adManager?.init(mockContentProgressProvider)

        /** ad event listener is called with LOADED event **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.LOADED)
        verify(mockAdPlayer, times(1)).loadAd(anyObject())

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        reset(mockAdEventListener)

        /** skip ad **/
        (adManager as? DefaultAdManager)?.onSkipAdClick()

        /** ad event listener is called with SKIPPED event **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.SKIPPED)
        reset(mockAdEventListener)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        /** ad event listener is called with CONTENT_RESUME_REQUESTED event **/
        verify(mockAdEventListener, times(1)).onAdEvent(capture(adEventCaptor))
        assert(adEventCaptor.value.getType() == AdEventType.CONTENT_RESUME_REQUESTED)
    }

    /**
     * Test instance of [DefaultAdManager]
     */
    class MockedAdManager(
        data: VMAPData,
        adLoader: AdLoader<NetworkAdRequest>,
        networkLayer: NetworkLayer,
        adRenderer: AdRenderer,
        adEventListener: AdEventListener,
        adErrorListener: AdErrorListener,
        private val mockedVastAdProvider: VastAdProvider
    ) : DefaultAdManager(
        data,
        adLoader,
        networkLayer,
        adRenderer,
        adEventListener,
        adErrorListener
    ) {
        override fun createVastAdProvider(): VastAdProvider {
            return mockedVastAdProvider
        }
    }
}
