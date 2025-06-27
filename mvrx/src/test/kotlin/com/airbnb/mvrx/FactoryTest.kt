package com.airbnb.mvrx

import android.app.Application
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.parcelize.Parcelize
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric

private data class FactoryState(val greeting: String = "") : MavericksState {
    constructor(args: TestArgs) : this("${args.greeting} constructor")
}

@Parcelize
data class TestArgs(val greeting: String) : Parcelable

class ViewModelFactoryTestFragment : Fragment()

/**
 * Tests ViewModel creation when there is no factory.
 *
 * 这个地方的套路太深了，解析完了这个类，所有的调用都解析了
 *
 * 第一次运行比较慢
 */
class NoFactoryTest : BaseTest() {

    private class MyViewModelWithNonFactoryCompanion(initialState: FactoryState) : TestMavericksViewModel<FactoryState>(initialState) {
        companion object {
            // Companion object does not implement MvRxViewModelFactory
        }
    }

    private lateinit var activity: FragmentActivity

    @Before
    fun setup() {
        @Suppress("DEPRECATION")
        activity = Robolectric.setupActivity(FragmentActivity::class.java)
    }

    @Test
    fun createFromActivityOwner() {
        class MyViewModel(initialState: FactoryState) : TestMavericksViewModel<FactoryState>(initialState)

        val viewModel =
            MavericksViewModelProvider.get(MyViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
    }

    @Test
    fun createFromFragmentOwner() {
        val (_, fragment) = createFragment<ViewModelFactoryTestFragment, TestActivity>()
        class MyViewModel(initialState: FactoryState) : TestMavericksViewModel<FactoryState>(initialState)

        val viewModel = MavericksViewModelProvider.get(
            MyViewModel::class.java,
            FactoryState::class.java,
            FragmentViewModelContext(activity, TestArgs("hello"), fragment)
        )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
    }

    /**
     * 没看出来上一个和这一个的区别是什么
     *
     * 就是少了一个 fragment
     *
     * 正常的  FactoryState(greeting=hello constructor)
     */
    @Test
    fun createWithNonFactoryCompanion() {
        val viewModel = MavericksViewModelProvider.get(
            MyViewModelWithNonFactoryCompanion::class.java,
            FactoryState::class.java,
            ActivityViewModelContext(activity, TestArgs("hello"))
        )
        println("-----createWithNonFactoryCompanion---------${viewModel.state}-------")
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
    }

    /**
     * 修改一下就有完整的错误日志了
     *
     * java.lang.IllegalArgumentException: java.lang.Class must have primary constructor with a single non-optional parameter that takes initial state of com.airbnb.mvrx.FactoryState.
     * 	at com.airbnb.mvrx.MavericksFactoryKt.createViewModel(MavericksFactory.kt:56)
     * 	at com.airbnb.mvrx.MavericksFactoryKt.access$createViewModel(MavericksFactory.kt:1)
     * 	at com.airbnb.mvrx.MavericksFactory.create(MavericksFactory.kt:22)
     * 	at androidx.lifecycle.ViewModelProvider$Factory.create(ViewModelProvider.kt:83)
     * 	at androidx.lifecycle.ViewModelProvider.get(ViewModelProvider.kt:187)
     * 	at com.airbnb.mvrx.MavericksViewModelProvider.get(MavericksViewModelProvider.kt:63)
     * 	at com.airbnb.mvrx.MavericksViewModelProvider.get$default(MavericksViewModelProvider.kt:31)
     * 	at com.airbnb.mvrx.NoFactoryTest.failOnDefaultState(FactoryTest.kt:91)
     * 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     * 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
     * 	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
     * 	at java.base/java.lang.reflect.Method.invoke(Method.java:569)
     * 	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
     * 	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
     * 	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
     * 	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
     * 	at org.junit.internal.runners.statements.ExpectException.evaluate(ExpectException.java:19)
     * 	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
     * 	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
     * 	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
     * 	at org.robolectric.RobolectricTestRunner$HelperTestRunner$1.evaluate(RobolectricTestRunner.java:591)
     * 	at org.robolectric.internal.SandboxTestRunner$2.lambda$evaluate$0(SandboxTestRunner.java:274)
     * 	at org.robolectric.internal.bytecode.Sandbox.lambda$runOnMainThread$0(Sandbox.java:88)
     * 	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
     * 	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
     * 	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
     * 	at java.base/java.lang.Thread.run(Thread.java:840)
     *
     * 	    val viewModel = requireNotNull(factoryViewModel ?: createDefaultViewModel(viewModelClass, initialState)) {
     *         if (viewModelClass.constructors.firstOrNull()?.parameterTypes?.size?.let { it > 1 } == true) {
     *             "${viewModelClass.name} takes dependencies other than initialState. " +
     *                 "It must have companion object implementing ${MavericksViewModelFactory::class.java.name} " +
     *                 "with a create method returning a non-null ViewModel."
     *         } else {
     *             "${viewModelClass::class.java.name} must have primary constructor with a " +
     *                 "single non-optional parameter that takes initial state of ${stateClass.name}."
     *         }
     *     }
     *
     *  主要解读一下  requireNotNull 这个方法，没有创建成功的时候抛出了异常，没有创建成功是null
     *
     *  即使修改了得到的也是下面的这个，传递的是null的情况
     *  greeting=
     */
    @Test(expected = IllegalArgumentException::class)
    fun failOnDefaultState() {
        try {
            class MyViewModel(initialState: FactoryState = FactoryState()) : TestMavericksViewModel<FactoryState>(initialState)
//            class MyViewModel(initialState: FactoryState) : TestMavericksViewModel<FactoryState>(initialState)
            //下面两个都是一样的问题
            val result = MavericksViewModelProvider.get(MyViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, null))
            println("--failOnDefaultState---------$result----${result.state}--")
//            MavericksViewModelProvider.get(MyViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun failOnWrongSingleParameterType() {
        class ViewModel : MavericksViewModel<FactoryState>(initialState = FactoryState())
        MavericksViewModelProvider.get(ViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, null))
    }

