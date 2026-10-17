package app.edgevol.system

/**
 * Checks whether [packageName]/[className] appears in the colon-separated
 * `enabled_accessibility_services` setting. Entries may use a fully qualified
 * or a short (".Cls") class name. Matching is exact and case-sensitive.
 */
internal fun isServiceEnabled(enabledServices: String?, packageName: String, className: String): Boolean {
    if (enabledServices.isNullOrBlank()) return false
    return enabledServices.split(':').any { raw ->
        val entry = raw.trim()
        val slash = entry.indexOf('/')
        if (slash <= 0 || slash == entry.length - 1) return@any false
        val pkg = entry.substring(0, slash)
        val cls = entry.substring(slash + 1)
        if (cls.contains('/')) return@any false
        val resolved = if (cls.startsWith(".")) pkg + cls else cls
        pkg == packageName && resolved == className
    }
}
