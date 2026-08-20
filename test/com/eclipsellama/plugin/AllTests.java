package com.eclipsellama.plugin;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.eclipsellama.plugin.api.LlmProviderRegistryTest;
import com.eclipsellama.plugin.api.RetryPolicyTest;
import com.eclipsellama.plugin.context.CodeContextCollectorTest;
import com.eclipsellama.plugin.context.ModelRouterTest;
import com.eclipsellama.plugin.mcp.McpConnectionManagerTest;
import com.eclipsellama.plugin.mcp.McpProviderTest;
import com.eclipsellama.plugin.mcp.McpServerRegistryTest;
import com.eclipsellama.plugin.mcp.McpTests;
import com.eclipsellama.plugin.search.BraveSearchProviderTest;
import com.eclipsellama.plugin.search.BuiltInSearchProviderTest;
import com.eclipsellama.plugin.search.SearXNGSearchProviderTest;
import com.eclipsellama.plugin.search.SearchPromptBuilderTest;
import com.eclipsellama.plugin.search.SearchProviderRegistryTest;
import com.eclipsellama.plugin.search.WebSearchRouterTest;
import com.eclipsellama.plugin.search.WebSearchServiceTest;
import com.eclipsellama.plugin.storage.ChatHistoryStoreTest;
import com.eclipsellama.plugin.ui.CodeDiffDialogTest;

/**
 * JUnit 4 test suite that aggregates all non-Eclipse-runtime tests. Run via
 * "Run As → JUnit Test" in Eclipse or via a plain JUnit launcher on the
 * classpath.
 *
 * <p>
 * Tests that require the Eclipse OSGi runtime (SWT, IWorkbench, etc.) are
 * excluded from this suite and must be run as "JUnit Plug-in Test".
 * </p>
 */
@RunWith(Suite.class)
@SuiteClasses({
		// Phase 1 – API layer
		LlmProviderRegistryTest.class, RetryPolicyTest.class,

		// Phase 2 – MCP
		McpTests.class, McpConnectionManagerTest.class, McpProviderTest.class, McpServerRegistryTest.class,

		// Phase 3 – Search
		BraveSearchProviderTest.class, BuiltInSearchProviderTest.class, SearchPromptBuilderTest.class,
		SearchProviderRegistryTest.class, SearXNGSearchProviderTest.class, WebSearchRouterTest.class,
		WebSearchServiceTest.class,

		// Phase 4 – Features
		CodeContextCollectorTest.class, ModelRouterTest.class, ChatHistoryStoreTest.class, CodeDiffDialogTest.class,
		com.eclipsellama.plugin.setup.SetupLauncherTest.class, })
public class AllTests {
	// This class intentionally left empty.
	// The @RunWith and @SuiteClasses annotations define the test suite.
}