    @Test(expected = IllegalArgumentException::class)
    fun failOnMultipleParametersAndNoCompanion() {
        class OptionalParamViewModel(initialState: FactoryState, @Suppress("UNUSED_PARAMETER") someOtherParam: Int) :
            MavericksViewModel<FactoryState>(initialState)
        MavericksViewModelProvider.get(OptionalParamViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, null))
    }

    @Test(expected = IllegalArgumentException::class)
    fun failOnNoViewModelParameters() {
        class OptionalParamViewModel : MavericksViewModel<FactoryState>(initialState = FactoryState())
        MavericksViewModelProvider.get(OptionalParamViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, null))
    }
}

/**
 * Test a factory which only uses a custom ViewModel create.
 */
class FactoryViewModelTest : BaseTest() {

    private class TestFactoryViewModel(initialState: FactoryState, val otherProp: Long) : TestMavericksViewModel<FactoryState>(initialState) {
        companion object : MavericksViewModelFactory<TestFactoryViewModel, FactoryState> {
            override fun create(viewModelContext: ViewModelContext, state: FactoryState): TestFactoryViewModel {
                return when (viewModelContext) {
                    // Use Fragment args to test that there is a valid fragment reference.
                    is FragmentViewModelContext -> TestFactoryViewModel(state, viewModelContext.fragment.arguments?.getLong("otherProp")!!)
                    else -> TestFactoryViewModel(state, 5L)
                }
            }
        }
    }

    private class TestFactoryJvmStaticViewModel(initialState: FactoryState, val otherProp: Long) :
        TestMavericksViewModel<FactoryState>(initialState) {
        companion object : MavericksViewModelFactory<TestFactoryJvmStaticViewModel, FactoryState> {
            @JvmStatic
            override fun create(viewModelContext: ViewModelContext, state: FactoryState) = TestFactoryJvmStaticViewModel(state, 5)
        }
    }

    private class TestNullFactory(initialState: FactoryState) : TestMavericksViewModel<FactoryState>(initialState) {
        companion object : MavericksViewModelFactory<TestFactoryViewModel, FactoryState> {
            override fun create(viewModelContext: ViewModelContext, state: FactoryState) = null
        }
    }

    private class NamedFactoryViewModel(initialState: FactoryState) : TestMavericksViewModel<FactoryState>(initialState) {

