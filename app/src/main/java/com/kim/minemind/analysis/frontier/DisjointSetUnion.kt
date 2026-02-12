package com.kim.minemind.analysis.frontier

class DisjointSetUnion(n: Int) {
    private val parent = IntArray(n) { it }
    private val rank = IntArray(n)

    fun find(x: Int): Int {
        var v = x
        while (parent[v] != v) {
            parent[v] = parent[parent[v]]
            v = parent[v]
        }
        return v
    }

    fun union(a: Int, b: Int) {
        var ra = find(a)
        var rb = find(b)
        if (ra == rb) return
        if (rank[ra] < rank[rb]) {
            val t = ra; ra = rb; rb = t
        }
        parent[rb] = ra
        if (rank[ra] == rank[rb]) rank[ra]++
    }
}