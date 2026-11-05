# API REST

Base `/api`. Todas as rotas, exceto `/health`, `/api/auth/csrf` e o processamento de login, exigem sessão autenticada. Mutações exigem o cabeçalho retornado pela rota CSRF. CORS aceita apenas as origens configuradas, com credenciais.

## Autenticação

1. `GET /api/auth/csrf`: cria sessão anônima e retorna `{token, headerName}`. Envie cookies (`credentials: include`).
2. `POST /api/auth/login`: `application/x-www-form-urlencoded`, campos `email` e `password`, mais cabeçalho CSRF. Depois do login, obtenha um novo CSRF.
3. `GET /api/auth/me`: conta atual.
4. `POST /api/auth/logout`: invalida sessão, requer CSRF.
5. `POST /api/auth/password`: JSON `currentPassword`, `newPassword`; invalida as sessões após a alteração.

Sessões persistem em PostgreSQL e expiram após 30 minutos de inatividade. Cookie HttpOnly, Secure por padrão, SameSite configurável. Limitador de 10 tentativas/5 minutos global para a única conta, em memória por instância. Não use múltiplas réplicas sem um limitador compartilhado na borda ou banco.

## Registros e histórico

- `GET /api/records/{module}`: lista da proprietária.
- `POST /api/records/{module}`: criação/edição `{id: UUID, version: número, data: objeto}`. Reutilize o UUID ao repetir uma criação. Ao editar, envie a versão devolvida pelo servidor. Conflito resulta em HTTP 409.
- `DELETE /api/records/{module}/{id}`: exclusão autenticada. Compras/lançamentos vinculados seguem as regras transacionais.
- `GET /api/history/{id}`: versões anteriores da proprietária.
- `GET /api/export`: registros, preferências e metadados, sem bytes de arquivos.

Módulos: `tasks`, `transactions`, `accounts`, `budgets`, `investments`, `workouts`, `sessions`, `meals`, `recipes`, `groceries`, `water`, `notes`, `studySessions`, `projects`, `prompts`, `house`, `purchases`, `comparisons`, `documents` (somente leitura nesta rota).

`data.title` é obrigatório. Limites, datas, links e quantidades são validados no servidor. Valores monetários admitem duas casas decimais; números negativos são rejeitados. `notes.html` passa por sanitização. Os campos dos formulários estão documentados em `web/src/lib/modules.js`. O servidor valida regras críticas sem confiar no navegador.

## Finanças e casa

- `GET /api/summary?month=AAAA-MM`: receitas recebidas, despesas pagas, saldo realizado e despesas previstas. Transferências e aportes não contam como consumo.
- `GET /api/finance/annual?year=AAAA`: 12 resumos mensais.
- `POST /api/finance/installments`: formato de registro, `data.amount` é o total e `data.installments` é a quantidade de parcelas (2–120). Arredondamento residual fica na última parcela. Identificadores determinísticos evitam duplicação.
- `POST /api/finance/recurring/{id}/next`: gera o próximo mês, sem duplicar. A recorrência financeira é explicitamente acionada, não um agendamento em segundo plano.
- `GET /api/house/summary`: itens com disponíveis, comprados, faltantes, situação, progresso e totais calculados.
- `GET /api/house/analytics`: orçamento disponível e agrupamentos por ambiente, prioridade e período.
- Compras: `POST /api/records/purchases` com `houseId`, `quantity`, `amount` (itens), `freight`, `date`, `createExpense`. Quantidade não pode exceder faltantes. Edição e exclusão recalculam quantidades e despesa vinculada na mesma transação.
- Próprios/presentes: campos `owned`/`gifted` do item; não geram despesas. `notNeeded=true` exclui do denominador.

## Preferências, arquivos e fontes

- `GET/PUT /api/preferences`: tema (`rose`/`sage`), fuso IANA, meta de água, orçamento da casa e cartões ordenados.
- `GET /api/documents/status`: configuração do armazenamento privado.
- `POST /api/documents`: multipart `id`, `metadata` (JSON), `file`. PDF, PNG, JPEG e WebP reais, no máximo 10 MB; tipo detectado por Tika.
- `PUT /api/documents/{id}`: formato de registro para atualizar metadados; chave do objeto não é editável.
- `GET /api/documents/{id}/download`: autorização por proprietária; bytes transmitidos pela API, sem URL pública permanente e com `Cache-Control: no-store`.
- `DELETE /api/documents/{id}`: apaga objeto privado e metadados.
- `GET /api/external/exchange` e `/news`: status `available`, `stale`, `not_configured` ou `unavailable`, dados originais e atualização do cache. Cache persistente de 30 minutos; falhas preservam último resultado e limitam novas tentativas.
- `POST /api/house/import/preview`: multipart `.xlsx`.
- `POST /api/house/import/confirm`: array de registros novos confirmados; uma colisão aborta a transação.

Erros compreensíveis têm `{message}` e status 400/401/403/404/409/413/429/503. Não são devolvidos detalhes internos de banco, chaves ou stack traces.
