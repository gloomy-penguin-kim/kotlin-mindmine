package com.kim.minemind.analysis.caching

data class BoardSignature(
    val rows: Int,
    val cols: Int,
    val visibleHash: Int
)
