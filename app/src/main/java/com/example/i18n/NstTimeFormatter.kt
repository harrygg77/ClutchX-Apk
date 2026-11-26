package com.example.i18n

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object NstTimeFormatter {
    private val nstZone: TimeZone = TimeZone.getTimeZone("Asia/Kathmandu")

    fun formatDateTimeNst(epochMs: Long): String {
        if (epochMs <= 0L) return "TBD NST"
        val formatter = SimpleDateFormat("MMM dd, hh:mm a", Locale.ENGLISH).apply {
            timeZone = nstZone
        }
        return "${formatter.format(Date(epochMs))} NST"
    }

    fun formatShortTimeNst(epochMs: Long): String {
        if (epochMs <= 0L) return "TBD"
        val formatter = SimpleDateFormat("dd MMM • HH:mm", Locale.ENGLISH).apply {
            timeZone = nstZone
        }
        return "${formatter.format(Date(epochMs))} NST"
    }

    fun formatNpr(amount: Long, freeLabel: String = "FREE"): String {
        if (amount <= 0L) return freeLabel
        val formatted = NumberFormat.getNumberInstance(Locale("en", "IN")).format(amount)
        return "रु $formatted"
    }

    fun formatPrizeNpr(amount: Long): String {
        val formatted = NumberFormat.getNumberInstance(Locale("en", "IN")).format(amount)
        return "रु $formatted"
    }
}
