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

## 커밋 메시지 (필수)

`deploy.ps1`을 실행하기 **전에** 반드시:

1. `git status`와 `git diff` (및 필요 시 `git log -5 --oneline`)로 **이번 배포에 들어갈 변경**을 확인한다.
2. 수정한 내용의 **의도/효과**를 한글로 1줄 커밋 메시지로 정한다.
   - 좋음: `메인 공지 모달 관리자 ON/OFF 및 admin 그리드 첫 로딩 수정`
   - 나쁨: `deploy: 2026-07-29 16:15`, `수정`, `업데이트`, `작업`
3. **반드시** `-Message "..."` 로 넘긴다. 타임스탬프 기본 메시지에 맡기지 않는다.
4. 커밋할 변경이 없으면(working tree clean) `-Message` 없이 실행해도 된다.

```powershell
Set-Location d:\work\rent
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy.ps1 -Message "여기에 변경 요약"
```

## 동작 순서

1. `git add` (upload/ 제외) → 변경 있으면 `-Message`로 commit → `git push`
2. `mvn/mvnw clean package -DskipTests` (Windows)
3. WSL `Ubuntu-20.04`에서 `/mnt/d/work/rent`로 이동
4. `docker build -t dage5500/rent .`
5. `docker tag dage5500/rent dage5500/rent:latest`
6. `docker push dage5500/rent`

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