        // Ensures we don't accidently consider this to be the factory.
        class NestedClass

        companion object NamedFactory : MavericksViewModelFactory<NamedFactoryViewModel, FactoryState> {
            override fun create(viewModelContext: ViewModelContext, state: FactoryState) = NamedFactoryViewModel(state)
        }
    }

    private class ViewModelContextApplicationFactory(initialState: FactoryState) : TestMavericksViewModel<FactoryState>(initialState) {
        companion object : MavericksViewModelFactory<TestFactoryJvmStaticViewModel, FactoryState> {
            override fun create(viewModelContext: ViewModelContext, state: FactoryState): TestFactoryJvmStaticViewModel? {
                // If this doesn't crash then there was an application that successfully casted.
                viewModelContext.app<Application>()
                return null
            }
        }
    }

    private lateinit var activity: FragmentActivity

    @Before
    fun setup() {
        @Suppress("DEPRECATION")
        activity = Robolectric.setupActivity(FragmentActivity::class.java)
    }

    @Test
    fun createFromActivityOwner() {
        val viewModel = MavericksViewModelProvider.get(
            TestFactoryViewModel::class.java,
            FactoryState::class.java,
            ActivityViewModelContext(activity, TestArgs("hello"))
        )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
        assertEquals(5, viewModel.otherProp)
    }

    @Test
    fun createFromFragmentOwner() {
        val (_, fragment) = createFragment<ViewModelFactoryTestFragment, TestActivity>()
        fragment.arguments = Bundle().apply { putLong("otherProp", 6L) }
        val viewModel = MavericksViewModelProvider.get(
            TestFactoryViewModel::class.java,
            FactoryState::class.java,
            FragmentViewModelContext(activity, TestArgs("hello"), fragment)
        )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
        assertEquals(6, viewModel.otherProp)
    }

    @Test
    fun createWithJvmStatic() {
        val viewModel = MavericksViewModelProvider.get(
            TestFactoryJvmStaticViewModel::class.java,
            FactoryState::class.java,
            ActivityViewModelContext(activity, TestArgs("hello"))
        )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
        assertEquals(5, viewModel.otherProp)
    }

    @Test
    fun createWithNamedFactory() {
        val viewModel = MavericksViewModelProvider.get(
            NamedFactoryViewModel::class.java,
            FactoryState::class.java,
            ActivityViewModelContext(activity, TestArgs("hello"))
        )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
    }

    @Test
    fun nullInitialStateDelegatesToConstructor() {
        val viewModel =
            MavericksViewModelProvider.get(
                TestNullFactory::class.java,
                FactoryState::class.java,
                ActivityViewModelContext(activity, TestArgs("hello"))
            )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
    }

    @Test
    fun testApplicationCanBeAccessed() {
        MavericksViewModelProvider.get(
            ViewModelContextApplicationFactory::class.java,
            FactoryState::class.java,
            ActivityViewModelContext(activity, TestArgs("hello"))
        )
    }
}

/**
 * Test a factory which only uses a custom initialState.
 */
class FactoryStateTest : BaseTest() {

    private class TestFactoryViewModel(initialState: FactoryState) : TestMavericksViewModel<FactoryState>(initialState) {
        companion object : MavericksViewModelFactory<TestFactoryViewModel, FactoryState> {
            override fun initialState(viewModelContext: ViewModelContext): FactoryState {
                return when (viewModelContext) {
                    is FragmentViewModelContext -> FactoryState("${viewModelContext.fragment.arguments?.getString("greeting")!!} and ${viewModelContext.args<TestArgs>().greeting} factory")
                    else -> FactoryState("${viewModelContext.args<TestArgs>().greeting} factory")
                }
            }
        }
    }

    private class TestFactoryJvmStaticViewModel(initialState: FactoryState) : TestMavericksViewModel<FactoryState>(initialState) {
        companion object : MavericksViewModelFactory<TestFactoryJvmStaticViewModel, FactoryState> {
            override fun initialState(viewModelContext: ViewModelContext): FactoryState =
                FactoryState("${viewModelContext.args<TestArgs>().greeting} factory")
        }
    }

    private class TestNullFactory(initialState: FactoryState) : TestMavericksViewModel<FactoryState>(initialState) {
        companion object : MavericksViewModelFactory<TestNullFactory, FactoryState> {
            override fun initialState(viewModelContext: ViewModelContext): FactoryState? = null
        }
    }

