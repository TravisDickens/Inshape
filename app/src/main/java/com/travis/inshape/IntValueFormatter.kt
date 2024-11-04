package com.travis.inshape

import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import java.text.DecimalFormat

class IntValueFormatter : IValueFormatter {
    // Format as an integer
    private val format = DecimalFormat("###,###,##0")

    override fun getFormattedValue(
        value: Float,
        entry: Entry?,
        dataSetIndex: Int,
        viewPortHandler: ViewPortHandler?
    ): String {
        return format.format(value.toInt())
    }
}