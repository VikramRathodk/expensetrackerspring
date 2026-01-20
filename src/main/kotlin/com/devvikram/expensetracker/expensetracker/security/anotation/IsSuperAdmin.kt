package com.devvikram.expensetracker.expensetracker.security.anotation

import org.springframework.security.access.prepost.PreAuthorize

/**
 * Annotation to restrict access to SUPER_ADMIN only
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('SUPER_ADMIN')")
annotation class IsSuperAdmin