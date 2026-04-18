package com.typeassist.app.utils

import java.io.File

object CpuChecker {
    private val features: String by lazy {
        try {
            File("/proc/cpuinfo").readText()
        } catch (e: Exception) {
            ""
        }
    }

    fun hasDotProd(): Boolean = features.contains("dotprod", ignoreCase = true)
    fun hasSve(): Boolean = features.contains("sve", ignoreCase = true)

    fun getBestLibraryName(): String {
        return when {
            hasSve() -> "typeassist_v8_sve"
            hasDotProd() -> "typeassist_v8_dotprod"
            else -> "typeassist_v8"
        }
    }
}
