# netsupport

Программа для мониторинга сетевых узлов и диагностики соединений у абонентов.

## Описание

Инструмент для техподдержки интернет-провайдера. Позволяет:
- проверять доступность сетевых узлов (ping);
- находить порт абонента по MAC-адресу через Telnet;
- проверять состояние порта: линк, скорость, ошибки, output drops;
- находить порты в состоянии `err-disabled` с указанием причины;
- хранить список устройств (коммутаторы/узлы) в PostgreSQL;
- веб-интерфейс: диагностика абонента по MAC и просмотр VLAN по домам.
- присылать уведомления о проблемах (в планах).

## Технологии

  - Java 17
  - Maven
  - Spring Boot 3 (web, Spring Data JPA)
  - Hibernate ORM (через Spring Data JPA)
  - PostgreSQL
  - Jackson
  - Apache Commons Net (Telnet)

**В планах:**
- JUnit 5 (тесты)
- Spring Boot (REST API)
- Email / Битрикс24 (уведомления)
- Веб-дашборд

  ## Настройка

  ### База данных

  Устройства хранятся в PostgreSQL в схеме `net`, таблица `devices`
  (сущность `com.egs.netsupport.shared.model.Device`).
  Схема не создаётся автоматически (`ddl-auto=validate`) — она должна уже существовать.

  Параметры подключения задаются в `src/main/resources/application.properties`:properties
  spring.datasource.url=${db.url}
  spring.datasource.username=${DBUSER}
  spring.datasource.password=${DBPASSWORD}

  db.url=jdbc:postgresql://localhost:5432/netsupport
  Логин и пароль берутся из переменных окружения `DB_USER` и `DB_PASSWORD`.
  Для локального запуска их можно задать в Run Configuration IDE (Environment variables) или в системе.

  ### Учётки для Telnet

  Логин/пароль коммутаторов читаются из `credentials.json` в рабочей директории (в репозиторий не входит).

  ### Запускbash
  mvn spring-boot:run
  Приложение поднимается на `http://localhost:8080`.

  ## Веб-интерфейс

  - `/` — главная
  - `/diagnostic.html` — диагностика абонента: выбор города, улицы и дома (оба поля с
    автодополнением, ввод только из списка) и MAC (полный или последние 4 символа)
  - `/errors.html` — поиск err-disabled портов по кнопке: выбор города («Все города») →
    «Найти»; в таблице только коммутаторы с портами в ошибке
  - `/vlan.html` — VLAN по домам: все VLAN'ы, найденные на коммутаторах дома

  ## Мониторинг

  Мониторинг реализован на Spring `@Scheduled` и по умолчанию **выключен**.
  Включается в `application.properties`:

  ```properties
  # Пинг коммутаторов (NodeMonitor)
  app.monitoring.ping.enabled=true
  app.monitoring.ping.interval-ms=30000

  # Периодический поиск err-disabled портов (PortHealthMonitor)
  app.monitoring.errdisabled.enabled=true
  app.monitoring.errdisabled.interval-ms=1800000
  app.monitoring.errdisabled.initial-delay-ms=60000

  # размер пула потоков для параллельного опроса коммутаторов
  app.monitoring.errdisabled.pool-size=20
  ```

  Поиск err-disabled (и по кнопке в `/errors.html`, и в `PortHealthMonitor`) опрашивает
  коммутаторы параллельно в пуле из `pool-size` потоков.

## Структура проекта

```
  com.egs.netsupport
  ├── NetSupportApplication   — Spring Boot точка входа
  ├── web                     — REST API (ApiController), DTO
  ├── config                  — Jackson
  ├── diagnostic              — диагностика абонента
  │   ├── model               — DiagnosticResult, FdbEntry, PortDetails
  │   └── parser              — FdbParser, InterfaceParser, SubscriberVlanParser
  ├── monitoring              — мониторинг узлов, портов, VLAN
  │   ├── NodeMonitor         — @Scheduled пинг коммутаторов (флаг app.monitoring.ping.enabled)
  │   ├── PortHealthMonitor   — @Scheduled поиск err-disabled (флаг app.monitoring.errdisabled.enabled)
  │   ├── PortStatusService   — общий опрос err-disabled (по городу / по списку свичей)
  │   ├── model               — NodeState, PortError, ErrDisabledDto
  │   └── parser              — ErrDisabledParser
  └── shared
      ├── model               — Device, Node, Credentials
      ├── repository          — DeviceRepository (Spring Data JPA)
      └── telnet              — TelnetService
  Статика: `src/main/resources/static/{index,diagnostic,errors,vlan}.html`
```

## Roadmap

### ✅ Сделано
- [x] Структура проекта (Maven, пакеты по фичам)
- [x] Модели данных: `Device`, `Node`, `Credentials`, `FdbEntry`, `PortDetails`, `DiagnosticResult`, `PortError`
- [x] Переход на PostgreSQL: `NodeRepository` работает через Hibernate `SessionFactory` (таблица `net.devices`)
- [x] Чтение `credentials.json`
- [x] Параметры подключения к БД вынесены в `application.properties` (с подстановкой из переменных окружения)
- [x] Логирование через Log4j2 (мост `log4j-jboss-logmanager` для Hibernate)
- [x] `PingService` — пинг через системный `ping` (Windows/Linux)
- [x] `NodeMonitor` — параллельный опрос по расписанию + антифлаппинг
- [x] Вывод статуса узлов в консоль
- [x] `TelnetService` — подключение, логин, выполнение команд, отключение paging (`terminal length 0`)
- [x] `FdbParser` — поиск порта по последним 4 символам MAC
- [x] `InterfaceParser` — парсинг линка, скорости, ошибок, output drops
- [x] `SubscriberDiagnostic` — полная диагностика абонента:
  - поиск MAC на всех коммутаторах дома;
  - фильтрация транковых портов (через `show interfaces status`);
  - фильтрация по трёхзначному VLAN;
  - парсинг `show interface` для найденного порта;
  - подсказка при `NOT FOUND`: проверка `err-disabled` портов на коммутаторах дома.
- [x] `PortStatusService` — универсальный сервис проверки `err-disabled` портов
- [x] `ErrDisabledParser` — парсинг `show interfaces status err-disabled` с причиной
- [x] `PortHealthMonitor` — периодическая проверка `err-disabled` (раз в 30 мин)
- [x] Переход на Spring Boot + Spring Data JPA
- [x] Веб-интерфейс: главная, диагностика абонента, VLAN по домам
- [x] Сборщик VLAN с коммутаторов (`VlanService`)

### 🚧 В работе
- [ ] Тесты на парсеры (JUnit) на сохранённых выводах с реальных коммутаторов

### 📋 Запланировано

**Диагностика абонентов**
- [ ] Проверка статуса порта: `disabled` (вручную выключен) в диагностике
- [ ] Автоматический подъём порта (только после согласования с техдиром)

**Мониторинг**
- [ ] Уведомления (email / Битрикс24)
- [ ] Хранение истории в PostgreSQL
- [ ] Веб-дашборд

**Инвентаризация портов**
- [ ] Проверка портов по VLAN (настроены, но не активны)

**Инфраструктура**
- [ ] Переход с Telnet на SSH (JSch)
- [ ] Docker

### 💡 Идеи на будущее
- [ ] Мобильное приложение / веб-интерфейс для операторов
- [ ] Интеграция с тикет-системой
- [ ] Автоматическое построение карты сети
- [ ] Логирование медленных коммутаторов (для диагностики проблем с сетью)
