# TechStore Monolith --- PRD v1.0

**Status:** Draft para revisão\
**Data:** 17/10/2026\
**Projeto:** Nova implementação independente do TechStore\
**Referência:** https://github.com/Renan01010/e-commerce\
**Arquitetura:** Monólito modular, Spring Boot, arquitetura hexagonal

## 1. Resumo executivo

O TechStore Monolith será uma nova versão do e-commerce TechStore em um
repositório Git independente. A aplicação manterá os fluxos e regras de
negócio relevantes do projeto de referência, mas substituirá os
microsserviços backend por uma única aplicação Spring Boot organizada em
módulos internos.

O produto será uma **vitrine comercial**: Home, catálogo, detalhes,
cadastro/login, conta, carrinho e encaminhamento para contato comercial.
Checkout transacional, pagamentos, pedidos e rastreamento estão fora do
escopo atual.

O projeto também servirá como exercício prático para comparar
microsserviços com um monólito modular. O repositório original
permanecerá intacto.

## 2. Problema e oportunidade

A versão de microsserviços exige múltiplos processos, configurações,
comunicação HTTP e deploys independentes. A nova versão deve reduzir
essa complexidade operacional sem transformar o código em um monólito
desorganizado.

A solução é um backend implantável como uma unidade, com módulos de
domínio separados, interfaces explícitas e dependências controladas.

## 3. Visão, público e proposta de valor

**Visão:** oferecer uma vitrine moderna e responsiva para descobrir
produtos, consultar especificações, manter um carrinho de interesse e
contatar a equipe comercial.

**Público-alvo:** - Visitantes que pesquisam produtos e categorias. -
Clientes que criam uma conta e mantêm seu carrinho. - Pessoas que querem
informações ou orçamento. - Operadores internos, futuramente, caso um
painel administrativo seja aprovado.

**Proposta de valor:** - Catálogo fácil de explorar. - Informações
reais, sem descontos, avaliações ou disponibilidade inventados. - Conta
segura e carrinho associado ao usuário. - Contato comercial claro. -
Execução local documentada e testes automatizados.

## 4. Objetivos e métricas de sucesso

1.  Backend executado como uma única aplicação Spring Boot.
2.  Uma API REST consumida pelo React, sem API Gateway.
3.  PostgreSQL e Flyway para persistência.
4.  Separação modular com arquitetura hexagonal.
5.  Cadastro, autenticação, catálogo, carrinho e contato comercial
    funcionando.
6.  Isolamento dos dados privados por usuário.
7.  Telas principais utilizáveis em desktop e celular.
8.  Aplicação executável localmente seguindo o README.
9.  Testes relevantes executados ou bloqueios explicitamente
    documentados.
10. Nenhuma dependência obrigatória dos microsserviços originais.

## 5. Escopo

### 5.1 Incluído

-   Home e navegação.
-   Catálogo, categorias, busca, filtros, ordenação e paginação.
-   Detalhes e imagens de produtos.
-   Produtos com variantes e atributos técnicos quando necessário.
-   Cadastro, verificação de e-mail, login, logout e perfil.
-   Recuperação e alteração de senha.
-   Carrinho persistente por usuário.
-   Snapshot de preço, quantidades, subtotais e total.
-   Contato comercial ou solicitação de orçamento.
-   Design responsivo e estados de carregamento, vazio e erro.
-   Testes, logs, health check, documentação e configuração local.
-   Um backend Spring Boot e uma instância PostgreSQL.

### 5.2 Fora do escopo

-   Checkout transacional.
-   Pagamentos e armazenamento de dados de cartão.
-   Pedidos, expedição e rastreamento.
-   Reserva ou baixa transacional de estoque.
-   API Gateway ou microsserviços independentes.
-   Kafka, RabbitMQ, Outbox e arquitetura orientada a eventos.
-   Redis, salvo justificativa futura baseada em medição.
-   Aplicativo mobile nativo.
-   Marketplace com múltiplos vendedores.
-   Reviews e avaliações.
-   Painel administrativo completo, compras e gestão de fornecedores.
-   Migração automática dos dados de produção do projeto original.

