package com.thikeiw.lolinfo.ui.champions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.thikeiw.lolinfo.domain.model.Champion
import com.thikeiw.lolinfo.ui.theme.LoLSkinScopeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChampionsScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun showsChampionsAndPatchSubtitle() {
    composeRule.setContent {
      LoLSkinScopeTheme {
        ChampionsContent(
          uiState = stateWith(FAKE_CHAMPIONS),
          onQueryChange = {},
          onRefresh = {},
          onChampionClick = {},
        )
      }
    }

    composeRule.onNodeWithText("Campeones").assertIsDisplayed()
    composeRule.onNodeWithText("Parche 16.21.1 · 2 campeones").assertIsDisplayed()
    composeRule.onNodeWithText("Ahri").assertIsDisplayed()
  }

  @Test
  fun forwardsChampionClicks() {
    var clicked: String? = null
    composeRule.setContent {
      LoLSkinScopeTheme {
        ChampionsContent(
          uiState = stateWith(FAKE_CHAMPIONS),
          onQueryChange = {},
          onRefresh = {},
          onChampionClick = { clicked = it },
        )
      }
    }

    composeRule.onNodeWithText("Aatrox").performClick()

    assertEquals("Aatrox", clicked)
  }

  @Test
  fun typesIntoTheSearchField() {
    var query = ""
    composeRule.setContent {
      LoLSkinScopeTheme {
        ChampionsContent(
          uiState = stateWith(FAKE_CHAMPIONS),
          onQueryChange = { query = it },
          onRefresh = {},
          onChampionClick = {},
        )
      }
    }

    composeRule.onNodeWithTag(ChampionsSearchTestTag).performTextInput("ah")

    assertEquals("ah", query)
  }

  @Test
  fun showsEmptyStateWithoutChampions() {
    composeRule.setContent {
      LoLSkinScopeTheme {
        ChampionsContent(
          uiState = ChampionsUiState(version = null),
          onQueryChange = {},
          onRefresh = {},
          onChampionClick = {},
        )
      }
    }

    composeRule.onNodeWithText("No hay campeones todavía. Pulsa actualizar para sincronizar.")
      .assertIsDisplayed()
    composeRule.onNodeWithText("Catálogo sin sincronizar").assertIsDisplayed()
  }

  private fun stateWith(champions: List<Champion>) =
    ChampionsUiState(
      champions = champions,
      totalChampions = champions.size,
      version = "16.21.1",
    )

  private companion object {
    val FAKE_CHAMPIONS =
      listOf(
        Champion(
          id = "Ahri",
          key = 103,
          name = "Ahri",
          title = "la Zorra de Nueve Colas",
          blurb = "",
          tags = listOf("Maga"),
          partype = "Maná",
          attack = 3,
          defense = 4,
          magic = 8,
          difficulty = 5,
          imageUrl = "",
        ),
        Champion(
          id = "Aatrox",
          key = 266,
          name = "Aatrox",
          title = "la Espada Darkin",
          blurb = "",
          tags = listOf("Luchador"),
          partype = "Sangre",
          attack = 8,
          defense = 4,
          magic = 3,
          difficulty = 4,
          imageUrl = "",
        ),
      )
  }
}
