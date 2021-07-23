package com.example.mywallet.data.source

import com.example.mywallet.data.Result
import com.example.mywallet.data.Result.Error
import com.example.mywallet.data.Result.Success
import com.example.mywallet.data.Task
import com.example.mywallet.di.AppModule.TasksLocalDataSource
import com.example.mywallet.di.AppModule.TasksRemoteDataSource
import com.example.mywallet.util.EspressoIdlingResource
import com.example.mywallet.util.wrapEspressoIdlingResource
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTasksRepository @Inject constructor(
    @TasksRemoteDataSource private val tasksRemoteDataSource: TasksDataSource,
    @TasksLocalDataSource private val tasksLocalDataSource: TasksDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TasksRepository {

    private var cachedTasks: ConcurrentMap<String, Task>? = null

    override suspend fun getTasks(forceUpdate: Boolean): Result<List<Task>> {

        wrapEspressoIdlingResource {

            return withContext(ioDispatcher) {
                // Respond immediately with cache if available and not dirty
                if (!forceUpdate) {
                    cachedTasks?.let { cachedTasks ->
                        return@withContext Success(cachedTasks.values.sortedBy { it.id })
                    }
                }

                val newTasks = fetchTasksFromRemoteOrLocal(forceUpdate)

                // Refresh the cache with the new tasks
                (newTasks as? Success)?.let { refreshCache(it.data) }

                cachedTasks?.values?.let { tasks ->
                    return@withContext Success(tasks.sortedBy { it.id })
                }

                (newTasks as? Success)?.let {
                    if (it.data.isEmpty()) {
                        return@withContext Success(it.data)
                    }
                }

                return@withContext Error(Exception("Illegal state"))
            }
        }
    }

    private suspend fun fetchTasksFromRemoteOrLocal(forceUpdate: Boolean): Result<List<Task>> {
        // Remote first
        when (val remoteTasks = tasksRemoteDataSource.getTasks()) {
            is Error -> Timber.w("Remote data source fetch failed")
            is Success -> {
                refreshLocalDataSource(remoteTasks.data)
                return remoteTasks
            }
            else -> throw IllegalStateException()
        }

        // Don't read from local if it's forced
        if (forceUpdate) {
            return Error(Exception("Can't force refresh: remote data source is unavailable"))
        }

        // Local if remote fails
        val localTasks = tasksLocalDataSource.getTasks()
        if (localTasks is Success) return localTasks
        return Error(Exception("Error fetching from remote and local"))
    }

    /**
     * Relies on [getTasks] to fetch data and picks the task with the same ID.
     */
    override suspend fun getTask(taskId: String, forceUpdate: Boolean): Result<Task> {

        wrapEspressoIdlingResource {

            return withContext(ioDispatcher) {
                // Respond immediately with cache if available
                if (!forceUpdate) {
                    getTaskWithId(taskId)?.let {
                        EspressoIdlingResource.decrement() // Set app as idle.
                        return@withContext Success(it)
                    }
                }

                val newTask = fetchTaskFromRemoteOrLocal(taskId, forceUpdate)

                // Refresh the cache with the new tasks
                (newTask as? Success)?.let { cacheTask(it.data) }

                return@withContext newTask
            }
        }
    }

    private suspend fun fetchTaskFromRemoteOrLocal(
        taskId: String,
        forceUpdate: Boolean
    ): Result<Task> {
        // Remote first
        when (val remoteTask = tasksRemoteDataSource.getTask(taskId)) {
            is Error -> Timber.w("Remote data source fetch failed")
            is Success -> {
                refreshLocalDataSource(remoteTask.data)
                return remoteTask
            }
            else -> throw IllegalStateException()
        }

        // Don't read from local if it's forced
        if (forceUpdate) {
            return Error(Exception("Refresh failed"))
        }

        // Local if remote fails
        val localTasks = tasksLocalDataSource.getTask(taskId)
        if (localTasks is Success) return localTasks
        return Error(Exception("Error fetching from remote and local"))
    }

    override suspend fun saveTask(task: Task) {
        // Do in memory cache update to keep the app UI up to date
        cacheAndPerform(task) {
            coroutineScope {
                launch { tasksRemoteDataSource.saveTask(it) }
                launch { tasksLocalDataSource.saveTask(it) }
            }
        }
    }

    override suspend fun completeTask(task: Task) {
        // Do in memory cache update to keep the app UI up to date
        cacheAndPerform(task) {
            it.isCompleted = true
            coroutineScope {
                launch { tasksRemoteDataSource.completeTask(it) }
                launch { tasksLocalDataSource.completeTask(it) }
            }
        }
    }

    override suspend fun completeTask(taskId: String) {
        withContext(ioDispatcher) {
            getTaskWithId(taskId)?.let {
                completeTask(it)
            }
        }
    }

    override suspend fun activateTask(task: Task) = withContext(ioDispatcher) {
        // Do in memory cache update to keep the app UI up to date
        cacheAndPerform(task) {
            it.isCompleted = false
            coroutineScope {
                launch { tasksRemoteDataSource.activateTask(it) }
                launch { tasksLocalDataSource.activateTask(it) }
            }

        }
    }

    override suspend fun activateTask(taskId: String) {
        withContext(ioDispatcher) {
            getTaskWithId(taskId)?.let {
                activateTask(it)
            }
        }
    }

    override suspend fun clearCompletedTasks() {
        coroutineScope {
            launch { tasksRemoteDataSource.clearCompletedTasks() }
            launch { tasksLocalDataSource.clearCompletedTasks() }
        }
        withContext(ioDispatcher) {
            cachedTasks?.entries?.removeAll { it.value.isCompleted }
        }
    }

    override suspend fun deleteAllTasks() {
        withContext(ioDispatcher) {
            coroutineScope {
                launch { tasksRemoteDataSource.deleteAllTasks() }
                launch { tasksLocalDataSource.deleteAllTasks() }
            }
        }
        cachedTasks?.clear()
    }

    override suspend fun deleteTask(taskId: String) {
        coroutineScope {
            launch { tasksRemoteDataSource.deleteTask(taskId) }
            launch { tasksLocalDataSource.deleteTask(taskId) }
        }

        cachedTasks?.remove(taskId)
    }

    private fun refreshCache(tasks: List<Task>) {
        cachedTasks?.clear()
        tasks.sortedBy { it.id }.forEach {
            cacheAndPerform(it) {}
        }
    }

    private suspend fun refreshLocalDataSource(tasks: List<Task>) {
        tasksLocalDataSource.deleteAllTasks()
        for (task in tasks) {
            tasksLocalDataSource.saveTask(task)
        }
    }

    private suspend fun refreshLocalDataSource(task: Task) {
        tasksLocalDataSource.saveTask(task)
    }

    private fun getTaskWithId(id: String) = cachedTasks?.get(id)

    private fun cacheTask(task: Task): Task {
        val cachedTask =
            Task(
                task.base,
                task.counter,
                sellPrice = task.sellPrice,
                buyPrice = task.buyPrice,
                icon = task.icon,
                name = task.name,
                isCompleted = task.isCompleted,
                id = task.id
            )
        // Create if it doesn't exist.
        if (cachedTasks == null) {
            cachedTasks = ConcurrentHashMap()
        }
        cachedTasks?.put(cachedTask.id, cachedTask)
        return cachedTask
    }

    private inline fun cacheAndPerform(task: Task, perform: (Task) -> Unit) {
        val cachedTask = cacheTask(task)
        perform(cachedTask)
    }
}