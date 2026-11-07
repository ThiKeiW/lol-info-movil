package com.thikeiw.lolinfo.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "champions")
data class ChampionEntity(
  @PrimaryKey val id: String,
  val key: Int,
  val name: String,
  val title: String,
  val blurb: String,
  val lore: String = "",
  val tags: List<String>,
  val partype: String,
  val attack: Int,
  val defense: Int,
  val magic: Int,
  val difficulty: Int,
  val imageUrl: String,
  val version: String,
)

@Entity(
  tableName = "skins",
  foreignKeys =
    [
      ForeignKey(
        entity = ChampionEntity::class,
        parentColumns = ["id"],
        childColumns = ["championId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("championId")],
)
data class SkinEntity(
  @PrimaryKey val id: Int,
  val championId: String,
  val num: Int,
  val name: String,
  val rarity: String?,
  val isLegacy: Boolean,
  val skinLines: List<String>,
  val splashUrl: String,
  val loadingUrl: String,
  val priceRp: Int? = null,
  val priceMe: Int? = null,
)

class StringListConverter {
  @TypeConverter fun fromList(value: List<String>): String = value.joinToString(SEPARATOR)

  @TypeConverter
  fun toList(value: String): List<String> =
    if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

  private companion object {
    const val SEPARATOR = "\u001F"
  }
}
