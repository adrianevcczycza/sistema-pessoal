$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$toolsDirectory = Join-Path $projectRoot '.tools'
$mavenExecutable = Join-Path $toolsDirectory 'apache-maven-3.9.9/bin/mvn.cmd'
if (-not (Test-Path -LiteralPath $mavenExecutable)) {
  New-Item -ItemType Directory -Force -Path $toolsDirectory | Out-Null
  node -e "const fs=require('fs'),crypto=require('crypto');(async()=>{const u='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip';const r=await fetch(u);if(!r.ok)throw Error('Falha ao baixar Maven');const b=Buffer.from(await r.arrayBuffer());const hash=(await(await fetch(u+'.sha512')).text()).trim().split(/\s/)[0];if(crypto.createHash('sha512').update(b).digest('hex')!==hash)throw Error('Checksum inválido');fs.writeFileSync('.tools/maven.zip',b)})().catch(e=>{console.error(e.message);process.exit(1)})"
  if ($LASTEXITCODE -ne 0) { throw 'Não foi possível preparar o Maven.' }
  Expand-Archive -LiteralPath (Join-Path $toolsDirectory 'maven.zip') -DestinationPath $toolsDirectory -Force
}
$repositoryPath = Join-Path $toolsDirectory 'm2'
& $mavenExecutable "-Dmaven.repo.local=$repositoryPath" -f api/pom.xml test-compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java '-Dexec.mainClass=br.com.meuespaco.LocalDevelopment' '-Dexec.classpathScope=test'
