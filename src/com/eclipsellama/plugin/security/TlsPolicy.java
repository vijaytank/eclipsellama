package com.eclipsellama.plugin.security;

import java.net.URL;

/**
 * TLS enforcement policy for outbound remote connections (MCP SSE / Streamable
 * HTTP, provider endpoints). Pure logic — unit-testable without Eclipse.
 *
 * <p>
 * When TLS enforcement is enabled, remote endpoints must use the {@code https:}
 * scheme. {@code localhost}/{@code 127.0.0.1}/LAN hosts stay permitted over
 * plain HTTP (local Ollama etc. are not considered remote attack surfaces).
 */
public final class TlsPolicy {

	private static final String HTTPS = "https";

	private TlsPolicy() {
		// utility class
	}

	/**
	 * Returns {@code true} if the given endpoint is permitted under the TLS policy.
	 * When {@code enforceTls} is false every URL scheme is allowed (policy
	 * disabled). When true, non-loopback/private hosts must use {@code https}.
	 *
	 * @param url        the endpoint URL, must not be null.
	 * @param enforceTls whether TLS enforcement is active.
	 */
	public static boolean isPermitted(URL url, boolean enforceTls) {
		String scheme = url.getProtocol();
		if (scheme == null || scheme.isBlank()) {
			return false;
		}
		if (HTTPS.equalsIgnoreCase(scheme) || !enforceTls) {
			return true; // policy disabled
		}
		// Enforced: plain HTTP only allowed for non-remote hosts (any other
		// scheme, e.g. ftp, is never permitted)
		return "http".equalsIgnoreCase(scheme) && isLocalHostOrPrivate(url.getHost());
	}

	/** True for localhost / loopback / RFC1918 + link-local private hosts. */
	public static boolean isLocalHostOrPrivate(String host) {
		if (host == null || host.isBlank()) {
			return false;
		}
		String h = host.toLowerCase().trim();
		if ("localhost".equals(h) || h.endsWith(".localhost") || h.equalsIgnoreCase("[::1]")) {
			return true;
		}
		// IPv4 literal? strip brackets
		String ip = h.startsWith("[") && h.endsWith("]") ? h.substring(1, h.length() - 1) : h;
		return "127.0.0.1".equals(ip) || "::1".equals(ip) || ip.startsWith("127.") || ip.startsWith("10.")
				|| ip.startsWith("192.168.") || (ip.startsWith("172.") && isPrivate172(ip));
	}

	private static boolean isPrivate172(String ip) {
		String[] parts = ip.split("\\.");
		if (parts.length < 2) {
			return false;
		}
		try {
			int second = Integer.parseInt(parts[1]);
			return second >= 16 && second <= 31;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}