package org.fenixedu.learning;

import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;

public class FenixEduLearningConfiguration {

    @ConfigurationManager(description = "FenixEdu Learning Configuration")
    public static interface ConfigurationProperties {

        @ConfigurationProperty(key = "fenixedu.learning.domain.listeners.enabled", defaultValue = "false")
        public Boolean getDomainListenersEnabled();

    }

    public static ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(ConfigurationProperties.class);
    }

}
