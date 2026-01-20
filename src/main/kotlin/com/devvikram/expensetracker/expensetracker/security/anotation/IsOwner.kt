package com.devvikram.expensetracker.expensetracker.security.anotation

import org.springframework.security.access.prepost.PreAuthorize


/**
 * Custom annotation to check if user owns the resource
 * Usage: @IsOwner("#userId")
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("#userId == authentication.principal.username or hasRole('ADMIN')")
annotation class IsOwner