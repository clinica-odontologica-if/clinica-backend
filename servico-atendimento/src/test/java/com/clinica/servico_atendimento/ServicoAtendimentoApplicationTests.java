package com.clinica.servico_atendimento;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.main.web-application-type=none",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
class ServicoAtendimentoApplicationTests {

    @Test
    void contextLoads() {
    }
}
