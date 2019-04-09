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

package com.ibm.microclimate.ui.internal.editors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;

public class MCApplicationEditorInput implements IEditorInput {
	
	public static final String EDITOR_ID = "com.ibm.microclimate.ui.editors.appOverview";
	
	public final MicroclimateApplication app;
	
	public MCApplicationEditorInput(MicroclimateApplication app) {
		this.app = app;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	@Override
	public boolean exists() {
		return app != null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return MicroclimateUIPlugin.getDefaultIcon();
	}

	@Override
	public String getName() {
		return app.name;
	}

	@Override
	public IPersistableElement getPersistable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getToolTipText() {
		// TODO Auto-generated method stub
		return null;
	}

}
