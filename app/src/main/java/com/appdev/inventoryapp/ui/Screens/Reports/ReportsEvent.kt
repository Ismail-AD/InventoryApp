package com.appdev.inventoryapp.ui.Screens.Reports

import java.time.LocalDate

sealed class ReportEvent {
    data object LoadReports : ReportEvent()
    data object DismissError : ReportEvent()
    data object ShowDateRangePicker : ReportEvent()
    data object HideDateRangePicker : ReportEvent()
    data class UpdateDateRange(val startDate: LocalDate, val endDate: LocalDate) : ReportEvent()
    data object ToggleCategoryFilterMenu : ReportEvent()
    data class FilterByCategory(val category: String?) : ReportEvent()
    data object ToggleSalespersonFilterMenu : ReportEvent()
    data class FilterBySalesperson(val salesperson: String?) : ReportEvent()
    data object ResetFilters : ReportEvent()

    data object ShowStartDatePicker : ReportEvent()
    data object HideStartDatePicker : ReportEvent()
    data object ShowEndDatePicker : ReportEvent()
    data object HideEndDatePicker : ReportEvent()
    data class TempStartDateSelected(val date: LocalDate) : ReportEvent()
    data class TempEndDateSelected(val date: LocalDate) : ReportEvent()
    data object CancelCustomDateRange : ReportEvent()
}