### 5.3 Regras de escopo

-   O sistema é uma **vitrine comercial**, não uma loja transacional.
-   O carrinho representa interesse e não cria pedido nem reserva
    estoque.
-   O caminho final deve direcionar a contato, WhatsApp/e-mail ou
    solicitação de orçamento, conforme decisão posterior.
-   O catálogo poderá servir produtos de marcenaria, como chapas,
    ferragens, acabamentos e móveis planejados, sem exigir todos os
    atributos para todos os produtos.
-   Nenhuma funcionalidade fora do escopo pode ser incluída sem uma
    feature aprovada.

## 6. Personas e jornadas

### Visitante

1.  Abre a Home.
2.  Explora categorias ou pesquisa.
3.  Filtra e ordena o catálogo.
4.  Consulta um produto.
5.  Adiciona ao carrinho ou inicia contato comercial.

### Cliente registrado

1.  Cria conta e confirma e-mail.
2.  Faz login.
3.  Explora produtos.
4.  Adiciona, altera ou remove itens do carrinho.
5.  Revisa os valores informativos.
6.  Envia interesse comercial.
7.  Gerencia perfil e segurança.

### Recuperação de conta

1.  Informa e-mail.
2.  Recebe resposta genérica que não revela a existência da conta.
3.  Usa token temporário de uso único.
4.  Define nova senha.
5.  Sessões antigas são invalidadas conforme política documentada.

## 7. Requisitos funcionais

### 7.1 Home e navegação

-   **FR-HOME-001:** Home com identidade visual consistente.
-   **FR-HOME-002:** Categorias e produtos reais obtidos da API.
-   **FR-HOME-003:** Conteúdo editorial não pode inventar preço,
    desconto, avaliação ou disponibilidade.
-   **FR-HOME-004:** Header com acesso à Home, catálogo, busca, conta e
    carrinho.
-   **FR-HOME-005:** Contador do carrinho sincronizado com estado
    confirmado pelo backend.
-   **FR-HOME-006:** Estados de carregamento, erro, tentar novamente e
    ausência de resultados.

### 7.2 Catálogo

-   **FR-CAT-001:** Listar produtos ativos/publicáveis.
-   **FR-CAT-002:** Consultar detalhes por ID ou slug conforme contrato.
-   **FR-CAT-003:** Busca textual.
-   **FR-CAT-004:** Filtro por categoria.
-   **FR-CAT-005:** Filtros de preço, marca e disponibilidade quando
    suportados pelos dados.
-   **FR-CAT-006:** Ordenação por nome, preço, data ou relevância
    conforme contrato.
-   **FR-CAT-007:** Paginação com limite máximo.
-   **FR-CAT-008:** Exibir nome, marca quando houver, preço, imagem e
    disponibilidade real.
-   **FR-CAT-009:** Placeholder consistente para produto sem imagem.
-   **FR-CAT-010:** Não mostrar reviews, preço anterior ou selo
    promocional sem dados reais.

### 7.3 Produtos, variantes e marcenaria

-   **FR-PROD-001:** Produto com ID estável, SKU, nome, slug, descrição,
    estado e timestamps.
-   **FR-PROD-002:** SKU e slug únicos conforme modelo.
-   **FR-PROD-003:** Suportar múltiplas imagens ordenadas e imagem
    principal.
-   **FR-PROD-004:** Suportar categoria e, se necessário, subcategorias.
-   **FR-PROD-005:** Marca opcional.
-   **FR-PROD-006:** Variantes podem possuir SKU, preço, disponibilidade
    e atributos próprios.
-   **FR-PROD-007:** Atributos técnicos tipados e unidades quando
    aplicável.
-   **FR-PROD-008:** Diferenciar estoque não controlado de estoque igual
    a zero.
-   **FR-PROD-009:** Para marcenaria, suportar atributos como material,
    espessura, largura, comprimento, acabamento, cor e unidade de venda.