    private lateinit var activity: FragmentActivity

    @Before
    fun setup() {
        @Suppress("DEPRECATION")
        activity = Robolectric.setupActivity(FragmentActivity::class.java)
    }

    @Test
    fun createFromActivityOwner() {
        val viewModel = MavericksViewModelProvider.get(
            TestFactoryViewModel::class.java,
            FactoryState::class.java,
            ActivityViewModelContext(activity, TestArgs("hello"))
        )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello factory"), state)
        }
    }

    @Test
    fun createFromFragmentOwner() {
        val (_, fragment) = createFragment<ViewModelFactoryTestFragment, TestActivity>()
        fragment.arguments = Bundle().apply { putString("greeting", "howdy") }
        val viewModel = MavericksViewModelProvider.get(
            TestFactoryViewModel::class.java,
            FactoryState::class.java,
            FragmentViewModelContext(activity, TestArgs("hello"), fragment)
        )
        withState(viewModel) { state ->
            assertEquals(FactoryState("howdy and hello factory"), state)
        }
    }

    @Test
    fun createWithJvmStatic() {
        val viewModel = MavericksViewModelProvider.get(
            TestFactoryJvmStaticViewModel::class.java,
            FactoryState::class.java,
            ActivityViewModelContext(activity, TestArgs("hello"))
        )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello factory"), state)
        }
    }

    @Test
    fun nullInitialStateDelegatesToConstructor() {
        val viewModel =
            MavericksViewModelProvider.get(
                TestNullFactory::class.java,
                FactoryState::class.java,
                ActivityViewModelContext(activity, TestArgs("hello"))
            )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
    }
}

/**
 * Test a factory which uses both a custom State and ViewModel create.
 */
class FactoryViewModelAndStateTest : BaseTest() {

    private class TestFactoryViewModel(initialState: FactoryState, val otherProp: Long) : TestMavericksViewModel<FactoryState>(initialState) {
        companion object : MavericksViewModelFactory<TestFactoryViewModel, FactoryState> {
            override fun create(viewModelContext: ViewModelContext, state: FactoryState) =
                TestFactoryViewModel(FactoryState("${viewModelContext.args<TestArgs>().greeting} factory"), 5)
        }
    }

    private class TestFactoryJvmStaticViewModel(initialState: FactoryState, val otherProp: Long) :
        TestMavericksViewModel<FactoryState>(initialState) {
        companion object : MavericksViewModelFactory<TestFactoryJvmStaticViewModel, FactoryState> {
            @JvmStatic
            override fun create(viewModelContext: ViewModelContext, state: FactoryState) =
                TestFactoryJvmStaticViewModel(FactoryState("${viewModelContext.args<TestArgs>().greeting} factory"), 5)
        }
    }

    private lateinit var activity: FragmentActivity

    @Before
    fun setup() {
        @Suppress("DEPRECATION")
        activity = Robolectric.setupActivity(FragmentActivity::class.java)
    }

    @Test
    fun createFromActivityOwner() {
        val viewModel = MavericksViewModelProvider.get(
            TestFactoryViewModel::class.java,
            FactoryState::class.java,
            ActivityViewModelContext(activity, TestArgs("hello"))
        )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello factory"), state)
        }
        assertEquals(5, viewModel.otherProp)
    }

    @Test
    fun createFromFragmentOwner() {
        val (_, fragment) = createFragment<ViewModelFactoryTestFragment, TestActivity>()

        val viewModel = MavericksViewModelProvider.get(
            TestFactoryViewModel::class.java,
            FactoryState::class.java,
            FragmentViewModelContext(activity, TestArgs("hello"), fragment)
        )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello factory"), state)
        }
        assertEquals(5, viewModel.otherProp)
    }

    @Test
    fun createWithJvmStatic() {
        val viewModel = MavericksViewModelProvider.get(
            TestFactoryJvmStaticViewModel::class.java,
            FactoryState::class.java,
            ActivityViewModelContext(activity, TestArgs("hello"))
        )
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello factory"), state)
        }
        assertEquals(5, viewModel.otherProp)
    }
}
