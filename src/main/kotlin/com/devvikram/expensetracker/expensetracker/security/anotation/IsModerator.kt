package com.devvikram.expensetracker.expensetracker.security.anotation

import org.springframework.security.access.prepost.PreAuthorize

/**
 * Annotation for moderator access
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
annotation class IsModerator