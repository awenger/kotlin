/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.gradle.utils.chainedFinalizeValueOnRead
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import java.io.File
import javax.inject.Inject

interface UsesKotlinJavaToolchain {
    @get:Nested
    val kotlinJavaToolchainProvider: Provider<KotlinJavaToolchain>

    @get:Internal
    val kotlinJavaToolchain: KotlinJavaToolchain get() = kotlinJavaToolchainProvider.get()
}

abstract class KotlinJavaToolchain @Inject constructor(
    objects: ObjectFactory,
    files: ProjectLayout
) {
    private val currentJvm: Property<Jvm> = objects
        .property<Jvm>(Jvm.current())
        .chainedFinalizeValueOnRead()

    private val _javaVersion: Property<JavaVersion> = objects
        .propertyWithConvention(
            currentJvm.map { jvm ->
                jvm.javaVersion
                    ?: throw GradleException(
                        "Kotlin could not get java version for the JDK installation: " +
                                jvm.javaHome?.let { "'$it' " }.orEmpty()
                    )
            }
        )

    @get:Input
    val javaVersion: Provider<JavaVersion>
        get() = _javaVersion

    @get:Internal
    internal val javaExecutable: RegularFileProperty = objects
        .fileProperty()
        .convention(
            currentJvm.flatMap { jvm ->
                files.file(
                    objects.property<File>(
                        jvm.javaExecutable
                            ?: throw GradleException(
                                "Kotlin could not find 'java' executable in the JDK installation: " +
                                        jvm.javaHome?.let { "'$it' " }.orEmpty()
                            )
                    )
                )
            }
        )
        .chainedFinalizeValueOnRead()

    private val _jdkToolsJar = objects
        .propertyWithConvention(
            currentJvm.flatMap { jvm ->
                objects.propertyWithConvention(jvm.toolsJar)
            }
        )
        .chainedFinalizeValueOnRead()

    @get:Internal
    internal val jdkToolsJar: Provider<File?> = _jdkToolsJar
        .orElse(javaVersion.flatMap {
            if (it < JavaVersion.VERSION_1_9) {
                throw GradleException(
                    "Kotlin could not find the required JDK tools in the Java installation. " +
                            "Make sure Kotlin compilation is running on a JDK, not JRE."
                )
            }

            objects.propertyWithConvention<File?>(null)
        })

    /**
     * Set JDK to use for Kotlin compilation.
     *
     * Major JDK version is considered as compile task input.
     *
     * @param jdkHomeLocation path to JDK.
     * *Note*: project build will fail on providing here JRE instead of JDK!
     * @param jdkVersion provided JDK version
     */
    fun setJdkHome(
        jdkHomeLocation: File,
        jdkVersion: JavaVersion
    ) {
        val jvm = Jvm.forHome(jdkHomeLocation) as Jvm

        javaExecutable.set(jvm.javaExecutable)
        _jdkToolsJar.set(jvm.toolsJar)
        _javaVersion.set(jdkVersion)
    }

    /**
     * Set JDK to use for Kotlin compilation.
     *
     * @see [setJdkHome]
     */
    fun setJdkHome(
        jdkHomeLocation: String,
        jdkVersion: JavaVersion
    ) = setJdkHome(File(jdkHomeLocation), jdkVersion)
}