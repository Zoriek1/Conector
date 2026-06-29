@echo off
REM ============================================================================
REM  Sobe o app em modo desenvolvimento, a partir do codigo-fonte (sem rebuild
REM  de imagem Docker). Carrega as variaveis do .env.dev (inclui CRIPTO_KEY, que
REM  decifra as credenciais Pluggy/Cora salvas no banco) e roda via Maven.
REM
REM  Pre-requisito: o banco precisa estar de pe ->  docker compose up -d db
REM  E o container "app" do Docker parado, para liberar a porta 8080 ->
REM      docker compose stop app
REM
REM  Uso:  dev.cmd
REM ============================================================================
setlocal

if not exist .env.dev (
    echo [dev] .env.dev nao encontrado; seguindo com os defaults do application.yml.
) else (
    echo [dev] Carregando variaveis de .env.dev...
    for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env.dev") do (
        if not "%%~a"=="" set "%%a=%%b"
    )
)

echo [dev] Iniciando o app em http://localhost:8080/page  (Ctrl+C para parar)
call mvnw.cmd spring-boot:run

endlocal
