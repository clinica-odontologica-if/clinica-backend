package com.clinica.discovery_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.EurekaServerAutoConfiguration;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"eureka.client.enabled=false",
				"spring.cloud.service-registry.auto-registration.enabled=false"
		}
)
@EnableAutoConfiguration(exclude = { EurekaServerAutoConfiguration.class })
class DiscoveryServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
