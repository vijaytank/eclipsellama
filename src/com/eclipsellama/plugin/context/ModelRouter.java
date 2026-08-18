package com.eclipsellama.plugin.context;

/**
 * Resolves a model id for a given task from the configured preferences. Falls
 * back to the default model when a per-task override is not configured.
 */
public final class ModelRouter {

	public enum Task {
		EXPLAIN("explain"), FIX("fix"), TEST("test"), DOCUMENT("document"), REFACTOR("refactor"), REVIEW("review"),
		CONVERT("convert");

		private final String key;

		Task(String key) {
			this.key = key;
		}

		public String preferenceKey() {
			return "eclipsellama.model." + key;
		}
	}

	private final java.util.function.Function<String, String> prefReader;
	private final String defaultModel;

	public ModelRouter(java.util.function.Function<String, String> prefReader, String defaultModel) {
		this.prefReader = prefReader;
		this.defaultModel = defaultModel;
	}

	/**
	 * Resolves the model for the given task.
	 *
	 * @return the configured model for the task, or the default model if empty.
	 */
	public String resolve(Task task) {
		String configured = prefReader.apply(task.preferenceKey());
		return (configured == null || configured.isBlank()) ? defaultModel : configured.trim();
	}
}
