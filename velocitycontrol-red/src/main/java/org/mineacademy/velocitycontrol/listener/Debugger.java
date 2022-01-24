package org.mineacademy.velocitycontrol.listener;

import org.mineacademy.velocitycontrol.VelocityControl;

public class Debugger {
	public static void debug(String a, String b) {
		VelocityControl.getLogger().debug("[" + a + "] " + b);
	}
	public static void debug(String a, String[] b) {
		for (String v : b) debug(a, v);
	}
}