-   **FR-PROD-010:** Não criar dezenas de colunas específicas de
    marcenaria na tabela-base de todos os produtos.

### 7.4 Identidade e conta

-   **FR-AUTH-001:** Cadastro com nome, e-mail, senha e confirmação de
    senha na interface.
-   **FR-AUTH-002:** Validar formato do e-mail, senha e unicidade.
-   **FR-AUTH-003:** Armazenar somente hash seguro da senha.
-   **FR-AUTH-004:** Registro público sempre cria papel `USER`; cliente
    não escolhe role.
-   **FR-AUTH-005:** Verificação de e-mail com token aleatório, uso
    único, expiração e armazenamento seguro.
-   **FR-AUTH-006:** Login respeita estado da conta e política de
    verificação.
-   **FR-AUTH-007:** Reenvio de confirmação invalida token anterior.
-   **FR-AUTH-008:** Recuperação usa resposta genérica para evitar
    enumeração de contas.
-   **FR-AUTH-009:** Token de reset é de uso único, armazenado como hash
    e com expiração.
-   **FR-AUTH-010:** Alteração/reset de senha invalida sessões antigas
    conforme mecanismo testável.
-   **FR-AUTH-011:** Consultar e atualizar perfil.
-   **FR-AUTH-012:** Alterar senha autenticado exige validação da senha
    atual.
-   **FR-AUTH-013:** Logout limpa o estado do cliente; semântica de
    revogação no servidor deve ser explícita.
-   **FR-AUTH-014:** Mensagens de autenticação não revelam dados
    desnecessários sobre existência de conta.
-   **FR-AUTH-015:** Logs nunca registram senhas, JWTs ou tokens de
    confirmação/reset.

### 7.5 Carrinho

-   **FR-CART-001:** Cada usuário autenticado possui carrinho próprio.
-   **FR-CART-002:** Bloquear acesso ao carrinho de outro usuário.
-   **FR-CART-003:** Adicionar item existente consolida quantidades.
-   **FR-CART-004:** Quantidade deve ser inteira e maior que zero.
-   **FR-CART-005:** Limite de quantidade configurável.
-   **FR-CART-006:** Operação inválida não pode alterar parcialmente o
    carrinho.
-   **FR-CART-007:** Registrar snapshot de preço segundo regra definida.
-   **FR-CART-008:** Subtotal = preço registrado × quantidade.
-   **FR-CART-009:** Total = soma dos subtotais conhecidos; se houver
    preço desconhecido, informar indisponibilidade em vez de usar zero.
-   **FR-CART-010:** Permitir alteração, remoção e limpeza.
-   **FR-CART-011:** Mudança de preço no catálogo não altera
    silenciosamente snapshot existente.
-   **FR-CART-012:** Carrinho não reserva nem baixa estoque.
-   **FR-CART-013:** Backend é fonte da verdade para quantidades,
    preços, subtotais e total.
-   **FR-CART-014:** Após mutações, frontend sincroniza com
    resposta/consulta oficial.

### 7.6 Interesse comercial

-   **FR-COM-001:** Oferecer caminho claro para contato comercial.
-   **FR-COM-002:** Permitir iniciar contato a partir de produto ou
    carrinho.
-   **FR-COM-003:** Coletar apenas dados necessários: nome, e-mail,
    telefone opcional, mensagem e itens de interesse.
-   **FR-COM-004:** Se persistida, solicitação recebe ID, data, estado
    inicial e validação.
-   **FR-COM-005:** Solicitação não é pedido de compra.
-   **FR-COM-006:** Informar se o usuário será encaminhado a canal
    externo ou se a solicitação foi registrada.
-   **FR-COM-007:** Não cobrar, autorizar pagamento, reservar estoque ou
    criar pedido.

### 7.7 UX e acessibilidade

-   **FR-UX-001:** Telas utilizáveis em desktop, tablet e celular.
-   **FR-UX-002:** Formulários com labels, erros compreensíveis e
    indicação de campos inválidos.
