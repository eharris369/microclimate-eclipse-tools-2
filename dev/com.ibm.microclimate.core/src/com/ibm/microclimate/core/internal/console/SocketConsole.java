package com.ibm.microclimate.core.internal.console;

import java.io.IOException;

import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateSocket;

public class SocketConsole extends IOConsole {

	// This message is displayed when the console is created, until the server sends the first set of logs.
	private static final String INITIAL_MSG = "Waiting for server to send logs...";

	public final String projectID;
	private final MicroclimateSocket socket;

	private IOConsoleOutputStream outputStream;
	private int previousLength = 0;
	private boolean isInitialized = false;

	public SocketConsole(String name, MicroclimateApplication app) {
		super(name, MicroclimateConsoleFactory.MC_CONSOLE_TYPE,
				MicroclimateCorePlugin.getIcon(MicroclimateCorePlugin.DEFAULT_ICON_PATH),
				true);

		this.projectID = app.projectID;
		this.outputStream = newOutputStream();
		this.socket = app.mcConnection.mcSocket;
		socket.registerSocketConsole(this);

		try {
			this.outputStream.write(INITIAL_MSG);
		} catch (IOException e) {
			MCLogger.logError("Error writing initial message to " + this.getName(), e);
		}
	}

	public void update(String contents) throws IOException {
		if (!isInitialized) {
			// Clear the INITIAL_MSG
			clearConsole();
			isInitialized = true;
		}

		String newContents = "";
		int diff = contents.length() - previousLength;
		if (diff == 0) {
			// nothing to do
			return;
		}
		else if (diff < 0) {
			// The app log was cleared
			// eg if the dockerfile was changed and the container had to be rebuilt
			MCLogger.log("Console was cleared");
			clearConsole();
			// write the whole new console
			newContents = contents;
		}
		else {
			// write only the new characters to the console
			newContents = contents.substring(previousLength, previousLength + diff);
		}

		MCLogger.log(newContents.length() + " new characters to write to " + this.getName());		// $NON-NLS-1$
		outputStream.write(newContents);
		previousLength = contents.length();
	}

	@Override
	protected void dispose() {
		MCLogger.log("Dispose console " + getName()); //$NON-NLS-1$

		socket.deregisterSocketConsole(this);

		try {
			outputStream.close();
		} catch (IOException e) {
			MCLogger.logError("Error closing console output stream", e); //$NON-NLS-1$
		}

		super.dispose();
	}
}
