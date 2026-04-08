# EclipseLlama Plugin Export and Install Script
# Run this from PowerShell
# Usage: .\scripts\export-and-install.ps1 [eclipse-path]

param(
    [string]$EclipsePath = "C:\Program Files\Eclipse",
    [string]$ExportDir = "$PSScriptRoot\..\export"
)

Write-Host "========================================"
Write-Host "  EclipseLlama Export and Install"
Write-Host "========================================"
Write-Host ""

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$PluginJar = "com.eclipsellama.plugin_2.0.1.jar"

# Step 1: Check if Eclipse export directory exists
Write-Host "--- Step 1: Preparing Export ---"

# Create export directory
if (-not (Test-Path $ExportDir)) {
    New-Item -ItemType Directory -Path $ExportDir -Force | Out-Null
    Write-Host "  Created export directory: $ExportDir"
}

# Step 2: Manual export required
Write-Host ""
Write-Host "--- Step 2: Export Plugin (Manual) ---"
Write-Host "  In Eclipse:"
Write-Host "    1. Right-click 'eclipsellama' project"
Write-Host "    2. Export - Plug-in Development - Deployable plug-ins"
Write-Host "    3. Select 'com.eclipsellama.plugin'"
Write-Host "    4. Destination: Directory - $ExportDir"
Write-Host "    5. Click Finish"
Write-Host ""
Write-Host "  Press Enter when export is complete..."
Read-Host

# Step 3: Verify export
Write-Host ""
Write-Host "--- Step 3: Verify Export ---"

$PluginsDir = Join-Path $ExportDir "plugins"
$ExportedJar = $null

if (Test-Path $PluginsDir) {
    $ExportedJar = Get-ChildItem -Path $PluginsDir -Filter "*.jar" | Select-Object -First 1
}

if ($ExportedJar) {
    Write-Host "  Found exported JAR: $($ExportedJar.Name)"
}
else {
    Write-Host "  ERROR: No JAR found in $PluginsDir"
    Write-Host "  Please export the plugin first."
    exit 1
}

# Step 4: Find Eclipse installation
Write-Host ""
Write-Host "--- Step 4: Locate Eclipse ---"

$DropinsPath = Join-Path $EclipsePath "dropins"

if (-not (Test-Path $EclipsePath)) {
    Write-Host "  Eclipse not found at: $EclipsePath"
    Write-Host "  Looking in common locations..."
    
    $CommonPaths = @(
        "C:\Users\vijay\eclipse\jee-2025-06\eclipse",
        "C:\Program Files\Eclipse",
        "C:\eclipse",
        "E:\eclipse",
        "$env:USERPROFILE\eclipse"
    )
    
    foreach ($path in $CommonPaths) {
        if (Test-Path $path) {
            $EclipsePath = $path
            $DropinsPath = Join-Path $EclipsePath "dropins"
            Write-Host "  Found Eclipse at: $EclipsePath"
            break
        }
    }
}

if (-not (Test-Path $EclipsePath)) {
    Write-Host "  ERROR: Could not find Eclipse installation"
    Write-Host "  Please specify path: .\export-and-install.ps1 'C:\path\to\eclipse'"
    exit 1
}

# Step 5: Install plugin
Write-Host ""
Write-Host "--- Step 5: Install Plugin ---"

# Create dropins if not exists
if (-not (Test-Path $DropinsPath)) {
    New-Item -ItemType Directory -Path $DropinsPath -Force | Out-Null
    Write-Host "  Created dropins folder"
}

# Remove old version if exists
Get-ChildItem -Path $DropinsPath -Filter "com.eclipsellama.plugin*.jar" -ErrorAction SilentlyContinue | Remove-Item -Force

# Copy JAR to dropins
$DestJar = Join-Path $DropinsPath $ExportedJar.Name
Copy-Item -Path $ExportedJar.FullName -Destination $DestJar -Force
Write-Host "  Copied plugin to: $DestJar"

# Step 6: Done
Write-Host ""
Write-Host "========================================"
Write-Host "  Installation Complete!"
Write-Host "========================================"
Write-Host ""
Write-Host "  Next steps:"
Write-Host "    1. Close all Eclipse instances"
Write-Host "    2. Start Eclipse"
Write-Host "    3. Test: Help menu - EclipseLlama Chat"
Write-Host "    4. Test: Ctrl+Shift+L"
Write-Host ""
