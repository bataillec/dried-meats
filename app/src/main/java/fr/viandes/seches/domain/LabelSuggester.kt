package fr.viandes.seches.domain

object LabelSuggester {
    /** R6: "Boeuf 1", "Boeuf 2"... first number not already used by an existing label. */
    fun suggest(
        meatTypeName: String,
        existingLabels: Collection<String>,
    ): String {
        val used = existingLabels.map { it.trim().lowercase() }.toSet()
        var n = 1
        while ("${meatTypeName.trim()} $n".lowercase() in used) n++
        return "${meatTypeName.trim()} $n"
    }
}
