package com.thikeiw.lolinfo.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SkinSpotlightTest {

  @Test
  fun `links to channel search with encoded skin name`() {
    assertEquals(
      "https://www.youtube.com/@SkinSpotlights/search?query=Dynasty+Ahri",
      skinSpotlightUrl("Ahri", "Dynasty Ahri"),
    )
  }

  @Test
  fun `keeps champion and skin separate when skin name does not include the champion`() {
    assertEquals(
      "https://www.youtube.com/@SkinSpotlights/search?query=Parche+Skin",
      skinSpotlightUrl("Parche", "Skin"),
    )
  }

  @Test
  fun `uses skin name only for base skins`() {
    assertEquals(
      "https://www.youtube.com/@SkinSpotlights/search?query=Nami",
      skinSpotlightUrl("Nami", "Nami"),
    )
  }
}
