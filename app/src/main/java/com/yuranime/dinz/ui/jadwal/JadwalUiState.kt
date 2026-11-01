package com.yuranime.dinz.ui.jadwal

import com.yuranime.dinz.data.model.AnimeItem
import com.yuranime.dinz.data.model.CuplixItem

enum class ScheduleDay(val apiDay: String, val label: String) {
    SENIN("SENIN", "SEN"),
    SELASA("SELASA", "SEL"),
    RABU("RABU", "RAB"),
    KAMIS("KAMIS", "KAM"),
    JUMAT("JUMAT", "JUM"),
    SABTU("SABTU", "SAB"),
    MINGGU("MINGGU", "MIN")
}

data class JadwalUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedDay: ScheduleDay = ScheduleDay.SENIN,
    val scheduledAnimeList: List<AnimeItem> = emptyList(),
    val cuplixList: List<CuplixItem> = emptyList()
)
