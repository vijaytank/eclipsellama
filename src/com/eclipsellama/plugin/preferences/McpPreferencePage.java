package com.eclipsellama.plugin.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.json.JSONArray;
import org.json.JSONObject;

import com.eclipsellama.plugin.mcp.McpConnection;
import com.eclipsellama.plugin.mcp.McpConnectionManager;
import com.eclipsellama.plugin.mcp.McpServerConfig;
import com.eclipsellama.plugin.mcp.StreamableHttpMcpConnection;

public class McpPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	/** All configured MCP servers */
	private final List<McpServerConfig> servers = new ArrayList<>();

	/** TableViewer that shows the list */
	private TableViewer viewer;

	/** Action buttons (selection-based). */
	private Button btnAdd, btnEdit, btnRemove, btnTest, btnDisconnect;

	/* ----------------------------------------------------------- */
	@Override
	public void init(IWorkbench wb) {
		setDescription("Configure MCP (Model Context Protocol) servers");
	}

	/* ----------------------------------------------------------- */
	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		/* ---------- TableViewer (shows servers) ---------- */
		viewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		createColumn(viewer, "Transport", 110, element -> {
			McpServerConfig c = (McpServerConfig) element;
			return c.getTransport() == null ? "(unset)" : c.getTransport();
		});
		createColumn(viewer, "Target / Command", 300, element -> {
			McpServerConfig c = (McpServerConfig) element;
			if (c.getEndpoint() != null && !c.getEndpoint().isEmpty()) {
				return c.getEndpoint();
			}
			if (c.getCommand() != null && !c.getCommand().isEmpty()) {
				return c.getCommand();
			}
			return "";
		});
		createColumn(viewer, "Auth", 80, element -> {
			McpServerConfig c = (McpServerConfig) element;
			return (c.getBearerToken() != null && !c.getBearerToken().isEmpty()) ? "Bearer" : "None";
		});
		createColumn(viewer, "Tools", 240, element -> {
			McpServerConfig c = (McpServerConfig) element;
			java.util.List<String> tools = c.getTools();
			return (tools == null || tools.isEmpty()) ? "" : String.join(", ", tools);
		});
		createColumn(viewer, "Status", 110, element -> {
			McpServerConfig c = (McpServerConfig) element;
			boolean connected = McpConnectionManager.getInstance().isConnected(c.getId());
			return connected ? "Connected" : "Disconnected";
		});

		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/* ---------- Bottom horizontal action bar ---------- */
		Composite bar = new Composite(container, SWT.NONE);
		bar.setLayout(new GridLayout(5, false));
		bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		btnAdd = addButton(bar, "Add...", () -> addServer());
		btnEdit = addButton(bar, "Edit...", () -> editSelected());
		btnRemove = addButton(bar, "Remove", () -> removeSelected());
		btnTest = addButton(bar, "Test", () -> testSelected());
		btnDisconnect = addButton(bar, "Disconnect", () -> disconnectSelected());

		/* ---------- Hint label ---------- */
		Label hint = new Label(container, SWT.WRAP);
		hint.setText(
				"Supported transports: stdio, sse, http-streamable. Test opens a real connection and runs a JSON-RPC initialize round-trip.");
		hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		/* ---------- Load persisted data ---------- */
		servers.addAll(McpServerStore.load());
		viewer.setInput(servers);
		updateButtonState();

		/* ---------- Refresh button state when selection changes ---------- */
		viewer.addSelectionChangedListener(event -> updateButtonState());

		return container;
	}

	private void createColumn(TableViewer viewer, String title, int width,
			java.util.function.Function<Object, String> textFn) {
		TableViewerColumn col = new TableViewerColumn(viewer, SWT.NONE);
		col.getColumn().setText(title);
		col.getColumn().setWidth(width);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return textFn.apply(element);
			}
		});
	}

	/* ----------------------------------------------------------- */
	/** Helper that creates a Button in a bar and wires a listener */
	private Button addButton(Composite parent, String text, Runnable action) {
		Button b = new Button(parent, SWT.PUSH);
		b.setText(text);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		b.setLayoutData(gd);
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				action.run();
			}
		});
		return b;
	}

	/* ----------------------------------------------------------- */
	private void updateButtonState() {
		boolean sel = !viewer.getStructuredSelection().isEmpty();
		btnEdit.setEnabled(sel);
		btnRemove.setEnabled(sel);
		btnTest.setEnabled(sel);
		btnDisconnect.setEnabled(sel);
	}

	/* ----------------------------------------------------------- */
	private void refreshView() {
		viewer.refresh();
		updateButtonState();
	}

	/* ----------------------------------------------------------- */
	private McpServerConfig selectedConfig() {
		IStructuredSelection sel = viewer.getStructuredSelection();
		if (sel.isEmpty()) {
			return null;
		}
		return (McpServerConfig) sel.getFirstElement();
	}

	/* ----------------------------------------------------------- */
	private void editSelected() {
		McpServerConfig cfg = selectedConfig();
		if (cfg != null) {
			editConfig(cfg);
		}
	}

	/* ----------------------------------------------------------- */
	private void removeSelected() {
		McpServerConfig cfg = selectedConfig();
		if (cfg != null) {
			removeConfig(cfg);
		}
	}

	/* ----------------------------------------------------------- */
	private void testSelected() {
		McpServerConfig cfg = selectedConfig();
		if (cfg != null) {
			testConfig(cfg);
		}
	}

	/* ----------------------------------------------------------- */
	private void disconnectSelected() {
		McpServerConfig cfg = selectedConfig();
		if (cfg != null) {
			disconnectConfig(cfg);
		}
	}

	/* ----------------------------------------------------------- */
	private void addServer() {
		McpServerConfig cfg = new McpServerConfig();
		if (promptAndConfigure(cfg, "Add MCP Server")) {
			try {
				cfg.validate();
			} catch (Exception ex) {
				MessageDialog.openError(getShell(), "Error", ex.getMessage());
				return;
			}
			// Test immediately; only add once it succeeds (or the user chooses to save
			// an unreachable server anyway).
			runConnectionTest(cfg, ok -> {
				if (ok) {
					servers.add(cfg);
					refreshView();
				} else {
					boolean saveAnyway = MessageDialog.openConfirm(getShell(), "Add MCP Server",
							"Connection failed. Save this server anyway?");
					if (saveAnyway) {
						servers.add(cfg);
						refreshView();
					}
				}
			});
		}
	}

	/* ----------------------------------------------------------- */
	private void editConfig(McpServerConfig cfg) {
		if (promptAndConfigure(cfg, "Edit MCP Server")) {
			refreshView();
		}
	}

	/* ----------------------------------------------------------- */
	private void removeConfig(McpServerConfig cfg) {
		if (MessageDialog.openConfirm(getShell(), "Remove", "Remove this server?")) {
			servers.remove(cfg);
			refreshView();
		}
	}

	/* ----------------------------------------------------------- */
	private void disconnectConfig(McpServerConfig cfg) {
		try {
			McpConnectionManager.getInstance().close(cfg.getId());
			refreshView();
			MessageDialog.openInformation(getShell(), "Disconnect", "Disconnected server.");
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Disconnect error",
					e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
		}
	}

	/* ----------------------------------------------------------- */
	private void testConfig(McpServerConfig cfg) {
		try {
			cfg.validate();
		} catch (Exception ex) {
			MessageDialog.openError(getShell(), "Test error", ex.getMessage());
			return;
		}
		runConnectionTest(cfg, ok -> {
		});
	}

	/*
	 * ----------------------------------------------------------- Opens a real
	 * connection, sends a JSON-RPC initialize request and shows the result dialog.
	 * The onResult callback runs on the UI thread after the dialog is closed.
	 * -----------------------------------------------------------
	 */
	private void runConnectionTest(McpServerConfig cfg, java.util.function.Consumer<Boolean> onResult) {
		new Thread(() -> {
			McpConnectionManager manager = McpConnectionManager.getInstance();
			String result;
			boolean ok;
			McpConnection conn = null;
			try {
				conn = manager.getOrCreate(cfg);
				{
					// MCP requires an initialize handshake as the FIRST request; it binds the
					// client session. Sending ping first leaves tools/list without a valid
					// session id on streamable_http servers.
					String initParams = "{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},"
							+ "\"clientInfo\":{\"name\":\"eclipsellama\",\"version\":\"1.0.0\"}}";
					String initResp = sendRpc(conn, "initialize", initParams);
					String initErr = errorMessage(initResp);
					if (initErr != null) {
						throw new IOException(initErr);
					}
					// Fire-and-forget initialized notification (no id, no response).
					conn.writeMessage("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
					// 2. Discover available tools via tools/list.
					String toolsResp = sendRpc(conn, "tools/list");
					String toolsErr = errorMessage(toolsResp);
					if (toolsErr != null) {
						result = "Connected, but tools/list failed: " + toolsErr;
						cfg.setTools(new java.util.ArrayList<>());
					} else {
						cfg.setTools(parseToolNames(toolsResp));
						result = "Connected. Available tools: " + String.join(", ", cfg.getTools());
					}
				}
				ok = true;
			} catch (Exception e) {
				// On failure, release the connection so it does not linger half-open.
				try {
					manager.close(cfg.getId());
				} catch (Exception ignored) {
					// best-effort cleanup
				}
				String detail = (e.getMessage() == null) ? e.getClass().getSimpleName() : e.getMessage();
				// For http-streamable, surface whether a session id was actually bound so an
				// "invalid session id" error can be traced.
				if (conn instanceof StreamableHttpMcpConnection) {
					String sid = ((StreamableHttpMcpConnection) conn).getSessionId();
					detail = detail + " (session id captured: " + (sid == null ? "none" : sid) + ")";
				}
				result = detail;
				ok = false;
			}
			final boolean success = ok;
			final String msg = (result == null) ? "<no response>" : result;
			Display.getDefault().asyncExec(() -> {
				viewer.refresh();
				if (success) {
					MessageDialog.openInformation(getShell(), "Test", msg);
				} else {
					MessageDialog.openError(getShell(), "Test error", "Connection failed: " + msg);
				}
				onResult.accept(success);
			});
		}).start();
	}

	private String sendRpc(McpConnection conn, String method) throws IOException {
		return sendRpc(conn, method, null);
	}

	private String sendRpc(McpConnection conn, String method, String paramsJson) throws IOException {
		String req = buildRequest("eclipsellama-" + System.nanoTime(), method, paramsJson);
		conn.writeMessage(req);
		return conn.readMessage();
	}

	private String buildRequest(String id, String method, String paramsJson) {
		JSONObject o = new JSONObject();
		o.put("jsonrpc", "2.0");
		o.put("id", id);
		o.put("method", method);
		if (paramsJson == null) {
			o.put("params", JSONObject.NULL);
		} else {
			o.put("params", new JSONObject(paramsJson));
		}
		return o.toString();
	}

	private String errorMessage(String response) {
		if (response == null) {
			return "No response from server.";
		}
		try {
			JSONObject obj = new JSONObject(response);
			if (obj.has("error")) {
				JSONObject err = obj.getJSONObject("error");
				return err.optString("message", err.toString());
			}
			return null;
		} catch (Exception e) {
			return "Invalid response from server: " + response;
		}
	}

	private java.util.List<String> parseToolNames(String response) {
		java.util.List<String> tools = new java.util.ArrayList<>();
		if (response == null) {
			return tools;
		}
		try {
			JSONObject obj = new JSONObject(response);
			if (!obj.has("result")) {
				return tools;
			}
			JSONObject result = obj.getJSONObject("result");
			if (!result.has("tools")) {
				return tools;
			}
			JSONArray arr = result.getJSONArray("tools");
			for (int i = 0; i < arr.length(); i++) {
				String name = arr.getJSONObject(i).optString("name", null);
				if (name != null && !name.isEmpty()) {
					tools.add(name);
				}
			}
		} catch (Exception e) {
		}
		return tools;
	}

	/*
	 * ----------------------------------------------------------- Pops up a
	 * transport-aware dialog. For stdio only the command and arguments fields are
	 * shown; for sse / http-streamable only the endpoint and bearer token are
	 * shown. Returns true if the dialog closed with OK.
	 * -----------------------------------------------------------
	 */
	private boolean promptAndConfigure(McpServerConfig cfg, String title) {
		Dialog dialog = new Dialog(getShell()) {
			private Combo tfTransport;
			private Composite endpointTokenComposite;
			private GridData endpointTokenData;
			private Text tfEndpoint;
			private Text tfToken;
			private Composite stdioComposite;
			private GridData stdioData;
			private Text tfCommand;
			private Text tfArguments;

			@Override
			protected Control createDialogArea(Composite parent) {
				Composite area = (Composite) super.createDialogArea(parent);
				area.setLayout(new GridLayout(2, false));

				/* ---- Transport combo ---- */
				new Label(area, SWT.NONE).setText("Transport:");
				tfTransport = new Combo(area, SWT.READ_ONLY);
				tfTransport.setItems(new String[] { "stdio", "sse", "http-streamable" });
				tfTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				if (cfg.getTransport() != null) {
					tfTransport.setText(cfg.getTransport());
				} else {
					tfTransport.select(0);
				}

				/* ---- Endpoint + token (sse / http-streamable) ---- */
				endpointTokenComposite = new Composite(area, SWT.NONE);
				endpointTokenComposite.setLayout(new GridLayout(2, false));
				endpointTokenData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
				endpointTokenComposite.setLayoutData(endpointTokenData);

				new Label(endpointTokenComposite, SWT.NONE).setText("Endpoint:");
				tfEndpoint = new Text(endpointTokenComposite, SWT.BORDER);
				tfEndpoint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				if (cfg.getEndpoint() != null) {
					tfEndpoint.setText(cfg.getEndpoint());
				}

				new Label(endpointTokenComposite, SWT.NONE).setText("Bearer token (optional):");
				tfToken = new Text(endpointTokenComposite, SWT.BORDER);
				tfToken.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				if (cfg.getBearerToken() != null) {
					tfToken.setText(cfg.getBearerToken());
				}

				/* ---- Command + arguments (stdio) ---- */
				stdioComposite = new Composite(area, SWT.NONE);
				stdioComposite.setLayout(new GridLayout(2, false));
				stdioData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
				stdioComposite.setLayoutData(stdioData);

				new Label(stdioComposite, SWT.NONE).setText("Command:");
				tfCommand = new Text(stdioComposite, SWT.BORDER);
				tfCommand.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				if (cfg.getCommand() != null) {
					tfCommand.setText(cfg.getCommand());
				}

				new Label(stdioComposite, SWT.NONE).setText("Arguments:");
				tfArguments = new Text(stdioComposite, SWT.BORDER);
				tfArguments.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				if (cfg.getArguments() != null) {
					tfArguments.setText(cfg.getArguments());
				}

				updateTransportVisibility();
				tfTransport.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						updateTransportVisibility();
						area.layout();
					}
				});

				return area;
			}

			private void updateTransportVisibility() {
				boolean stdio = "stdio".equals(tfTransport.getText().trim());
				// Exclude the hidden composite from the grid so it takes up no vertical
				// space; otherwise setVisible(false) leaves an empty gap in the layout.
				stdioComposite.setVisible(stdio);
				stdioData.exclude = !stdio;
				endpointTokenComposite.setVisible(!stdio);
				endpointTokenData.exclude = stdio;
			}

			@Override
			protected org.eclipse.swt.graphics.Point getInitialSize() {
				org.eclipse.swt.graphics.Point size = super.getInitialSize();
				// Give the dialog a modest width boost so the endpoint/command fields aren't
				// cramped, without making it too wide.
				return new org.eclipse.swt.graphics.Point(Math.max(size.x, 520), size.y);
			}

			@Override
			protected void okPressed() {
				String selectedTransport = tfTransport.getText().trim();
				if (selectedTransport.isEmpty()) {
					MessageDialog.openError(getShell(), "Error", "Transport must be selected.");
					return;
				}
				cfg.setTransport(selectedTransport);

				if ("stdio".equals(selectedTransport)) {
					String command = tfCommand.getText().trim();
					String arguments = tfArguments.getText().trim();
					if (command.isEmpty()) {
						MessageDialog.openError(getShell(), "Error", "Command is required for stdio transport.");
						return;
					}
					cfg.setCommand(command);
					cfg.setArguments(arguments.isEmpty() ? null : arguments);
					cfg.setEndpoint(null);
					cfg.setBearerToken(null);
				} else {
					String endpoint = tfEndpoint.getText().trim();
					if (endpoint.isEmpty()) {
						MessageDialog.openError(getShell(), "Error",
								"Endpoint is required for " + selectedTransport + " transport.");
						return;
					}
					cfg.setEndpoint(endpoint);
					String token = tfToken.getText().trim();
					cfg.setBearerToken(token.isEmpty() ? null : token);
					cfg.setCommand(null);
					cfg.setArguments(null);
				}
				super.okPressed();
			}
		};

		dialog.setBlockOnOpen(true);
		dialog.create();
		if (dialog.getShell() != null) {
			dialog.getShell().setText(title);
		}
		return dialog.open() == Window.OK;
	}

	/*
	 * ----------------------------------------------------------- Serialize the
	 * current list to Eclipse preferences.
	 * -----------------------------------------------------------
	 */
	@Override
	public boolean performOk() {
		McpServerStore.save(servers);
		return super.performOk();
	}

	@Override
	protected void performApply() {
		McpServerStore.save(servers);
		super.performApply();
	}

	@Override
	protected void performDefaults() {
		servers.clear();
		viewer.setInput(servers);
		super.performDefaults();
	}
}