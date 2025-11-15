package com.maintenance.maintenance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"firebase.enabled=false",
		"firebase.api.key=dummy-test-key",
		"firebase.project.id=maintenance-3c65e",
		"firebase.realtime.database.url=https://maintenance-3c65e-default-rtdb.europe-west1.firebasedatabase.app/"
})
class MaintenanceApplicationTests {

	@Test
	void contextLoads() {
	}

}
