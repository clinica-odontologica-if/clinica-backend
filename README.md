# Clinica Docker

Repositorio preparado apenas com a configuracao de ambiente da sprint.

## Escopo Atual

- Banco MySQL em Docker Compose
- Documentacao da sprint em `docs/sprint 0`
- Sem implementacao de microservicos neste repositorio

## Aderencia ao Documento

O documento descreve uma arquitetura orientada a microsservicos para a clinica odontologica. Neste estado do projeto foi mantida somente a base de ambiente, sem codigo de servicos, para evitar implementacoes parciais fora do escopo desejado.

## Tecnologias Mantidas

- MySQL 8
- Docker
- Docker Compose

## Ambiente

O arquivo `docker-compose.yml` sobe apenas a infraestrutura de banco de dados:

1. `mysql`
   - Banco relacional da aplicacao
   - Porta externa `3306`
   - Volume persistente `mysql_data`
   - Rede `clinica-net`

## Estrutura

```text
Clinica-Odontologica/
|-- docker-compose.yml
|-- README.md
`-- docs/
    `-- sprint 0/
```
