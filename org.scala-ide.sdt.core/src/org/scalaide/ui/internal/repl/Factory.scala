package org.scalaide.ui.internal.repl

import org.eclipse.core.resources.IProject
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IPackageFragment
import org.eclipse.jdt.core.ITypeRoot
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.IFileEditorInput
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.swt.widgets.Shell
import org.eclipse.ui.console.IConsoleFactory
import org.scalaide.core.internal.repl.InterpreterLaunchConstants

/**
 * This is the factory definition that supplies a Scala interpreter factory.
 */
class Factory extends IConsoleFactory {
  override def openConsole() = {
  val p = Factory.getCurrentProject

  p match {
      case Some(project: IProject) =>
      Factory.openConsoleInProject(project)
      case None =>
      val shell = new Shell
        MessageDialog.openInformation(shell, "Scala Development Tools", "Must have a currently opened Scala or Java Project")
    }
  }

}

object Factory {
  val SCALA_INTERPRETER_LAUNCH_ID = "scala.interpreter"

  def openConsoleInProject(project: IProject) = {
  openConsoleInProjectFromTarget(project, None)
  }

  def openConsoleInProjectFromTarget(project: IProject, target: Option[IJavaElement]) = {
    import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants._
    import org.eclipse.debug.core.DebugPlugin
    val manager = DebugPlugin.getDefault().getLaunchManager();
    val launchType = manager.getLaunchConfigurationType(SCALA_INTERPRETER_LAUNCH_ID);
    val projectName = project.getName
    //Pull out the first interpreter configuration we can find for the project.
    val configuration = manager.getLaunchConfigurations(launchType).filter({ config =>
      projectName.equals(config.getAttribute(ATTR_PROJECT_NAME, ""))
    }).headOption

    val workingCopy = configuration match {
      case Some(config) =>
        config.getWorkingCopy
      case None =>
        val newConfig = launchType.newInstance(null, projectName);
        newConfig.setAttribute(ATTR_PROJECT_NAME, projectName);
        //TODO - What else do we need to set on the properties for this to work...
        newConfig
    }

    target match {
      case Some(x : IPackageFragment) =>
        workingCopy.setAttribute(InterpreterLaunchConstants.PACKAGE_IMPORT, x.getElementName)
      case Some(x : ITypeRoot) =>

      case _ => //Ignore
    }

    import org.eclipse.debug.ui.DebugUITools
    import org.eclipse.debug.core.ILaunchManager
    DebugUITools.launch(workingCopy, ILaunchManager.RUN_MODE)
  }

  def getCurrentProject() = {
    val editorPart = PlatformUI.getWorkbench.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    var current_project : Option[IProject] = None

    if(editorPart  != null) {
      val input = editorPart.getEditorInput().asInstanceOf[IFileEditorInput]
      val file = input.getFile()
      val project = file.getProject();
      if (project != null) {
        current_project = Some(project)
      }
    }
    current_project
  }
}
