@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.airbnb.mvrx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue

data class OrderingState(val count: Int = 0) : MavericksState

/**
 * 结论是select 是有偏向性的，谁快选择谁，同步选择先注册的任务
 *
 * 具体的解释请看 Kotlin Select Expression.md
 *
 */
class SetStateWithStateOrderingTest : MavericksViewModel<OrderingState>(OrderingState()) {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            // We need to set main but don't want a synchronous state store to make sure that the real ordering is correct
            Dispatchers.setMain(StandardTestDispatcher())
            Mavericks.viewModelConfigFactory = MavericksViewModelConfigFactory(false)
        }

        @AfterClass
        @JvmStatic
        fun cleanup() {
            Dispatchers.resetMain()
            Mavericks.viewModelConfigFactory = MavericksViewModelConfigFactory(true)
        }
    }

    /**
     * 这个是理解的，单线程，按照队里执行
     *
     *  setState 先到先执行
     *
     */
    @Test
    fun test1() = runBlocking {
        val calls = mutableListOf<String>()
        setState {
            calls += "s1"
            copy(count = 1)
        }
        withState {
            calls += "w1"
        }
        assertMatches(calls, "s1", "w1")
    }

    /**
     * 这个包裹一层与否和第一个是一致的，不产生影响
     */
    @Test
    fun test2() = runBlocking {
        val calls = mutableListOf<String>()
        withState {
            calls += "w1"
            setState {
                calls += "s1"
                copy(count = 1)
            }
            withState {
                calls += "w2"
            }
        }
        assertMatches(calls, "w1", "s1", "w2")
    }

    /**
     * 这个的执行就开始产生问题了
     */
    @Test
    fun test3() = runBlocking {
        val calls = mutableListOf<String>()
        withState {
            calls += "w1"
            withState {
                calls += "w2"
            }
            setState {
                calls += "s1"
                copy(count = 1)
            }
        }
        assertMatches(calls, "w1", "s1", "w2")
    }

    @Test
    fun test4() = runBlocking {
        val calls = ConcurrentLinkedQueue<String>()
        withState {
            calls += "w1"
            withState {
                calls += "w2"
            }
            setState {
                calls += "s1"
                setState {
                    calls += "s2"
                    setState {
                        calls += "s3"
                        copy(count = 3)
                    }
                    copy(count = 2)
                }
                copy(count = 1)
            }
        }
        // In MvRx 1.x, this was [w1, s1, w2, s2, s3]
        assertMatches(calls, "w1", "s1", "s2", "s3", "w2")
    }

    @Test
    fun test5() = runBlocking {
        val calls = ConcurrentLinkedQueue<String>()
        withState {
            calls += "w1"
            withState {
                setState {
                    calls += "s4"
                    copy(count = 4)
                }
                calls += "w2"
            }
            setState {
                calls += "s1"
                setState {
                    calls += "s2"
                    setState {
                        calls += "s3"
                        copy(count = 3)
                    }
                    copy(count = 2)
                }
                copy(count = 1)
            }
        }
        // In MvRx 1.x, this was [w1, s1, w2, s2, s4, s3]
        assertMatches(calls, "w1", "s1", "s2", "s3", "w2", "s4")
    }

    @Test
    fun test6() = runBlocking {
        val calls = ConcurrentLinkedQueue<String>()
        withState {
            calls += "w1"
            withState {
                setState {
                    calls += "s4"
                    withState {
                        calls += "w3"
                    }
                    copy(count = 4)
                }
                calls += "w2"
            }
            setState {
                calls += "s1"
                setState {
                    calls += "s2"
                    setState {
                        calls += "s3"
                        copy(count = 3)
                    }
                    copy(count = 2)
                }
                copy(count = 1)
            }
        }
        // In MvRx 1.x, this was [w1, s1, w2, s2, s4, w3, s3]
        assertMatches(calls, "w1", "s1", "s2", "s3", "w2", "s4", "w3")
    }

    private suspend fun assertMatches(calls: Collection<String>, vararg expectedCalls: String) {
        while (calls.size != expectedCalls.size) {
            delay(1)
        }

        println("  assertMatches        ${expectedCalls.toList()}")
        assertEquals(expectedCalls.toList(), calls.toList())
    }
}
