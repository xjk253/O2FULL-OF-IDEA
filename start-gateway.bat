@echo off
setlocal enabledelayedexpansion
title BubblePet Gateway Launcher
cd /d "%~dp0gateway"

echo ============================================
echo   BubblePet Gateway Launcher
echo ============================================
echo.

REM ---------- 1. 端口检测 ----------
echo [1/3] 检查 8080 端口...
set "pid_="
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr LISTENING') do (
    set "pid_=%%a"
    goto :portfound
)
:portfound
if defined pid_ (
    echo     [警告] 8080 端口已被占用, PID=!pid_!
    echo     进程信息:
    tasklist /FI "PID eq !pid_!" /NH 2>nul
    echo.
    set /p "kill=    是否杀掉该进程? (Y/N): "
    if /i "!kill!"=="Y" (
        taskkill /PID !pid_! /F >nul 2>&1
        if errorlevel 1 (
            echo     [错误] 杀进程失败, 可能需要管理员权限
            pause
            exit /b 1
        )
        echo     已终止 PID=!pid_!
        timeout /t 1 /nobreak >nul
    ) else (
        echo     用户取消, 退出
        pause
        exit /b 1
    )
) else (
    echo     8080 端口空闲
)
echo.

REM ---------- 2. 设备检测 ----------
echo [2/3] 检查 adb 设备...
set "count=0"
for /f "skip=1 tokens=1,2" %%a in ('adb devices 2^>nul') do (
    if not "%%b"=="" (
        set /a count+=1
        set "dev!count!=%%a"
        set "devstate!count!=%%b"
    )
)

if !count! == 0 (
    echo     [错误] 未检测到任何设备
    echo     请确认: 1^) USB 已连接  2^) USB 调试已开启  3^) 手机已授权
    pause
    exit /b 1
)

set "authcount=0"
for /l %%i in (1,1,!count!) do (
    if "!devstate%%i!"=="device" (
        set /a authcount+=1
        set "authdev!authcount!=!dev%%i!"
    )
)

if !authcount! == 0 (
    echo     [警告] 设备未授权, 请在手机上点击"允许 USB 调试"
    pause
    exit /b 1
)

if !authcount! == 1 (
    set "selected=!authdev1!"
    echo     设备: !selected!
) else (
    echo     检测到多个已授权设备:
    for /l %%i in (1,1,!authcount!) do (
        echo       %%i. !authdev%%i!
    )
    set /p "choice=    请选择设备编号 (1-!authcount!): "
    set "selected=!authdev%choice%!"
    if "!selected!"=="" (
        echo     [错误] 无效选择
        pause
        exit /b 1
    )
    echo     已选择: !selected!
)
echo.

REM ---------- 3. 端口转发 ----------
echo [3/3] 建立端口转发...
adb -s !selected! reverse tcp:8080 tcp:8080
if errorlevel 1 (
    echo     [错误] adb reverse 失败
    pause
    exit /b 1
)
echo     转发已建立: 手机 -^> 电脑 (localhost:8080)
echo.

REM ---------- 4. 启动 ----------
echo ============================================
echo   启动 gateway
echo   (关闭此窗口将停止服务)
echo ============================================
npm run dev

echo.
echo Gateway 已退出
pause