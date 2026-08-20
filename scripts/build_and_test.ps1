param()
$p2Jars = (Get-ChildItem "C:\Users\Vijay\.p2\pool\plugins" -Filter "*.jar" | Where-Object { $_.Name -notmatch "\.source_" } | Select-Object -ExpandProperty FullName)
$cpList = @("bin", "lib\json-20240303.jar") + $p2Jars
$cpString = $cpList -join [System.IO.Path]::PathSeparator

# Write classpath to argsfile
Set-Content -Path "javac_cp.txt" -Value $cpString

if (!(Test-Path "bin")) {
    New-Item -ItemType Directory -Force "bin" | Out-Null
}

$srcFiles = Get-ChildItem -Recurse -Filter "*.java" "src" | Select-Object -ExpandProperty FullName
Set-Content -Path "sources.txt" -Value ($srcFiles -join "`n")

Write-Host "Compiling src..."
& javac --release 21 -cp "@javac_cp.txt" -d bin "@sources.txt"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Src compilation failed"
    exit 1
}

$testFiles = Get-ChildItem -Recurse -Filter "*.java" "test" | Select-Object -ExpandProperty FullName
Set-Content -Path "tests.txt" -Value ($testFiles -join "`n")

Write-Host "Compiling test..."
& javac --release 21 -cp "@javac_cp.txt" -d bin "@tests.txt"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Test compilation failed"
    exit 1
}

Write-Host "Running JUnit test suite..."
& java -cp "@javac_cp.txt" org.junit.runner.JUnitCore com.eclipsellama.plugin.AllTests

Remove-Item -Force "javac_cp.txt", "sources.txt", "tests.txt" -ErrorAction SilentlyContinue
