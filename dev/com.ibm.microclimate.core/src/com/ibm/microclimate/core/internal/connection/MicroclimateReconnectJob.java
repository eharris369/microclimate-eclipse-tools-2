/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.connection;

import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.Messages;

public class MicroclimateReconnectJob {

	/**
	 * Keep trying to create a MicroclimateConnection to the given url until the user cancels the job or deletes
	 * the connection from the Preferences page.
	 * To be used when an initial connection cannot be established when loading from prefs on Eclipse start-up.
	 */
	static void createAndStart(final URI url) {
		final String msg = NLS.bind(Messages.MicroclimateReconnectJob_ReconnectJobName, url);

		Job reconnectJob = Job.create(msg, new ICoreRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {

				monitor.beginTask(msg, 100);

				while (!monitor.isCanceled() &&
						// Note the connection can still be deleted through the Prefs page.
						MicroclimateConnectionManager.brokenConnections().contains(url.toString())) {

					tryReconnect(monitor);

					if (monitor.isCanceled()) {
						// If they cancel the monitor, we try to delete the connection, but they can still choose
						// to not delete it.
						boolean deleted = MicroclimateConnectionManager.removeConnection(url.toString());
						// If they decide to not delete, recreate this job so they can continue trying to connect.
						if (!deleted) {
							MicroclimateReconnectJob.createAndStart(url);
							// Loop, and then this method, will exit after this - so still only one job of this kind
							// should exist at a time.
						}
					}
				}

				MCLogger.log("Done waiting for Microclimate reconnect - monitor is canceled? " + monitor.isCanceled()); //$NON-NLS-1$
				monitor.done();
			}

			private void tryReconnect(IProgressMonitor monitor) {
				// each re-connect attempt takes 2 seconds because that's how long the socket tries to connect for
				// so, we delay for 5 seconds, try to connect for 2 seconds, repeat.
				final int delay = 5000;

				try {
					Thread.sleep(delay);
					//i++;

					MCLogger.log("Trying to reconnect to Microclimate at " + url); //$NON-NLS-1$

					MicroclimateConnection newConnection = new MicroclimateConnection(url);
					if (newConnection != null) {
						// connection re-established!
						MCLogger.log("Successfully re-connected to Microclimate at " + url); //$NON-NLS-1$
						MicroclimateConnectionManager.remove(url.toString());
						MicroclimateConnectionManager.add(newConnection);
						return;
					}
				}
				catch (InterruptedException e) {
					MCLogger.logError(e);
				}
				catch (MicroclimateConnectionException e) {
					// nothing, the connection just failed. we'll try again.
				}
				catch (Exception e) {
					// If any other exception occurs,
					// it is most likely that this connection will never succeed.
					MCLogger.logError(e);
					monitor.setCanceled(true);

					MCUtil.openDialog(true, Messages.MicroclimateReconnectJob_ReconnectErrorDialogTitle,
							NLS.bind(Messages.MicroclimateReconnectJob_ReconnectErrorDialogMsg, url));
				}
			}
		});

		reconnectJob.schedule();
	}
}
