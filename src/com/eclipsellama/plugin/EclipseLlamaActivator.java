package com.eclipsellama.plugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class for EclipseLlama plugin. Controls the plug-in life cycle.
 */
public class EclipseLlamaActivator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.eclipsellama.plugin";

	private static EclipseLlamaActivator plugin;

	public EclipseLlamaActivator() {
		// Default constructor
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		System.out.println("EclipseLlama: Plugin started");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			com.eclipsellama.plugin.mcp.McpConnectionManager.getInstance().closeAll();
		} catch (Throwable ignored) {
		}
		try {
			com.eclipsellama.plugin.ui.chat.ChatStyles.disposeInstance();
		} catch (Throwable ignored) {
		}
		plugin = null;
		super.stop(context);
		System.out.println("EclipseLlama: Plugin stopped");
	}

	/**
	 * Returns the shared instance.
	 */
	public static EclipseLlamaActivator getDefault() {
		return plugin;
	}
}