-   **FR-UX-003:** Loading impede envios duplicados.
-   **FR-UX-004:** Navegação por teclado e foco visível.
-   **FR-UX-005:** Sistema visual consistente.
-   **FR-UX-006:** Viewports de referência 375 px e 1280 px, além de
    validação em navegador real para layout/overflow.
-   **FR-UX-007:** Não exibir UUIDs internos como conteúdo principal.

## 8. Requisitos não funcionais

### Arquitetura

-   **NFR-ARCH-001:** Uma aplicação Spring Boot implantável.
-   **NFR-ARCH-002:** Sem API Gateway e sem deploy separado por módulo.
-   **NFR-ARCH-003:** Módulos por domínio com limites explícitos.
-   **NFR-ARCH-004:** Arquitetura hexagonal: domain, application e
    adapters.
-   **NFR-ARCH-005:** Comunicação interna por interfaces/casos de uso
    Java, nunca HTTP.
-   **NFR-ARCH-006:** Evitar dependências circulares.
-   **NFR-ARCH-007:** Cada módulo controla suas regras e acesso às
    tabelas.
-   **NFR-ARCH-008:** `shared` deve ser mínimo.

### Stack proposta

-   Java 21, Spring Boot, Maven.
-   Spring Web MVC, Spring Security, Spring Data JPA.
-   PostgreSQL, Flyway, Bean Validation.
-   React, TypeScript, Vite, React Router.
-   Testes com JUnit e ferramentas compatíveis; frontend com
    Vitest/React Testing Library conforme estrutura.
-   Docker Compose para ambiente local, quando disponível.
-   Fixar versões nos arquivos de dependências e documentá-las.

### Segurança

-   Segredos via variáveis de ambiente/secret manager.
-   Hash de senha forte.
-   Tokens aleatórios, persistidos como hash, com expiração e uso único.
-   Autorização validada no backend.
-   Isolamento por usuário.
-   Limites de entrada, paginação e quantidade.
-   CORS restrito.
-   HTTPS em produção.
-   Logs sanitizados.
-   Avaliar rate limiting em endpoints sensíveis; se não estiver na
    primeira versão, registrar como risco.
-   Testes de acesso não autorizado e revogação.

### Persistência

-   PostgreSQL.
-   Flyway para versionamento.
-   Nunca editar migration já aplicada.
-   Constraints e índices para regras importantes.
-   Transações locais quando necessário.
-   Dinheiro com `NUMERIC/DECIMAL`, nunca `float/double`.
-   Evitar persistir totais derivados sem necessidade.

### Operação

-   Health check.
-   Logs com correlation ID.
-   Erros HTTP padronizados.
-   Métricas essenciais.
-   Configuração por ambiente.
-   README com execução, testes e troubleshooting.
-   Sem necessidade de tracing distribuído nesta fase.

### Desempenho

-   Paginação obrigatória.
-   Limite máximo de page size.
-   Índices para filtros frequentes.
-   Evitar N+1 queries.
-   Carregamento eficiente de imagens.
-   Não introduzir cache distribuído sem medição.
-   Definir metas de latência após medir o ambiente real.

## 9. Modelo de dados conceitual

O modelo é uma proposta de domínio. Cada feature deve criar apenas as
tabelas necessárias. O desenho final depende das decisões de variantes,
estoque e contato.

### Identity

**`users`** - `id UUID PK` - `name VARCHAR(120)` -
`email VARCHAR(255) NOT NULL` - `password_hash VARCHAR(255) NOT NULL` -
`role VARCHAR(30) NOT NULL DEFAULT 'USER'` -
`active BOOLEAN NOT NULL DEFAULT TRUE` -
`email_verified BOOLEAN NOT NULL DEFAULT FALSE` -
`token_version INTEGER NOT NULL DEFAULT 0` ou mecanismo equivalente -
`created_at`, `updated_at TIMESTAMPTZ` - índice único para e-mail
normalizado

