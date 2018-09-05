package com.ibm.microclimate.core;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.ibm.microclimate.core.internal.MCLogger;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.microclimate.core"; //$NON-NLS-1$

	public static final String DEFAULT_ICON_PATH = "icons/microclimate.ico";

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		context.registerService(DebugOptionsListener.class, MCLogger.instance(), null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public static ImageDescriptor getIcon(String path) {
		final URL url = Activator.getDefault().getBundle().getEntry(DEFAULT_ICON_PATH);
		return ImageDescriptor.createFromURL(url);
	}

}