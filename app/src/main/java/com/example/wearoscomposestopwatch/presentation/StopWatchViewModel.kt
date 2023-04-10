package com.example.wearoscomposestopwatch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Created by Jaehyeon on 2023/04/10.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StopWatchViewModel : ViewModel(){

    private val _elapsedTime = MutableStateFlow(0L)
    private val _timerState = MutableStateFlow(TimerState.RESET)
    val timerState = _timerState.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")

    val stopWatchText = _elapsedTime
        .map { millis ->
            LocalTime.ofNanoOfDay(millis * 1_000_000).format(formatter)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "00:00:00:000"
        )

    init {
        _timerState
            .flatMapLatest { timerState ->
                getTimeFlow(
                    isRunning = timerState == TimerState.RUNNING
                )
            }.onEach { timeDiff ->
                _elapsedTime.update { it + timeDiff }
            }.launchIn(viewModelScope)
    }

    fun toggleIsRunning() {
        when(timerState.value) {
            TimerState.RUNNING -> _timerState.update { TimerState.PAUSE }
            TimerState.PAUSE,
                TimerState.RESET -> _timerState.update { TimerState.RUNNING }
        }
    }

    fun resetTimer() {
        _timerState.update { TimerState.RESET }
        _elapsedTime.update { 0L }
    }

    private fun getTimeFlow(isRunning: Boolean): Flow<Long> = flow {
        var startMillis = System.currentTimeMillis()
        while (isRunning) {
            val currentMillis = System.currentTimeMillis()
            val timeDiff = if (currentMillis > startMillis) {
                currentMillis - startMillis
            } else 0L
            emit(timeDiff)
            startMillis = System.currentTimeMillis()
            delay(10L)
        }
    }
}