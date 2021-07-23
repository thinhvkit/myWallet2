package com.example.mywallet.data.source.remote

import com.example.mywallet.data.Result
import com.example.mywallet.data.Result.Error
import com.example.mywallet.data.Result.Success
import com.example.mywallet.data.Task
import com.example.mywallet.data.source.TasksDataSource
import kotlinx.coroutines.delay

/**
 * Implementation of the data source that adds a latency simulating network.
 */
class TasksRemoteDataSource internal constructor(private val apiHelper: ApiHelper) :
    TasksDataSource {

    private val SERVICE_LATENCY_IN_MILLIS = 2000L

    private var TASKS_SERVICE_DATA = LinkedHashMap<String, Task>(2)

    override suspend fun getTasks(): Result<List<Task>> {
        apiHelper.getTasks().let {
            return if (it.isSuccessful) {
                val body = it.body()
                Success(body?.data.orEmpty())
            }else{
                Error(Exception(it.errorBody().toString()))
            }
        }
    }

    override suspend fun getTask(taskId: String): Result<Task> {

        // Simulate network by delaying the execution.
        delay(SERVICE_LATENCY_IN_MILLIS)
        TASKS_SERVICE_DATA[taskId]?.let {
            return Success(it)
        }
        return Error(Exception("Task not found"))
    }

    override suspend fun saveTask(task: Task) {
        TASKS_SERVICE_DATA[task.id] = task
    }

    override suspend fun completeTask(task: Task) {
        //
    }

    override suspend fun completeTask(taskId: String) {
       //
    }

    override suspend fun activateTask(task: Task) {
       //
    }

    override suspend fun activateTask(taskId: String) {
        //
    }

    override suspend fun clearCompletedTasks() {
       //
    }

    override suspend fun deleteAllTasks() {
        TASKS_SERVICE_DATA.clear()
    }

    override suspend fun deleteTask(taskId: String) {
        TASKS_SERVICE_DATA.remove(taskId)
    }
}