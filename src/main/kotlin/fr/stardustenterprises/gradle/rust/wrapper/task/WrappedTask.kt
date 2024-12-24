package fr.stardustenterprises.gradle.rust.wrapper.task

import fr.stardustenterprises.gradle.rust.wrapper.TargetManager
import fr.stardustenterprises.gradle.rust.wrapper.ext.WrapperExtension
import fr.stardustenterprises.stargrad.task.ConfigurableTask
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecOperations
import org.gradle.process.internal.ExecException

open class WrappedTask(
    @Internal
    protected val command: String,
    private var execOperations: ExecOperations
) : ConfigurableTask<WrapperExtension>() {

    @Throws(ExecException::class)
    override fun run() {
        TargetManager.ensureTargetsInstalled(project, configuration, execOperations)

        configuration.targets.forEach { target ->
            execOperations.exec {
                it.commandLine(target.command)
                it.args(target.subcommand(command))
                it.workingDir(
                    configuration.crate.asFile.orNull
                        ?: throw RuntimeException("Invalid working dir.")
                )
                it.environment(target.env)
            }.assertNormalExitValue()
        }
    }
}
