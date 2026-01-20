package com.devvikram.expensetracker.expensetracker.models

import com.devvikram.expensetracker.expensetracker.enums.RoleType
import jakarta.persistence.*
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