**`email_verification_tokens`** - `id UUID PK`, `user_id`,
`token_hash UNIQUE`, `expires_at`, `used_at NULL`, `created_at`

**`password_reset_tokens`** - Estrutura semelhante, com uso único e
expiração.

**`user_addresses`** - Opcional; criar apenas se endereços forem
realmente necessários nesta fase. Não coletar endereço por antecipação
enquanto frete/entrega estiverem fora do escopo.

### Catalog

**`categories`** - `id UUID PK`, `name`, `slug UNIQUE`,
`description NULL`, `parent_id NULL`, `display_order`, `active`,
timestamps.

**`brands`** - `id UUID PK`, `name`, `slug UNIQUE`, `active`.

**`products`** - `id UUID PK`, `category_id`, `brand_id NULL`,
`sku UNIQUE`, `slug UNIQUE`, `name`, `description NULL`,
`price NUMERIC(12,2)`, `status`, `stock_tracking_enabled`,
`stock_quantity NULL`, timestamps.

**`product_images`** - `id UUID PK`, `product_id`, `image_url`,
`alt_text NULL`, `display_order`, `is_primary`.

**`product_variants`** - `id UUID PK`, `product_id`, `sku UNIQUE`,
`name NULL`, `price NUMERIC(12,2)`, `active`, `stock_tracking_enabled`,
`stock_quantity NULL`, timestamps.

**Atributos técnicos, se necessários** - `product_attributes`: nome,
tipo, unidade e validações. - `variant_attribute_values`: valor tipado
vinculado à variante. - `units_of_measure`: código, nome e símbolo, se a
variedade de unidades justificar a tabela.

O planejamento deve definir se preço/SKU pertencem ao produto ou
variante; não manter duas fontes de verdade sem regra explícita.

### Cart

**`carts`** - `id UUID PK`, `user_id UUID UNIQUE`, timestamps.

**`cart_items`** - `id UUID PK`, `cart_id`, referência a
produto/variante, `quantity INTEGER CHECK (quantity > 0)`,
`unit_price NUMERIC(12,2) NULL`, `price_snapshot_status`, timestamps. -
Restrição única por carrinho e produto/variante. - Não substituir preço
desconhecido por zero.

### Commercial (se for persistido)

**`contact_requests`** - `id UUID PK`, `user_id NULL`, `name`, `email`,
`phone NULL`, `message`, `status`, `created_at`.

**`contact_request_items`** - `id UUID PK`, `contact_request_id`,
`product_id NULL`, `variant_id NULL`, `product_name_snapshot`,
`sku_snapshot NULL`, `quantity CHECK (quantity > 0)`.

### Integridade

-   E-mail, SKU e slug únicos onde aplicável.
-   Quantidade positiva e preço não negativo.
-   Tokens de uso único.
-   Chaves estrangeiras e política explícita de exclusão.
-   Produto inativo não pode ser adicionado como item novo.
-   Desativação de produto deve considerar referências existentes.
-   Atualizações do carrinho devem ser transacionais.

## 10. Arquitetura lógica

``` text
React + TypeScript
       | HTTP/JSON
       v
Spring Boot Monolith
  ├── identity
  │    ├── domain
  │    ├── application
  │    └── adapters (web, persistence, email)
  ├── catalog
  │    ├── domain
  │    ├── application
  │    └── adapters (web, persistence)
  ├── cart
  │    ├── domain
  │    ├── application
  │    └── adapters (web, persistence)
  ├── commercial
  │    ├── domain
  │    ├── application
  │    └── adapters
  └── shared (mínimo)
       |
       v
PostgreSQL + Flyway
```

### Regras de dependência

-   Domain não depende de Spring MVC, JPA ou infraestrutura.
-   Application contém casos de uso e portas.
-   Adaptadores web convertem HTTP em chamadas de aplicação.
-   Adaptadores de persistência implementam portas.
-   Adaptador de e-mail implementa porta de envio.
-   Cart pode consultar uma interface pública do catálogo; não deve
    acessar diretamente seus repositórios.
