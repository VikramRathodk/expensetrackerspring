package com.devvikram.expensetracker.expensetracker.security.anotation

import org.springframework.security.access.prepost.PreAuthorize

/**
 * Annotation to restrict access to ADMIN and SUPER_ADMIN
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
annotation class IsAdmin