package com.devvikram.expensetracker.expensetracker.security.anotation

import org.springframework.security.access.prepost.PreAuthorize

/**
 * Annotation for accountant access
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN', 'SUPER_ADMIN')")
annotation class IsAccountant