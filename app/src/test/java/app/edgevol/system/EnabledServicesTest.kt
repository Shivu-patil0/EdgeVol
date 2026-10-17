package app.edgevol.system

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnabledServicesTest {

    private val pkg = "app.edgevol"
    private val cls = "app.edgevol.service.VolumeAccessibilityService"
    private val full = "app.edgevol/app.edgevol.service.VolumeAccessibilityService"
    private val short = "app.edgevol/.service.VolumeAccessibilityService"

    private fun enabled(setting: String?) = isServiceEnabled(setting, pkg, cls)

    @Test
    fun nullSetting_isNotEnabled() {
        assertFalse(enabled(null))
    }

    @Test
    fun emptySetting_isNotEnabled() {
        assertFalse(enabled(""))
    }

    @Test
    fun blankSetting_isNotEnabled() {
        assertFalse(enabled("   "))
    }

    @Test
    fun fullForm_isEnabled() {
        assertTrue(enabled(full))
    }

    @Test
    fun shortForm_isEnabled() {
        assertTrue(enabled(short))
    }

    @Test
    fun serviceInMiddleOfMultiEntryList_isEnabled() {
        assertTrue(enabled("com.a/com.a.A:$full:com.b/.B"))
    }

    @Test
    fun shortFormInMiddleOfMultiEntryList_isEnabled() {
        assertTrue(enabled("com.a/com.a.A:$short:com.b/.B"))
    }

    @Test
    fun surroundingWhitespace_isEnabled() {
        assertTrue(enabled("  $full  "))
        assertTrue(enabled("com.a/com.a.A: $short :com.b/.B"))
    }

    @Test
    fun trailingColon_isEnabled() {
        assertTrue(enabled("$full:"))
    }

    @Test
    fun leadingColon_isEnabled() {
        assertTrue(enabled(":$full"))
    }

    @Test
    fun malformedEntryWithoutSlashAlongsideValidOne_stillMatches() {
        assertTrue(enabled("garbage:$full"))
        assertTrue(enabled("$full:garbage"))
    }

    @Test
    fun malformedEntryWithoutSlashAlone_isNotEnabled() {
        assertFalse(enabled("garbage"))
    }

    @Test
    fun differentPackageSameClassName_isNotEnabled() {
        assertFalse(enabled("com.other/app.edgevol.service.VolumeAccessibilityService"))
    }

    @Test
    fun prefixLookalikePackage_isNotEnabled() {
        assertFalse(enabled("app.edgevol.other/app.edgevol.service.VolumeAccessibilityService"))
    }

    @Test
    fun samePackageDifferentClass_isNotEnabled() {
        assertFalse(enabled("app.edgevol/app.edgevol.service.OtherService"))
    }

    @Test
    fun classPrefixLookalike_isNotEnabled() {
        assertFalse(enabled("app.edgevol/app.edgevol.service.VolumeAccessibilityServiceX"))
    }

    @Test
    fun shortFormClassPrefixLookalike_isNotEnabled() {
        assertFalse(enabled("app.edgevol/.service.VolumeAccessibilityServiceX"))
    }

    @Test
    fun emptyClassAfterSlash_isNotEnabled() {
        assertFalse(enabled("app.edgevol/"))
    }

    @Test
    fun emptyPackageBeforeSlash_isNotEnabled() {
        assertFalse(enabled("/app.edgevol.service.VolumeAccessibilityService"))
    }

    @Test
    fun multipleSlashes_isNotEnabled() {
        assertFalse(enabled("a/b/c"))
    }

    @Test
    fun multipleSlashesWithTargetClassSuffix_isNotEnabled() {
        assertFalse(enabled("app.edgevol/x/app.edgevol.service.VolumeAccessibilityService"))
    }

    @Test
    fun dotlessClassName_isTreatedLiterally_isNotEnabled() {
        assertFalse(enabled("app.edgevol/service.VolumeAccessibilityService"))
    }

    @Test
    fun caseDifferenceInPackage_isNotEnabled() {
        assertFalse(enabled("App.Edgevol/app.edgevol.service.VolumeAccessibilityService"))
    }

    @Test
    fun caseDifferenceInClass_isNotEnabled() {
        assertFalse(enabled("app.edgevol/app.edgevol.service.volumeaccessibilityservice"))
        assertFalse(enabled("app.edgevol/.service.volumeaccessibilityservice"))
    }
}
