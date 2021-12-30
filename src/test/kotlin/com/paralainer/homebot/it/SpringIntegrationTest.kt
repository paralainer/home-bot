package com.paralainer.homebot.it

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@SpringBootTest
@TestPropertySource("classpath:test-env.properties")
annotation class SpringIntegrationTest
