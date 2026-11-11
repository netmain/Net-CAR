package com.netcar.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Primary Brand Colors (Net-CAR Modern Electric Cobalt / Sky)
val PrimaryLight = Color(0xFF0284C7)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE0F2FE)
val OnPrimaryContainerLight = Color(0xFF082F49)

val PrimaryDark = Color(0xFF38BDF8)
val OnPrimaryDark = Color(0xFF082F49)
val PrimaryContainerDark = Color(0xFF0369A1)
val OnPrimaryContainerDark = Color(0xFFE0F2FE)

// Secondary Colors (Indigo / Slate Accent)
val SecondaryLight = Color(0xFF4F46E5)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE0E7FF)
val OnSecondaryContainerLight = Color(0xFF1E1B4B)

val SecondaryDark = Color(0xFF818CF8)
val OnSecondaryDark = Color(0xFF1E1B4B)
val SecondaryContainerDark = Color(0xFF3730A3)
val OnSecondaryContainerDark = Color(0xFFE0E7FF)

// Tertiary Colors (Warm Amber / Telemetry Highlight)
val TertiaryLight = Color(0xFFD97706)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFEF3C7)
val OnTertiaryContainerLight = Color(0xFF451A03)

val TertiaryDark = Color(0xFFFBBF24)
val OnTertiaryDark = Color(0xFF451A03)
val TertiaryContainerDark = Color(0xFF78350F)
val OnTertiaryContainerDark = Color(0xFFFEF3C7)

// Neutral / Background & Surface
val BackgroundLight = Color(0xFFF8FAFC)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val OnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight = Color(0xFF94A3B8)

val BackgroundDark = Color(0xFF0B0F17)
val OnBackgroundDark = Color(0xFFF1F5F9)
val SurfaceDark = Color(0xFF111827)
val OnSurfaceDark = Color(0xFFF1F5F9)
val SurfaceVariantDark = Color(0xFF1E293B)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF334155)

// Error / Critical
val ErrorLight = Color(0xFFE11D48)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFE4E6)
val OnErrorContainerLight = Color(0xFF4C0519)

val ErrorDark = Color(0xFFFB7185)
val OnErrorDark = Color(0xFF4C0519)
val ErrorContainerDark = Color(0xFF881337)
val OnErrorContainerDark = Color(0xFFFFE4E6)

// ---------------------------------------------------------------------------
// CarLog white theme palette (taken from the CarLog mockup design)
// ---------------------------------------------------------------------------
val Paper = Color(0xFFE9EEF0)
val Ink = Color(0xFF12303B)
val InkSoft = Color(0xFFC9D6DB)
val Amber = Color(0xFFF2A900)
val AmberDeep = Color(0xFF7A5200)
val Teal = Color(0xFF0E7C86)
val TealDark = Color(0xFF0A6770)
val Alert = Color(0xFFC8452F)
val Mute = Color(0xFF4F656E)
val Sand = Color(0xFFF6DFA0)

/**
 * Semantic accent colors used across the app (fuel, expense, maintenance,
 * reminders...). They are provided by the theme so the same screens look
 * correct in both the dark and the white (CarLog) theme.
 */
data class NetCarPalette(
    val fuel: Color,
    val fuelContainer: Color,
    val onFuelContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val maintenance: Color,
    val maintenanceContainer: Color,
    val onMaintenanceContainer: Color,
    val overdue: Color,
    val overdueContainer: Color,
    val onOverdueContainer: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color
)

val DarkNetCarPalette = NetCarPalette(
    fuel = Color(0xFF10B981),
    fuelContainer = Color(0xFF064E3B),
    onFuelContainer = Color(0xFFECFDF5),
    expense = Color(0xFFF59E0B),
    expenseContainer = Color(0xFF78350F),
    onExpenseContainer = Color(0xFFFEF3C7),
    maintenance = Color(0xFF38BDF8),
    maintenanceContainer = Color(0xFF0C4A6E),
    onMaintenanceContainer = Color(0xFFE0F2FE),
    overdue = Color(0xFFFB7185),
    overdueContainer = Color(0xFF881337),
    onOverdueContainer = Color(0xFFFFE4E6),
    success = Color(0xFF10B981),
    successContainer = Color(0xFF064E3B),
    onSuccessContainer = Color(0xFFECFDF5),
    warning = Color(0xFFF97316),
    warningContainer = Color(0xFF7C2D12)
)

val LightNetCarPalette = NetCarPalette(
    fuel = Color(0xFFB07800),
    fuelContainer = Sand,
    onFuelContainer = Color(0xFF4A3200),
    expense = Alert,
    expenseContainer = Color(0xFFF7DDD7),
    onExpenseContainer = Color(0xFF7A2417),
    maintenance = TealDark,
    maintenanceContainer = Color(0xFFD5E9EB),
    onMaintenanceContainer = Color(0xFF064C52),
    overdue = Color(0xFFB3261E),
    overdueContainer = Color(0xFFF9DEDC),
    onOverdueContainer = Color(0xFF6E1512),
    success = TealDark,
    successContainer = Color(0xFFD5E9EB),
    onSuccessContainer = Color(0xFF064C52),
    warning = Color(0xFFB07800),
    warningContainer = Sand
)

val LocalNetCarPalette = compositionLocalOf { DarkNetCarPalette }

/** True when the dark theme is currently applied. */
val LocalIsDarkTheme = compositionLocalOf { true }

// Theme aware accessors: the existing screens keep using these names and now
// automatically follow the selected theme.
val FuelGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.fuel

val FuelGreenContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.fuelContainer

val OnFuelGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.onFuelContainer

val MaintenanceBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.maintenance

val MaintenanceBlueContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.maintenanceContainer

val OnMaintenanceBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.onMaintenanceContainer

val ExpenseAmber: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.expense

val ExpenseAmberContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.expenseContainer

val OnExpenseAmber: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.onExpenseContainer

val OverdueRed: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.overdue

val OverdueRedContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.overdueContainer

val OnOverdueRed: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.onOverdueContainer

val WarningOrange: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.warning

val WarningOrangeContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.warningContainer

val SuccessGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.success

val SuccessGreenContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.successContainer

val OnSuccessGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNetCarPalette.current.onSuccessContainer