-   Não compartilhar entidades JPA entre módulos.
-   Usar modelos/DTOs próprios nas fronteiras internas quando
    necessário.
-   Não criar chamadas HTTP entre módulos.

### Endpoints externos sugeridos

-   `/api/auth/**`
-   `/api/users/**`
-   `/api/categories/**`
-   `/api/products/**`
-   `/api/cart/**`
-   `/api/contact-requests/**`

O contrato definitivo será especificado por feature e documentado em
OpenAPI.

## 11. E-mail e notificações

-   Usar provedor SMTP/API externo.
-   Credenciais por variáveis de ambiente.
-   Remetente configurável.
-   Tokens nunca aparecem em logs.
-   Falha no envio não pode marcar conta como verificada.
-   Reenvio substitui token anterior.
-   Testes usam adaptador falso.
-   Caixa de e-mail local pode ser adicionada se aprovada para
    desenvolvimento.

## 12. Design e telas

Telas principais: 1. Home. 2. Catálogo/todos os produtos. 3. Categoria.
4. Detalhe de produto. 5. Carrinho. 6. Login. 7. Cadastro. 8.
Verificação/reenvio de e-mail. 9. Recuperação de senha. 10. Redefinição
de senha. 11. Perfil/minha conta. 12. Segurança/alterar senha. 13.
Contato/solicitação comercial. 14. Erros e indisponibilidade.

Diretrizes: - Design consistente, dark/tech ou identidade visual
aprovada. - Cards de produto compactos o suficiente para boa densidade
de catálogo. - Imagens com proporções consistentes e placeholders. - Sem
UUIDs como conteúdo visível. - Sem botões de checkout/pagamento. -
Deixar claro que carrinho é de interesse, não compra concluída. -
Validar layout em navegador real; JSDOM não prova ausência de overflow.

## 13. Testes

### Backend

-   Unitários de domínio e casos de uso.
-   Controllers e validação.
-   Persistência em PostgreSQL/Testcontainers.
-   Migrations em banco limpo.
-   Autenticação, autorização e isolamento.
-   Revogação após alteração/reset de senha.
-   Tokens de uso único e expiração.
-   Consolidação, limite, snapshots e concorrência do carrinho.
-   Integridade de catálogo/variantes.
-   Solicitação comercial.
-   Conformidade OpenAPI.

### Frontend

-   Componentes e formulários.
-   Estados de loading/erro/vazio.
-   Navegação.
-   Integração com API.
-   Sincronização do carrinho.
-   Viewports de referência.
-   Browser real para layout e jornadas críticas quando disponível.

### Definition of Done

-   Critérios de aceite satisfeitos.
-   Testes relevantes criados e executados ou bloqueio documentado.
-   Nenhum teste falho escondido.
-   Migrations executadas em banco limpo.
-   Segurança e isolamento verificados.
-   API documentada.
-   UI com estados pertinentes.
-   README atualizado.
-   `git diff --check` limpo.
-   Sem dependências dos microsserviços originais.
-   Tarefas indicam estado real: implementado, executado, bloqueado ou
    pendente.

## 14. Roadmap

### Fase 0 --- Repositório e SpecKit

-   Criar repo independente.
-   Configurar SpecKit.
-   Criar Constitution.
-   Aprovar PRD e convenções.

### Fase 1 --- Monolith Foundation

-   Aplicação Spring Boot única.
-   Maven.
-   PostgreSQL/Flyway.
-   Health check, erros padronizados e correlation ID.
-   Testes básicos.
-   Docker Compose e README.

### Fase 2 --- Catálogo

-   Categorias, produtos, imagens e marcas.
-   Busca, filtros, ordenação e paginação.
-   Variantes/atributos técnicos conforme decisões.
-   Home e catálogo conectados à API.

### Fase 3 --- Identidade

-   Cadastro, login e JWT.
-   Verificação/reenvio.
-   Recuperação/reset.
-   Perfil, alteração de senha e revogação.

