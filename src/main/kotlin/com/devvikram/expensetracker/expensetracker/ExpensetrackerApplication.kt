package com.devvikram.expensetracker.expensetracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ExpensetrackerApplication

fun main(args: Array<String>) {
	runApplication<ExpensetrackerApplication>(*args)
}

