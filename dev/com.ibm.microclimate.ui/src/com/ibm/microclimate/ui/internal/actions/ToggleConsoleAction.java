/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;

/**
 * Abstract base action for toggling the display of Microclimate logs.
 */
public abstract class ToggleConsoleAction implements IObjectActionDelegate {

    protected MCEclipseApplication app;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof MicroclimateApplication) {
            	app = (MCEclipseApplication)obj;
            	if (app.isAvailable() && supportsConsole()) {
	            	action.setChecked(hasConsole());
	            	action.setEnabled(true);
	            	return;
            	}
            }
        }
        action.setChecked(false);
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (app == null) {
        	// should not be possible
        	MCLogger.logError("ToggleConsolesAction ran but no Microclimate application was selected");
			return;
		}

        if (action.isChecked()) {
        	IConsole console = createConsole();
        	ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
        	setConsole(console);
        } else {
        	IConsole console = getConsole();
        	if (console != null) {
	        	IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
	        	consoleManager.removeConsoles(new IConsole[] { console });
	        	setConsole(null);
        	}
        }
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
	
	protected abstract boolean supportsConsole();
	protected abstract IConsole createConsole();
	protected abstract void setConsole(IConsole console);
	protected abstract boolean hasConsole();
	protected abstract IConsole getConsole();
}
