package com.yuranime.dinz.ui.jadwal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuranime.dinz.data.repository.AnimeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JadwalViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(JadwalUiState(selectedDay = getTodayScheduleDay()))
    val uiState: StateFlow<JadwalUiState> = _uiState.asStateFlow()

    init {
        loadSchedule(_uiState.value.selectedDay)
    }

    private fun getTodayScheduleDay(): ScheduleDay {
        val sdf = SimpleDateFormat("EEEE", Locale("id", "ID"))
        val dayName = sdf.format(Date()).uppercase(Locale.ROOT)
        return when (dayName) {
            "SENIN" -> ScheduleDay.SENIN
            "SELASA" -> ScheduleDay.SELASA
            "RABU" -> ScheduleDay.RABU
            "KAMIS" -> ScheduleDay.KAMIS
            "JUMAT" -> ScheduleDay.JUMAT
            "SABTU" -> ScheduleDay.SABTU
            "MINGGU" -> ScheduleDay.MINGGU
            else -> ScheduleDay.SENIN
        }
    }

    fun selectDay(day: ScheduleDay) {
        if (_uiState.value.selectedDay == day && _uiState.value.scheduledAnimeList.isNotEmpty()) {
            return
        }
        _uiState.update { it.copy(selectedDay = day) }
        loadSchedule(day)
    }

    fun loadSchedule(day: ScheduleDay) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val scheduleDeferred = async { repository.getSchedule(day.apiDay) }
            val cuplixDeferred = async {
                repository.getRandomizedCuplixFeed(
                    limit = 15,
                    source = AnimeRepository.CuplixPageSource.JADWAL
                )
            }

            val scheduleResult = scheduleDeferred.await()
            val cuplixResult = cuplixDeferred.await()

            if (scheduleResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        scheduledAnimeList = scheduleResult.getOrDefault(emptyList()),
                        cuplixList = cuplixResult.getOrNull()?.takeIf { list -> list.isNotEmpty() } ?: it.cuplixList
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = scheduleResult.exceptionOrNull()?.message ?: "Gagal memuat jadwal tayang"
                    )
                }
            }
        }
    }
}
