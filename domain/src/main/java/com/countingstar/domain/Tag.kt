package com.countingstar.domain

data class Tag(
    val id: String,
    val ledgerId: String,
    val name: String,
    val color: String? = null,
    val icon: String? = null,
)
