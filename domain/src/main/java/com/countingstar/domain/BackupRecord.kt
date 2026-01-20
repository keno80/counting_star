package com.countingstar.domain

data class BackupRecord(
    val id: String,
    val createdAt: Long,
    val fileName: String,
    val checksum: String? = null,
    val size: Long,
)
