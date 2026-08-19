package dev.rustdiagnostics

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.lang.reflect.Proxy

class DiagnosticsStartupActivity :
    ProjectActivity {

    override suspend fun execute(
        project: Project
    ) {
        val manager =
            DiagnosticInlayManager(
                project
            )

        subscribeToDaemon(
            project,
            manager
        )

        ApplicationManager
            .getApplication()
            .invokeLater {
                refreshOpenRustEditors(
                    project,
                    manager
                )
            }
    }

    private fun subscribeToDaemon(
        project: Project,
        manager: DiagnosticInlayManager
    ) {
        val listenerClass =
            DaemonCodeAnalyzer
                .DaemonListener::class.java

        val listener =
            Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass)
            ) { proxy, method, args ->

                when (
                    method.name
                ) {
                    "daemonFinished" -> {

                        ApplicationManager
                            .getApplication()
                            .invokeLater {

                                if (
                                    !project.isDisposed
                                ) {
                                    refreshOpenRustEditors(
                                        project,
                                        manager
                                    )
                                }
                            }

                        null
                    }

                    "toString" ->
                        "RustInlineDiagnosticsDaemonListener"

                    "hashCode" ->
                        System.identityHashCode(
                            proxy
                        )

                    "equals" ->
                        proxy ===
                                args
                                    ?.firstOrNull()

                    else ->
                        null
                }
            } as DaemonCodeAnalyzer.DaemonListener

        project
            .messageBus
            .connect(project)
            .subscribe(
                DaemonCodeAnalyzer
                    .DAEMON_EVENT_TOPIC,
                listener
            )
    }

    private fun refreshOpenRustEditors(
        project: Project,
        manager: DiagnosticInlayManager
    ) {
        if (project.isDisposed) {
            return
        }

        EditorFactory
            .getInstance()
            .allEditors
            .asSequence()
            .filter {
                it.project == project
            }
            .filter {
                !it.isDisposed
            }
            .filter {
                it.virtualFile
                    ?.extension
                    ?.equals(
                        "rs",
                        ignoreCase = true
                    ) == true
            }
            .forEach {
                manager.refresh(it)
            }
    }
}