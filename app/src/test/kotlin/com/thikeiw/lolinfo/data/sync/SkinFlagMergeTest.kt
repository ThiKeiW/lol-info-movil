package com.thikeiw.lolinfo.data.sync

import com.thikeiw.lolinfo.data.local.SkinFlagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinFlagMergeTest {

  private val uid = "user-1"
  private val deviceId = "device-local"

  private fun local(
    skinId: Int,
    favorite: Boolean = false,
    owned: Boolean = false,
    revision: Long = 0,
    updatedBy: String = "device-a",
    pending: Boolean = false,
  ) = SkinFlagEntity(
    uid = uid,
    skinId = skinId,
    isFavorite = favorite,
    isOwned = owned,
    revision = revision,
    updatedBy = updatedBy,
    pendingSync = pending,
  )

  private fun remote(
    skinId: Int,
    favorite: Boolean = false,
    owned: Boolean = false,
    revision: Long = 0,
    updatedBy: String = "device-a",
  ) = RemoteSkinFlag(
    skinId = skinId,
    isFavorite = favorite,
    isOwned = owned,
    revision = revision,
    updatedBy = updatedBy,
  )

  @Test
  fun `remote only is inserted without pushing`() {
    val plan = mergeSkinFlags(uid, emptyList(), listOf(remote(1, favorite = true, revision = 3)), deviceId)

    assertEquals(1, plan.toUpsert.size)
    assertEquals(true, plan.toUpsert.single().isFavorite)
    assertEquals(3L, plan.toUpsert.single().revision)
    assertEquals(false, plan.toUpsert.single().pendingSync)
    assertTrue(plan.toPush.isEmpty())
  }

  @Test
  fun `local only is kept and pushed`() {
    val plan = mergeSkinFlags(uid, listOf(local(2, owned = true, revision = 1)), emptyList(), deviceId)

    assertEquals(1, plan.toPush.size)
    assertEquals(2, plan.toPush.single().skinId)
    assertTrue(plan.toPush.single().pendingSync)
  }

  @Test
  fun `remote with higher revision wins`() {
    val plan =
      mergeSkinFlags(
        uid,
        listOf(local(3, favorite = false, revision = 1, updatedBy = "device-a")),
        listOf(remote(3, favorite = true, revision = 2, updatedBy = "device-b")),
        deviceId,
      )

    assertEquals(true, plan.toUpsert.single().isFavorite)
    assertTrue(plan.toPush.isEmpty())
    assertEquals(false, plan.toUpsert.single().pendingSync)
  }

  @Test
  fun `local with higher revision wins and is pushed`() {
    val plan =
      mergeSkinFlags(
        uid,
        listOf(local(4, owned = true, revision = 5, updatedBy = "device-a")),
        listOf(remote(4, owned = false, revision = 2, updatedBy = "device-b")),
        deviceId,
      )

    assertEquals(1, plan.toPush.size)
    assertEquals(true, plan.toPush.single().isOwned)
    assertEquals(deviceId, plan.toPush.single().updatedBy)
  }

  @Test
  fun `tie on revision is broken by greater updatedBy`() {
    val remoteWins =
      mergeSkinFlags(
        uid,
        listOf(local(5, favorite = false, revision = 4, updatedBy = "device-a")),
        listOf(remote(5, favorite = true, revision = 4, updatedBy = "device-z")),
        deviceId,
      )
    assertEquals(true, remoteWins.toUpsert.single().isFavorite)
    assertTrue(remoteWins.toPush.isEmpty())

    val localWins =
      mergeSkinFlags(
        uid,
        listOf(local(5, favorite = true, revision = 4, updatedBy = "device-z")),
        listOf(remote(5, favorite = false, revision = 4, updatedBy = "device-a")),
        deviceId,
      )
    assertEquals(true, localWins.toPush.single().isFavorite)
  }

  @Test
  fun `identical flags are marked synced without pushing`() {
    val plan =
      mergeSkinFlags(
        uid,
        listOf(local(6, favorite = true, owned = true, revision = 7, updatedBy = "device-a", pending = true)),
        listOf(remote(6, favorite = true, owned = true, revision = 7, updatedBy = "device-a")),
        deviceId,
      )

    assertTrue(plan.toPush.isEmpty())
    assertEquals(false, plan.toUpsert.single().pendingSync)
  }

  @Test
  fun `same revision and device with different values bumps revision and pushes`() {
    val plan =
      mergeSkinFlags(
        uid,
        listOf(local(7, favorite = true, revision = 3, updatedBy = "device-a")),
        listOf(remote(7, favorite = false, revision = 3, updatedBy = "device-a")),
        deviceId,
      )

    assertEquals(4L, plan.toPush.single().revision)
    assertEquals(true, plan.toPush.single().isFavorite)
    assertEquals(deviceId, plan.toPush.single().updatedBy)
  }
}
