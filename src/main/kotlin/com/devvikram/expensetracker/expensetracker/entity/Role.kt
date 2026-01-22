package com.devvikram.expensetracker.expensetracker.entity

import com.devvikram.expensetracker.expensetracker.enums.RoleType
import com.devvikram.expensetracker.expensetracker.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "roles")
class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    @Enumerated(EnumType.STRING)
    val name: RoleType,

    @Column(length = 500)
    val description: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToMany(mappedBy = "roles",fetch = FetchType.LAZY)
    val users: MutableSet<User> = mutableSetOf()
)