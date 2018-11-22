/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IMarkerResolution;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.messages.Messages;

public class MicroclimateMarkerResolution implements IMarkerResolution {
	
	private final MicroclimateApplication app;
	private final String quickFixId;
	private final String quickFixDescription;
	
	public MicroclimateMarkerResolution(MicroclimateApplication app, String quickFixId, String quickFixDescription) {
		this.app = app;
		this.quickFixId = quickFixId;
		this.quickFixDescription = quickFixDescription;
	}

	@Override
	public String getLabel() {
		return quickFixDescription;
	}

	@Override
	public void run(IMarker marker) {
		// Some day there should be a API that takes the quick fix id and executes it
		try {
			app.mcConnection.requestValidateGenerate(app);
			IResource resource = marker.getResource();
			if (resource != null) {
				Job job = new Job(Messages.refreshResourceJobLabel) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							resource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				            return Status.OK_STATUS;
						} catch (Exception e) {
							MCLogger.logError("An error occurred while refreshing the resource: " + resource.getLocation()); //$NON-NLS-1$
							return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID,
									NLS.bind(Messages.RefreshResourceError, resource.getLocation()), e);
						}
					}
				};
				job.setPriority(Job.LONG);
				job.schedule();
			}
		} catch (Exception e) {
			MCLogger.logError("The generate request failed for application: " + app.name, e); //$NON-NLS-1$
		}
	}
}
