---
name: deploy
description: >-
  rent 프로젝트 git push 후 Maven package, WSL Ubuntu에서 docker build/tag/push 하는 배포 단축 워크플로.
  사용자가 "배포해", "도커 올려", "빌드하고 도커", "deploy", "/deploy", "docker push" 등을
  요청하면 deploy.ps1을 실행한다. 로컬 docker compose 기동은 하지 않는다.
---

# 빌드 & Docker Hub Push (WSL)

사용자가 빌드·도커 배포를 요청하면 **코드를 직접 조립하지 말고** 프로젝트 루트의 `deploy.ps1`을 실행한다.

이 프로젝트는 **로컬 컨테이너를 기동하지 않는다.**  
빌드 전에 **git 커밋(필요 시)+push** 하고, Windows에서 JAR을 만든 뒤 **WSL `Ubuntu-20.04`** 에서 `dage5500/rent` 이미지를 build / tag / push 한다.

## 기본

```powershell
Set-Location d:\work\rent
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy.ps1
```

동작:
1. `git add` (upload/ 제외) → 변경 있으면 commit → `git push`
2. `mvn/mvnw clean package -DskipTests` (Windows)
3. WSL `Ubuntu-20.04`에서 `/mnt/d/work/rent`로 이동
4. `docker build -t dage5500/rent .`
5. `docker tag dage5500/rent dage5500/rent:latest`
6. `docker push dage5500/rent`

커밋 메시지 지정:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy.ps1 -Message "결재 알림/로그인 로그 수정"
```

## 옵션

```powershell
.\deploy.ps1 -SkipGit      # git 단계 생략
.\deploy.ps1 -SkipBuild    # Maven 생략, WSL docker만
.\deploy.ps1 -SkipPush     # docker push 생략 (build/tag만)
```

## 주의

- `block_until_ms`는 git push·빌드·이미지·docker push가 길 수 있으므로 충분히 크게 (예: 600000).
- **docker compose / 로컬 컨테이너 up은 하지 않는다.**
- `upload/` 디렉터리는 git에 올리지 않는다.
- 서버(`dage_rent`) 재기동은 이 스크립트 범위 밖이다. push 후 안내만 한다.
- 실패 시 스크립트 출력만 요약해 보고하고, 임의로 다른 배포 경로로 바꾸지 않는다.
- 터미널 기록 기준 WSL 배포판 이름은 `Ubuntu-20.04`, 이미지는 `dage5500/rent` 이다.
