/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IPath;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.BuildStatus;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.constants.StartMode;

/**
 * Represents a Microclimate Application / Project
 */
public class MicroclimateApplication {

	public final MicroclimateConnection mcConnection;
	public final String projectID, name, host;
	public final String contextRoot;	// can be null
	public final IPath fullLocalPath;
	public final ProjectType projectType;

	private StartMode startMode;
	private AppState appState;
	private BuildStatus buildStatus;
	private String buildDetails;
	private boolean enabled = true;

	// Must be updated whenever httpPort changes. Can be null
	private URL baseUrl;

	// These are set by the MicroclimateSocket so we have to make sure the reads and writes are synchronized
	// An httpPort of -1 indicates the app is not started - could be building or disabled.
	private int httpPort = -1, debugPort = -1;

	MicroclimateApplication(MicroclimateConnection mcConnection,
			String id, String name, ProjectType projectType, String pathInWorkspace, String contextRoot)
					throws MalformedURLException {

		this.mcConnection = mcConnection;
		this.projectID = id;
		this.name = name;
		this.projectType = projectType;
		this.contextRoot = contextRoot;
		this.host = mcConnection.baseUrl.getHost();

		// The mcConnection.localWorkspacePath will end in /microclimate-workspace
		// and the path passed here will start with /microclimate-workspace, so here we fix the duplication.
		this.fullLocalPath = MCUtil.appendPathWithoutDupe(mcConnection.localWorkspacePath, pathInWorkspace);

		setBaseUrl();

		this.startMode = StartMode.RUN;
		this.appState = AppState.UNKNOWN;
		this.buildStatus = BuildStatus.UNKOWN;
	}

	private void setBaseUrl() throws MalformedURLException {
		if (httpPort == -1) {
			MCLogger.log("Un-setting baseUrl because httpPort is not valid"); //$NON-NLS-1$
			baseUrl = null;
			return;
		}

		baseUrl = new URL("http", host, httpPort, ""); //$NON-NLS-1$ //$NON-NLS-2$

		if (contextRoot != null) {
			baseUrl = new URL(baseUrl, contextRoot);
		}
	}
	
	public synchronized void setAppStatus(String appStatus) {
		this.appState = AppState.get(appStatus);
	}
	
	public synchronized void setBuildStatus(String buildStatus, String buildDetails) {
		if (buildStatus != null) {
			this.buildStatus = BuildStatus.get(buildStatus);
			if (buildDetails != null && buildDetails.trim().isEmpty()) {
				this.buildDetails = null;
			} else {
				this.buildDetails = buildDetails;
			}
		}
	}
	
	public synchronized void setStartMode(StartMode startMode) {
		this.startMode = startMode;
	}
	
	public synchronized void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	// Getters for our project state fields

	/**
	 * Can return null if this project hasn't started yet (ie httpPort == -1)
	 */
	public URL getBaseUrl() {
		return baseUrl;
	}
	
	public synchronized AppState getAppState() {
		return appState;
	}
	
	public synchronized BuildStatus getBuildStatus() {
		return buildStatus;
	}
	
	public synchronized String getBuildDetails() {
		return buildDetails;
	}

	public synchronized int getHttpPort() {
		return httpPort;
	}

	public synchronized int getDebugPort() {
		return debugPort;
	}
	
	public synchronized StartMode getStartMode() {
		return startMode;
	}
	
	public synchronized boolean isEnabled() {
		return enabled;
	}

	public boolean isActive() {
		return getAppState() == AppState.STARTING || getAppState() == AppState.STARTED;
	}

	public boolean isRunning() {
		return baseUrl != null;
	}

	public boolean isSupportedProject() {
		return mcConnection.supportsProjectType(projectType);
	}
	
	public boolean hasBuildLog() {
		return (projectType != ProjectType.NODE);
	}

	public synchronized void setHttpPort(int httpPort) {
		MCLogger.log("Set HTTP port for " + baseUrl + " to " + httpPort); //$NON-NLS-1$ //$NON-NLS-2$
		this.httpPort = httpPort;
		try {
			setBaseUrl();
		} catch (MalformedURLException e) {
			MCLogger.logError(e);
		}
	}

	public synchronized void setDebugPort(int debugPort) {
		MCLogger.log("Set debug port for " + baseUrl + " to " + debugPort); //$NON-NLS-1$ //$NON-NLS-2$
		this.debugPort = debugPort;
	}

	/**
	 * Invalidate fields that can change when the application is restarted.
	 * On restart success, these will be updated by the Socket handler for that event.
	 * This is done because the application will wait for the ports to be
	 * set to something other than -1 before trying to connect.
	 */
	public synchronized void invalidatePorts() {
		MCLogger.log("Invalidate ports for " + name); //$NON-NLS-1$
		httpPort = -1;
		debugPort = -1;
	}
	
	public void connectDebugger() {
		// Override as needed
	}
	
	public void reconnectDebugger() {
		// override as needed
	}

	public void dispose() {
		// Override as needed
	}

	@Override
	public String toString() {
		return String.format("%s@%s id=%s name=%s type=%s loc=%s", //$NON-NLS-1$
				MicroclimateApplication.class.getSimpleName(), baseUrl.toString(),
				projectID, name, projectType, fullLocalPath.toOSString());
	}
}

