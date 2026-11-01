package com.thikeiw.lolinfo

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Champions : NavKey

@Serializable data class ChampionDetail(val championId: String) : NavKey
