package com.irkut.tc.io.session;

import com.irkut.tc.io.ConnectionProperties;
import com.teamcenter.schemas.soa._2006_03.exceptions.InvalidCredentialsException;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.bom.StructureManagementService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.services.strong.core._2006_03.Session;
import com.teamcenter.soa.SoaConstants;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.strong.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.Objects;

/**
 * Менеджер сессий для подключения к Teamcenter и управления сеансами пользователей.
 */
@Getter
@RequiredArgsConstructor(onConstructor_=@Autowired)
@Component
public class TCSession {

    private final Logger logger = LoggerFactory.getLogger(TCSession.class);
    private final ConnectionProperties connectionProperties;
    @Getter
    private Connection connection;
    @Getter
    private SessionService sessionService;
    @Getter
    private DataManagementService dataManagementService;
    @Getter
    private StructureManagementService bomService;
    @Getter
    private FileManagementUtility fileManagementUtility;
    @Getter
    private com.teamcenter.services.strong.cad.StructureManagementService structService;
    @Getter
    private User currentUser;

    /**
     * Инициализирует TCSession путем создания соединения, входа в систему и управления сеансами пользователя.
     */
    @PostConstruct
    public void initialize() {
        try {
            establishConnection();
            login();
            initializeServices();
        } catch (Exception exception) {
            logger.error("Error initializing TCSession", exception);
            throw new RuntimeException(exception);
        }
    }

    /**
     * Инициирует соединение на основе свойств подключенияю
     */
    private void establishConnection() {
        if(connectionProperties == null){
            logger.info("Connection properties is null");
        }
        assert connectionProperties != null;
        String protocol = getProtocol(connectionProperties.getHost());
        connection = new Connection(
                connectionProperties.getHost(),
                new CredentialManagerImpl(),
                SoaConstants.REST, protocol);
        connection.setExceptionHandler(new CustomExceptionHandler());
        connection.getModelManager().addPartialErrorListener(new CustomPartialErrorListener());
        connection.getModelManager().addModelEventListener(new CustomModelEventListener());
    }

    /**
     * Определяет протокол на основе учетных данных хоста, указанных в свойствах подключения.
     * @param host Адрес хоста для подключения.
     * @return Протокол, который будет использоваться для подключения.
     */
    private String getProtocol(String host) {
        if (host.startsWith("http")) {
            return SoaConstants.HTTP;
        } else if (host.startsWith("tccs")) {
            return SoaConstants.TCCS;
        } else {
            return SoaConstants.IIOP;
        }
    }

    /**
     * Вход пользователя в Teamcenter и получение информации о текущем пользователе.
     * @throws InvalidCredentialsException если происходит ошибка во время выполнения входа.
     */
    private void login() throws InvalidCredentialsException {
        sessionService = SessionService.getService(connection);
        Session.LoginResponse resp = sessionService.login(
                connectionProperties.getLogin(),
                connectionProperties.getPassword(),
                connectionProperties.getGroup(), null,
                connectionProperties.getLocale().name(),
                Instant.now().toString());
        if(resp != null)
            currentUser = resp.user;
    }

    /**
     * Инициализирует различные сервисы после успешной авторизации
     */
    private void initializeServices() {
        sessionService.refreshPOMCachePerRequest(true);
        dataManagementService = DataManagementService.getService(connection);
        structService = com.teamcenter.services.strong.cad.StructureManagementService.getService(connection);
        bomService = StructureManagementService.getService(connection);
        fileManagementUtility = new FileManagementUtility(connection);
    }

    /**
     * Завершение сеанса для данного пользователя.
     * @throws ServiceException Если происходит ошибка во время разрыва соединения.
     */
    @PreDestroy
    public void close() throws ServiceException {
        logger.info("Logout");
        sessionService.logout();
    }

    @Override
    public String toString() {
        return "TCSession {" +
                "connectionProperties = " + connectionProperties +
                ", connection = " + connection +
                ", sessionService = " + sessionService +
                ", dataManagementService = " + dataManagementService +
                ", bomService = " + bomService +
                ", structService = " + structService +
                ", currentUser = " + currentUser +
                '}';
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(connectionProperties, connection, sessionService, bomService, structService, currentUser);
        result = 31 * result + Objects.hash(dataManagementService);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TCSession tcsession = (TCSession) o;
        return Objects.equals(connectionProperties, tcsession.connectionProperties) &&
                Objects.equals(connection, tcsession.connection) &&
                Objects.equals(sessionService, tcsession.sessionService) &&
                Objects.equals(dataManagementService, tcsession.dataManagementService) &&
                Objects.equals(bomService, tcsession.bomService) &&
                Objects.equals(structService, tcsession.structService) &&
                Objects.equals(currentUser, tcsession.currentUser);
    }
}

