# Git push → Maven package (Windows) → Docker build/tag/push (WSL Ubuntu)
# 사용:
#   .\deploy.ps1                         # git → package → WSL docker push
#   .\deploy.ps1 -Message "메시지"        # 커밋 메시지 지정
#   .\deploy.ps1 -SkipGit                # git 단계 생략
#   .\deploy.ps1 -SkipBuild              # Maven 생략
#   .\deploy.ps1 -SkipPush               # docker push 생략 (build/tag만)

param(
    [switch]$SkipGit,
    [switch]$SkipBuild,
    [switch]$SkipPush,
    [string]$Message = "",
    [string]$Image = "dage5500/rent",
    [string]$WslDistro = "Ubuntu-20.04"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Step($msg) {
    Write-Host ""
    Write-Host "==> $msg" -ForegroundColor Cyan
}

function Fail($msg) {
    Write-Host "ERROR: $msg" -ForegroundColor Red
    exit 1
}

# --- 0) Git commit + push (빌드 전) ---
if (-not $SkipGit) {
    Step "Git: 변경사항 커밋 후 push"

    # upload/ 첨부·암호화 파일은 배포 커밋에 포함하지 않음
    git add -A -- . ":(exclude)upload" ":(exclude)upload/**"
    if ($LASTEXITCODE -ne 0) { Fail "git add 실패" }

    $porcelain = git status --porcelain
    if ($porcelain) {
        if ([string]::IsNullOrWhiteSpace($Message)) {
            Fail "커밋할 변경이 있습니다. -Message `"변경 요약`" 을 지정하세요."
        }
        git commit -m $Message
        if ($LASTEXITCODE -ne 0) { Fail "git commit 실패" }
        Write-Host "committed: $Message" -ForegroundColor Green
    } else {
        Write-Host "커밋할 변경 없음" -ForegroundColor DarkGray
    }

    git push
    if ($LASTEXITCODE -ne 0) { Fail "git push 실패" }
    Write-Host "git push 완료" -ForegroundColor Green
}

# --- 1) Maven package (Windows) ---
if (-not $SkipBuild) {
    Step "Maven clean package (skipTests)"
    if (Test-Path ".\mvnw.cmd") {
        & .\mvnw.cmd clean package -DskipTests
    } else {
        mvn clean package -DskipTests
    }
    if ($LASTEXITCODE -ne 0) { Fail "Maven 빌드 실패" }

    $jar = Get-ChildItem -Path ".\target" -Filter "rent-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "sources|javadoc" } |
        Select-Object -First 1
    if (-not $jar) { Fail "target/rent-*.jar 없음" }
    Write-Host "JAR: $($jar.FullName)" -ForegroundColor Green
}

# --- 2) Docker build / tag / push (WSL Ubuntu) ---
$drive = $PSScriptRoot.Substring(0, 1).ToLower()
$wslPath = "/mnt/$drive" + ($PSScriptRoot.Substring(2) -replace "\\", "/")

$dockerPushCmd = if ($SkipPush) { "echo skip docker push" } else { "docker push $Image" }

# CRLF가 WSL bash에 들어가면 set/cd가 깨지므로 LF 스크립트 파일로 실행
$bashLf = @"
set -e
cd '$wslPath'
echo '==> docker build -t $Image .'
docker build -t $Image .
echo '==> docker tag $Image ${Image}:latest'
docker tag $Image ${Image}:latest
echo '==> $dockerPushCmd'
$dockerPushCmd
echo DONE
"@
$bashLf = $bashLf -replace "`r`n", "`n" -replace "`r", "`n"

$winScript = Join-Path $env:TEMP "rent-deploy-docker.sh"
[System.IO.File]::WriteAllText($winScript, $bashLf, [System.Text.UTF8Encoding]::new($false))

$scriptDrive = $winScript.Substring(0, 1).ToLower()
$wslScript = "/mnt/$scriptDrive" + ($winScript.Substring(2) -replace "\\", "/")

Step "WSL ($WslDistro) docker build / tag / push"
Write-Host "WSL path: $wslPath"
Write-Host "WSL script: $wslScript"

wsl.exe -d $WslDistro -- bash "$wslScript"
if ($LASTEXITCODE -ne 0) { Fail "WSL docker 단계 실패 (exit=$LASTEXITCODE)" }

Write-Host ""
Write-Host "완료. 서버에서 pull 후 재기동하면 반영됩니다." -ForegroundColor Green
Write-Host "  docker pull $Image"
Write-Host "  docker rm -f dage_rent"
Write-Host "  docker run --name dage_rent -d -p 8080 -e VIRTUAL_HOST=rent.dage.co.kr -v rent-file:/upload $Image"
