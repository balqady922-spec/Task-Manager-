package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskRepository
import com.example.notifications.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTask(title: String, description: String, reminderTime: Long?) {
        viewModelScope.launch {
            val task = Task(title = title, description = description, reminderTime = reminderTime)
            val id = repository.insert(task)
            if (reminderTime != null) {
                alarmScheduler.scheduleAlarm(task.copy(id = id))
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.update(task)
            if (task.isCompleted || task.reminderTime == null) {
                alarmScheduler.cancelAlarm(task.id)
            } else {
                alarmScheduler.scheduleAlarm(task)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
            alarmScheduler.cancelAlarm(task.id)
        }
    }
}

class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository, alarmScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
