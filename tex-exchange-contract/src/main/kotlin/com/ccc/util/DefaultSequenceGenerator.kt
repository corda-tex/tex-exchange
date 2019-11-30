package com.ccc.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class DefaultSequenceGenerator : SequenceGenerator<String> {
    private val atomicIntegerMap: MutableMap<String, AtomicInteger> =  ConcurrentHashMap()

    fun addSequence(vararg sequenceNames: String) {
        mutableListOf(*sequenceNames)
            .forEach { sequenceName -> atomicIntegerMap[sequenceName] = AtomicInteger() }
    }

    override fun getNextSequence(sequenceName: String): String {
        val andIncrement = atomicIntegerMap[sequenceName]!!.getAndIncrement()
        return String.format("%s%d", sequenceName, andIncrement)
    }
}