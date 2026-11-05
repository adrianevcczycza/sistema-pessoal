# Estado de implementação

Esta entrega é uma versão funcional local, com estrutura de produção e vários fluxos integrados. **Não representa a conclusão de todos os detalhes do escopo extenso original.**

## Disponível

- Interface responsiva em português, menu recolhível, temas claros rosa e verde, relógio/fuso, estados vazios e de erro.
- Conta única, BCrypt, sessão HttpOnly, CSRF, CORS restrito, logout, alteração de senha e bloqueio de acessos não autorizados.
- Registros persistidos no PostgreSQL; migrações, versão contra conflitos e histórico de alterações.
- Tarefas com edição/conclusão/reabertura, filtros, calendário diário/semanal/mensal, recorrência gerada ao concluir e lembretes enquanto a aplicação está aberta.
- Finanças com valores decimais, previsto/realizado, contas, categorias, metas, investimentos manuais, parcelas e geração explícita do próximo lançamento mensal. Transferências/aportes não entram nas despesas de consumo.
- Fichas e sessões de treino separadas, com snapshot da ficha; registros alimentares, receitas, ingredientes na lista de compras, água e meta configurável.
- Cadernos/seções/páginas representados nos registros; editor rico, títulos/listas/checklists/tabelas/links/código, busca, favoritos, etiquetas, histórico, exportação textual e salvamento automático de páginas existentes com recuperação de rascunhos.
- Projetos em lista/Kanban por status, checklist e progresso no servidor; biblioteca de prompts, modelos editáveis, copiar e histórico.
- Casa: quantidades próprias/recebidas/compradas, exclusão do cálculo por “Não precisamos”, compras sem duplicação, compra parcial, correção/reversão, despesa vinculada transacional, orçamento e agrupamentos por ambiente/prioridade/período, pesquisas de lojas e comparação de totais.
- Arquivos privados S3 via API, validação de tipo/tamanho, metadados/certificados/carga horária, download autenticado e exclusão confirmada.
- Preferências no servidor e exportação JSON. Adaptadores reais de cotações/RSS com cache persistente e fallback explícito.

## Ainda depende de configuração ou dados

- Importar e conferir a **planilha real de 110 itens**, incluindo a correspondência exata entre abas e links.
- Configurar S3 e verificar upload/download com o provedor real, inclusive recuperação de backups.
- Escolher/configurar fontes de notícias e cotações.
- Publicar no GitHub, Vercel e Render com os domínios e segredos da proprietária.

## Detalhes avançados ainda não concluídos

- Contas financeiras não têm conciliação contábil completa; transferências são registros excluídos do consumo, sem livro-razão entre contas. Categorias são texto, sem catálogo central editável. Visão anual existe na API, ainda sem gráfico completo na interface.
- Fichas de treino usam exercícios em texto, com parâmetros gerais; ainda faltam editor estruturado de cada exercício, execução por série e gráficos de evolução/frequência/medidas.
- Planejamento de refeições é por data; ajuste automático de porções/quantidades, catálogo estruturado de ingredientes e visão semanal dedicada ainda faltam.
- Anexos/imagens privados inseridos diretamente dentro do editor, arrastar páginas entre cadernos e editor hierárquico dedicado ainda faltam. Rascunhos locais não são criptografados e devem ser usados apenas no dispositivo pessoal.
- Projetos têm Kanban por status editável, sem arrastar cartões; vínculos avançados com tarefas/arquivos ainda usam identificadores.
- Prévia de planilha não possui resolução automática de conflitos de registros existentes; revisão é manual. Situações antigas não geram compras automaticamente sem conferir quantidade/origem/valor.
- Comparação de preços é manual. Não existe rastreamento automático de lojas.
- Notícias usam um feed por vez, sem seleção dedicada dos quatro temas, imagens ou resumos licenciados.
- Não há notificações push ou tarefas executadas com o navegador fechado. A recorrência financeira do próximo mês é acionada explicitamente.
- Listas têm carregamento progressivo visual, mas a API ainda retorna o conjunto do módulo; paginação no servidor deve ser adicionada se o volume crescer.

As limitações acima são funcionais, não integrações fictícias. Os controles implementados não substituem a validação final de todos os critérios de aceite antes do uso permanente.
