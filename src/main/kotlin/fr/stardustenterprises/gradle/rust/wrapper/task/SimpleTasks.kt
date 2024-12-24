package fr.stardustenterprises.gradle.rust.wrapper.task

import fr.stardustenterprises.gradle.rust.wrapper.ext.WrapperExtension
import fr.stardustenterprises.stargrad.task.ConfigurableTask
import fr.stardustenterprises.stargrad.task.Task
import org.apache.commons.io.FileUtils.deleteDirectory
import org.gradle.process.ExecOperations
import javax.inject.Inject

@Task(group = "rust", name = "test")
open class TestTask @Inject constructor(execOperations: ExecOperations) : WrappedTask("test", execOperations)

@Task(group = "rust", name = "run")
open class RunTask @Inject constructor(execOperations: ExecOperations) : WrappedTask("run", execOperations)

@Task(group = "rust", name = "clean")
open class CleanTask : ConfigurableTask<WrapperExtension>() {
    override fun run() {
        val workingDir = this.configuration.crate.asFile.getOrElse(
            this.project.projectDir
        )

        deleteDirectory(workingDir.resolve("target"))
        deleteDirectory(this.project.projectDir.resolve("build"))
    }
}
