# TechStore Monolith

Fundação do novo TechStore em um repositório independente: um backend Spring Boot
modular, uma aplicação React/Vite e uma instância PostgreSQL. O antigo repositório de
microsserviços é somente referência de requisitos, contratos e identidade visual; não
é necessário para executar este projeto.

## Pré-requisitos

- Docker Desktop/Engine com Docker Compose v2
- Java 21 e Maven 3.9+ para executar testes do backend fora do Compose
- Node.js 24 LTS e npm 10+ para executar testes do frontend fora do Compose

## Executar localmente

Na raiz do repositório, copie `.env.example` para `.env` e substitua a senha
PostgreSQL por uma senha de desenvolvimento local. O arquivo de exemplo não contém
segredos e nenhum valor local deve ser reutilizado em ambientes compartilhados ou de
produção.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Em macOS/Linux, use `cp .env.example .env`. O Compose executa somente:

- Frontend Vite: <http://localhost:5173>
- Backend Spring Boot: <http://localhost:8080>
- PostgreSQL, acessível aos outros serviços pela rede interna do Compose

O backend espera o health check do PostgreSQL e aplica a migration Flyway inicial
durante a inicialização. A prontidão está disponível em
<http://localhost:8080/actuator/health/readiness>. O frontend mostra o estado da API
usando essa mesma rota através do proxy de desenvolvimento do Vite. Esse proxy não é
um Gateway e não roteia operações de negócio.

Verifique a stack completa com:

```text
node scripts/verify-local-stack.mjs
```

Encerre com `docker compose down`. O volume local do banco é preservado; use
`docker compose down -v` somente quando quiser apagar intencionalmente os dados
locais.

## Testes

Backend:

```text
mvn -f backend/pom.xml verify
```

Os testes de integração PostgreSQL usam Testcontainers e precisam de Docker.

Frontend:

```text
npm --prefix frontend ci
npm --prefix frontend test
npm --prefix frontend run build
```

Com a stack executando, valide o caminho do frontend ao backend:

```text
npm --prefix frontend run test:smoke
```

## Arquitetura

`backend/pom.xml` é o único projeto Maven. `identity`, `catalog`, `cart` e `shared`
são pacotes do mesmo processo, com limites hexagonais; não são serviços ou builds
separados. Cada módulo contém fronteiras de domínio, aplicação e adaptadores.

Os módulos podem invocar casos de uso de outro módulo somente por interfaces Java
explícitas, em processo. Chamadas HTTP internas, dependências circulares e acesso ao
schema de outro módulo são proibidos. A verificação de arquitetura automatizada
protege os limites entre módulos.

O PostgreSQL é uma única base com schemas `identity`, `catalog` e `cart` e um
histórico Flyway compartilhado. A migration inicial cria apenas os schemas, sem
tabelas ou comportamento de negócio. Migrations aplicadas não devem ser alteradas.

Esta fundação não implementa catálogo, autenticação, carrinho, checkout, pagamentos,
pedidos ou rastreamento. Esses fluxos exigem especificações aprovadas antes de sua
implementação. Não há API Gateway, Kafka, Redis ou dependências de serviços do
projeto legado.
