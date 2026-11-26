package id.algaffar.jwstv.ui.settings

import id.algaffar.jwstv.data.media.MediaStorageManager
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.FloatingOverlayItem
import id.algaffar.jwstv.domain.model.MediaDisplaySettings
import id.algaffar.jwstv.domain.model.QurbanGroup
import id.algaffar.jwstv.domain.model.SlideGlobalConfig
import id.algaffar.jwstv.domain.model.SlideItem
import id.algaffar.jwstv.domain.model.SlideTextStyle
import id.algaffar.jwstv.domain.model.SlideshowGroup
import id.algaffar.jwstv.domain.model.SlideshowTargetState
import id.algaffar.jwstv.domain.model.StandbySlideshowSettings
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsEnhancementsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Test
    fun `VirtualKeyboardPolicy auto mode disables virtual keyboard when physical keyboard is present`() {
        val withPcKeyboard = VirtualKeyboardPolicy(
            mode = VirtualKeyboardPolicy.MODE_AUTO,
            hasPhysicalKeyboard = true,
            detectedKeyboardNames = listOf("USB QWERTY Keyboard")
        )
        assertFalse(withPcKeyboard.allowVirtualKeyboard)

        val remoteOrTouchOnly = VirtualKeyboardPolicy(
            mode = VirtualKeyboardPolicy.MODE_AUTO,
            hasPhysicalKeyboard = false
        )
        assertTrue(remoteOrTouchOnly.allowVirtualKeyboard)
    }

    @Test
    fun `VirtualKeyboardPolicy manual modes override physical keyboard detection`() {
        val forcedEnabled = VirtualKeyboardPolicy(
            mode = VirtualKeyboardPolicy.MODE_ENABLED,
            hasPhysicalKeyboard = true
        )
        assertTrue(forcedEnabled.allowVirtualKeyboard)

        val forcedDisabled = VirtualKeyboardPolicy(
            mode = VirtualKeyboardPolicy.MODE_DISABLED,
            hasPhysicalKeyboard = false
        )
        assertFalse(forcedDisabled.allowVirtualKeyboard)

        val unknownFallbackToAuto = VirtualKeyboardPolicy(
            mode = "unexpected_value",
            hasPhysicalKeyboard = true
        )
        assertEquals(VirtualKeyboardPolicy.MODE_AUTO, unknownFallbackToAuto.normalizedMode)
        assertFalse(unknownFallbackToAuto.allowVirtualKeyboard)
    }

    @Test
    fun `MediaStorageManager protects all Khutbah, Ceramah, Sholat, and Tartil-Tarhim media fields from orphan cleanup`() {
        val settings = AppStateSettings(
            media = MediaDisplaySettings(
                tartilVideo = "jws-assets/videos/tartil_recitation.mp4",
                tarhimVideo = "jws-assets/videos/tarhim_recitation.mp4",
                tartilImages = listOf("jws-assets/images/tartil/01.jpg"),
                tarhimImages = listOf("jws-assets/images/tarhim/01.jpg"),
                doaAdzanImage = "jws-assets/images/media/doa_adzan.jpg",
                khutbahJumatImage = "jws-assets/images/media/khutbah_jumat.jpg",
                khutbahIdulFitriImage = "jws-assets/images/media/khutbah_idul_fitri.jpg",
                khutbahIdulAdhaImage = "jws-assets/images/media/khutbah_idul_adha.jpg",
                ceramahTarawihImage = "jws-assets/images/media/ceramah_tarawih.jpg",
                sholat5WaktuImage = "jws-assets/images/media/sholat_5waktu.jpg",
                sholatJumatImage = "jws-assets/images/media/sholat_jumat.jpg",
                sholatIdulFitriImage = "jws-assets/images/media/sholat_idul_fitri.jpg",
                sholatIdulAdhaImage = "jws-assets/images/media/sholat_idul_adha.jpg",
                sholatTarawihWitirImage = "jws-assets/images/media/sholat_tarawih.jpg"
            )
        )

        val refs = MediaStorageManager.collectReferencedMediaPaths(settings)
        assertTrue("videos/tartil_recitation.mp4" in refs)
        assertTrue("videos/tarhim_recitation.mp4" in refs)
        assertTrue("images/tartil/01.jpg" in refs)
        assertTrue("images/tarhim/01.jpg" in refs)
        assertTrue("images/media/doa_adzan.jpg" in refs)
        assertTrue("images/media/khutbah_jumat.jpg" in refs)
        assertTrue("images/media/khutbah_idul_fitri.jpg" in refs)
        assertTrue("images/media/khutbah_idul_adha.jpg" in refs)
        assertTrue("images/media/ceramah_tarawih.jpg" in refs)
        assertTrue("images/media/sholat_5waktu.jpg" in refs)
        assertTrue("images/media/sholat_jumat.jpg" in refs)
        assertTrue("images/media/sholat_idul_fitri.jpg" in refs)
        assertTrue("images/media/sholat_idul_adha.jpg" in refs)
        assertTrue("images/media/sholat_tarawih.jpg" in refs)
    }

    @Test
    fun `AppStateSettings round-trips new SlideGlobalConfig, SlideTextStyle, virtualKeyboardMode and keeps legacy JSON compatibility`() {
        val original = AppStateSettings(
            virtualKeyboardMode = VirtualKeyboardPolicy.MODE_DISABLED,
            slideConfig = SlideGlobalConfig(
                autoPlay = false,
                defaultDuration = 20,
                maxVideoResolution = "720p"
            ),
            slides = listOf(
                SlideItem(
                    id = "s-text-1",
                    title = "Kajian Rutin",
                    content = "Ba'da Maghrib",
                    type = "text",
                    textStyle = SlideTextStyle(
                        bold = true,
                        italic = true,
                        fontSize = "5xl",
                        fontColor = "#FDE047",
                        textAlign = "left"
                    )
                ),
                SlideItem(
                    id = "s-sys-1",
                    title = "Petugas Jum'at",
                    type = "jumat_petugas",
                    showInJumat = true
                )
            )
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppStateSettings>(encoded)
        assertEquals(VirtualKeyboardPolicy.MODE_DISABLED, decoded.virtualKeyboardMode)
        assertFalse(decoded.slideConfig.autoPlay)
        assertEquals(20, decoded.slideConfig.defaultDuration)
        assertEquals("720p", decoded.slideConfig.maxVideoResolution)
        assertEquals(2, decoded.slides.size)
        assertTrue(decoded.slides[0].textStyle.bold)
        assertEquals("#FDE047", decoded.slides[0].textStyle.fontColor)
        assertEquals("jumat_petugas", decoded.slides[1].type)

        // Legacy minimal JSON without new fields must still parse cleanly with safe defaults
        val legacyParsed = json.decodeFromString<AppStateSettings>("""{"schemaVersion":3}""")
        assertEquals(VirtualKeyboardPolicy.MODE_AUTO, legacyParsed.virtualKeyboardMode)
        assertTrue(legacyParsed.slideConfig.autoPlay)
        assertEquals("1080p", legacyParsed.slideConfig.maxVideoResolution)
    }

    @Test
    fun `SlideshowGroup computes total duration, equal static overlay durations, opposite QR corner, default names, and multi-state filters`() {
        // 1. Default naming sequence: Group 1, Group 2, Group 3
        val g1 = SlideshowGroup(id = "g1", name = SlideshowGroup.nextDefaultGroupName(emptyList()))
        assertEquals("Group 1", g1.name)
        val g2 = SlideshowGroup(id = "g2", name = SlideshowGroup.nextDefaultGroupName(listOf(g1)))
        assertEquals("Group 2", g2.name)
        val g3 = SlideshowGroup(id = "g3", name = SlideshowGroup.nextDefaultGroupName(listOf(g1, g2)))
        assertEquals("Group 3", g3.name)

        // 2. Opposite left/right QR Card corner mapping
        assertEquals(SlideshowGroup.POS_TOP_RIGHT, SlideshowGroup.oppositeHorizontalPosition(SlideshowGroup.POS_TOP_LEFT))
        assertEquals(SlideshowGroup.POS_TOP_LEFT, SlideshowGroup.oppositeHorizontalPosition(SlideshowGroup.POS_TOP_RIGHT))
        assertEquals(SlideshowGroup.POS_BOTTOM_RIGHT, SlideshowGroup.oppositeHorizontalPosition(SlideshowGroup.POS_BOTTOM_LEFT))
        assertEquals(SlideshowGroup.POS_BOTTOM_LEFT, SlideshowGroup.oppositeHorizontalPosition(SlideshowGroup.POS_BOTTOM_RIGHT))

        // 3. Total slide duration & equal Floating Overlay duration division
        val overlays = listOf(
            FloatingOverlayItem(id = "ov1", title = "Adab Masjid", message = "Luruskan shaf"),
            FloatingOverlayItem(id = "ov2", title = "Kajian Rutin", message = "Ba'da Maghrib"),
            FloatingOverlayItem(id = "ov3", title = "Infaq Digital", message = "Scan QRIS")
        )
        val group = SlideshowGroup(
            id = "grp-kajian",
            name = "Group 1",
            enabled = true,
            slides = listOf(
                SlideItem(id = "s1", type = "image", imageUrl = "jws-assets/images/slides/poster1.jpg", duration = 10, enabled = true),
                SlideItem(id = "s2", type = "video", videoUrl = "jws-assets/videos/profil.mp4", duration = 20, enabled = true),
                SlideItem(id = "s3", type = "text", content = "Nonaktif", duration = 15, enabled = false)
            ),
            overlayIds = listOf("ov1", "ov2", "ov3"),
            overlayPosition = SlideshowGroup.POS_TOP_LEFT,
            showQrCard = true,
            showInMain = true,
            showInSlideshow = true,
            showInJumat = true,
            showInRamadhan = false,
            showInIdulFitri = true,
            showInIdulAdha = false
        )

        assertEquals(30, group.totalDurationSeconds)
        assertEquals(SlideshowGroup.POS_TOP_RIGHT, group.qrCardPosition)
        val resolvedOverlays = group.resolveActiveOverlays(overlays)
        assertEquals(3, resolvedOverlays.size)
        assertEquals(10_000L, group.overlayDurationMillis(resolvedOverlays.size))

        // 4. Multi-state filtering & MediaStorageManager protection
        val settings = AppStateSettings(
            floatingOverlays = overlays,
            slideGroups = listOf(group),
            standbySlideshow = StandbySlideshowSettings(enabled = true, durationMain = 60, repeatCount = 1)
        )
        assertEquals(1, settings.resolveGroupsForState(SlideshowTargetState.MAIN).size)
        assertEquals(1, settings.resolveGroupsForState(SlideshowTargetState.FULLSCREEN).size)
        assertEquals(1, settings.resolveGroupsForState(SlideshowTargetState.JUMAT).size)
        assertEquals(0, settings.resolveGroupsForState(SlideshowTargetState.RAMADHAN).size)
        assertEquals(1, settings.resolveGroupsForState(SlideshowTargetState.IDUL_FITRI).size)
        assertEquals(0, settings.resolveGroupsForState(SlideshowTargetState.IDUL_ADHA).size)

        val refs = MediaStorageManager.collectReferencedMediaPaths(settings)
        assertTrue("images/slides/poster1.jpg" in refs)
        assertTrue("videos/profil.mp4" in refs)

        // 5. JwsStateMachine STANDBY_SLIDESHOW cycle uses group total duration (60s standby + 30s slideshow = 90s cycle)
        val sm = JwsStateMachine()
        val zone = ZoneId.of("Asia/Makassar")
        // 09:00:30 -> 32430 % 90 = 30 (< 60 -> STANDBY)
        val tickMain = sm.evaluateTick(ZonedDateTime.of(2026, 3, 10, 9, 0, 30, 0, zone), settings)
        assertEquals(AppState.STANDBY, tickMain.state)
        // 09:01:10 -> 32470 % 90 = 70 (>= 60 -> STANDBY_SLIDESHOW with 20s remaining)
        val tickFs = sm.evaluateTick(ZonedDateTime.of(2026, 3, 10, 9, 1, 10, 0, zone), settings)
        assertEquals(AppState.STANDBY_SLIDESHOW, tickFs.state)
        assertEquals(30L, tickFs.totalStageSeconds)
        assertEquals(20L, tickFs.countdownSeconds)
    }

    @Test
    fun `QurbanGroup supports full tab CRUD, enforces max 7 members per group, and preserves JSON compatibility`() {
        // 1. Add group tabs with auto-incrementing default names
        val (after1, g1) = QurbanGroup.addGroup(emptyList(), id = "q1")
        assertEquals("Kelompok 1", g1.groupName)
        assertEquals(QurbanGroup.ANIMAL_SAPI, g1.animalType)
        assertTrue(g1.members.isEmpty())

        val (after2, g2) = QurbanGroup.addGroup(after1, animalType = "Kambing", id = "q2")
        assertEquals("Kelompok 2", g2.groupName)
        assertEquals(QurbanGroup.ANIMAL_KAMBING, g2.animalType)

        // Deleting Kelompok 1 then adding again reuses lowest unused number ("Kelompok 1")
        val withoutFirst = QurbanGroup.deleteGroup(after2, "q1")
        val (readdedList, gReadded) = QurbanGroup.addGroup(withoutFirst, id = "q3")
        assertEquals("Kelompok 1", gReadded.groupName)
        assertEquals(listOf("q2", "q3"), readdedList.map { it.id })

        // Move tab left/right
        val reorderedTabs = QurbanGroup.moveGroup(readdedList, "q3", -1)
        assertEquals(listOf("q3", "q2"), reorderedTabs.map { it.id })

        // 2. Add members up to MAX_MEMBERS (7) and verify 8th member is rejected
        var groups = reorderedTabs
        for (i in 1..7) {
            groups = QurbanGroup.addMember(groups, "q3", "  Peserta $i  ")
        }
        val fullGroup = groups.first { it.id == "q3" }
        assertEquals(7, fullGroup.members.size)
        assertTrue(fullGroup.isFull)
        assertEquals("Peserta 1", fullGroup.members.first())
        assertEquals("Peserta 7", fullGroup.members.last())

        // Attempt to add 8th member or blank member -> ignored
        val after8th = QurbanGroup.addMember(groups, "q3", "Peserta 8")
        assertEquals(7, after8th.first { it.id == "q3" }.members.size)
        val afterBlank = QurbanGroup.addMember(groups, "q2", "   ")
        assertTrue(afterBlank.first { it.id == "q2" }.members.isEmpty())

        // 3. Update, reorder, and delete members inside a group
        groups = QurbanGroup.updateMember(groups, "q3", 0, "H. Ahmad, S.Pd")
        assertEquals("H. Ahmad, S.Pd", groups.first { it.id == "q3" }.members[0])

        groups = QurbanGroup.moveMember(groups, "q3", 0, 1)
        assertEquals("Peserta 2", groups.first { it.id == "q3" }.members[0])
        assertEquals("H. Ahmad, S.Pd", groups.first { it.id == "q3" }.members[1])

        groups = QurbanGroup.deleteMember(groups, "q3", 1)
        assertEquals(6, groups.first { it.id == "q3" }.members.size)
        assertFalse(groups.first { it.id == "q3" }.isFull)

        // Now that a slot freed up, adding a 7th member succeeds again
        groups = QurbanGroup.addMember(groups, "q3", "Peserta Pengganti")
        assertEquals(7, groups.first { it.id == "q3" }.members.size)
        assertEquals("Peserta Pengganti", groups.first { it.id == "q3" }.members.last())

        // 4. Update group metadata & sanitize oversized legacy members
        groups = QurbanGroup.updateGroup(
            groups = groups,
            groupId = "q2",
            groupName = "Sapi RT 02",
            animalType = "sapi",
            members = (1..10).map { "Warga $it" }
        )
        val updatedQ2 = groups.first { it.id == "q2" }
        assertEquals("Sapi RT 02", updatedQ2.groupName)
        assertEquals(QurbanGroup.ANIMAL_SAPI, updatedQ2.animalType)
        assertEquals(7, updatedQ2.members.size)

        // 5. activeMembers filters blank entries and caps legacy >7 lists at 7
        val legacyOverflow = QurbanGroup(
            id = "legacy",
            animalType = "Sapi",
            groupName = "Legacy",
            members = listOf("A", " ", "B", "C", "D", "E", "F", "G", "H", "I")
        )
        assertEquals(listOf("A", "B", "C", "D", "E", "F", "G"), legacyOverflow.activeMembers)

        // 6. JSON round-trip preserves exact fields
        val encoded = json.encodeToString(updatedQ2)
        val decoded = json.decodeFromString<QurbanGroup>(encoded)
        assertEquals(updatedQ2, decoded)
    }
}
