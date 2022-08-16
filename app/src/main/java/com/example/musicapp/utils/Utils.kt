package com.example.musicapp.utils

import java.lang.StringBuilder

const val MILLISEC_IN_SEC = 1000
const val SEC_IN_MIN = 60

class Utils {
    companion object {
        fun durationToString(duration: Int): String {
            val durationInSeconds = duration / MILLISEC_IN_SEC
            val builder = StringBuilder()
                .append((durationInSeconds / SEC_IN_MIN).toString().padStart(2, '0'))
                .append(":")
                .append((durationInSeconds % SEC_IN_MIN).toString().padStart(2, '0'))
            return builder.toString()
        }
    }
}
