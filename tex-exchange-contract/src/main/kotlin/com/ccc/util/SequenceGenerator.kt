package com.ccc.util


interface SequenceGenerator<T> {
    fun getNextSequence(sequenceName: String): T
}