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

package com.ibm.microclimate.ui.internal.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SearchPattern;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ProjectSelectionPage extends WizardPage {
	
	private SearchPattern pattern = new SearchPattern(SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_BLANK_MATCH);
	private IProject project = null;

	protected ProjectSelectionPage() {
		super("Select Project");
		setTitle("Project Selection");
		setDescription("Select the project to add to Codewind");
		pattern.setPattern("*");
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Text projectLabel = new Text(composite, SWT.READ_ONLY);
        projectLabel.setText("Choose the project to add:");
        projectLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
        
		// Filter text
		Text filterText = new Text(composite, SWT.BORDER);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		filterText.setMessage("Type filter text");
        
        CheckboxTableViewer projectList = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
        projectList.setContentProvider(new WorkbenchContentProvider());
        projectList.setLabelProvider(new WorkbenchLabelProvider());
        projectList.setInput(ResourcesPlugin.getWorkspace().getRoot());
        projectList.getTable().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        projectList.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElem, Object elem) {
				if (!(elem instanceof IProject)) {
					return false;
				}
				IProject project = (IProject)elem;
				if (!project.isAccessible()) {
					return false;
				}
				if (!pattern.matches(project.getName())) {
					return false;
				}
				return true;
			}
        });
        
        filterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				String filter = filterText.getText();
				if (filter == null || filter.isEmpty()) {
					filterText.setMessage("Type filter text");
					pattern.setPattern("*");
				} else {
					pattern.setPattern("*" + filter + "*");
				}
				projectList.refresh();
			}
        });
        
        projectList.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					projectList.setCheckedElements(new Object[] {event.getElement()});
					project = (IProject) event.getElement();
				} else {
					project = null;
				}
				getWizard().getContainer().updateButtons();
			}
        });
        
        setControl(composite);
	}

	@Override
	public boolean canFlipToNextPage() {
		return canFinish();
	}

	public boolean canFinish() {
		return project != null;
	}
	
	public IProject getProject() {
		return project;
	}
}
