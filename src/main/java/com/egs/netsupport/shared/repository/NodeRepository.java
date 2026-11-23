package com.egs.netsupport.shared.repository;

import com.egs.netsupport.shared.model.Credentials;
import com.egs.netsupport.shared.model.Device;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class NodeRepository {

    private static final SessionFactory SESSION_FACTORY = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        Properties props = new Properties();
        try (InputStream in = NodeRepository.class.getResourceAsStream("/application.properties")) {
            if (in == null) {
                throw new IllegalStateException("application.properties not found on classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }

        Configuration cfg = new Configuration().configure();
        cfg.setProperty("hibernate.connection.url", resolve(props.getProperty("db.url")));
        cfg.setProperty("hibernate.connection.username", resolve(props.getProperty("db.username")));
        cfg.setProperty("hibernate.connection.password", resolve(props.getProperty("db.password")));
        return cfg.buildSessionFactory();
    }

    /** Подставляет ${VAR} из переменных окружения. */
    private static String resolve(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.startsWith("${") && v.endsWith("}")) {
            String name = v.substring(2, v.length() - 1);
            String env = System.getenv(name);
            if (env == null || env.isBlank()) {
                throw new IllegalStateException("Environment variable " + name + " is not set");
            }
            return env;
        }
        return v;
    }



    private final Credentials defaultCredentials;

    public NodeRepository() {
        this.defaultCredentials = loadCredentials();
    }

    private Credentials loadCredentials() {
        Path path = Path.of("credentials.json");
        if (!Files.exists(path)) {
            return null; // работаем без кредов
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(path.toFile(), Credentials.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load credentials.json", e);
        }
    }

    public Credentials getDefaultCredentials() {
        return defaultCredentials;
    }

    // Все узлы (мониторим всё, что есть в net.devices)
    public List<Device> getMonitoredNodes() {
        try (Session session = SESSION_FACTORY.openSession()) {
            return session.createQuery("from Device", Device.class).list();
        }
    }

    // IP коммутаторов дома: "Финютина 10" -> street='Финютина', house_num='10'
    public List<String> getSwitchIpsForHouse(String houseName) {
        if (houseName == null || houseName.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = houseName.trim();
        int lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace <= 0) {
            // не смогли разобрать — пытаемся хотя бы по house_num
            return querySwitchIps(null, trimmed);
        }
        String street = trimmed.substring(0, lastSpace).trim();
        String houseNum = trimmed.substring(lastSpace + 1).trim();
        return querySwitchIps(street, houseNum);
    }

    private List<String> querySwitchIps(String street, String houseNum) {
        try (Session session = SESSION_FACTORY.openSession()) {
            String hql;
            if (street == null) {
                hql = "select d.ip from Device d " +
                        "where d.type = 'switch' and d.houseNum = :hn and d.ip is not null";
            } else {
                hql = "select d.ip from Device d " +
                        "where d.type = 'switch' and lower(d.street) = lower(:st) " +
                        "and d.houseNum = :hn and d.ip is not null";
            }
            var query = session.createQuery(hql, String.class)
                    .setParameter("hn", houseNum);
            if (street != null) {
                query.setParameter("st", street);
            }
            return query.list();
        }
    }

    // Найти узел по IP
    public Optional<Device> getNodeByIp(String ip) {
        if (ip == null) return Optional.empty();
        try (Session session = SESSION_FACTORY.openSession()) {
            return session.createQuery(
                            "from Device d where d.ip = :ip", Device.class)
                    .setParameter("ip", ip)
                    .uniqueResultOptional();
        }
    }

    // Проставить VLAN устройству (коммутатору) по IP
    public int updateVlan(String ip, Integer vlan) {
        if (ip == null || vlan == null) return 0;
        Session session = SESSION_FACTORY.openSession();
        try {
            session.beginTransaction();
            int updated = session.createMutationQuery(
                            "update Device d set d.vlan = :vlan where d.ip = :ip")
                    .setParameter("vlan", vlan)
                    .setParameter("ip", ip)
                    .executeUpdate();
            session.getTransaction().commit();
            return updated;
        } catch (RuntimeException e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }


    // Закрыть фабрику при остановке приложения (по желанию)
    public static void shutdown() {
        SESSION_FACTORY.close();
    }
}
