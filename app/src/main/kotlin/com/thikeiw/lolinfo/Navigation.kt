package com.thikeiw.lolinfo

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.thikeiw.lolinfo.ui.championdetail.ChampionDetailScreen
import com.thikeiw.lolinfo.ui.champions.ChampionsScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Champions)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Champions> {
          ChampionsScreen(
            onChampionClick = { championId -> backStack.add(ChampionDetail(championId)) }
          )
        }
        entry<ChampionDetail> { key ->
          ChampionDetailScreen(
            championId = key.championId,
            onBack = { backStack.removeLastOrNull() },
          )
        }
      },
  )
}
