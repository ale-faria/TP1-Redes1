@echo off
echo Limpando binarios anteriores...
if exist bin rmdir /s /q bin
mkdir bin

echo Compilando...
javac -d bin -sourcepath src src\chat\common\Message.java src\chat\server\ChatServer.java src\chat\server\ClientHandler.java src\chat\client\ChatClient.java

if %errorlevel% == 0 (
    echo.
    echo Compilacao concluida! Binarios em: bin\
    echo.
    echo Para executar:
    echo   Servidor : run_server.bat
    echo   Cliente  : run_client.bat
) else (
    echo.
    echo ERRO na compilacao. Verifique se o JDK esta instalado corretamente.
)
