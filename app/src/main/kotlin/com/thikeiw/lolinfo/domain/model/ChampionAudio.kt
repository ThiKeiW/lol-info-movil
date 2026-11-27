package com.thikeiw.lolinfo.domain.model

data class ChampionAudioCatalog(
  val championId: String,
  val championName: String,
  val pageTitle: String?,
  val hasPage: Boolean,
  val groups: Map<Int, SkinAudioGroup>,
) {
  val baseGroup: SkinAudioGroup? get() = groups[0]

  fun groupFor(skinNum: Int): SkinAudioGroup? = groups[skinNum] ?: baseGroup

  val hasAnyAudio: Boolean
    get() =
      groups.values.any { group ->
        group.forms.any { form -> form.sections.any { section -> section.lines.isNotEmpty() } }
      }
}

data class SkinAudioGroup(
  val skinNum: Int,
  val skinName: String,
  val forms: List<AudioForm>,
)

data class AudioForm(
  val id: String,
  val label: String?,
  val sections: List<AudioSection>,
)

data class AudioSection(
  val title: String,
  val lines: List<AudioLine>,
)

data class AudioLine(
  val id: String,
  val fileName: String,
  val urls: List<String>,
  val transcript: String?,
  val section: String,
  val formLabel: String? = null,
  val isLocalized: Boolean = false,
)
