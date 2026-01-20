package com.countingstar.domain

data class Merchant(
    val id: String,
    val ledgerId: String,
    val name: String,
    val alias: String? = null,
)
