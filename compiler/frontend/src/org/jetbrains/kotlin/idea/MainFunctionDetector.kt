/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.types.Variance

abstract class MainFunctionDetector(protected val languageVersionSettings: LanguageVersionSettings) {
    @JvmOverloads
    fun isMain(
        descriptor: DeclarationDescriptor,
        checkJvmStaticAnnotation: Boolean = true,
        checkReturnType: Boolean = true,
        allowParameterless: Boolean = true
    ): Boolean {
        if (descriptor !is FunctionDescriptor) return false

        if (getJVMFunctionName(descriptor) != "main") {
            return false
        }

        val parameters = descriptor.valueParameters.mapTo(mutableListOf()) { it.type }
        descriptor.extensionReceiverParameter?.type?.let { parameters += it }

        if (!isParameterNumberSuitsForMain(parameters.size, DescriptorUtils.isTopLevelDeclaration(descriptor), allowParameterless)) {
            return false
        }

        if (descriptor.typeParameters.isNotEmpty()) return false

        if (parameters.size == 1) {
            val parameterType = parameters[0]
            if (!KotlinBuiltIns.isArray(parameterType)) return false

            val typeArguments = parameterType.arguments
            if (typeArguments.size != 1) return false

            val typeArgument = typeArguments[0].type
            if (!KotlinBuiltIns.isString(typeArgument)) {
                return false
            }
            if (typeArguments[0].projectionKind === Variance.IN_VARIANCE) {
                return false
            }
        } else {
            assert(parameters.size == 0) { "Parameter list is expected to be empty" }
            assert(DescriptorUtils.isTopLevelDeclaration(descriptor)) { "main without parameters works only for top-level" }
            // We do not support parameterless entry points having JvmName("name") but different real names
            // See more at https://github.com/Kotlin/KEEP/blob/master/proposals/enhancing-main-convention.md#parameterless-main
            if (descriptor.name.asString() != "main") return false

            if (descriptor.getFunctionsFromTheSameFile().any { declaration ->
                    isMain(declaration, checkJvmStaticAnnotation, allowParameterless = false)
                }) return false
        }

        if (descriptor.isSuspend && !languageVersionSettings.supportsFeature(LanguageFeature.ExtendedMainConvention)) return false

        if (checkReturnType && !isMainReturnType(descriptor)) return false

        if (DescriptorUtils.isTopLevelDeclaration(descriptor)) return true

        val containingDeclaration = descriptor.containingDeclaration
        return containingDeclaration is ClassDescriptor
                && containingDeclaration.kind.isSingleton
                && (descriptor.hasJvmStaticAnnotation() || !checkJvmStaticAnnotation)
    }

    protected abstract fun FunctionDescriptor.getFunctionsFromTheSameFile(): Collection<FunctionDescriptor>

    protected fun isParameterNumberSuitsForMain(
        parametersCount: Int,
        isTopLevel: Boolean,
        allowParameterless: Boolean
    ): Boolean = when (parametersCount) {
        1 -> true
        0 -> isTopLevel && allowParameterless && languageVersionSettings.supportsFeature(LanguageFeature.ExtendedMainConvention)
        else -> false
    }

    companion object {
        private fun isMainReturnType(descriptor: FunctionDescriptor): Boolean {
            val returnType = descriptor.returnType
            return returnType != null && KotlinBuiltIns.isUnit(returnType)
        }

        private fun getJVMFunctionName(functionDescriptor: FunctionDescriptor): String {
            return DescriptorUtils.getJvmName(functionDescriptor) ?: functionDescriptor.name.asString()
        }
    }
}
