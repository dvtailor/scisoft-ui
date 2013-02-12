/*
 * Copyright 2012 Diamond Light Source Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.diamond.scisoft.analysis.rcp.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.rcp.AnalysisRCPActivator;
import uk.ac.diamond.scisoft.analysis.rcp.GDADataNature;
import uk.ac.gda.common.rcp.CommonRCPActivator;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "png". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */

public class DataWizard extends Wizard implements INewWizard {
	private static final String DATA_WIZARD = "DataWizard";
	public static final String DIALOG_SETTING_KEY_DIRECTORY = "directory";
	public static final String DIALOG_SETTING_KEY_FOLDER = "folder";
	public static final String DIALOG_SETTING_KEY_PROJECT = "project";
	private static final Logger logger = LoggerFactory.getLogger(DataWizard.class);	
	private DataWizardPage page;
	private ISelection selection;
	private String defaultDataLocation, defaultFolderName;

	/**
	 * Constructor for TestWizard.
	 */
	public DataWizard() {
		super();
		setNeedsProgressMonitor(true);
		IDialogSettings dialogSettings = AnalysisRCPActivator.getDefault().getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(DATA_WIZARD);
		if(section == null){
			section = dialogSettings.addNewSection(DATA_WIZARD);
		}
		setDialogSettings(section);
	}

	/**
	 * Adding the page to the wizard.
	 */
	@Override
	public void addPages() {
		String prevProject = null , prevFolder = null, prevDirectory = null;
		IDialogSettings  settings = getDialogSettings();
		if( settings != null){
			prevProject = settings.get(DIALOG_SETTING_KEY_PROJECT);
			prevFolder = settings.get(DIALOG_SETTING_KEY_FOLDER);
			prevDirectory = settings.get(DIALOG_SETTING_KEY_DIRECTORY);
		}
		if (defaultDataLocation!=null) {
			prevDirectory = defaultDataLocation;
		}
		if (defaultFolderName!=null) {
			prevFolder = defaultFolderName;
		}
		
		page = new DataWizardPage(selection, prevProject, prevFolder, prevDirectory);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	@Override
	public boolean performFinish() {
	
		final String project   = page.getProject();
		final String directory = page.getDirectory();
		final String folder    = page.getFolder();

		try { // Runs job in wizard
			getContainer().run(true, true, new IRunnableWithProgress() {
	
				 @Override
				 public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Importing data", 100);
					try {
						createProject(project, folder, directory, GDADataNature.ID, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (Exception ne) {
			logger.error("Error creating project " + project, ne);
			String message = "Cannot create project because an internal error occurred.";
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "Project creation failure", message, new Status(IStatus.WARNING, "org.dawnsci.plotting", ne.getMessage(), ne));
		}

		IDialogSettings settings = getDialogSettings();
		if( settings != null){
			settings.put(DIALOG_SETTING_KEY_PROJECT, project);
			settings.put(DIALOG_SETTING_KEY_FOLDER, folder);
			settings.put(DIALOG_SETTING_KEY_DIRECTORY, directory);
		}
		return true;
	}
	
	/**
	 * Designed to be run from a wizard job.
	 * @param projectName
	 * @param folderName
	 * @param importFolder
	 * @param natureId
	 * @param monitor
	 * @throws CoreException
	 */
	public static void createProject(final String     projectName, 
									final String     folderName,
									final String     importFolder, 
									final String     natureId, 
									IProgressMonitor monitor) throws CoreException {

		File file = new File(importFolder);
		final String finalFolder;
		if (!file.exists()) {
			finalFolder = importFolder.trim();
			file = new File(finalFolder);
			if (!file.exists())
				throw new CoreException(new Status(IStatus.ERROR, CommonRCPActivator.PLUGIN_ID, 
					"Unable to create project folder " + projectName + "." + folderName + " as folder " + finalFolder + " does not exist "));
		} else {
			finalFolder = importFolder;
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();

		IProject project = root.getProject(projectName);
		if (!project.exists()) {
			monitor.subTask("Creating project :" + projectName);
			project.create(monitor);
			if (natureId != null) {
				project.open(monitor);
				IProjectDescription description = project.getDescription();
				description.setNatureIds(new String[] { natureId });
				project.setDescription(description, monitor);
			}
		}

		project.open(monitor);
		if (project.findMember(folderName) == null) {
			final IFolder src = project.getFolder(folderName);
			src.createLink(new Path(finalFolder), IResource.BACKGROUND_REFRESH, monitor);

		}
		project = root.getProject(projectName);
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
	}

	public static void addRemoveNature(IProject project, IProgressMonitor monitor, boolean add, String natureId) throws CoreException{
		IProjectDescription description = project.getDescription();
		boolean hasNature = project.hasNature(natureId);
		String [] newNatures=null;
		if( add ){
			if( !hasNature){
				String[] natures = description.getNatureIds();
				newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = natureId;
			}
		} else {
			if( hasNature){
				String[] natures = description.getNatureIds();
				Vector<String> v_newNatures= new  Vector<String>();
				for(int i=0; i< natures.length; i++){
					if( !natures[i].equals(natureId))
						v_newNatures.add(natures[i]);
				}
				newNatures = v_newNatures.toArray(new String[0]);
			}
		}
		if( newNatures != null){
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		}
	}


	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	public void setDataLocation(File selectedPath) {
		this.defaultDataLocation = selectedPath.getAbsolutePath();
		this.defaultFolderName   = selectedPath.getName();
	}
}
