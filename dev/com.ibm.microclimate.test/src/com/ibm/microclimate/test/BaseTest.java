/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.test;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.ide.IDE;
import org.json.JSONException;

import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.MicroclimateObjectFactory;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.console.MicroclimateConsoleFactory;
import com.ibm.microclimate.core.internal.console.ProjectLogInfo;
import com.ibm.microclimate.core.internal.console.ProjectTemplateInfo;
import com.ibm.microclimate.core.internal.console.SocketConsole;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.constants.StartMode;
import com.ibm.microclimate.test.util.Condition;
import com.ibm.microclimate.test.util.ImportUtil;
import com.ibm.microclimate.test.util.MicroclimateUtil;
import com.ibm.microclimate.test.util.TestUtil;
import com.ibm.microclimate.ui.internal.actions.ImportProjectAction;

import junit.framework.TestCase;

public abstract class BaseTest extends TestCase {

	protected static final String MICROCLIMATE_URI = "http://localhost:9090";
	
	protected static final String MARKER_TYPE = "com.ibm.microclimate.core.validationMarker";
	
	protected static MicroclimateConnection connection;
	protected static IProject project;
	
	protected static String projectName;
	protected static ProjectType projectType;
	protected static String relativeURL;
	protected static String srcPath;
	
	protected static Boolean origAutoBuildSetting = null;
	
    public void doSetup() throws Exception {
    	// Disable workspace auto build
    	origAutoBuildSetting = setWorkspaceAutoBuild(false);
    	
        // Create a microclimate connection
        connection = MicroclimateObjectFactory.createMicroclimateConnection(new URI(MICROCLIMATE_URI));
        MicroclimateConnectionManager.add(connection);
        
        // Create a new microprofile project
        createProject(projectType, projectName);
        
        // Wait for the project to be created
        assertTrue("The application " + projectName + " should be created", MicroclimateUtil.waitForProject(connection, projectName, 300, 5));
        
        // Wait for the project to be started
        assertTrue("The application " + projectName + " should be running", MicroclimateUtil.waitForProjectStart(connection, projectName, 600, 5));
        
        // Import the application into eclipse
        MicroclimateApplication app = connection.getAppByName(projectName);
        ImportProjectAction.importProject(app);
        project = ImportUtil.waitForProject(projectName);
        assertNotNull("The " + projectName + " project should be imported in eclipse", project);
    }
    
	public void doTearDown() {
		try {
			MicroclimateUtil.cleanup(connection);
		} catch (Exception e) {
			TestUtil.print("Test case cleanup failed", e);
		}
    	
		// Restore workspace auto build setting
		if (origAutoBuildSetting != null) {
			setWorkspaceAutoBuild(origAutoBuildSetting.booleanValue());
		}
	}
    
