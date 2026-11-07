package com.thikeiw.lolinfo.ui.champions

import com.thikeiw.lolinfo.data.CatalogRepository
import com.thikeiw.lolinfo.domain.model.Champion
import com.thikeiw.lolinfo.domain.model.Skin
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChampionsViewModelTest {

  private val dispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `exposes every champion when the query is empty`() = runTest(dispatcher) {
    val viewModel = ChampionsViewModel(FakeCatalogRepository())
    backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.totalChampions)
    assertEquals(listOf("Ahri", "Aatrox"), state.champions.map { it.id })
    assertFalse(state.isRefreshing)
  }

  @Test
  fun `filters champions by name ignoring case`() = runTest(dispatcher) {
    val viewModel = ChampionsViewModel(FakeCatalogRepository())
    backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
    advanceUntilIdle()

    viewModel.onQueryChange("ah")
    advanceUntilIdle()

    assertEquals(listOf("Ahri"), viewModel.uiState.value.champions.map { it.id })
    assertEquals(2, viewModel.uiState.value.totalChampions)
  }

  @Test
  fun `shows the error message when the refresh fails`() = runTest(dispatcher) {
    val repository =
      FakeCatalogRepository().apply { refreshResult = Result.failure(IOException("sin red")) }
    val viewModel = ChampionsViewModel(repository)
    backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
    advanceUntilIdle()

    assertEquals("sin red", viewModel.uiState.value.errorMessage)
    assertFalse(viewModel.uiState.value.isRefreshing)
  }

  @Test
  fun `clears the error after a successful refresh`() = runTest(dispatcher) {
    val repository =
      FakeCatalogRepository().apply { refreshResult = Result.failure(IOException("sin red")) }
    val viewModel = ChampionsViewModel(repository)
    backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
    advanceUntilIdle()
    assertEquals("sin red", viewModel.uiState.value.errorMessage)

    repository.refreshResult = Result.success(Unit)
    viewModel.refresh(force = true)
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.errorMessage)
  }
}

private class FakeCatalogRepository : CatalogRepository {
  private val championsFlow = MutableStateFlow(FAKE_CHAMPIONS)
  var refreshResult: Result<Unit> = Result.success(Unit)

  override fun observeChampions(): Flow<List<Champion>> = championsFlow

  override fun observeChampion(id: String): Flow<Champion?> =
    championsFlow.map { champions -> champions.firstOrNull { it.id == id } }

  override fun observeSkins(championId: String): Flow<List<Skin>> = flowOf(emptyList())

  override fun observeCatalogVersion(): Flow<String?> = flowOf("16.21.1")

  override fun observeLocale(): Flow<String> = flowOf("es_mx")

  override suspend fun setLocale(locale: String) {}

  override suspend fun englishSkinName(championId: String, skinNum: Int): String? = null

  override suspend fun refresh(force: Boolean): Result<Unit> = refreshResult

  override suspend fun fetchChampionLore(championId: String): Result<String> = Result.success("")
}

private val FAKE_CHAMPIONS =
  listOf(
    Champion(
      id = "Ahri",
      key = 103,
      name = "Ahri",
      title = "la Zorra de Nueve Colas",
      blurb = "",
      tags = listOf("Maga"),
      partype = "ManÃ¡",
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
