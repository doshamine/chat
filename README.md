# Проект "Сетевой чат"

## Описание проекта

Проект представляет собой два приложения для обмена текстовыми сообщениями по сети с помощью консоли между двумя и более пользователями. 

**Первое приложение - сервер чата**, ожидает подключения пользователей.

- Установка порта для подключения клиентов осуществляется через [файл настроек](https://github.com/doshamine/chat/tree/master/server/src/main/resources);
- Сервер осуществляет отправку новых сообщений клиентам;
- Сервер фиксирует все отправленные через сервер сообщения с указанием имени пользователя и времени отправки.
- Все основные события в приложении логируются в консоль и файл.

**Второе приложение - клиент чата**, подключается к серверу чата и осуществляет доставку и получение новых сообщений.

- Клиент предлагает выбор имени для участия в чате;
- Настройки приложения указываются в [файле настроек](https://github.com/doshamine/chat/tree/master/client/src/main/resources);
- После ввода имени производится подключение к указанному в настройках серверу;
- Для выхода из чата нужно набрать команду выхода - “/exit”;
- Все основные события в приложении логируются в файл.

## Запуск проекта

- Проверьте параметры сервера в файле [conf.properties](https://github.com/doshamine/chat/blob/master/server/src/main/resources/conf.properties);
- Запустите класс [Server](https://github.com/doshamine/chat/blob/master/server/src/main/java/ru/netology/server/Server.java);
- Проверьте настройки клиента в файле [conf.properties](https://github.com/doshamine/chat/blob/master/client/src/main/resources/conf.properties);
- Запустите класс [Client](https://github.com/doshamine/chat/blob/master/client/src/main/java/ru/netology/client/Client.java).
