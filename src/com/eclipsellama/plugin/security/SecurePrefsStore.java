package com.eclipsellama.plugin.security;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

/**
 * Secure storage migration wrapper around Eclipse's
 * {@code org.eclipse.equinox.security.storage} bundle.
 *
 * <p>
 * Provides encrypted persistence for sensitive values (OpenAI API keys, Brave
 * Search keys, MCP bearer tokens). Secrets are stored with {@code encrypt=true}
 * in the OS-protected secure store, never in plain-text
 * {@code config.properties}.
 *
 * <p>
 * A {@link StoreBackend} seam keeps the class unit-testable without the Eclipse
 * OSGi runtime: production uses the Equinox-backed store, tests inject an
 * in-memory fake.
 */
public final class SecurePrefsStore {

	/** Backend seam — abstracts where secrets actually live. */
	public interface StoreBackend {
		boolean isAvailable();

		void put(String nodePath, String key, String value, boolean encrypt) throws Exception;

		Optional<String> get(String nodePath, String key);

		void removeKey(String nodePath, String key);
	}

	private static final StoreBackend DEFAULT_BACKEND = new EquinoxStoreBackend();

	private static volatile StoreBackend backend = DEFAULT_BACKEND;

	private SecurePrefsStore() {
		// utility class
	}

	/** Visible for tests only — swaps the backend. */
	public static void setBackendForTest(StoreBackend testBackend) {
		backend = (testBackend == null) ? DEFAULT_BACKEND : testBackend;
	}

	public static boolean isAvailable() {
		return backend.isAvailable();
	}

	/** Stores a value in the named node, encrypted for secrets. */
	public static void securePut(String nodePath, String key, String value, boolean encrypt) {
		if (!backend.isAvailable() || value == null) {
			return;
		}
		try {
			backend.put(nodePath, key, value, encrypt);
		} catch (Exception e) {
			log("Failed to store secret under " + nodePath + "/" + key + ": " + e.getMessage());
		}
	}

	/** Stores a value in the named node, encrypted. */
	public static void securePutEncrypted(String nodePath, String key, String value) {
		securePut(nodePath, key, value, true);
	}

	/** Retrieves a stored secret; empty if unavailable/absent. */
	public static Optional<String> secureGet(String nodePath, String key) {
		if (!backend.isAvailable()) {
			return Optional.empty();
		}
		try {
			return backend.get(nodePath, key);
		} catch (Exception e) {
			log("Failed to read secret under " + nodePath + "/" + key + ": " + e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Removes a key (used after successful migration to clear plaintext legacy).
	 */
	public static void secureRemove(String nodePath, String key) {
		if (!backend.isAvailable()) {
			return;
		}
		backend.removeKey(nodePath, key);
	}

	/** Returns the secure value as String or the supplied fallback. */
	public static String secureGetOrDefault(String nodePath, String key, String fallback) {
		return secureGet(nodePath, key).orElse(fallback);
	}

	private static void log(String message) {
		System.err.println("SecurePrefsStore: " + message);
	}

	/**
	 * Plain-JUnit-friendly in-memory backend used when Equinox storage is absent.
	 */
	public static final class InMemoryStoreBackend implements StoreBackend {
		private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> nodes = new ConcurrentHashMap<>();

		@Override
		public boolean isAvailable() {
			return true;
		}

		@Override
		public void put(String nodePath, String key, String value, boolean encrypt) {
			nodes.computeIfAbsent(nodePath, k -> new ConcurrentHashMap<>()).put(key, value);
		}

		@Override
		public Optional<String> get(String nodePath, String key) {
			ConcurrentHashMap<String, String> node = nodes.get(nodePath);
			if (node == null) {
				return Optional.empty();
			}
			return Optional.ofNullable(node.get(key));
		}

		@Override
		public void removeKey(String nodePath, String key) {
			ConcurrentHashMap<String, String> node = nodes.get(nodePath);
			if (node != null) {
				node.remove(key);
			}
		}

		public int depth(String nodePath) {
			ConcurrentHashMap<String, String> node = nodes.get(nodePath);
			return node == null ? 0 : node.size();
		}

		public boolean contains(String nodePath, String key) {
			ConcurrentHashMap<String, String> node = nodes.get(nodePath);
			return node != null && node.containsKey(key);
		}
	}

	/** Default backend delegating to the Eclipse OSGi secure store. */
	private static final class EquinoxStoreBackend implements StoreBackend {
		private volatile ISecurePreferences rootCache;

		private ISecurePreferences root() throws StorageException {
			if (rootCache == null) {
				rootCache = SecurePreferencesFactory.getDefault();
			}
			return rootCache;
		}

		@Override
		public boolean isAvailable() {
			try {
				root();
				return true;
			} catch (Throwable t) {
				log("osgi security store not available: " + t.getMessage());
				return false;
			}
		}

		@Override
		public void put(String nodePath, String key, String value, boolean encrypt) throws StorageException {
			root().node(nodePath).put(key, value, encrypt);
		}

		@Override
		public Optional<String> get(String nodePath, String key) {
			try {
				ISecurePreferences node = root().node(nodePath);
				return Optional.ofNullable(node.get(key, null));
			} catch (Throwable t) {
				log("Could not read " + nodePath + "/" + key + ": " + t.getMessage());
				return Optional.empty();
			}
		}

		@Override
		public void removeKey(String nodePath, String key) {
			try {
				root().node(nodePath).remove(key);
			} catch (Throwable t) {
				log("Could not remove " + nodePath + "/" + key + ": " + t.getMessage());
			}
		}
	}
}