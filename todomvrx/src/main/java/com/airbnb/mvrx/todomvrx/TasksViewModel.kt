package com.airbnb.mvrx.todomvrx

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.airbnb.mvrx.todomvrx.core.MvRxViewModel
import com.airbnb.mvrx.todomvrx.data.Task
import com.airbnb.mvrx.todomvrx.data.Tasks
import com.airbnb.mvrx.todomvrx.data.findTask
import com.airbnb.mvrx.todomvrx.data.source.TasksDataSource
import com.airbnb.mvrx.todomvrx.data.source.db.DatabaseDataSource
import com.airbnb.mvrx.todomvrx.data.source.db.ToDoDatabase
import com.airbnb.mvrx.todomvrx.util.copy
import com.airbnb.mvrx.todomvrx.util.delete
import com.airbnb.mvrx.todomvrx.util.upsert
import io.reactivex.Observable

data class TasksState(
        val tasks: Tasks = emptyList(),
        val taskRequest: Async<Tasks> = Uninitialized,
        val isLoading: Boolean = false,
        val lastEditedTask: String? = null
) : MvRxState

class TasksViewModel(initialState: TasksState, private val sources: List<TasksDataSource>) : MvRxViewModel<TasksState>(initialState) {

    init {
        logStateChanges()
        refreshTasks()
    }

    fun refreshTasks() {
        Observable.merge(sources.map { it.getTasks().toObservable() })
                .doOnSubscribe { setState { copy(isLoading = true) } }
                .doOnComplete { setState { copy(isLoading = false) } }
                .execute {
                    /**
                     * 这个this 和it 不一致的原因解释
                     *
                     * 1.  这个是一个扩展函数，扩展函数最后返回的是S类型的
                     * 2. s类型在这个ViewModel 是 TasksState ，所以这个this 就是 TasksState
                     *
                     * 3.  it 是 Async 的子类类型 说的是这个参数类型
                     *
                     * 这个是官网的高阶函数
                     * https://kotlinlang.org/docs/lambdas.html
                     *
                     *
                     *
                     * 4.
                     *
                     * 这个不是函数式接口
                     * https://www.cnblogs.com/dgwblog/p/11739500.html
                     *
                     * 简称 SAM  ---Single Abstract Method
                     * https://www.kotlincn.net/docs/reference/fun-interfaces.html
                     * https://kotlinlang.org/docs/fun-interfaces.html
                     *
                     * 但是我记得额扩函数是可以和一部分内容进行互换的
                     *
                     * it-implicit-name-of-a-single-parameter 一个是返回值，一个是作用域
                     * https://kotlinlang.org/docs/lambdas.html#it-implicit-name-of-a-single-parameter
                     *
                     * 5. copy的部分 还是另外一部分 TasksState 也就是这个this 了
                     *
                     * 6. 转换成 SAM 就是
                     *
                     * interface CallBack{
                     *
                     *  S test(Async<T> t)
                     *
                     * }
                     *
                     * 7. 因为扩展函数，那个指示器返回的是 S，不是it 了吗？？？需要测试java 代码
                     */
                    println("====refreshTasks======$it======$this===")
                    copy(taskRequest = it, tasks = it() ?: tasks, lastEditedTask = null)
                }
    }

    fun upsertTask(task: Task) {
        setState { copy(tasks = tasks.upsert(task) { it.id == task.id }, lastEditedTask = task.id) }
        sources.forEach { it.upsertTask(task) }
    }

    fun setComplete(id: String, complete: Boolean) {
        setState {
            val task = tasks.findTask(id) ?: return@setState this
            if (task.complete == complete) return@setState this
            copy(tasks = tasks.copy(tasks.indexOf(task), task.copy(complete = complete)), lastEditedTask = id)
        }
        sources.forEach { it.setComplete(id, complete) }
    }

    fun clearCompletedTasks() = setState {
        sources.forEach { it.clearCompletedTasks() }
        copy(tasks = tasks.filter { !it.complete }, lastEditedTask = null)
    }

    fun deleteTask(id: String) {
        setState { copy(tasks = tasks.delete { it.id == id }, lastEditedTask = id) }
        sources.forEach { it.deleteTask(id) }
    }

    companion object : MvRxViewModelFactory<TasksViewModel, TasksState> {

        override fun create(viewModelContext: ViewModelContext, state: TasksState): TasksViewModel {
            val database = ToDoDatabase.getInstance(viewModelContext.activity)
            // Simulate data sources of different speeds.
            // The slower one can be thought of as the network data source.
            val dataSource1 = DatabaseDataSource(database.taskDao(), 2000)
            val dataSource2 = DatabaseDataSource(database.taskDao(), 3500)
            return TasksViewModel(state, listOf(dataSource1, dataSource2))
        }
    }
}