### Fase 4 --- Carrinho

-   Persistência por usuário.
-   Adicionar/consolidar, alterar, remover e limpar.
-   Limite configurável.
-   Snapshot, subtotal e total.
-   Estados de preço desconhecido.

### Fase 5 --- Interesse comercial

-   CTA de contato.
-   Solicitação a partir de produto/carrinho.
-   Persistência opcional aprovada.
-   Confirmação e configuração de canal.

### Fase 6 --- Design e validação

-   Unificação visual.
-   Responsividade/acessibilidade.
-   Smoke tests.
-   Revisão de segurança, banco e documentação.

### Fase 7 --- Deploy

-   Ambiente e PostgreSQL gerenciado.
-   Segredos.
-   Migrações e health check.
-   Smoke tests pós-deploy.
-   Monitoramento básico e rollback.

A ordem pode ser ajustada pelo SpecKit se houver dependências
justificadas.

## 15. Riscos e mitigação

  -----------------------------------------------------------------------
  Risco                               Mitigação
  ----------------------------------- -----------------------------------
  Copiar acoplamentos dos             Reutilizar requisitos, reconstruir
  microsserviços                      arquitetura

  Modelo de catálogo insuficiente     Definir variantes e atributos antes
  para marcenaria                     das migrations

  Entidades JPA compartilhadas entre  Interfaces públicas e modelos
  módulos                             próprios

  Inconsistência do carrinho          Transações, constraints e testes de
                                      concorrência

  Revogação JWT incompleta            Mecanismo explícito e testes ponta
                                      a ponta

  Vazamento de tokens                 Hash, expiração, uso único e logs
                                      sanitizados

  Semântica de estoque ambígua        Diferenciar não controlado de zero

  Expansão para checkout sem          Respeitar escopo aprovado
  aprovação                           

  Testes bloqueados por Docker        Separar autoria de teste da
                                      execução

  Dependência do backend antigo       Testar execução sem
                                      Gateway/microsserviços
  -----------------------------------------------------------------------

## 16. Decisões a resolver com `/speckit.clarify`

1.  Produto simples ou variante como unidade comercial?
2.  Quais atributos/unidades de marcenaria são necessários na primeira
    versão?
3.  Preço e SKU pertencem ao produto-base ou à variante?
4.  Haverá controle real de estoque ou apenas disponibilidade comercial?
5.  Contato será redirecionamento para WhatsApp/e-mail ou solicitação
    persistida?
6.  Carrinho exige login ou aceita anônimo com associação posterior?
7.  Qual provedor externo de e-mail será usado?
8.  Qual mecanismo de revogação de JWT será implementado?
9.  Como o catálogo será alimentado inicialmente: seed, importação ou
    painel futuro?
10. Qual plataforma/orçamento de deploy está disponível?

Nenhuma decisão pendente deve ser resolvida por suposição silenciosa.

## 17. Glossário

-   **Monólito modular:** uma aplicação implantável única, separada
    internamente por módulos.
-   **Arquitetura hexagonal:** separação de domínio, casos de uso e
    adaptadores.
-   **Porta:** interface de uma dependência/capacidade.
-   **Adaptador:** implementação concreta, como REST, PostgreSQL ou
    e-mail.
-   **Snapshot de preço:** preço registrado no carrinho sem alteração
    silenciosa pelo catálogo.
-   **Vitrine comercial:** experiência de descoberta/interesse sem
    compra transacional.
-   **Variante:** configuração comercial específica, por exemplo, chapa
    de determinada espessura.

## 18. Próxima ação

Revisar e aprovar este PRD; criar o repositório independente; configurar
a Constitution do SpecKit; então executar a Feature 001 --- Monolith
Foundation pelo fluxo:

`/speckit.specify` → `/speckit.clarify` → `/speckit.plan` →
`/speckit.tasks` → `/speckit.analyze` → `/speckit.implement`

A Feature 001 deve construir a fundação, não implementar antecipadamente
todo o e-commerce.
