package com.sch.sekolah_mobile_app.ui.screens.ujian

/**
 * Utility to format mathematical strings, formulas, and scientific notations
 * for mobile Compose Text displays (e.g. converting LaTeX \frac{a}{b} to clean a/b).
 */
fun formatMathForMobile(input: String?): String {
    if (input.isNullOrBlank()) return ""
    var s = input
    // LaTeX fractions: \frac{a}{b} -> a/b
    s = s.replace(Regex("""\\frac\{([^}]+)\}\{([^}]+)\}""")) { match ->
        val num = match.groupValues[1].trim()
        val den = match.groupValues[2].trim()
        if (num.contains(" ") || den.contains(" ") || num.contains("+") || num.contains("-")) "($num)/($den)" else "$num/$den"
    }
    // \sqrt[n]{x} -> n√(x)
    s = s.replace(Regex("""\\sqrt\[([^\]]+)\]\{([^}]+)\}""")) { match ->
        "${match.groupValues[1]}√(${match.groupValues[2]})"
    }
    // \sqrt{x} -> √(x)
    s = s.replace(Regex("""\\sqrt\{([^}]+)\}""")) { match ->
        "√(${match.groupValues[1]})"
    }
    // ^{exp} -> ^exp
    s = s.replace(Regex("""\^\{([^}]+)\}""")) { match ->
        "^${match.groupValues[1]}"
    }
    // _{sub} -> _sub
    s = s.replace(Regex("""_\{([^}]+)\}""")) { match ->
        "_${match.groupValues[1]}"
    }
    return s
}
