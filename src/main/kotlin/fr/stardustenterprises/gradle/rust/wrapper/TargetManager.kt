package fr.stardustenterprises.gradle.rust.wrapper

import fr.stardustenterprises.gradle.rust.wrapper.ext.WrapperExtension
import org.gradle.api.Project
import java.io.ByteArrayOutputStream

object TargetManager {
    fun ensureTargetsInstalled(
        project: Project,
        wrapperExtension: WrapperExtension,
    ) {
        if (wrapperExtension.cargoInstallTargets.getOrElse(false)) {
            installTargets(project, wrapperExtension)
        }
    }

    private fun installTargets(
        project: Project,
        wrapperExtension: WrapperExtension,
    ) {
        val rustupCommand = wrapperExtension.rustupCommand.get()

        val stdout = ByteArrayOutputStream()
        project.providers.exec { exec ->
            exec.commandLine(rustupCommand)
            exec.args("target", "list", "--installed")
            exec.workingDir(wrapperExtension.crate.get().asFile)
            exec.environment(wrapperExtension.env)
            exec.standardOutput = stdout
        }.result.get().assertNormalExitValue()

        val installed = stdout.toString().split("\n")
            .toMutableList()
            .also { it.removeIf(String::isNullOrBlank) }

        wrapperExtension.targets.forEach { targetOptions ->
            if (installed.contains(targetOptions.target)) {
                return@forEach
            }
            println("Installing target \"${targetOptions.target}\" via rustup.")

            val command = targetOptions.command!!.lowercase()
            if (command.contains("cargo") &&
                !command.contains("cross")
            ) {
                project.providers.exec { exec ->
                    exec.commandLine(rustupCommand)
                    exec.args("target", "add", targetOptions.target)
                    exec.workingDir(wrapperExtension.crate.get().asFile)
                    exec.environment(wrapperExtension.env)
                }.result.get().assertNormalExitValue()
            }
        }
    }
}
