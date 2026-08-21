#!/usr/bin/env python3
"""
Generate Eclipse p2 update site metadata (content.jar, artifacts.jar)
for EclipseLlama v2.1.0
"""
import os
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from datetime import datetime

REPO_ROOT = "E:/Projects/eclipsellama"
UPDATESITE = os.path.join(REPO_ROOT, "docs", "updatesite")
PLUGINS = os.path.join(UPDATESITE, "plugins")
FEATURES = os.path.join(UPDATESITE, "features")

PLUGIN_JAR = os.path.join(PLUGINS, "com.eclipsellama.plugin_2.1.0.jar")
FEATURE_JAR = os.path.join(FEATURES, "com.eclipsellama.feature_2.1.0.jar")

def run(cmd, cwd=None):
    print(f"$ {cmd}")
    result = subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"  ERROR: {result.stderr}")
        return False
    if result.stdout.strip():
        print(f"  {result.stdout.strip()[:200]}")
    return True

def main():
    # Verify jars exist
    if not os.path.exists(PLUGIN_JAR):
        print(f"Missing: {PLUGIN_JAR}")
        return 1
    if not os.path.exists(FEATURE_JAR):
        print(f"Missing: {FEATURE_JAR}")
        return 1
    
    print(f"Plugin JAR: {os.path.getsize(PLUGIN_JAR)} bytes")
    print(f"Feature JAR: {os.path.getsize(FEATURE_JAR)} bytes")
    
    # Use Eclipse p2 publisher to generate metadata
    # We need an Eclipse installation with p2 publisher app
    eclipse_path = r"C:\Users\Vijay\eclipse\committers-2026-06"
    
    # Generate p2 metadata
    eclipse_exe = os.path.join(eclipse_path, "eclipse.exe")
    updatesite_uri = UPDATESITE.replace(os.sep, "/")
    cmd = f'"{eclipse_exe}" -nosplash -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher -metadataRepository file:///{updatesite_uri} -artifactRepository file:///{updatesite_uri} -source {UPDATESITE} -compress -publishArtifacts'
    
    if not run(cmd):
        print("Failed to generate p2 metadata")
        return 1
    
    # Verify content.jar and artifacts.jar were updated
    for f in ["content.jar", "artifacts.jar"]:
        fp = os.path.join(UPDATESITE, f)
        if os.path.exists(fp):
            print(f"Generated: {fp} ({os.path.getsize(fp)} bytes)")
        else:
            print(f"Missing: {fp}")
            return 1
    
    print("Update site generation complete!")
    return 0

if __name__ == "__main__":
    sys.exit(main())