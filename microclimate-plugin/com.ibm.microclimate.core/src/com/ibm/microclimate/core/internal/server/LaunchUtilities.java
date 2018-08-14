package com.ibm.microclimate.core.internal.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.jdi.Bootstrap;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.wst.server.core.IServer;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;

/**
 * Largely from com.ibm.ws.st.core.internal.launch.LaunchUtilities
 * and com.ibm.ws.st.core.internal.launch.BaseLibertyLaunchConfiguration
 *
 * Static utilities to support launching and debugging Microclimate application servers.
 *
 * @author timetchells@ibm.com
 *
 */
@SuppressWarnings("restriction")
public class LaunchUtilities {

	private LaunchUtilities() { }

    /**
     * Returns a free port number on localhost, or -1 if unable to find a free port.
     *
     * @return a free port number on localhost, or -1 if unable to find a free port
     */
    public static int findFreePort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } catch (IOException e) {
            // ignore
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return -1;
    }

    /**
     * Returns the default 'com.sun.jdi.SocketAttach' connector.
     *
     * @return the {@link AttachingConnector}
     */
	public static AttachingConnector getAttachingConnector() {
        List<?> connectors = Bootstrap.virtualMachineManager().attachingConnectors();
        for (int i = 0; i < connectors.size(); i++) {
            AttachingConnector c = (AttachingConnector) connectors.get(i);
            if ("com.sun.jdi.SocketAttach".equals(c.name())) {
				return c;
			}
        }

        return null;
    }

    public static Map<String, Connector.Argument> configureConnector(
    		Map<String, Connector.Argument> argsToConfigure, String host, int portNumber) {

        Connector.StringArgument hostArg = (Connector.StringArgument) argsToConfigure.get("hostname");
        hostArg.setValue(host);

        Connector.IntegerArgument portArg = (Connector.IntegerArgument) argsToConfigure.get("port");
        portArg.setValue(portNumber);

        Connector.IntegerArgument timeoutArg = (Connector.IntegerArgument) argsToConfigure.get("timeout");
        if (timeoutArg != null) {
            int timeout = Platform.getPreferencesService().getInt(
                                                                  "org.eclipse.jdt.launching",
                                                                  JavaRuntime.PREF_CONNECT_TIMEOUT,
                                                                  JavaRuntime.DEF_CONNECT_TIMEOUT,
                                                                  null);
            timeoutArg.setValue(timeout);
        }

        return argsToConfigure;
    }

    protected static IProcess newProcess(final IServer server, ILaunch launch, Process p, String label) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(IProcess.ATTR_PROCESS_TYPE, "java");

        return new RuntimeProcess(launch, p, label, attributes) {
            @Override
            public boolean canTerminate() {
            	return false;
            }
        };
    }

    /**
     * Creates a new debug target for the given virtual machine and system process
     * that is connected on the specified port for the given launch.
     *
     * @param launch launch to add the target to
     * @param port port the VM is connected to
     * @param process associated system process
     * @param vm JDI virtual machine
     * @return the {@link IDebugTarget}
     */
    public static IDebugTarget createLocalJDTDebugTarget(ILaunch launch, int port, IProcess process, VirtualMachine vm,
    		String name) {

        return JDIDebugModel.newDebugTarget(launch, vm, name, process, true, false, true);
    }


    /**
     * Set the debug request timeout of a vm in ms, if supported by the vm implementation.
     */
    public static void setDebugTimeout(VirtualMachine vm) {
        IEclipsePreferences node = InstanceScope.INSTANCE.getNode(JDIDebugPlugin.getUniqueIdentifier());
        int timeOut = node.getInt(JDIDebugModel.PREF_REQUEST_TIMEOUT, 0);
        if (timeOut <= 0) {
			return;
		}
        if (vm instanceof org.eclipse.jdi.VirtualMachine) {
            org.eclipse.jdi.VirtualMachine vm2 = (org.eclipse.jdi.VirtualMachine) vm;
            vm2.setRequestTimeout(timeOut);
        }
    }
}
