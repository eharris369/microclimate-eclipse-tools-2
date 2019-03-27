/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.net.URI;

import com.ibm.microclimate.core.internal.connection.ICPMicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.LocalMicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.constants.ProjectType;

/**
 * Factory for creating the correct Microclimate objects.  This is used to keep the Eclipse
 * code and the Microclimate code separate.
 * 
 * Currently only MicroclimateApplication has an Eclipse version.  Rather than let Eclipse
 * code leak into MicroclimateConnection an Eclipse version of it should be created if necessary.
 */
public class MicroclimateObjectFactory {
	
	public static LocalMicroclimateConnection createLocalConnection(URI uri) throws Exception {
		LocalMicroclimateConnection connection = new LocalMicroclimateConnection(uri);
		connection.initialize();
		return connection;
	}
	
	public static ICPMicroclimateConnection createICPConnection(URI ingressURI, String masterIP, String namespace) throws Exception {
		ICPMicroclimateConnection connection = new ICPMicroclimateConnection(ingressURI, masterIP, namespace);
		connection.initialize();
		return connection;
	}
	
	public static MicroclimateApplication createMicroclimateApplication(MicroclimateConnection mcConnection,
			String id, String name, ProjectType projectType, String pathInWorkspace, String contextRoot) throws Exception {
		return new MCEclipseApplication(mcConnection, id, name, projectType, pathInWorkspace, contextRoot);
	}

}
