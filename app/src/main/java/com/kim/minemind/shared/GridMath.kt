package com.kim.minemind.shared

object GridMath {

    fun neighborsOf(id: Int, rows: Int, cols: Int): IntArray {
        val r = id / cols
        val c = id % cols

        val tmp = IntArray(8)
        var n = 0

        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols) {
                tmp[n++] = nr * cols + nc
            }
        }
        return tmp.copyOf(n)
    }
}