    public void checkApp(String text) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	assertTrue("App should be in started state.  Current state is: " + app.getAppState(), MicroclimateUtil.waitForAppState(app, AppState.STARTED, 120, 2));
    	pingApp(text);
    	checkMode(StartMode.RUN);
    	showConsoles();
    	checkConsoles();
    }
    
    protected void pingApp(String expectedText) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	URL url = app.getBaseUrl();
    	url = new URL(url.toExternalForm() + relativeURL);
    	HttpUtil.HttpResult result = HttpUtil.get(url.toURI());
    	for (int i = 0; i < 15 && !result.isGoodResponse; i++) {
    		Thread.sleep(1000);
    		result = HttpUtil.get(url.toURI());
    	}
    	assertTrue("The response code should be 200: " + result.responseCode, result.responseCode == 200);
    	assertTrue("The response should contain the expected text: " + expectedText, result.response != null && result.response.contains(expectedText));   	
    }
    
    protected void checkMode(StartMode mode) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	for (int i = 0; i < 5 && app.getStartMode() != mode; i++) {
    		Thread.sleep(1000);
    	}
    	assertTrue("App is in " + app.getStartMode() + " when it should be in " + mode + " mode.", app.getStartMode() == mode);
    	ILaunch launch = ((MCEclipseApplication)app).getLaunch();
    	if (StartMode.DEBUG_MODES.contains(mode)) {
    		assertNotNull("There should be a launch for the app", launch);
        	IDebugTarget debugTarget = launch.getDebugTarget();
	    	assertNotNull("The launch should have a debug target", debugTarget);
	    	assertTrue("The debug target should have threads", debugTarget.hasThreads());
    	} else {
    		assertNull("There should be no launch when in run mode", launch);
    	}
    }
    
    protected void switchMode(StartMode mode) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	connection.requestProjectRestart(app, mode.startMode);
    	// For Java builds the states can go by quickly so don't do an assert on this
    	MicroclimateUtil.waitForAppState(app, AppState.STOPPED, 30, 1);
    	assertTrue("App should be in started state instead of: " + app.getAppState(), MicroclimateUtil.waitForAppState(app, AppState.STARTED, 120, 1));
    	checkMode(mode);
    }
    
    protected void showConsoles() throws Exception {
    	MCEclipseApplication app = (MCEclipseApplication) connection.getAppByName(projectName);
		for (ProjectLogInfo logInfo : app.getLogInfos()) {
    		if (app.getConsole(logInfo) == null) {
    			SocketConsole console = MicroclimateConsoleFactory.createLogFileConsole(app, logInfo);
    			app.addConsole(console);
    		}
    	}
    }

    protected void checkConsoles() throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	Set<String> expectedConsoles = new HashSet<String>();
    	Set<String> foundConsoles = new HashSet<String>();
		for (ProjectLogInfo logInfo : app.getLogInfos()) {
			expectedConsoles.add(logInfo.logName);
		}
    	
    	IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    	for (IConsole console : manager.getConsoles()) {
    		if (console.getName().contains(projectName)) {
    			TestUtil.print("Found console: " + console.getName());
    			assertTrue("The " + console.getName() + " console should be a TextConsole", console instanceof TextConsole);
    			TestUtil.wait(new Condition() {
    				@Override
    				public boolean test() {
    					return ((TextConsole)console).getDocument().getLength() > 0;
    				}
    			}, 20, 1);
    			assertTrue("The " + console.getName() + " console should not be empty", ((TextConsole)console).getDocument().getLength() > 0);
    			for (String name : expectedConsoles) {
    				if (console.getName().contains(name)) {
    					foundConsoles.add(name);
    					break;
    				}
    			}
    		}
    	}
    	assertTrue("Did not find all expected consoles", foundConsoles.size() == expectedConsoles.size());
    }
    
    protected void buildIfWindows() throws Exception {
    	if (TestUtil.isWindows()) {
    		build();
    	}
    }
    
    protected void build() throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
		connection.requestProjectBuild(app, MCConstants.VALUE_ACTION_BUILD);
    }
    
    protected void setAutoBuild(boolean enabled) throws Exception {
    	String actionKey = enabled ? MCConstants.VALUE_ACTION_ENABLEAUTOBUILD : MCConstants.VALUE_ACTION_DISABLEAUTOBUILD;
    	MicroclimateApplication app = connection.getAppByName(projectName);
		connection.requestProjectBuild(app, actionKey);
    }
    
    protected IMarker[] getMarkers(IResource resource) throws Exception {
    	return resource.findMarkers(MARKER_TYPE, false, IResource.DEPTH_ONE);
    }
    
    protected void runValidation() throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
		connection.requestValidate(app);
    }
    
    protected void runQuickFix(IResource resource) throws Exception {
    	IMarker[] markers = getMarkers(resource);
    	assertTrue("There should be at least one marker for " + resource.getName() + ": " + markers.length, markers.length > 0);

        IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(markers[0]);
        assertTrue("Did not get any marker resolutions.", resolutions.length > 0);
        resolutions[0].run(markers[0]);
        TestUtil.waitForJobs(10, 1);
    }
    
	public static Boolean setWorkspaceAutoBuild(boolean enabled) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription wsDescription = workspace.getDescription();
		boolean origEnabled = wsDescription.isAutoBuilding();
		if (enabled != origEnabled) {
			try {
				wsDescription.setAutoBuilding(enabled);
				workspace.setDescription(wsDescription);
				return origEnabled ? Boolean.TRUE : Boolean.FALSE;
			} catch (CoreException e) {
				TestUtil.print("Failed to set workspace auto build enabled to: " + enabled, e);
			}
		}
		return null;
	}
	
	protected void createProject(ProjectType type, String name) throws IOException, JSONException {
		ProjectTemplateInfo templateInfo = null;
		List<ProjectTemplateInfo> templates = connection.requestProjectTemplates();
		for (ProjectTemplateInfo template : templates) {
			if (type.language.equals(template.getLanguage())) {
				if (type.isLanguage(ProjectType.LANGUAGE_JAVA)) {
					String extension = template.getExtension();
					if (type.isType(ProjectType.TYPE_LIBERTY) && extension.toLowerCase().contains("microprofile")) {
						templateInfo = template;
						break;
					}
					if (type.isType(ProjectType.TYPE_SPRING) && extension.toLowerCase().contains("spring")) {
						templateInfo = template;
						break;
					}
				} else {
					templateInfo = template;
					break;
				}
			}
		}
		assertNotNull("No template found that matches the project type: " + projectType, templateInfo);
		connection.requestProjectCreate(templateInfo, name);

	}

}
