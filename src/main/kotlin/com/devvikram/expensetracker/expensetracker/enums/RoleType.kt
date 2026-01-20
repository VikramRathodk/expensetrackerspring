package com.devvikram.expensetracker.expensetracker.enums
enum class RoleType {
    USER,           // Regular user - can manage own expenses
    ADMIN,          // Admin - can manage global categories
    SUPER_ADMIN,    // Super Admin - full system access
    MODERATOR,      // Moderator - can view all users' data
    ACCOUNTANT,     // Accountant - financial reports access
    VIEWER          // Read-only access
}