# Publicação, arquivos e continuidade

## GitHub

Versione `web`, `api`, `scripts`, `docs` e os arquivos de infraestrutura. `.env`, `.env.local`, `.local`, `.tools`, `node_modules`, `.next` e `target` estão ignorados. Nunca adicione a senha inicial, banco local, capturas privadas ou arquivos enviados. Antes de enviar, revise `git diff --cached` e `git status`. O repositório remoto ainda precisa ser escolhido/criado na conta da proprietária.

## Vercel

1. Conecte o repositório GitHub e selecione Root Directory `web`, framework Next.js.
2. Configure `NEXT_PUBLIC_API_URL` com a URL HTTPS da API. Essa URL é pública; não configure senhas/chaves como `NEXT_PUBLIC_*`.
3. Use `npm ci` e `npm run build`. O lockfile de `web` é versionado.
4. Configure um domínio como `app.seu-dominio` e use `api.seu-dominio` para a API: mantém cookies no mesmo site. A escolha do domínio não foi feita pelo projeto.

## Render / PostgreSQL

1. Crie PostgreSQL com backups e acesso de rede restrito ao serviço da API.
2. Crie Web Service Docker com Root Directory `api`, Dockerfile `Dockerfile`.
3. Configure `DATABASE_URL` em formato JDBC: `jdbc:postgresql://HOST:PORT/BANCO?sslmode=require`. URLs `postgres://...` não são JDBC. Configure usuário e senha separadamente.
4. Configure `INITIAL_PASSWORD` privadamente, `FRONTEND_ORIGIN` com a origem exata HTTPS da interface, `COOKIE_SECURE=true` e `COOKIE_SAME_SITE=lax` para subdomínios do mesmo domínio. Flyway aplica migrações ao iniciar; `/health` é a rota de saúde.
5. Sem domínio comum, os domínios `vercel.app` e `onrender.com` são sites diferentes: seria necessário `COOKIE_SAME_SITE=none`, HTTPS e suporte a cookies de terceiros no navegador. Prefira domínio comum para evitar esse bloqueio. Não desative CSRF.
6. Depois da criação da conta, remova `INITIAL_PASSWORD` do ambiente e altere a senha pela aplicação. A API nunca recria a senha de uma conta existente.
7. Use uma instância inicialmente: o limitador de login é em memória, global por conta. Para escalar, implemente limite compartilhado no banco ou na borda.

Não foi efetuada publicação: faltam projeto/repositório de destino, domínio e credenciais dos provedores. Os scripts locais não são usados na hospedagem.

## Armazenamento privado S3

Configure `S3_ENDPOINT` (se necessário), `S3_REGION`, `S3_BUCKET`, `S3_ACCESS_KEY` e `S3_SECRET_KEY` somente no servidor. O adaptador usa endpoint S3 compatível, path style e criptografia AES256 do lado do servidor. O provedor escolhido precisa suportar isso.

- Crie bucket privado, bloqueie acesso público e ACLs públicas.
- Use identidade de serviço limitada a `PutObject`, `GetObject` e `DeleteObject` no prefixo `private/` do bucket.
- Habilite criptografia padrão e versionamento. Não configure hospedagem de site nesse bucket.
- A interface envia o arquivo para a API. A API detecta o tipo, limita a 10 MB, cria chave aleatória e registra metadados sob a conta autenticada. Downloads passam pela autorização do servidor, sem links públicos.
- O adaptador recusa HTML/SVG/ZIP/executáveis. A detecção do tipo não é antivírus; um scanner de malware pode ser adicionado antes do armazenamento se necessário.
- Banco e S3 não compartilham transação distribuída: a API compensa falha de gravação do banco removendo o objeto recém-enviado. Programe reconciliação periódica de objetos órfãos e metadados sem objeto; não apague objetos por varredura sem conferência.
- Disco temporário da API não é usado como persistência de documentos. Configure retenção e restauração do bucket em conjunto com o banco.

## Cotações e notícias

`EXCHANGE_URL` pode apontar para `https://economia.awesomeapi.com.br/json/last/USD-BRL,EUR-BRL`, conforme o [contrato da AwesomeAPI](https://docs.awesomeapi.com.br/api-de-moedas). O adaptador usa `bid`, `timestamp` e `pctChange`; a data da cotação é distinta da atualização do cache. Avalie limites e termos do provedor antes da produção. Se uma chave for necessária, mantenha-a apenas na configuração do servidor. A URL não é registrada em logs.

`NEWS_RSS_URL` aceita um feed RSS 2.0 escolhido pela proprietária. A leitura XML desabilita DTD e entidades externas. Exibe apenas títulos, fonte, data e links, sem inventar imagens/resumos. Um feed sem data válida não aparece como notícia do dia. O horário de publicação é sempre mostrado. Categorias disponíveis dependem do feed.

## Backups

Defina RPO/RTO antes de uso permanente. Sugestão operacional: backup diário criptografado com 30 dias de retenção e teste mensal de restauração, ajustados à necessidade da proprietária.

1. Ative backups gerenciados/PITR no plano PostgreSQL selecionado. Para backup lógico, use `pg_dump --format=custom --no-owner --file=backup.dump` com conexão por variáveis de ambiente ou arquivo de senha protegido. Não coloque senha na linha de comando.
2. Armazene backups fora do repositório e fora do mesmo serviço de banco. Criptografe e restrinja acesso.
3. Habilite versionamento no bucket, retenção de versões excluídas e, quando disponível, replicação para outro destino privado.
4. Guarde um inventário com data do backup do banco e do armazenamento; documentos dependem de ambos.

### Restauração

1. Pause gravações. Crie banco novo isolado com a mesma versão principal PostgreSQL.
2. Execute `pg_restore --no-owner --dbname=DESTINO backup.dump`, com credenciais fora da linha de comando.
3. Recupere versões dos objetos que correspondam ao inventário. Verifique chaves referenciadas por metadados.
4. Aponte uma instância de homologação para banco e bucket restaurados; valide login, quantidade de registros, totais financeiros, compras parciais e downloads autenticados.
5. Invalide sessões antigas (`DELETE FROM spring_session`), revise migrações e só então troque a conexão de produção.

Para o banco local `.local/postgres`, prefira `pg_dump` enquanto estiver em execução. Não copie os arquivos internos do banco em uso como se fossem backup consistente. Copiar o diretório só é apropriado após parada limpa e com versões compatíveis.
