package me.bataillec.meatmelater.ui.theme

import androidx.compose.ui.graphics.Color
import me.bataillec.meatmelater.domain.AccentColor

/** R27: the accent-dependent roles of a colour scheme (tertiary and error stay semantic, see Theme.kt). */
internal class AccentRoles(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
)

/** Hand-checked light/dark palettes per accent (AA verified by ThemeRulesTest). OLED derives from dark. */
internal class AccentPalette(
    val light: AccentRoles,
    val dark: AccentRoles,
    /** Darker containers used on the OLED background. */
    val oledPrimaryContainer: Color,
    val oledSecondaryContainer: Color,
)

internal fun paletteOf(accent: AccentColor): AccentPalette =
    when (accent) {
        AccentColor.VIANDE_SECHEE -> VIANDE_SECHEE
        AccentColor.VERT -> VERT
        AccentColor.BLEU -> BLEU
        AccentColor.VIOLET -> VIOLET
        AccentColor.AMBRE -> AMBRE
        AccentColor.BRUN_GRIS -> BRUN_GRIS
    }

private val VIANDE_SECHEE =
    AccentPalette(
        light =
            AccentRoles(
                primary = Color(0xFF8B2E1F),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDAD2),
                onPrimaryContainer = Color(0xFF3A0A02),
                secondary = Color(0xFF6B4F3A),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFF3DFD0),
                onSecondaryContainer = Color(0xFF2A1708),
                background = Color(0xFFFFF8F5),
                onBackground = Color(0xFF211A18),
                surface = Color(0xFFFFF8F5),
                onSurface = Color(0xFF211A18),
                surfaceVariant = Color(0xFFF5DDD7),
                onSurfaceVariant = Color(0xFF53433F),
                outline = Color(0xFF85736E),
            ),
        dark =
            AccentRoles(
                primary = Color(0xFFFFB4A2),
                onPrimary = Color(0xFF561003),
                primaryContainer = Color(0xFF722719),
                onPrimaryContainer = Color(0xFFFFDAD2),
                secondary = Color(0xFFE0C0A8),
                onSecondary = Color(0xFF3F2A19),
                secondaryContainer = Color(0xFF58402D),
                onSecondaryContainer = Color(0xFFFDDCC4),
                background = Color(0xFF1A1110),
                onBackground = Color(0xFFF1DFDB),
                surface = Color(0xFF1A1110),
                onSurface = Color(0xFFF1DFDB),
                surfaceVariant = Color(0xFF53433F),
                onSurfaceVariant = Color(0xFFD8C2BC),
                outline = Color(0xFFA08C87),
            ),
        oledPrimaryContainer = Color(0xFF3A140C),
        oledSecondaryContainer = Color(0xFF241A12),
    )

private val VERT =
    AccentPalette(
        light =
            AccentRoles(
                primary = Color(0xFF27683D),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD2F9DF),
                onPrimaryContainer = Color(0xFF0E2516),
                secondary = Color(0xFF3F644B),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFD6F0DF),
                onSecondaryContainer = Color(0xFF141F18),
                background = Color(0xFFF5FCF7),
                onBackground = Color(0xFF18201B),
                surface = Color(0xFFF5FCF7),
                onSurface = Color(0xFF18201B),
                surfaceVariant = Color(0xFFDEEDE3),
                onSurfaceVariant = Color(0xFF3F5045),
                outline = Color(0xFF6C7F72),
            ),
        dark =
            AccentRoles(
                primary = Color(0xFFA0EEBA),
                onPrimary = Color(0xFF153721),
                primaryContainer = Color(0xFF235C36),
                onPrimaryContainer = Color(0xFFD2F9DF),
                secondary = Color(0xFFB5D9C1),
                onSecondary = Color(0xFF24382A),
                secondaryContainer = Color(0xFF375742),
                onSecondaryContainer = Color(0xFFDAF1E2),
                background = Color(0xFF0E1812),
                onBackground = Color(0xFFE0EBE4),
                surface = Color(0xFF0E1812),
                onSurface = Color(0xFFE0EBE4),
                surfaceVariant = Color(0xFF38473D),
                onSurfaceVariant = Color(0xFFC2D6C9),
                outline = Color(0xFF91A196),
            ),
        oledPrimaryContainer = Color(0xFF14341E),
        oledSecondaryContainer = Color(0xFF16221A),
    )

private val BLEU =
    AccentPalette(
        light =
            AccentRoles(
                primary = Color(0xFF264C82),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD0E2FB),
                onPrimaryContainer = Color(0xFF0B1728),
                secondary = Color(0xFF3B4E68),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFD4E0F2),
                onSecondaryContainer = Color(0xFF121821),
                background = Color(0xFFF5F8FC),
                onBackground = Color(0xFF181B20),
                surface = Color(0xFFF5F8FC),
                onSurface = Color(0xFF181B20),
                surfaceVariant = Color(0xFFDEE4ED),
                onSurfaceVariant = Color(0xFF3F4650),
                outline = Color(0xFF6C747F),
            ),
        dark =
            AccentRoles(
                primary = Color(0xFF9ABFF4),
                onPrimary = Color(0xFF11233B),
                primaryContainer = Color(0xFF1D3A63),
                onPrimaryContainer = Color(0xFFD0E2FB),
                secondary = Color(0xFFB2C3DC),
                onSecondary = Color(0xFF212C3B),
                secondaryContainer = Color(0xFF34445B),
                onSecondaryContainer = Color(0xFFD8E3F3),
                background = Color(0xFF0E1218),
                onBackground = Color(0xFFE0E5EB),
                surface = Color(0xFF0E1218),
                onSurface = Color(0xFFE0E5EB),
                surfaceVariant = Color(0xFF383E47),
                onSurfaceVariant = Color(0xFFC2CAD6),
                outline = Color(0xFF9198A1),
            ),
        oledPrimaryContainer = Color(0xFF102037),
        oledSecondaryContainer = Color(0xFF141B24),
    )

