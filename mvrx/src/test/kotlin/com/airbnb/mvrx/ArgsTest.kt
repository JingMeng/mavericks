package com.airbnb.mvrx

import android.os.Parcelable
import androidx.fragment.app.Fragment
import kotlinx.parcelize.Parcelize
import org.junit.Assert
import org.junit.Test

@Parcelize
data class MvrxArgsTestArgs(val count: Int = 0) : Parcelable

@Parcelize
data class MvrxArgsTestArgs2(val count: Int = 0) : Parcelable

class MvRxArgsFragment : Fragment(), MavericksView {
    val args: MvrxArgsTestArgs by args()

    override fun invalidate() {}
}

class MvRxArgsOrNullFragment : Fragment(), MavericksView {
    val args: MvrxArgsTestArgs? by argsOrNull()

    override fun invalidate() {}
}

class MvRxFragmentTest : BaseTest() {
    @Test
    fun testByArgsWithArgs() {
        //解构声明.md
        val (_, fragment) = createFragment<MvRxArgsFragment, TestMvRxActivity>(args = MvrxArgsTestArgs())
        Assert.assertEquals(0, fragment.args.count)
    }

    @Test
    fun testByArgsSetArgs() {
        val (_, fragment) = createFragment<MvRxArgsFragment, TestMvRxActivity>(args = MvrxArgsTestArgs(2))
        Assert.assertEquals(2, fragment.args.count)
    }

    /**
     * 没运行起来，应该是 args 这个地方的 getValue
     *
     *    value = argUntyped as V  这里报错导致的
     */
    @Test(expected = ClassCastException::class)
    fun testByArgsSetWrongArgs() {
        val (_, fragment) = createFragment<MvRxArgsFragment, TestMvRxActivity>(args = MvrxArgsTestArgs2(2))
        fragment.args
    }

    /**
     * 和 args 的源码最后一行抛出的
     */
    @Test(expected = IllegalArgumentException::class)
    fun testByArgsWithNoArgs() {
        val (_, fragment) = createFragment<MvRxArgsFragment, TestMvRxActivity>()
        fragment.args
    }

    @Test
    fun testByArgsOrNullWithArgs() {
        val (_, fragment) = createFragment<MvRxArgsOrNullFragment, TestMvRxActivity>(args = MvrxArgsTestArgs())
        Assert.assertEquals(0, fragment.args?.count)
    }

    @Test
    fun testByArgsOrNullSetArgs() {
        val (_, fragment) = createFragment<MvRxArgsOrNullFragment, TestMvRxActivity>(args = MvrxArgsTestArgs(2))
        Assert.assertEquals(2, fragment.args?.count)
    }

    @Test
    fun testByArgsOrNullWithNoArgs() {
        val (_, fragment) = createFragment<MvRxArgsOrNullFragment, TestMvRxActivity>()
        Assert.assertNull(fragment.args?.count)
    }

    @Test(expected = ClassCastException::class)
    fun testByArgsOrNullSetWrongArgs() {
        val (_, fragment) = createFragment<MvRxArgsOrNullFragment, TestMvRxActivity>(args = MvrxArgsTestArgs2(2))
        fragment.args
    }
}
