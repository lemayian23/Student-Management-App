package com.smisapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(
    tableName = "students",
    indices = [Index(value = ["reg_number"], unique = true)]
)
data class Student(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "reg_number")
    val regNumber: String,

    @ColumnInfo(name = "course")
    val course: String
)