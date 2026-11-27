package com.thikeiw.lolinfo.data.audio

import com.thikeiw.lolinfo.domain.model.AudioLine
import com.thikeiw.lolinfo.domain.model.AudioSection
import com.thikeiw.lolinfo.domain.model.ChampionAudioCatalog
import com.thikeiw.lolinfo.domain.model.SkinAudioGroup
import javax.inject.Inject

class LolSoundAudioCatalogBuilder @Inject constructor() {
  fun build(
    championId: String,
    championName: String,
    selectedSkinNum: Int,
    selectedSkinName: String,
    baseLines: List<LolSoundMappedLine>,
    skinForms: List<LolSoundSkinForm> = emptyList(),
  ): ChampionAudioCatalog {
    val groups = linkedMapOf<Int, SkinAudioGroup>()
    val baseGroup = buildGroup(0, championName, listOf(LolSoundSkinForm("base", null, baseLines)))
    if (baseGroup.forms.isNotEmpty()) groups[0] = baseGroup
    if (selectedSkinNum != 0) {
      val skinGroup = buildGroup(selectedSkinNum, selectedSkinName, skinForms)
      if (skinGroup.forms.isNotEmpty()) groups[selectedSkinNum] = skinGroup
    }
    return ChampionAudioCatalog(
      championId = championId,
      championName = championName,
      pageTitle = null,
      hasPage = groups.isNotEmpty(),
      groups = groups,
    )
  }

  private fun buildGroup(skinNum: Int, skinName: String, formsData: List<LolSoundSkinForm>): SkinAudioGroup {
    val forms =
      formsData.mapNotNull { formData ->
        val sections =
          formData.lines
            .groupBy { it.section }
            .entries
            .sortedBy { sectionRank(it.key) }
            .map { (title, sectionLines) ->
              AudioSection(
                title = title,
                lines =
                  sectionLines.map { line ->
                    AudioLine(
                      id = "${skinNum}-${formData.id}-${line.id}",
                      fileName = line.name,
                      urls = listOf(line.url),
                      transcript = line.label.ifBlank { null },
                      section = title,
                      formLabel = formData.label,
                      isLocalized = false,
                    )
                  },
              )
            }
            .filter { it.lines.isNotEmpty() }
        if (sections.isEmpty()) null
        else com.thikeiw.lolinfo.domain.model.AudioForm(id = formData.id, label = formData.label, sections = sections)
      }
    return SkinAudioGroup(skinNum = skinNum, skinName = skinName, forms = forms)
  }

  private fun sectionRank(title: String): Int =
    when (title.lowercase()) {
      "movement" -> 0
      "attack" -> 1
      "abilities" -> 2
      "kills and objectives" -> 3
      "death / respawn" -> 4
      "taunt / joke" -> 5
      "interactions" -> 6
      "other" -> 90
      else -> 50
    }
}

data class LolSoundMappedLine(
  val id: Int,
  val name: String,
  val label: String,
  val section: String,
  val url: String,
)

data class LolSoundSkinForm(
  val id: String,
  val label: String?,
  val lines: List<LolSoundMappedLine>,
)