private val VIOLET =
    AccentPalette(
        light =
            AccentRoles(
                primary = Color(0xFF5A3276),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE8D4F7),
                onPrimaryContainer = Color(0xFF1B0F24),
                secondary = Color(0xFF544162),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFE5D7EF),
                onSecondaryContainer = Color(0xFF1A141F),
                background = Color(0xFFF9F5FC),
                onBackground = Color(0xFF1D1820),
                surface = Color(0xFFF9F5FC),
                onSurface = Color(0xFF1D1820),
                surfaceVariant = Color(0xFFE7DEED),
                onSurfaceVariant = Color(0xFF493F50),
                outline = Color(0xFF776C7F),
            ),
        dark =
            AccentRoles(
                primary = Color(0xFFCDA2EB),
                onPrimary = Color(0xFF291736),
                primaryContainer = Color(0xFF442659),
                onPrimaryContainer = Color(0xFFE8D4F7),
                secondary = Color(0xFFCAB6D8),
                onSecondary = Color(0xFF2F2537),
                secondaryContainer = Color(0xFF4A3956),
                onSecondaryContainer = Color(0xFFE7DAF1),
                background = Color(0xFF140E18),
                onBackground = Color(0xFFE6E0EB),
                surface = Color(0xFF140E18),
                onSurface = Color(0xFFE6E0EB),
                surfaceVariant = Color(0xFF413847),
                onSurfaceVariant = Color(0xFFCEC2D6),
                outline = Color(0xFF9A91A1),
            ),
        oledPrimaryContainer = Color(0xFF261532),
        oledSecondaryContainer = Color(0xFF1D1622),
    )

private val AMBRE =
    AccentPalette(
        light =
            AccentRoles(
                primary = Color(0xFF916D08),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFF1CC),
                onPrimaryContainer = Color(0xFF302403),
                secondary = Color(0xFF76632D),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFF8EDCE),
                onSecondaryContainer = Color(0xFF251F0E),
                background = Color(0xFFFCFAF5),
                onBackground = Color(0xFF201E18),
                surface = Color(0xFFFCFAF5),
                onSurface = Color(0xFF201E18),
                surfaceVariant = Color(0xFFEDE9DE),
                onSurfaceVariant = Color(0xFF504B3F),
                outline = Color(0xFF7F7A6C),
            ),
        dark =
            AccentRoles(
                primary = Color(0xFFFFE18F),
                onPrimary = Color(0xFF493604),
                primaryContainer = Color(0xFF795B06),
                onPrimaryContainer = Color(0xFFFFF1CC),
                secondary = Color(0xFFE6D5A8),
                onSecondary = Color(0xFF433819),
                secondaryContainer = Color(0xFF685627),
                onSecondaryContainer = Color(0xFFF8EED3),
                background = Color(0xFF18150E),
                onBackground = Color(0xFFEBE8E0),
                surface = Color(0xFF18150E),
                onSurface = Color(0xFFEBE8E0),
                surfaceVariant = Color(0xFF474338),
                onSurfaceVariant = Color(0xFFD6D1C2),
                outline = Color(0xFFA19D91),
            ),
        oledPrimaryContainer = Color(0xFF443304),
        oledSecondaryContainer = Color(0xFF29220F),
    )

private val BRUN_GRIS =
    AccentPalette(
        light =
            AccentRoles(
                primary = Color(0xFF54473B),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFF2E5D9),
                onPrimaryContainer = Color(0xFF1E1915),
                secondary = Color(0xFF59514A),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFECE2DA),
                onSecondaryContainer = Color(0xFF1C1917),
                background = Color(0xFFFCF8F5),
                onBackground = Color(0xFF201C18),
                surface = Color(0xFFFCF8F5),
                onSurface = Color(0xFF201C18),
                surfaceVariant = Color(0xFFEDE5DE),
                onSurfaceVariant = Color(0xFF50473F),
                outline = Color(0xFF7F756C),
            ),
        dark =
            AccentRoles(
                primary = Color(0xFFDFC5AF),
                onPrimary = Color(0xFF2D261F),
                primaryContainer = Color(0xFF4B3F34),
                onPrimaryContainer = Color(0xFFF2E5D9),
                secondary = Color(0xFFD2C6BC),
                onSecondary = Color(0xFF322E2A),
                secondaryContainer = Color(0xFF4E4741),
                onSecondaryContainer = Color(0xFFEDE5DE),
                background = Color(0xFF18130E),
                onBackground = Color(0xFFEBE5E0),
                surface = Color(0xFF18130E),
                onSurface = Color(0xFFEBE5E0),
                surfaceVariant = Color(0xFF473F38),
                onSurfaceVariant = Color(0xFFD6CBC2),
                outline = Color(0xFFA19891),
            ),
        oledPrimaryContainer = Color(0xFF2A231D),
        oledSecondaryContainer = Color(0xFF1F1C1A),
    )

/** R27: colour shown on the swatch of [accent] in Réglages. */
fun accentSwatch(accent: AccentColor): Color = paletteOf(accent).light.primary
