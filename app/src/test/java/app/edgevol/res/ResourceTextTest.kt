package app.edgevol.res

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

/** Reads the module's source `src/main` files directly (no Android runtime needed). */
class ResourceTextTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"

    private fun mainDir(): File {
        var dir: File? = File(System.getProperty("user.dir").orEmpty()).absoluteFile
        while (dir != null) {
            for (candidate in listOf(File(dir, "src/main"), File(dir, "app/src/main"))) {
                if (File(candidate, "AndroidManifest.xml").isFile) return candidate
            }
            dir = dir.parentFile
        }
        throw AssertionError("src/main not found walking up from ${System.getProperty("user.dir")}")
    }

    private fun parse(relative: String): Document {
        val file = File(mainDir(), relative)
        assertTrue("missing $file", file.isFile)
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newDocumentBuilder().parse(file)
    }

    private fun unescape(raw: String): String = raw.replace("\\'", "'").replace("\\\"", "\"").trim()

    private fun stringResource(file: String, name: String): String {
        val strings = parse(file).getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val e = strings.item(i) as Element
            if (e.getAttribute("name") == name) return unescape(e.textContent.orEmpty())
        }
        throw AssertionError("string '$name' not found in $file")
    }

    private fun description() = stringResource("res/values/strings_system.xml", "a11y_service_description")

    private fun disclosure() = stringResource("res/values/strings.xml", "setup_disclosure_body")

    @Test
    fun `disclosure body equals the accessibility service description`() {
        assertEquals(description(), disclosure())
    }

    @Test
    fun `accessibility service description does not mention canRetrieveWindowContent or Quick Settings`() {
        assertFalse(description().contains("canRetrieveWindowContent"))
        assertFalse(description().contains("Quick Settings"))
    }

    @Test
    fun `setup disclosure body does not mention canRetrieveWindowContent or Quick Settings`() {
        assertFalse(disclosure().contains("canRetrieveWindowContent"))
        assertFalse(disclosure().contains("Quick Settings"))
    }

    @Test
    fun `accessibility config declares no accessibilityEventTypes`() {
        val root = parse("res/xml/accessibility_service_config.xml").documentElement
        assertEquals("accessibility-service", root.tagName)
        assertFalse(root.hasAttributeNS(androidNs, "accessibilityEventTypes"))
        assertFalse(root.hasAttribute("android:accessibilityEventTypes"))
    }

    @Test
    fun `accessibility config sets canRetrieveWindowContent to false`() {
        val root = parse("res/xml/accessibility_service_config.xml").documentElement
        assertEquals("false", root.getAttributeNS(androidNs, "canRetrieveWindowContent"))
    }

    @Test
    fun `accessibility config description points at the description string`() {
        val root = parse("res/xml/accessibility_service_config.xml").documentElement
        assertEquals("@string/a11y_service_description", root.getAttributeNS(androidNs, "description"))
    }

    @Test
    fun `source manifest declares no uses-permission elements`() {
        val doc = parse("AndroidManifest.xml")
        assertEquals(0, doc.getElementsByTagName("uses-permission").length)
        assertEquals(0, doc.getElementsByTagName("uses-permission-sdk-23").length)
    }

    @Test
    fun `source manifest has no INTERNET anywhere outside comments`() {
        val doc = parse("AndroidManifest.xml")
        val offenders = mutableListOf<String>()

        fun walk(node: Node) {
            if (node is Element) {
                val attrs = node.attributes
                for (i in 0 until attrs.length) {
                    val a = attrs.item(i)
                    if (a.nodeValue.contains("INTERNET")) offenders += "${node.tagName}@${a.nodeName}=${a.nodeValue}"
                }
            }
            if (node.nodeType == Node.TEXT_NODE && node.nodeValue.contains("INTERNET")) {
                offenders += "text:${node.nodeValue}"
            }
            val children = node.childNodes
            for (i in 0 until children.length) walk(children.item(i))
        }

        walk(doc.documentElement)
        assertTrue("INTERNET found: $offenders", offenders.isEmpty())
    }
}
