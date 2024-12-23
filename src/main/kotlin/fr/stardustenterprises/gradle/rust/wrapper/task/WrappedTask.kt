package fr.stardustenterprises.gradle.rust.wrapper.task

import fr.stardustenterprises.gradle.rust.wrapper.TargetManager
import fr.stardustenterprises.gradle.rust.wrapper.ext.WrapperExtension
import fr.stardustenterprises.stargrad.task.ConfigurableTask
import org.gradle.api.tasks.Internal
import org.gradle.process.internal.ExecException

open class WrappedTask(
    @Internal
    protected val command: String
) : ConfigurableTask<WrapperExtension>() {

    @Throws(ExecException::class)
    override fun run() {
        TargetManager.ensureTargetsInstalled(project, configuration)

        configuration.targets.forEach { target ->
            project.providers.exec {
                it.commandLine(target.command)
                it.args(target.subcommand(command))
                it.workingDir(
                    configuration.crate.asFile.orNull
                        ?: throw RuntimeException("Invalid working dir.")
                )
                it.environment(target.env)
            }.result.get().assertNormalExitValue()
        }
    }
}
